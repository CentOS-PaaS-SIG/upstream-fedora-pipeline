#!groovy

/*
Find what data is grouped with certain containers
*/


// CANNED CI_MESSAGE
def CANNED_CI_MESSAGE = '{"commit":{"username":"eseyman","stats":{"files":{"perl-Net-FTPSSL.spec":{"deletions":2,"additions":5,"lines":7},".gitignore":{"deletions":0,"additions":1,"lines":1},"sources":{"deletions":1,"additions":1,"lines":2}},"total":{"deletions":3,"files":3,"additions":7,"lines":10}},"name":"Emmanuel Seyman","rev":"c1c7de158fa72de5bd279daaaac9f75d0b3e65cd","namespace":"rpms","agent":"eseyman","summary":"Update to 0.40","repo":"perl-Net-FTPSSL","branch":"master","seen":false,"path":"/srv/git/repositories/rpms/perl-Net-FTPSSL.git","message":"Update to 0.40\n","email":"emmanuel@seyman.fr"},"topic":"org.fedoraproject.prod.git.receive"}'

// Initialize all the ghprb variables we need
env.ghprbGhRepository = env.ghprbGhRepository ?: 'CentOS-PaaS-SIG/upstream-fedora-pipeline'
env.ghprbActualCommit = env.ghprbActualCommit ?: 'master'
env.ghprbPullAuthorLogin = env.ghprbPullAuthorLogin ?: ''
env.ghprbPullId = env.ghprbPullId ?: ''

// Task ID to bypass rpm build and grab artifacts from koji
env.PROVIDED_KOJI_TASKID = env.PROVIDED_KOJI_TASKID ?: ''

// Needed for podTemplate()
env.SLAVE_TAG = env.SLAVE_TAG ?: 'stable'
env.RPMBUILD_TAG = env.RPMBUILD_TAG ?: 'stable'
env.INQUIRER_TAG = env.INQUIRER_TAG ?: 'stable'
env.CLOUD_IMAGE_COMPOSE_TAG = env.CLOUD_IMAGE_COMPOSE_TAG ?: 'stable'
env.SINGLEHOST_TEST_TAG = env.SINGLEHOST_TEST_TAG ?: 'stable'
env.OSTREE_BOOT_IMAGE_TAG = env.OSTREE_BOOT_IMAGE_TAG ?: 'stable'

// Audit file for all messages sent.
msgAuditFile = "messages/message-audit.json"

// Number of times to keep retrying to make sure message is ingested
// by datagrepper
fedmsgRetryCount = 120

// Execution ID for this run of the pipeline
def executionID = UUID.randomUUID().toString()

// Pod name to use
def podName = 'fedora-cloud-' + executionID + '-allpkgs'

def libraries = ['cico-pipeline'           : ['master', 'https://github.com/CentOS/cico-pipeline-library.git'],
                 'ci-pipeline'             : ['master', 'https://github.com/CentOS-PaaS-SIG/ci-pipeline.git']]

libraries.each { name, repo ->
    library identifier: "${name}@${repo[0]}",
            retriever: modernSCM([$class: 'GitSCMSource',
                                  remote: repo[1]])

}

// Check out PR's version of library
library identifier: "fedora-upstream-pipeline@${env.ghprbActualCommit}",
        retriever: modernSCM([$class: 'GitSCMSource',
                              remote: "https://github.com/${env.ghprbGhRepository}",
                              traits: [[$class: 'jenkins.plugins.git.traits.BranchDiscoveryTrait'],
                                       [$class: 'RefSpecsSCMSourceTrait',
                                        templates: [[value: '+refs/heads/*:refs/remotes/@{remote}/*'],
                                                    [value: '+refs/pull/*:refs/remotes/origin/pr/*']]]]])

//noinspection GroovyAssignabilityCheck
properties(
        [
                buildDiscarder(logRotator(artifactDaysToKeepStr: '30', artifactNumToKeepStr: '100', daysToKeepStr: '90', numToKeepStr: '100')),
                [$class: 'JobPropertyImpl', throttle: [count: 15, durationName: 'hour', userBoost: false]],
                parameters(
                        [
                                string(defaultValue: '', description: 'Give an integer only task id to use those artifacts and bypass the rpm build stage (example 123456)', name: 'PROVIDED_KOJI_TASKID'),
                                string(defaultValue: 'master', description: '', name: 'ghprbActualCommit'),
                                string(defaultValue: '', description: '', name: 'ghprbGhRepository'),
                                string(defaultValue: '', description: '', name: 'sha1'),
                                string(defaultValue: '', description: 'Pull Request Number', name: 'ghprbPullId'),
                                string(defaultValue: '', description: 'Pull Request Author username', name: 'ghprbPullAuthorLogin'),
                                string(defaultValue: 'stable', description: 'Tag for slave image', name: 'SLAVE_TAG'),
                                string(defaultValue: 'stable', description: 'Tag for rpmbuild image', name: 'RPMBUILD_TAG'),
                                string(defaultValue: 'stable', description: 'Tag for inquirer image', name: 'INQUIRER_TAG'),
                                string(defaultValue: 'stable', description: 'Tag for cloud-image-compose image', name: 'CLOUD_IMAGE_COMPOSE_TAG'),
                                string(defaultValue: 'stable', description: 'Tag for ostree boot image', name: 'OSTREE_BOOT_IMAGE_TAG'),
                                string(defaultValue: 'stable', description: 'Tag for singlehost test image', name: 'SINGLEHOST_TEST_TAG'),
                                string(defaultValue: '172.30.254.79:5000', description: 'Docker repo url for Openshift instance', name: 'DOCKER_REPO_URL'),
                                string(defaultValue: 'continuous-infra', description: 'Project namespace for Openshift operations', name: 'OPENSHIFT_NAMESPACE'),
                                string(defaultValue: 'jenkins', description: 'Service Account for Openshift operations', name: 'OPENSHIFT_SERVICE_ACCOUNT'),
                                string(defaultValue: CANNED_CI_MESSAGE, description: 'CI_MESSAGE', name: 'CI_MESSAGE')
                        ]
                ),
        ]
)

podTemplate(name: podName,
            label: podName,
            cloud: 'openshift',
            serviceAccount: OPENSHIFT_SERVICE_ACCOUNT,
            idleMinutes: 0,
            namespace: OPENSHIFT_NAMESPACE,

        containers: [
                // This adds the custom slave container to the pod. Must be first with name 'jnlp'
                containerTemplate(name: 'jnlp',
                        image: DOCKER_REPO_URL + '/' + OPENSHIFT_NAMESPACE + '/jenkins-continuous-infra-slave:' + SLAVE_TAG,
                        ttyEnabled: false,
                        args: '${computer.jnlpmac} ${computer.name}',
                        command: '',
                        workingDir: '/workDir'),
                // This adds the rpmbuild container to the pod.
                containerTemplate(name: 'rpmbuild',
                        alwaysPullImage: true,
                        image: DOCKER_REPO_URL + '/' + OPENSHIFT_NAMESPACE + '/rpmbuild:' + RPMBUILD_TAG,
                        ttyEnabled: true,
                        command: 'cat',
                        privileged: true,
                        workingDir: '/workDir'),
                // This adds the inquirer container to the pod.
                containerTemplate(name: 'inquirer',
                        alwaysPullImage: true,
                        image: DOCKER_REPO_URL + '/' + OPENSHIFT_NAMESPACE + '/inquirer:' + RPMBUILD_TAG,
                        ttyEnabled: true,
                        command: 'cat',
                        privileged: true,
                        workingDir: '/workDir'),
                // This adds the cloud-image-compose test container to the pod.
                containerTemplate(name: 'cloud-image-compose',
                        alwaysPullImage: true,
                        image: DOCKER_REPO_URL + '/' + OPENSHIFT_NAMESPACE + '/cloud-image-compose:' + CLOUD_IMAGE_COMPOSE_TAG,
                        ttyEnabled: true,
                        command: 'cat',
                        privileged: true,
                        workingDir: '/workDir'),
                // This adds the singlehost test container to the pod.
                containerTemplate(name: 'singlehost-test',
                        alwaysPullImage: true,
                        image: DOCKER_REPO_URL + '/' + OPENSHIFT_NAMESPACE + '/singlehost-test:' + SINGLEHOST_TEST_TAG,
                        ttyEnabled: true,
                        command: 'cat',
                        privileged: true,
                        workingDir: '/workDir'),
                // This adds the ostree boot image container to the pod.
                containerTemplate(name: 'ostree-boot-image',
                        alwaysPullImage: true,
                        image: DOCKER_REPO_URL + '/' + OPENSHIFT_NAMESPACE + '/ostree-boot-image:' + OSTREE_BOOT_IMAGE_TAG,
                        ttyEnabled: true,
                        command: '/usr/sbin/init',
                        privileged: true,
                        workingDir: '/workDir')
        ],
        volumes: [emptyDirVolume(memory: false, mountPath: '/sys/class/net')])
{
    node(podName) {

        // pull in ciMetrics from ci-pipeline
        ciMetrics.prefix = 'Fedora_All_Packages_Pipeline'
        packagepipelineUtils.cimetrics = ciMetrics

        // Would do ~1.5 hours but kernel builds take a long time
        timeout(time: 5, unit: 'HOURS') {

            def currentStage = ""

            packagepipelineUtils.ciPipeline {
                    // We need to set env.HOME because the openshift slave image
                    // forces this to /home/jenkins and then ~ expands to that
                    // even though id == "root"
                    // See https://github.com/openshift/jenkins/blob/master/slave-base/Dockerfile#L5
                    //
                    // Even the kubernetes plugin will create a pod with containers
                    // whose $HOME env var will be its workingDir
                    // See https://github.com/jenkinsci/kubernetes-plugin/blob/master/src/main/java/org/csanchez/jenkins/plugins/kubernetes/KubernetesLauncher.java#L311
                    //
                    env.HOME = "/root"
                    //
                try {
                        // Prepare our environment
                    currentStage = "prepare-environment"
                    stage(currentStage) {

                        packagepipelineUtils.timedPipelineStep('stepName': currentStage, 'debug': true) {

                            deleteDir()

                            if (!env.PROVIDED_KOJI_TASKID?.trim()) {
                                // Parse the CI_MESSAGE and inject it as env vars
                                pipelineUtils.injectFedmsgVars(env.CI_MESSAGE)
                            }
                            packagepipelineUtils.setDefaultEnvVars()

                            // Prepare Credentials (keys, passwords, etc)
                            packagepipelineUtils.prepareCredentials('fedora-keytab')

                            // Decorate our build
                            pipelineUtils.updateBuildDisplayAndDescription()

                            // Gather some info about the node we are running on for diagnostics
                            pipelineUtils.verifyPod(OPENSHIFT_NAMESPACE, env.NODE_NAME)

                            // create audit message file
                            pipelineUtils.initializeAuditFile(msgAuditFile)
                        }

                    }

                    // Set our current stage value
                    currentStage = "koji-build"
                    stage(currentStage) {

                        // Set stage specific vars
                        packagepipelineUtils.timedPipelineStep('stepName': currentStage, 'debug': true) {
                            packagepipelineUtils.setStageEnvVars(currentStage)

                            // Return a map (messageFields) of our message topic, properties, and content
                            messageFields = packagepipelineUtils.setMessageFields("package.running")

                            // Send message org.centos.prod.ci.pipeline.allpackages.package.running on fedmsg
                            pipelineUtils.sendMessageWithAudit(messageFields['topic'], messageFields['properties'], messageFields['content'], msgAuditFile, fedmsgRetryCount)

                            // If a task id was provided, use those artifacts and
                            // bypass submitting a new rpm build
                            if (env.PROVIDED_KOJI_TASKID?.trim()) {
                                // Run script that simply downloads artifacts
                                // and stores them in jenkins workspace
                                pipelineUtils.executeInContainer(currentStage, "rpmbuild", "/tmp/pull_old_task.sh")

                            } else {
                                // Get DistBranch value to find rpm NVR
                                packagepipelineUtils.setDistBranch()

                                // Build rpms
                                pipelineUtils.executeInContainer(currentStage, "rpmbuild", "/tmp/rpmbuild-local.sh")
                            }
                          
                             // Inject variables
                             def job_props = "${env.WORKSPACE}/" + currentStage + "/logs/job.props"
                             def job_props_groovy = "${env.WORKSPACE}/job.props.groovy"
                             pipelineUtils.convertProps(job_props, job_props_groovy)
                             load(job_props_groovy)

                            // Make sure we generated a good repo
                            pipelineUtils.executeInContainer(currentStage, "rpmbuild", "/tmp/repoquery.sh")
                        }

                        if (env.PROVIDED_KOJI_TASKID?.trim()) {
                            // Decorate our build to not be null now
                            pipelineUtils.updateBuildDisplayAndDescription()
                        }

                        // Set our message topic, properties, and content
                        messageFields = packagepipelineUtils.setMessageFields("package.complete")

                        // Send message org.centos.prod.ci.pipeline.allpackages.package.complete on fedmsg
                        pipelineUtils.sendMessageWithAudit(messageFields['topic'], messageFields['properties'], messageFields['content'], msgAuditFile, fedmsgRetryCount)

                        // Set our message topic, properties, and content
                        messageFields = packagepipelineUtils.setMessageFields("image.queued")

                        // Send message org.centos.prod.ci.pipeline.allpackages.image.queued on fedmsg
                        pipelineUtils.sendMessageWithAudit(messageFields['topic'], messageFields['properties'], messageFields['content'], msgAuditFile, fedmsgRetryCount)
                    }

                    currentStage = "cloud-image-compose"
                    stage(currentStage) {

                        packagepipelineUtils.timedPipelineStep(stepName: currentStage, debug: true) {

                            // Set our message topic, properties, and content
                            messageFields = packagepipelineUtils.setMessageFields("image.running")

                            // Send message org.centos.prod.ci.pipeline.allpackages.image.running on fedmsg
                            pipelineUtils.sendMessageWithAudit(messageFields['topic'], messageFields['properties'], messageFields['content'], msgAuditFile, fedmsgRetryCount)

                            // Set stage specific vars
                            packagepipelineUtils.setStageEnvVars(currentStage)

                            // No messages defined for this stage

                            // Compose image
                            pipelineUtils.executeInContainer(currentStage, "cloud-image-compose", "/tmp/cloud-image-compose.sh")

                            // Set our message topic, properties, and content
                            messageFields = packagepipelineUtils.setMessageFields("image.complete")

                            // Send message org.centos.prod.ci.pipeline.allpackages.image.complete on fedmsg
                            pipelineUtils.sendMessageWithAudit(messageFields['topic'], messageFields['properties'], messageFields['content'], msgAuditFile, fedmsgRetryCount)

                            // Set our message topic, properties, and content
                            //messageFields = packagepipelineUtils.setMessageFields("image.test.smoke.queued")

                            // Send message org.centos.prod.ci.pipeline.allpackages.image.test.smoke.queued on fedmsg
                            //pipelineUtils.sendMessageWithAudit(messageFields['properties'], messageFields['content'], msgAuditFile, fedmsgRetryCount)
                        }
                    }

                    //currentStage = "image-boot-sanity"
                    //stage(currentStage) {

                        //packagepipelineUtils.timedPipelineStep(stepName: currentStage, debug: true) {
                            // Set stage specific vars
                            //packagepipelineUtils.setStageEnvVars(currentStage)

                            // Set our message topic, properties, and content
                            //messageFields = packagepipelineUtils.setMessageFields("image.test.smoke.running")

                            // Send message org.centos.prod.ci.pipeline.allpackages.image.test.smoke.running on fedmsg
                            //pipelineUtils.sendMessageWithAudit(messageFields['properties'], messageFields['content'], msgAuditFile, fedmsgRetryCount)

                            //env.image2boot = "${env.WORKSPACE}/images/untested-cloud.qcow2"

                            // Find out which rpm should be running
                            //pipelineUtils.executeInContainer(currentStage, "inquirer", "/tmp/find_nvr.sh")
                            // Inject the $expected variable
                            //def package_props = "${env.WORKSPACE}/" + currentStage + "/logs/package.props"
                            //def package_props_groovy = "${env.WORKSPACE}/package.props.groovy"
                            //pipelineUtils.convertProps(package_props, package_props_groovy)
                            //load(package_props_groovy)

                            // Run boot sanity on image
                            //pipelineUtils.executeInContainer(currentStage, "ostree-boot-image", "/home/ostree-boot-image.sh")

                            // Set our message topic, properties, and content
                            //messageFields = packagepipelineUtils.setMessageFields("image.test.smoke.complete")

                            // Send message org.centos.prod.ci.pipeline.allpackages.image.test.smoke.complete on fedmsg
                            //pipelineUtils.sendMessageWithAudit(messageFields['properties'], messageFields['content'], msgAuditFile, fedmsgRetryCount)
                        //}
                    //}

                    // Only run this stage if tests exist
                    if (pipelineUtils.checkTests(env.fed_repo, env.fed_branch, 'classic')) {

                        // Set our message topic, properties, and content
                        messageFields = packagepipelineUtils.setMessageFields("package.test.functional.queued")

                        // Send message org.centos.prod.ci.pipeline.allpackages.package.test.functional.queued on fedmsg
                        pipelineUtils.sendMessageWithAudit(messageFields['topic'], messageFields['properties'], messageFields['content'], msgAuditFile, fedmsgRetryCount)
                    }

                    currentStage = "package-tests"
                    stage(currentStage) {
                        // Only run this stage if tests exist
                        if (!pipelineUtils.checkTests(env.fed_repo, env.fed_branch, 'classic')) {
                            pipelineUtils.skip(currentStage)
                        } else {
                            packagepipelineUtils.timedPipelineStep(stepName: currentStage, debug: true) {
                                // Set stage specific vars
                                packagepipelineUtils.setStageEnvVars(currentStage)

                                // Set our message topic, properties, and content
                                messageFields = packagepipelineUtils.setMessageFields("package.test.functional.running")

                                // Send message org.centos.prod.ci.pipeline.allpackages.package.test.functional.running on fedmsg
                                pipelineUtils.sendMessageWithAudit(messageFields['topic'], messageFields['properties'], messageFields['content'], msgAuditFile, fedmsgRetryCount)

                                // This can't be in setStageEnvVars because it depends on env.WORKSPACE
                                env.TEST_SUBJECTS = "${env.WORKSPACE}/images/untested-cloud.qcow2"

                                // Run functional tests
                                pipelineUtils.executeInContainer(currentStage, "singlehost-test", "/tmp/package-test.sh")

                                // Set our message topic, properties, and content
                                messageFields = packagepipelineUtils.setMessageFields("package.test.functional.complete")

                                // Send message org.centos.prod.ci.pipeline.allpackages.package.test.functional.complete on fedmsg
                                pipelineUtils.sendMessageWithAudit(messageFields['topic'], messageFields['properties'], messageFields['content'], msgAuditFile, fedmsgRetryCount)
                            }
                        }
                    }

                    currentBuild.result = 'SUCCESS'

                } catch (e) {
                    // Set build result
                    currentBuild.result = 'FAILURE'

                    // Report the exception
                    echo "Error: Exception from " + currentStage + ":"
                    echo e.getMessage()

                    // Throw the error
                    throw e

                } finally {
                    pipelineUtils.getContainerLogsFromPod(OPENSHIFT_NAMESPACE, env.NODE_NAME)

                    // Archive our artifacts
                    step([$class: 'ArtifactArchiver', allowEmptyArchive: true, artifacts: '**/logs/**,*.txt,*.groovy,**/job.*,**/*.groovy,**/inventory.*', excludes: '**/job.props,**/job.props.groovy,**/*.example,**/*.qcow2', fingerprint: true])

                    // Set our message topic, properties, and content
                    messageFields = packagepipelineUtils.setMessageFields("complete")

                    // Send message org.centos.prod.ci.pipeline.allpackages.complete on fedmsg
                    pipelineUtils.sendMessageWithAudit(messageFields['topic'], messageFields['properties'], messageFields['content'], msgAuditFile, fedmsgRetryCount)

                    packagepipelineUtils.packageMetrics()

                }
            }
        }
    }
}
