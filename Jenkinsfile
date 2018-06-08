#!groovy

timestamps {
    // CANNED CI_MESSAGE
    def CANNED_CI_MESSAGE = '{"commit":{"username":"eseyman","stats":{"files":{"perl-Net-FTPSSL.spec":{"deletions":2,"additions":5,"lines":7},".gitignore":{"deletions":0,"additions":1,"lines":1},"sources":{"deletions":1,"additions":1,"lines":2}},"total":{"deletions":3,"files":3,"additions":7,"lines":10}},"name":"Emmanuel Seyman","rev":"c1c7de158fa72de5bd279daaaac9f75d0b3e65cd","namespace":"rpms","agent":"eseyman","summary":"Update to 0.40","repo":"perl-Net-FTPSSL","branch":"master","seen":false,"path":"/srv/git/repositories/rpms/perl-Net-FTPSSL.git","message":"Update to 0.40\n","email":"emmanuel@seyman.fr"},"topic":"org.fedoraproject.prod.git.receive"}'

    // Initialize all the ghprb variables we need
    env.ghprbGhRepository = env.ghprbGhRepository ?: 'CentOS-PaaS-SIG/upstream-fedora-pipeline'
    env.ghprbActualCommit = env.ghprbActualCommit ?: 'master'
    env.ghprbPullAuthorLogin = env.ghprbPullAuthorLogin ?: ''
    env.ghprbPullId = env.ghprbPullId ?: ''

    // Task ID to bypass rpm build and grab artifacts from koji
    env.PROVIDED_KOJI_TASKID = env.PROVIDED_KOJI_TASKID ?: ''
    // Default to build being scratch, will be overridden if triggered by nonscratch build
    env.isScratch = true

    // Needed for podTemplate()
    env.SLAVE_TAG = env.SLAVE_TAG ?: 'stable'
    env.RPMBUILD_TAG = env.RPMBUILD_TAG ?: 'stable'
    env.CLOUD_IMAGE_COMPOSE_TAG = env.CLOUD_IMAGE_COMPOSE_TAG ?: 'stable'
    env.SINGLEHOST_TEST_TAG = env.SINGLEHOST_TEST_TAG ?: 'stable'

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
    library identifier: "upstream-fedora-pipeline@${env.ghprbActualCommit}",
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
                    [$class: 'JobPropertyImpl', throttle: [count: 150, durationName: 'hour', userBoost: false]],
                    parameters(
                            [
                                    string(name: 'PROVIDED_KOJI_TASKID',
                                           defaultValue: '',
                                           description: 'Give an integer only task id to use those artifacts and bypass the rpm build stage (example 123456)'),
                                    string(name: 'ghprbActualCommit',
                                           defaultValue: 'master',
                                           description: 'The GitHub pull request commit'),
                                    string(name: 'ghprbGhRepository',
                                           defaultValue: '',
                                           description: 'The repo the PR is against'),
                                    string(name: 'sha1',
                                           defaultValue: '',
                                           description: ''),
                                    string(name: 'ghprbPullId',
                                           defaultValue: '',
                                           description: 'Pull Request Number'),
                                    string(name: 'ghprbPullAuthorLogin',
                                           defaultValue: '',
                                           description: 'Pull Request Author username'),
                                    string(name: 'SLAVE_TAG',
                                           defaultValue: 'stable',
                                           description: 'Tag for slave image'),
                                    string(name: 'RPMBUILD_TAG',
                                           defaultValue: 'stable',
                                           description: 'Tag for rpmbuild image'),
                                    string(name: 'CLOUD_IMAGE_COMPOSE_TAG',
                                           defaultValue: 'stable',
                                           description: 'Tag for cloud-image-compose image'),
                                    string(name: 'SINGLEHOST_TEST_TAG',
                                           defaultValue: 'stable',
                                           description: 'Tag for singlehost test image'),
                                    string(name: 'DOCKER_REPO_URL',
                                           defaultValue: '172.30.254.79:5000',
                                           description: 'Docker repo url for Openshift instance'),
                                    string(name: 'OPENSHIFT_NAMESPACE',
                                           defaultValue: 'continuous-infra',
                                           description: 'Project namespace for Openshift operations'),
                                    string(name: 'OPENSHIFT_SERVICE_ACCOUNT',
                                           defaultValue: 'jenkins',
                                           description: 'Service Account for Openshift operations'),
                                    string(name: 'CI_MESSAGE',
                                           defaultValue: CANNED_CI_MESSAGE,
                                           description: 'CI_MESSAGE')
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
                            workingDir: '/workDir')
            ],
            volumes: [emptyDirVolume(memory: false, mountPath: '/sys/class/net')])
    {
        node(podName) {

            // pull in ciMetrics from ci-pipeline
            ciMetrics.prefix = packagepipelineUtils.influxDBPrefix()
            packagepipelineUtils.cimetrics = ciMetrics
            def jobMeasurement = packagepipelineUtils.timedMeasurement()

            def buildResult = null

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
                                    env.artifact = 'pr'
                                    // Parse the CI_MESSAGE and inject it as env vars
                                    pipelineUtils.injectPRVars("fed", env.CI_MESSAGE)

                                    // Decorate our build
                                    String buildName = "PR-${env.fed_id}:${env.fed_repo}:${env.fed_branch}"
                                    // Once we have stage job running lets make build description
                                    // a hyperlink to PR like
                                    // <a href="https://src.fedoraproject.org/rpms/${env.fed_repo}/pull-request/${env.fed_id}"> PR #${env.fed_id} ${env.fed_repo}</a>
                                    pipelineUtils.setCustomBuildNameAndDescription(buildName, buildName)
                                } else {
                                    env.artifact = 'build'
                                    pipelineUtils.flattenJSON('fed', env.CI_MESSAGE)
                                    // Scratch build messages store things in info
                                    pipelineUtils.repoFromRequest(env.fed_request_0 ?: env.fed_info_request_0, "fed")
                                    pipelineUtils.setBuildBranch(env.fed_request_1 ?: env.fed_info_request_1, "fed")
                                    // Use message bus format to determine if scratch build
                                    env.isScratch = env.fed_info_request_0 ? true : false
                                }


                                packagepipelineUtils.setDefaultEnvVars()

                                // Prepare Credentials (keys, passwords, etc)
                                packagepipelineUtils.prepareCredentials('fedora-keytab')

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
                                messageFields = packagepipelineUtils.setMessageFields("package.running", artifact)

                                // Send message org.centos.prod.ci.pipeline.allpackages.package.running on fedmsg
                                pipelineUtils.sendMessageWithAudit(messageFields['topic'], messageFields['properties'], messageFields['content'], msgAuditFile, fedmsgRetryCount)

                                // Prepare to send stage.complete message on failure
                                env.messageStage = 'package.complete'

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
                                    pipelineUtils.executeInContainer(currentStage, "rpmbuild", "/tmp/koji_build_pr.sh")
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
                                // Check if to add scratch tag to build name
                                String scratchTag = env.isScratch ? ":S" : ""
                                // Decorate our build to not be null now
                                String buildName = "${env.koji_task_id}${scratchTag}:${env.nvr}"
                                pipelineUtils.setCustomBuildNameAndDescription(buildName, buildName)
                            }

                            // Set our message topic, properties, and content
                            messageFields = packagepipelineUtils.setMessageFields("package.complete", artifact)

                            // Send message org.centos.prod.ci.pipeline.allpackages.package.complete on fedmsg
                            pipelineUtils.sendMessageWithAudit(messageFields['topic'], messageFields['properties'], messageFields['content'], msgAuditFile, fedmsgRetryCount)

                            // Set our message topic, properties, and content
                            messageFields = packagepipelineUtils.setMessageFields("image.queued", artifact)

                            // Send message org.centos.prod.ci.pipeline.allpackages.image.queued on fedmsg
                            pipelineUtils.sendMessageWithAudit(messageFields['topic'], messageFields['properties'], messageFields['content'], msgAuditFile, fedmsgRetryCount)
                        }

                        currentStage = "cloud-image-compose"
                        stage(currentStage) {

                            packagepipelineUtils.timedPipelineStep(stepName: currentStage, debug: true) {

                                // Set our message topic, properties, and content
                                messageFields = packagepipelineUtils.setMessageFields("image.running", artifact)

                                // Send message org.centos.prod.ci.pipeline.allpackages.image.running on fedmsg
                                pipelineUtils.sendMessageWithAudit(messageFields['topic'], messageFields['properties'], messageFields['content'], msgAuditFile, fedmsgRetryCount)

                                // Set stage specific vars
                                packagepipelineUtils.setStageEnvVars(currentStage)

                                // Prepare to send stage.complete message on failure
                                env.messageStage = 'image.complete'

                                // Compose image
                                pipelineUtils.executeInContainer(currentStage, "cloud-image-compose", "/tmp/virt-customize.sh")

                                // Set our message topic, properties, and content
                                messageFields = packagepipelineUtils.setMessageFields("image.complete", artifact)

                                // Send message org.centos.prod.ci.pipeline.allpackages.image.complete on fedmsg
                                pipelineUtils.sendMessageWithAudit(messageFields['topic'], messageFields['properties'], messageFields['content'], msgAuditFile, fedmsgRetryCount)

                            }
                        }

                        currentStage = "nvr-verify"
                        stage(currentStage) {

                            packagepipelineUtils.timedPipelineStep(stepName: currentStage, debug: true) {
                                // Set stage specific vars
                                packagepipelineUtils.setStageEnvVars(currentStage)

                                // This can't be in setStageEnvVars because it depends on env.WORKSPACE
                                env.TEST_SUBJECTS = "${env.WORKSPACE}/images/test_subject.qcow2"

                                // Run nvr verification
                                pipelineUtils.executeInContainer(currentStage, "singlehost-test", "/tmp/verify-rpm.sh")
                            }
                        }

                        currentStage = "package-tests"
                        stage(currentStage) {
                            // Only run this stage if tests exist
                            if (!pipelineUtils.checkTests(env.fed_repo, (env.artifact == 'pr' ? env.fed_id : env.fed_branch), 'classic')) {
                                pipelineUtils.skip(currentStage)
                            } else {
                                packagepipelineUtils.timedPipelineStep(stepName: currentStage, debug: true) {
                                    // Set our message topic, properties, and content
                                    messageFields = packagepipelineUtils.setMessageFields("package.test.functional.queued", artifact)

                                    // Send message org.centos.prod.ci.pipeline.allpackages.package.test.functional.queued on fedmsg
                                    pipelineUtils.sendMessageWithAudit(messageFields['topic'], messageFields['properties'], messageFields['content'], msgAuditFile, fedmsgRetryCount)

                                    // Set stage specific vars
                                    packagepipelineUtils.setStageEnvVars(currentStage)

                                    // Set our message topic, properties, and content
                                    messageFields = packagepipelineUtils.setMessageFields("package.test.functional.running", artifact)

                                    // Send message org.centos.prod.ci.pipeline.allpackages.package.test.functional.running on fedmsg
                                    pipelineUtils.sendMessageWithAudit(messageFields['topic'], messageFields['properties'], messageFields['content'], msgAuditFile, fedmsgRetryCount)

                                    // This can't be in setStageEnvVars because it depends on env.WORKSPACE
                                    env.TEST_SUBJECTS = "${env.WORKSPACE}/images/test_subject.qcow2"

                                    // Prepare to send stage.complete message on failure
                                    env.messageStage = 'package.test.functional.complete'

                                    // Run functional tests
                                    try {
                                        pipelineUtils.executeInContainer(currentStage, "singlehost-test", "/tmp/package-test.sh")
                                    } catch(e) {
                                        if (pipelineUtils.fileExists("${WORKSPACE}/${currentStage}/logs/test.log")) {
                                            buildResult = 'UNSTABLE'
                                            // set currentBuild.result to update the message status
                                            currentBuild.result = buildResult

                                        } else {
                                            throw e
                                        }
                                    }

                                    // Set our message topic, properties, and content
                                    messageFields = packagepipelineUtils.setMessageFields("package.test.functional.complete", artifact)

                                    // Send message org.centos.prod.ci.pipeline.allpackages.package.test.functional.complete on fedmsg
                                    pipelineUtils.sendMessageWithAudit(messageFields['topic'], messageFields['properties'], messageFields['content'], msgAuditFile, fedmsgRetryCount)

                                }
                            }
                        }

                        buildResult = buildResult ?: 'SUCCESS'

                    } catch (e) {
                        // Set build result
                        buildResult = 'FAILURE'
                        currentBuild.result = buildResult

                        // Send message org.centos.prod.ci.pipeline.allpackages.<stage>.complete on fedmsg if stage failed
                        messageFields = packagepipelineUtils.setMessageFields(messageStage, artifact)
                        pipelineUtils.sendMessageWithAudit(messageFields['topic'], messageFields['properties'], messageFields['content'], msgAuditFile, fedmsgRetryCount)

                        // Report the exception
                        echo "Error: Exception from " + currentStage + ":"
                        echo e.getMessage()

                        // Throw the error
                        throw e

                    } finally {
                        currentBuild.result = buildResult
                        pipelineUtils.getContainerLogsFromPod(OPENSHIFT_NAMESPACE, env.NODE_NAME)

                        // Archive our artifacts
                        if (currentBuild.result == 'SUCCESS') {
                            step([$class: 'ArtifactArchiver', allowEmptyArchive: true, artifacts: '**/logs/**,*.txt,*.groovy,**/job.*,**/*.groovy,**/inventory.*', excludes: '**/job.props,**/job.props.groovy,**/*.example,**/*.qcow2', fingerprint: true])
                        } else {
                            step([$class: 'ArtifactArchiver', allowEmptyArchive: true, artifacts: '**/logs/**,*.txt,*.groovy,**/job.*,**/*.groovy,**/inventory.*,**/*.qcow2', excludes: '**/job.props,**/job.props.groovy,**/*.example', fingerprint: true])
                        }

                        // Set our message topic, properties, and content
                        messageFields = packagepipelineUtils.setMessageFields("complete", artifact)

                        // Send message org.centos.prod.ci.pipeline.allpackages.complete on fedmsg
                        pipelineUtils.sendMessageWithAudit(messageFields['topic'], messageFields['properties'], messageFields['content'], msgAuditFile, fedmsgRetryCount)

                        // set the metrics we want
                        def packageMeasurement = "${ciMetrics.prefix}_${env.fed_repo}"
                        ciMetrics.setMetricTag(jobMeasurement, 'package_name', env.fed_repo)
                        ciMetrics.setMetricTag(jobMeasurement, 'build_result', currentBuild.result)
                        ciMetrics.setMetricField(jobMeasurement, 'build_time', currentBuild.getDuration())
                        ciMetrics.setMetricField(packageMeasurement, 'build_time', currentBuild.getDuration())
                        ciMetrics.setMetricTag(packageMeasurement, 'package_name', env.fed_repo)

                    }
                }
            }
        }
    }
}
