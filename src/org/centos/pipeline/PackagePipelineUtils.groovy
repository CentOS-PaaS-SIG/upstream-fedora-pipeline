#!/usr/bin/groovy
package org.centos.pipeline

import org.centos.*
import groovy.json.JsonOutput

/**
 * Library to check the dist branch to as rawhide should map to a release number
 * This value will begin with 'fc'
 * @param String branch - the branch value from the CI_MESSAGE
 * @return
 */
def setDistBranch(String branch) {
    echo "Currently in setDistBranch for ${branch}"

    if (branch != 'rawhide') {
        if (branch[0] == 'f') {
            return 'fc' + branch.substring(1)
        } else {
            throw new Exception("Invalid Branch Name ${branch}")
        }
    } else {
        def dist_branch = sh (returnStdout: true, script: '''
            echo $(curl -s https://src.fedoraproject.org/rpms/fedora-release/raw/master/f/fedora-release.spec | awk '/%define dist_version/ {print $3}')
        ''').trim()
        try {
            assert dist_branch.isNumber()
        }
        catch (AssertionError e) {
            echo "There was a fatal error finding the proper mapping for ${branch}"
            echo "We will not continue without a proper DIST_BRANCH value. Throwing exception..."
            throw new Exception('Rsync branch identifier failed!')
        }
        return 'fc' + dist_branch
    }
}

/**
 * Library to set message fields to be published
 * @param messageType: ${MAIN_TOPIC}.ci.pipeline.allpackages.<defined-in-README>
 * @param artifact ${MAIN_TOPIC}.ci.pipeline.allpackages-${artifact}.<defined-in-README>
 * @param parsedMsg: The parsed fedmsg
 * @return
 */
def setMessageFields(String messageType, String artifact, Map parsedMsg) {
    topic = "${MAIN_TOPIC}.ci.pipeline.allpackages-${artifact}.${messageType}"
    print("Topic is " + topic)

    // Create a HashMap of default message content keys and values
    // These properties should be applicable to ALL message types.
    // If something is applicable to only some subset of messages,
    // add it below per the existing examples.

    if (parsedMsg.has('pullrequest')) {
        myBranch = parsedMsg['pullrequest']['branch']
        myRepo = parsedMsg['pullrequest']['project']['name']
        myRev = 'PR-' + parsedMsg['pullrequest']['id']
        myNamespace = parsedMsg['pullrequest']['project']['namespace']
        myCommentId = parsedMsg['pullrequest']['comments'].isEmpty() ? 0 : parsedMsg['pullrequest']['comments'].last()['id']
        myOwner = parsedMsg['pullrequest']['user']['name'].toString().split('\n')[0].replaceAll('"', '\'')
    } else {
        myBranch = env.fed_branch
        myRepo = env.fed_repo
        taskid = parsedMsg.has('task_id') ? parsedMsg['task_id'] : parsedMsg['info']['id']
        myRev = 'kojitask-' + taskid
        myNamespace = env.fed_namespace
        myCommentId = ''
        myOwner = parsedMsg['owner']
    }

    def messageContent = [
            branch           : myBranch,
            build_id         : env.BUILD_ID,
            build_url        : env.JENKINS_URL + 'blue/organizations/jenkins/' + env.JOB_NAME + '/detail/' + env.JOB_NAME + '/' + env.BUILD_NUMBER + '/pipeline/',
            namespace        : myNamespace,
            nvr              : env.nvr,
            original_spec_nvr: env.original_spec_nvr,
            ci_topic         : topic,
            ref              : env.basearch,
            scratch          : env.isScratch ? env.isScratch.toBoolean() : "",
            repo             : myRepo,
            rev              : myRev,
            status           : currentBuild.currentResult,
            test_guidance    : "''",
            comment_id       : myCommentId,
            username         : myOwner,
    ]

    if (artifact == 'pr') {
        messageContent.commit_hash = parsedMsg['pullrequest'].has('commit_stop') ? parsedMsg['pullrequest']['commit_stop'] : 'N/A'
    }

    // Add image type to appropriate message types
    if (messageType in ['image.queued', 'image.running', 'image.complete', 'image.test.smoke.queued', 'image.test.smoke.running', 'image.test.smoke.complete'
    ]) {
        messageContent.type = messageType == 'image.running' ? "''" : 'qcow2'
    }

    // Create a string to hold the data from the messageContent hash map
    String messageContentString = JsonOutput.toJson(messageContent)

    def messagePropertiesString = ''

    return [ 'topic': topic, 'properties': messagePropertiesString, 'content': messageContentString ]
}

/**
 * Method that uses contra-lib shared library
 * to create the ci.artifact.test.messageType messages
 * @param messageType: queued, running, complete, error
 * @param artifact: dist-git-pr, koji-build
 * @param parsedMsg: The parsed fedmsg
 * @return
 */
// Plan is to rename to setMessageFields and remove function above once everything seems fine and stable
def setTestMessageFields(String messageType, String artifact, Map parsedMsg) {
    // See https://pagure.io/fedora-ci/messages or
    // https://github.com/openshift/contra-lib/tree/master/resources
    myTopic = "${MAIN_TOPIC}.ci.${artifact}.test.${messageType}"
    print("Topic is " + myTopic)
    myNamespace = "fedora-ci." + artifact
    myResult = currentBuild.currentResult
    // convert some build Result to valid spec result
    switch (myResult) {
        case 'SUCCESS':
            myResult = 'PASSED'
            break
        case 'UNSTABLE':
            myResult = 'NEEDS_INSPECTION'
            break
        case 'FAILURE':
            myResult = 'FAILED'
            break
    }
    myResult = myResult.toLowerCase()

    // Create common message body content
    myContactContent = msgBusContactContent(name: "fedora-ci", team: "fedora-ci", irc: "#fedora-ci", email: "ci@lists.fedoraproject.org", docs: 'https://pagure.io/standard-test-roles')
    myStageContent = msgBusStageContent(name: env.currentStage)
    myPipelineContent = msgBusPipelineContent(id: env.pipelineId, stage: myStageContent())
    // The run array is filled in properly with its defaults

    if (artifact == "koji-build") {
        // Set variables that go in multiple closures
        myId = parsedMsg.has('task_id') ? parsedMsg['task_id'] : parsedMsg['info']['id']
        myScratch = env.isScratch.toBoolean()
        myNvr = env.nvr ?: 'N/A'
        myComponent = env.fed_repo
        myRepository = myComponent ? "https://src.fedoraproject.org/rpms/" + myComponent : 'N/A'
        myType = 'tier0'
        myIssuer =  parsedMsg['owner']
        myBranch = env.fed_branch

        myArtifactContent = msgBusArtifactContent(type: 'rpm-build', id: myId, component: myComponent, issuer: myIssuer, nvr: myNvr, scratch: myScratch, source: env.RPM_REQUEST_SOURCE ?: "UNKNOWN")
        myTestContent = (messageType == "complete") ? msgBusTestContent(category: "functional", namespace: myNamespace, type: "tier0", result: myResult) : msgBusTestContent(category: "functional", namespace: myNamespace, type: "tier0")
    }
    if (artifact == "dist-git-pr") {
        // Set variables that go in multiple closures
        myId = parsedMsg['pullrequest']['id']
        myUid = parsedMsg['pullrequest']['uid']
        myCommitHash = parsedMsg['pullrequest'].has('commit_stop') ? parsedMsg['pullrequest']['commit_stop'] : 'N/A'
        myCommentId = parsedMsg['pullrequest']['comments'].isEmpty() ? 0 : parsedMsg['pullrequest']['comments'].last()['id']
        myType = 'build'
        myIssuer =  parsedMsg['pullrequest']['user']['name'].toString().split('\n')[0].replaceAll('"', '\'')
        myBranch = parsedMsg['pullrequest']['branch']
        myRepository = "https://src.fedoraproject.org/" + parsedMsg['pullrequest']['project']['fullname']

        myArtifactContent = msgBusArtifactContent(type: 'pull-request', id: myId, issuer: myIssuer, repository: myRepository, commit_hash: myCommitHash, comment_id: myCommentId, uid: myUid)
        myTestContent = (messageType == "complete") ? msgBusTestContent(category: "static-analysis", namespace: myNamespace, type: "build", result: myResult) : msgBusTestContent(category: "static-analysis", namespace: myNamespace, type: "build")
    }

    // Create type specific content and construct messages
    switch (messageType) {
        // Queued and running messages have the same spec for now
        case ['queued', 'running']:
            myConstructedMessage = msgBusTestQueued(contact: myContactContent(), artifact: myArtifactContent(), pipeline: myPipelineContent(), test: myTestContent())
            break
        case 'complete':
            if (artifact == "koji-build") {
                myArtifactContent = msgBusArtifactContent(type: 'rpm-build', id: myId, component: myComponent, issuer: myIssuer, nvr: myNvr, scratch: myScratch, source: env.RPM_REQUEST_SOURCE ?: "UNKNOWN", dependencies: env.BUILD_DEPS ? env.BUILD_DEPS.split() : [])
            }
            if (artifact == "dist-git-pr") {
                myArtifactContent = msgBusArtifactContent(type: 'pull-request', id: myId, issuer: myIssuer, repository: myRepository, commit_hash: myCommitHash, comment_id: myCommentId, uid: myUid)
            }
            mySystemContent = msgBusSystemContent(label: "upstream-fedora-pipeline", os: myBranch, provider: "CentOS CI", architecture: "x86_64", variant: "Cloud")
            myConstructedMessage = msgBusTestComplete(contact: myContactContent(), artifact: myArtifactContent(), pipeline: myPipelineContent(), test: myTestContent(), system: [mySystemContent()])
            break
        case 'error':
            if (artifact == "koji-build") {
                myArtifactContent = msgBusArtifactContent(type: 'rpm-build', id: myId, component: myComponent, issuer: myIssuer, nvr: myNvr, scratch: myScratch, source: env.RPM_REQUEST_SOURCE ?: "UNKNOWN", dependencies: env.BUILD_DEPS ? env.BUILD_DEPS.split() : [])
            }
            if (artifact == "dist-git-pr") {
                myArtifactContent = msgBusArtifactContent(type: 'pull-request', id: myId, issuer: myIssuer, repository: myRepository, commit_hash: myCommitHash, comment_id: myCommentId, uid: myUid)
            }
            // Unknown execution error is added in an error closure by default
            myConstructedMessage = msgBusTestError(contact: myContactContent(), artifact: myArtifactContent(), pipeline: myPipelineContent(), test: myTestContent())
            break
    }

    return [ 'topic': myTopic, 'properties': '', 'content': myConstructedMessage() ]
}

/**
 * Library to prepare credentials
 * @return
 */
def prepareCredentials(String credentials) {
    withCredentials([file(credentialsId: credentials, variable: 'FEDORA_KEYTAB')]) {
        sh '''
            #!/bin/bash
            set -xeuo pipefail

            cp ${FEDORA_KEYTAB} fedora.keytab
            chmod 0600 fedora.keytab
        '''
    }
}
/**
 * Library to set default environmental variables. Performed once at start of Jenkinsfile
 * @param envMap: Key/value pairs which will be set as environmental variables.
 * @return
 */
def setDefaultEnvVars(Map envMap=null){

    // Check if we're working with a staging or production instance by
    // evaluating if env.ghprbActual is null, and if it's not, whether
    // it is something other than 'master'
    // If we're working with a staging instance:
    //      We default to an MAIN_TOPIC of 'org.centos.stage'
    // If we're working with a production instance:
    //      We default to an MAIN_TOPIC of 'org.centos.prod'
    // Regardless of whether we're working with staging or production,
    // if we're provided a value for MAIN_TOPIC in the build parameters:

    // We also set dataGrepperUrl which is needed for message tracking
    // and the correct jms-messaging message provider

    if (!env.MSG_PROVIDER || env.MSG_PROVIDER == '') {
        if (env.ghprbActualCommit != null && (env.ghprbActualCommit != "master" || env.ghprbPullId != "")) {
            env.MSG_PROVIDER = "fedora-fedmsg-stage"
        } else {
            env.MSG_PROVIDER = "fedora-fedmsg"
        }
    }
    if (env.MSG_PROVIDER == "fedora-fedmsg-stage" ){
        env.MAIN_TOPIC = env.MAIN_TOPIC ?: 'org.centos.stage'
        env.dataGrepperUrl = 'https://apps.stg.fedoraproject.org/datagrepper'
    } else if (env.MSG_PROVIDER == "fedora-fedmsg" ){
        env.MAIN_TOPIC = env.MAIN_TOPIC ?: 'org.centos.prod'
        env.dataGrepperUrl = 'https://apps.fedoraproject.org/datagrepper'
    } else {
        throw new Exception("Unsupported MSG_PROVIDER: ${env.MSG_PROVIDER}")
    }

    env.basearch = env.basearch ?: 'x86_64'
    env.commit = env.commit ?: ''
    env.FEDORA_PRINCIPAL = env.FEDORA_PRINCIPAL ?: 'bpeck/jenkins-continuous-infra.apps.ci.centos.org@FEDORAPROJECT.ORG'
    env.nvr = env.nvr ?: ''
    env.original_spec_nvr = env.original_spec_nvr ?: ''
    env.ANSIBLE_HOST_KEY_CHECKING = env.ANSIBLE_HOST_KEY_CHECKING ?: 'False'
    env.jobMeasurement = env.JOB_NAME
    env.packageMeasurement = env.fed_repo

    // If we've been provided an envMap, we set env.key = value
    // Note: This may overwrite above specified values.
    envMap.each { key, value ->
        env."${key.toSTring().trim()}" = value.toString().trim()
    }
}

/**
 * Library to set stage specific environmental variables.
 * @param stage - Current stage
 * @return map of vars
 */
def setStageEnvVars(String stage){
    def stages =
            ["koji-build"                                     : [
                    fed_branch                : env.fed_branch,
                    fed_repo                  : env.fed_repo,
                    fed_rev                   : env.fed_rev,
                    fed_id                    : (env.fed_pr_id) ?: '',
                    rpm_repo                  : env.WORKSPACE + "/" + env.fed_repo + "_repo",
                    KOJI_SERVER               : env.KOJI_SERVER ?: ''
            ],
             "cloud-image-compose"                            : [
                     rpm_repo                 : env.WORKSPACE + "/" + env.fed_repo + "_repo",
                     package                  : env.fed_repo,
                     namespace                : env.fed_namespace,
                     branch                   : env.branch,
                     fed_branch               : env.fed_branch,
                     DIST_BRANCH              : env.DIST_BRANCH

             ],
             "nvr-verify"                                     : [
                     rpm_repo                 : "/etc/yum.repos.d/" + env.fed_repo,
             ],
             "package-tests"                                   : [
                     package                  : env.fed_repo,
                     namespace                : env.fed_namespace,
                     TAG                      : "classic",
                     branch                   : env.fed_branch,
                     nvr                      : env.nvr,
                     build_pr_id              : (env.fed_pr_id) ?: '',
                     TEST_LOCATION            : "https://src.fedoraproject.org/${env.fed_namespace}/${env.fed_repo}"
             ],
             "container-tests"                                   : [
                     container                : env.fed_repo,
                     TAG                      : "container",
                     branch                   : env.fed_branch,
                     build_pr_id              : (env.fed_pr_id) ?: ''
             ]
            ]

    // Get the map of env var keys and values and write them to the env global variable
    if(stages.containsKey(stage)) {
        stages.get(stage).each { key, value ->
            env."${key}" = value
        }
        // Return map to pass to executeInContainer
        return stages.get(stage)
    }
    return null
}

/**
 * Watch for messages and verify their contents
 * @param msg_provider jms-messaging message provider
 * @param message trigger message
 */
def watchForMessages(String msg_provider, String message) {

    // load the variables from the message and validate it's a dist-git message
    def messageVars = getVariablesFromMessage(message)

    // Common attributes that all messages should have
    def commonAttributes = ["branch", "build_id", "build_url", "namespace",
            "ref", "repo", "rev", "status", "topic",
            "username"]

    // "nvr", "original_spec_nvr" are not added as common since they only get
    // getting resolved AFTER package.complete.
    //
    messageContentValidationMap = [:]
    messageContentValidationMap['org.centos.stage.ci.pipeline.allpackages.package.running'] =
            []
    messageContentValidationMap['org.centos.stage.ci.pipeline.allpackages.package.complete'] =
            ["nvr", "original_spec_nvr"]
    messageContentValidationMap['org.centos.stage.ci.pipeline.allpackages.complete'] =
            []

    messageContentValidationMap.each { k, v ->
        echo "Waiting for topic : ${k}"
        msg = waitForCIMessage providerName: "${msg_provider}",
                selector: "topic = \'${k}\'",
                checks: [[expectedValue: "${messageVars['branch']}", field: '$.branch'],
                         [expectedValue: "${messageVars['rev']}", field: '$.rev'],
                         [expectedValue: "${messageVars['repo']}", field: '$.repo']
                ],
                overrides: [topic: 'org.centos.stage']
        echo msg
        def msg_data = readJSON text: msg
        allFound = true

        def errorMsg = ""
        v.addAll(commonAttributes)
        v.each {
            if (!msg_data.containsKey(it)) {
                String err = "Error: Did not find message property: ${it}"
                errorMsg = "${errorMsg}\n${err}"
                echo "${err}"
                allFound = false
            } else {
                if (!msg_data[it]) {
                    allFound = false
                    String err = "Error: Found message property: ${it} - but it was empty!"
                    echo "${err}"
                    errorMsg = "${errorMsg}\n${err}"
                } else {
                    echo "Found message property: ${it} = ${msg_data[it]}"
                }
            }
        }
        if (!allFound) {
            errorMsg = "Message did not contain all expected message properties:\n\n${errorMsg}"
            error errorMsg
        }
    }

}

/**
 * Function to wrap a pipeline
 * @param body
 * @return
 */
def ciPipeline(Closure body) {
    ansiColor('xterm') {
        deleteDir()

        body()

    }
}

/**
 * Wrap a pipeline step with try/catch and debugging information
 * @param config
 * @param body
 * @return
 */
def handlePipelineStep(Map config, Closure body) {
    try {

        if (config.debug) {
            echo "Starting ${config.stepName}"
        }

        body()

    } catch (Throwable err) {

        echo err.getMessage()
        throw err

    } finally {

        if (config.debug) {
            echo "end of ${config.stepName}"
        }
    }
}

/**
 * Influxdb measurement for a step
 * @return
 */
def timedMeasurement() {
    return env.JOB_NAME
}

/**
 * Function to check if branch is master or fXX, XX > 19
 * @param branch - The branch to check
 * @return bool
 */
def checkBranch(String branch) {
    def result = false

    if (branch ==~ /f[2-9][0-9]/) {
        result = true
    } else if (branch == 'master') {
        result = true
    } else {
        println "Branch ${branch} is not being checked at this time."
    }

    return result
}

/**
 * Function to set env.isScratch, env.request_0, and
 * env.request_1 based on the parsedMsg key structure
 * @param parsedMsg - The parsed fedmsg
 * @return
 */
def setScratchVars(Map parsedMsg) {
    if (parsedMsg.has('info')) {
        env.isScratch = true
        env.request_0 = parsedMsg['info']['request'][0]
        env.request_1 = parsedMsg['info']['request'][1]
    } else {
        env.isScratch = false
        env.request_0 = parsedMsg['request'][0]
        env.request_1 = parsedMsg['request'][1]
    }
}
