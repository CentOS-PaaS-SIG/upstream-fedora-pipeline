#!/usr/bin/groovy
package org.centos.pipeline

import org.centos.*

import groovy.json.JsonSlurper

/**
 * Library to check the dist branch to as rawhide should map to a release number
 * This value will begin with 'fc'
 * @return
 */
def setDistBranch() {
    echo "Currently in setDistBranch for ${env.branch}"

    if (env.branch != 'rawhide') {
        if (env.branch[0] == 'f') {
            env.DIST_BRANCH = 'fc' + env.branch.substring(1)
        } else {
            throw new Exception("Invalid Branch Name ${env.branch}")
        }
    } else {
        def dist_branch = sh (returnStdout: true, script: '''
            echo $(curl -s https://src.fedoraproject.org/rpms/fedora-release/raw/master/f/fedora-release.spec | awk '/%define dist_version/ {print $3}')
        ''').trim()
        try {
            assert dist_branch.isNumber()
        }
        catch (AssertionError e) {
            echo "There was a fatal error finding the proper mapping for ${env.branch}"
            echo "We will not continue without a proper DIST_BRANCH value. Throwing exception..."
            throw new Exception('Rsync branch identifier failed!')
        }
        env.DIST_BRANCH = 'fc' + dist_branch
    }
}

/**
 * Library to set message fields to be published
 * @param messageType: ${MAIN_TOPIC}.ci.pipeline.allpackages.<defined-in-README>
 * @return
 */
def setMessageFields(String messageType) {
    topic = "${MAIN_TOPIC}.ci.pipeline.allpackages.${messageType}"

    // Create a HashMap of default message property keys and values
    // These properties should be applicable to ALL message types.
    // If something is applicable to only some subset of messages,
    // add it below per the existing examples.

    def messageProperties = [
            branch           : env.branch,
            build_id         : env.BUILD_ID,
            build_url        : env.JENKINS_URL + 'blue/organizations/jenkins/' + env.JOB_NAME + '/detail/' + env.JOB_NAME + '/' + env.BUILD_NUMBER + '/pipeline/',
            namespace        : env.fed_namespace,
            nvr              : env.nvr,
            original_spec_nvr: env.original_spec_nvr,
            ref              : env.basearch,
            repo             : env.fed_repo,
            rev              : env.fed_rev,
            status           : currentBuild.currentResult,
            test_guidance    : "''",
            topic            : topic,
            username         : env.fed_username,
    ]

    // Add image type to appropriate message types
    if (messageType in ['image.queued', 'image.running', 'image.complete', 'image.test.smoke.queued', 'image.test.smoke.running', 'image.test.smoke.complete'
    ]) {
        messageProperties.type = messageType == 'image.running' ? "''" : 'qcow2'
    }

    // Create a string to hold the data from the messageProperties hash map
    String messagePropertiesString = ''

    messageProperties.each { k,v ->
        // Don't add a new line to the last item in the hash map when adding it to the messagePropertiesString
        if ( k == messageProperties.keySet().last()){
            messagePropertiesString += "${k}=${v}"
        } else {
            messagePropertiesString += "${k}=${v}\n"
        }
    }

    def messageContentString = ''

    return [ 'topic': topic, 'properties': messagePropertiesString, 'content': messageContentString ]
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

    if (env.ghprbActualCommit != null && env.ghprbActualCommit != "master") {
        env.MAIN_TOPIC = env.MAIN_TOPIC ?: 'org.centos.stage'
        env.dataGrepperUrl = 'https://apps.stg.fedoraproject.org/datagrepper'
        env.MSG_PROVIDER = "fedora-fedmsg-stage"
    } else {
        env.MAIN_TOPIC = env.MAIN_TOPIC ?: 'org.centos.prod'
        env.dataGrepperUrl = 'https://apps.fedoraproject.org/datagrepper'
        env.MSG_PROVIDER = "fedora-fedmsg"
    }

    env.basearch = env.basearch ?: 'x86_64'
    env.commit = env.commit ?: ''
    env.FEDORA_PRINCIPAL = env.FEDORA_PRINCIPAL ?: 'bpeck/jenkins-continuous-infra.apps.ci.centos.org@FEDORAPROJECT.ORG'
    env.nvr = env.nvr ?: ''
    env.original_spec_nvr = env.original_spec_nvr ?: ''
    env.ANSIBLE_HOST_KEY_CHECKING = env.ANSIBLE_HOST_KEY_CHECKING ?: 'False'

    // If we've been provided an envMap, we set env.key = value
    // Note: This may overwrite above specified values.
    envMap.each { key, value ->
        env."${key.toSTring().trim()}" = value.toString().trim()
    }
}

/**
 * Library to set stage specific environmental variables.
 * @param stage - Current stage
 * @return
 */
def setStageEnvVars(String stage){
    def stages =
            ["koji-build"                                     : [
                    fed_branch                : "${env.fed_branch}",
                    fed_repo                  : "${env.fed_repo}",
                    fed_rev                   : "${env.fed_rev}",
                    rpm_repo                  : "${env.WORKSPACE}/${env.fed_repo}_repo",
            ],
             "cloud-image-compose"                            : [
                     rpm_repo                 : "${env.WORKSPACE}/${env.fed_repo}_repo",
                     package                  : "${env.fed_repo}",
                     branch                   : "${env.branch}",
                     fed_branch               : "${env.fed_branch}"

             ],
             "image-boot-sanity"                               : [
                     rpm_repo                 : "${env.WORKSPACE}/${env.fed_repo}_repo",
                     package                  : "${env.fed_repo}",
             ],
             "package-tests"                                   : [
                     package                  : "${env.fed_repo}",
                     python3                  : "yes",
                     TAG                      : "classic",
                     branch                   : "${env.fed_branch}"
             ]
            ]

    // Get the map of env var keys and values and write them to the env global variable
    if(stages.containsKey(stage)) {
        stages.get(stage).each { key, value ->
            env."${key}" = value
        }
    }
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
        def msg_data = new JsonSlurper().parseText(msg)
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

def ciPipeline(Closure body) {
    ansiColor('xterm') {
        timestamps {
            deleteDir()

            body()
        }
    }
}

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

def checkBranch() {
    def result = false

    if (env.fed_branch ==~ /f[2-9][0-9]/) {
        result = true
    } else if (env.fed_branch == 'master') {
        result = true
    }

    return result
}

def packageMetrics() {
    def tags = [:]
    def fields = [:]
    def measurement = 'package'
    tags['build_result'] = currentBuild.result
    tags['project_name'] = env.JOB_NAME
    tags['build_number'] = env.BUILD_NUMBER
    tags['name'] = env.fed_repo
    fields['build_time'] = currentBuild.getDuration().toString()

    return [measurement, tags, fields]
}

def pipelineMetrics() {
    def tags = [:]
    def fields = [:]
    def measurement = 'pipeline'
    tags['build_result'] = currentBuild.result
    fields['build_time'] = currentBuild.getDuration().toString()

    return [measurement, tags, fields]
}
