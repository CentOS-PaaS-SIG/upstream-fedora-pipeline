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
    env.PAGURE_URL = env.PAGURE_URL ?: 'https://src.fedoraproject.org'

    // Needed for podTemplate()
    env.SLAVE_TAG = env.SLAVE_TAG ?: 'stable'
    env.FEDORACI_RUNNER_TAG = env.FEDORACI_RUNNER_TAG ?: 'stable'

    // Execution ID for this run of the pipeline
    def executionID = UUID.randomUUID().toString()
    env.pipelineId = env.pipelineId ?: executionID

    // Pod name to use
    def podName = 'fedora-cloud-' + env.pipelineId + '-allpkgs'

    // Number of CPU cores for the fedoraci-runner container
    runnerCpuLimit = '1'

    def libraries = ['cico-pipeline'           : ['master', 'https://github.com/CentOS/cico-pipeline-library.git'],
                     'contra-lib'              : ['master', 'https://github.com/openshift/contra-lib.git']] // should probably pin this to a release

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
                                    string(name: 'FEDORACI_RUNNER_TAG',
                                           defaultValue: 'stable',
                                           description: 'Tag for fedoraci-runner image'),
                                    string(name: 'DOCKER_REPO_URL',
                                           defaultValue: '172.30.254.79:5000',
                                           description: 'Docker repo url for Openshift instance'),
                                    string(name: 'OPENSHIFT_NAMESPACE',
                                           defaultValue: 'continuous-infra',
                                           description: 'Project namespace for Openshift operations'),
                                    string(name: 'OPENSHIFT_SERVICE_ACCOUNT',
                                           defaultValue: 'jenkins',
                                           description: 'Service Account for Openshift operations'),
                                    string(name: 'MSG_PROVIDER',
                                           defaultValue: '',
                                           description: 'Main provider to send messages on'),
                                    string(name: 'KOJI_URL',
                                           defaultValue: '',
                                           description: 'Overwrites the default koji url'),
                                    string(name: 'KOJI_PARAMS',
                                           defaultValue: '',
                                           description: 'Parameters to pass to koji tool'),
                                    string(name: 'PAGURE_URL',
                                           defaultValue: '',
                                           description: 'Pagure instance url'),
                                    string(name: 'CI_MESSAGE',
                                           defaultValue: CANNED_CI_MESSAGE,
                                           description: 'CI_MESSAGE'),
                                    string(name: 'pipelineId',
                                           defaultValue: '',
                                           description: 'UUID for this pipeline run')
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
                            alwaysPullImage: true,
                            image: DOCKER_REPO_URL + '/' + OPENSHIFT_NAMESPACE + '/jenkins-fedoraci-slave:' + SLAVE_TAG,
                            ttyEnabled: false,
                            args: '${computer.jnlpmac} ${computer.name}',
                            command: '',
                            workingDir: '/workDir'),
                    // This adds the fedoraci-runner container to the pod.
                    containerTemplate(name: 'fedoraci-runner',
                            alwaysPullImage: true,
                            image: DOCKER_REPO_URL + '/' + OPENSHIFT_NAMESPACE + '/fedoraci-runner:' + FEDORACI_RUNNER_TAG,
                            ttyEnabled: true,
                            command: 'cat',
                            envVars: [
                                envVar(key: 'STR_CPU_LIMIT', value: runnerCpuLimit)
                            ],
                            // Request - minimum required, Limit - maximum possible (hard quota)
                            // https://kubernetes.io/docs/concepts/configuration/manage-compute-resources-container/#meaning-of-cpu
                            // https://blog.openshift.com/managing-compute-resources-openshiftkubernetes/
                            resourceRequestCpu: '1',
                            resourceLimitCpu: runnerCpuLimit,
                            resourceRequestMemory: '4Gi',
                            resourceLimitMemory: '6Gi',
                            privileged: true,
                            workingDir: '/workDir')
            ],
            volumes: [emptyDirVolume(memory: false, mountPath: '/sys/class/net')])
    {
        node(podName) {

            // pull in ciMetrics from ci-pipeline
            // ciMetrics.prefix = packagepipelineUtils.influxDBPrefix()
            // packagepipelineUtils.cimetrics = ciMetrics
            def jobMeasurement = packagepipelineUtils.timedMeasurement()

            def buildResult = null

            // Setting timeout to 8 hours, some packages can take few hours to build in koji
            // and tests can take up to 4 hours to run.
            timeout(time: 8, unit: 'HOURS') {

                env.currentStage = ""

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
                        env.currentStage = "prepare-environment"
                        stage(env.currentStage) {

                            packagepipelineUtils.handlePipelineStep('stepName': env.currentStage, 'debug': true) {

                                deleteDir()
                                // Parse the CI_MESSAGE and inject it as a var
                                parsedMsg = kojiMessage(message: env.CI_MESSAGE, ignoreErrors: true)

                                if (!env.PROVIDED_KOJI_TASKID?.trim()) {
                                    env.artifact = 'dist-git-pr'
                                    env.artifactOld = 'pr'

                                    // Set required env variables from msg
                                    env.fed_namespace = parsedMsg['pullrequest']['project']['namespace']
                                    env.fed_repo = parsedMsg['pullrequest']['project']['name']
                                    env.fed_branch = parsedMsg['pullrequest']['branch']
                                    env.fed_pr_id = parsedMsg['pullrequest']['id']
                                    env.branch = (env.fed_branch == 'master') ? 'rawhide' : env.fed_branch

                                    // Decorate our build
                                    String buildName = "PR-${env.fed_namespace}:${env.fed_pr_id}:${env.fed_repo}:${env.fed_branch}"
                                    // Once we have stage job running lets make build description
                                    // a hyperlink to PR like
                                    // <a href="https://src.fedoraproject.org/rpms/${env.fed_repo}/pull-request/${env.fed_pr_id}"> PR #${env.fed_pr_id} ${env.fed_repo}</a>
                                    currentBuild.displayName = buildName
                                    currentBuild.description = buildName
                                } else {
                                    env.artifact = 'koji-build'
                                    env.artifactOld = 'build'
                                    // Scratch build messages store things in info
                                    packagepipelineUtils.setScratchVars(parsedMsg)
                                    env.fed_repo = packagepipelineUtils.repoFromRequest(env.request_0)
                                    branches = packagepipelineUtils.setBuildBranch(env.request_1)
                                    env.branch = branches[0]
                                    env.fed_branch = branches[1]
                                    env.fed_namespace = 'rpms'
                                }


                                packagepipelineUtils.setDefaultEnvVars()

                                // Prepare Credentials (keys, passwords, etc)
                                packagepipelineUtils.prepareCredentials('fedora-keytab')

                                // Gather some info about the node we are running on for diagnostics
                                packagepipelineUtils.verifyPod(OPENSHIFT_NAMESPACE, env.NODE_NAME)

                                // Send message org.centos.prod.ci.<artifact>.test.running on fedmsg
                                messageFields = packagepipelineUtils.setTestMessageFields("running", artifact, parsedMsg)
                                packagepipelineUtils.sendMessage(messageFields['topic'], messageFields['properties'], messageFields['content'])
                            }
                        }

                        // Set our current stage value
                        env.currentStage = "koji-build"
                        stage(env.currentStage) {

                            // Set stage specific vars
                            packagepipelineUtils.handlePipelineStep('stepName': env.currentStage, 'debug': true) {
                                stageVars = packagepipelineUtils.setStageEnvVars(env.currentStage)

                                // Return a map (messageFields) of our message topic, properties, and content
                                messageFields = packagepipelineUtils.setMessageFields("package.running", artifactOld, parsedMsg)

                                // Send message org.centos.prod.ci.pipeline.allpackages.package.running on fedmsg
                                packagepipelineUtils.sendMessage(messageFields['topic'], messageFields['properties'], messageFields['content'])

                                // Prepare to send stage.complete message on failure
                                env.messageStage = 'package.complete'

                                // Get DistBranch value to find rpm NVR
                                env.DIST_BRANCH = packagepipelineUtils.setDistBranch(env.branch)
                                stageVars['DIST_BRANCH'] = env.DIST_BRANCH

                                // If a task id was provided, use those artifacts and
                                // bypass submitting a new rpm build
                                if (env.PROVIDED_KOJI_TASKID?.trim()) {
                                    stageVars['PROVIDED_KOJI_TASKID'] = env.PROVIDED_KOJI_TASKID
                                    // Run script that simply downloads artifacts
                                    // and stores them in jenkins workspace
                                    packagepipelineUtils.executeInContainer(containerName: "fedoraci-runner",
                                                                            containerScript: "/tmp/pull_old_task.sh",
                                                                            stageVars: stageVars,
                                                                            stageName: env.currentStage)
                                } else {
                                    // For tests namespace there is no package to build
                                    if (env.fed_namespace != "tests" ) {
                                        // koji_build_pr relies on fed_uid var
                                        stageVars['fed_uid'] = parsedMsg['pullrequest']['uid']
                                        // Build rpms
                                        packagepipelineUtils.executeInContainer(containerName: "fedoraci-runner",
                                                                                containerScript: "/tmp/koji_build_pr.sh",
                                                                                stageVars: stageVars,
                                                                                stageName: env.currentStage)
                                    }
                                }

                                // Inject variables
                                def job_props = "${env.WORKSPACE}/" + env.currentStage + "/logs/job.props"
                                if (fileExists(job_props)) {
                                     def job_props_groovy = "${env.WORKSPACE}/job.props.groovy"
                                     packagepipelineUtils.convertProps(job_props, job_props_groovy)
                                     load(job_props_groovy)

                                    // Make sure we generated a good repo
                                    packagepipelineUtils.executeInContainer(containerName: "fedoraci-runner",
                                                                            containerScript: "/tmp/repoquery.sh",
                                                                            stageVars: stageVars,
                                                                            stageName: env.currentStage)
                                }
                            }

                            if (env.PROVIDED_KOJI_TASKID?.trim()) {
                                // Check if to add scratch tag to build name
                                String scratchTag = env.isScratch.toBoolean() ? ":S" : ""
                                // Decorate our build to not be null now
                                String buildName = "${env.koji_task_id}${scratchTag}:${env.nvr}"
                                currentBuild.displayName = buildName
                                currentBuild.description = buildName
                            }

                            // Set our message topic, properties, and content
                            messageFields = packagepipelineUtils.setMessageFields("package.complete", artifactOld, parsedMsg)

                            // Send message org.centos.prod.ci.pipeline.allpackages.package.complete on fedmsg
                            packagepipelineUtils.sendMessage(messageFields['topic'], messageFields['properties'], messageFields['content'])

                            // Set our message topic, properties, and content
                            messageFields = packagepipelineUtils.setMessageFields("image.queued", artifactOld, parsedMsg)

                            // Send message org.centos.prod.ci.pipeline.allpackages.image.queued on fedmsg
                            packagepipelineUtils.sendMessage(messageFields['topic'], messageFields['properties'], messageFields['content'])
                        }

                        env.currentStage = "cloud-image-compose"
                        stage(env.currentStage) {

                            packagepipelineUtils.handlePipelineStep(stepName: env.currentStage, debug: true) {

                                // Set our message topic, properties, and content
                                messageFields = packagepipelineUtils.setMessageFields("image.running", artifactOld, parsedMsg)

                                // Send message org.centos.prod.ci.pipeline.allpackages.image.running on fedmsg
                                packagepipelineUtils.sendMessage(messageFields['topic'], messageFields['properties'], messageFields['content'])

                                // Set stage specific vars
                                stageVars = packagepipelineUtils.setStageEnvVars(env.currentStage)

                                // Prepare to send stage.complete message on failure
                                env.messageStage = 'image.complete'

                                // Compose image
                                packagepipelineUtils.executeInContainer(containerName: "fedoraci-runner",
                                                                        containerScript: "/tmp/virt-customize.sh",
                                                                        stageVars: stageVars,
                                                                        stageName: env.currentStage)

                                // Set our message topic, properties, and content
                                messageFields = packagepipelineUtils.setMessageFields("image.complete", artifactOld, parsedMsg)

                                // Send message org.centos.prod.ci.pipeline.allpackages.image.complete on fedmsg
                                packagepipelineUtils.sendMessage(messageFields['topic'], messageFields['properties'], messageFields['content'])

                            }
                        }

                        env.currentStage = "nvr-verify"
                        stage(env.currentStage) {

                            packagepipelineUtils.handlePipelineStep(stepName: env.currentStage, debug: true) {
                                // Set stage specific vars
                                stageVars = packagepipelineUtils.setStageEnvVars(currentStage)

                                // This can't be in setStageEnvVars because it depends on env.WORKSPACE
                                stageVars['TEST_SUBJECTS'] = "${env.WORKSPACE}/images/test_subject.qcow2"
                                stageVars['TEST_LOCATION'] = "${env.PAGURE_URL}/${env.fed_namespace}/${env.fed_repo}"

                                // tests namespace does not install any package, so do no need to verify rpm
                                if (env.fed_namespace != "tests" ) {
                                    // Run nvr verification
                                    packagepipelineUtils.executeInContainer(containerName: "fedoraci-runner",
                                                                            containerScript: "/tmp/verify-rpm.sh",
                                                                            stageVars: stageVars,
                                                                            stageName: env.currentStage)
                                }
                            }
                        }

                        env.currentStage = "package-tests"
                        stage(env.currentStage) {
                            // Only run this stage if tests exist
                            if (!packagepipelineUtils.checkTests(env.fed_repo, env.fed_branch, 'classic', (artifact == 'dist-git-pr' ? env.fed_pr_id : null), env.fed_namespace)) {
                                packagepipelineUtils.skip(env.currentStage)
                            } else {
                                packagepipelineUtils.handlePipelineStep(stepName: env.currentStage, debug: true) {
                                    // Set our message topic, properties, and content
                                    messageFields = packagepipelineUtils.setMessageFields("package.test.functional.queued", artifactOld, parsedMsg)

                                    // Send message org.centos.prod.ci.pipeline.allpackages.package.test.functional.queued on fedmsg
                                    packagepipelineUtils.sendMessage(messageFields['topic'], messageFields['properties'], messageFields['content'])

                                    // Set stage specific vars
                                    stageVars = packagepipelineUtils.setStageEnvVars(env.currentStage)

                                    // Set our message topic, properties, and content
                                    messageFields = packagepipelineUtils.setMessageFields("package.test.functional.running", artifactOld, parsedMsg)

                                    // Send message org.centos.prod.ci.pipeline.allpackages.package.test.functional.running on fedmsg
                                    packagepipelineUtils.sendMessage(messageFields['topic'], messageFields['properties'], messageFields['content'])

                                    // This can't be in setStageEnvVars because it depends on env.WORKSPACE
                                    stageVars['TEST_SUBJECTS'] = "${env.WORKSPACE}/images/test_subject.qcow2"

                                    // Prepare to send stage.complete message on failure
                                    env.messageStage = 'package.test.functional.complete'

                                    // Run functional tests
                                    try {
                                        packagepipelineUtils.executeInContainer(containerName: "fedoraci-runner",
                                                                                containerScript: "/tmp/package-test.sh",
                                                                                stageVars: stageVars,
                                                                                stageName: env.currentStage)
                                    } catch(e) {
                                        if (fileExists("${WORKSPACE}/${env.currentStage}/logs/test.log")) {
                                            buildResult = 'UNSTABLE'
                                            // set currentBuild.result to update the message status
                                            currentBuild.result = buildResult

                                        } else {
                                            throw e
                                        }
                                    }

                                    if (fileExists("${WORKSPACE}/${env.currentStage}/logs/results.yml")) {
                                        def test_results = readYaml file: "${WORKSPACE}/${env.currentStage}/logs/results.yml"
                                        def test_failed = false
                                        test_results['results'].each { result ->
                                            // some test case exited with error
                                            // handle this as test failure and not as infra one
                                            if (result.result == "error") {
                                                test_failed = true
                                            }
                                            if (result.result == "fail") {
                                                test_failed = true
                                            }
                                        }
                                        if (test_failed) {
                                            currentBuild.result = 'UNSTABLE'
                                        }
                                    }

                                    // Set our message topic, properties, and content
                                    messageFields = packagepipelineUtils.setMessageFields("package.test.functional.complete", artifactOld, parsedMsg)

                                    // Send message org.centos.prod.ci.pipeline.allpackages.package.test.functional.complete on fedmsg
                                    packagepipelineUtils.sendMessage(messageFields['topic'], messageFields['properties'], messageFields['content'])

                                    // Send message org.centos.prod.ci.<artifact>.test.complete on fedmsg
                                    messageFields = packagepipelineUtils.setTestMessageFields("complete", artifact, parsedMsg)
                                    packagepipelineUtils.sendMessage(messageFields['topic'], messageFields['properties'], messageFields['content'])


                                }
                            }
                        }

                        buildResult = buildResult ?: 'SUCCESS'

                    } catch (e) {
                        // Set build result
                        buildResult = 'FAILURE'
                        currentBuild.result = buildResult

                        // Send message org.centos.prod.ci.pipeline.allpackages.<stage>.complete on fedmsg if stage failed
                        messageFields = packagepipelineUtils.setMessageFields(messageStage, artifactOld, parsedMsg)
                        packagepipelineUtils.sendMessage(messageFields['topic'], messageFields['properties'], messageFields['content'])

                        // Send message org.centos.prod.ci.<artifact>.test.error on fedmsg
                        messageFields = packagepipelineUtils.setTestMessageFields("error", artifact, parsedMsg)
                        packagepipelineUtils.sendMessage(messageFields['topic'], messageFields['properties'], messageFields['content'])

                        // Report the exception
                        echo "Error: Exception from " + env.currentStage + ":"
                        echo e.getMessage()

                    } finally {
                        currentBuild.result = buildResult
                        packagepipelineUtils.getContainerLogsFromPod(OPENSHIFT_NAMESPACE, env.NODE_NAME)

                        // Archive our artifacts
                        if (currentBuild.result == 'SUCCESS') {
                            step([$class: 'ArtifactArchiver', allowEmptyArchive: true, artifacts: '**/logs/**,*.txt,*.groovy,**/job.*,**/*.groovy,**/inventory.*', excludes: '**/job.props,**/job.props.groovy,**/*.example,**/*.qcow2', fingerprint: true])
                        } else {
                            step([$class: 'ArtifactArchiver', allowEmptyArchive: true, artifacts: '**/logs/**,*.txt,*.groovy,**/job.*,**/*.groovy,**/inventory.*,**/*.qcow2', excludes: '**/job.props,**/job.props.groovy,**/*.example,artifacts.ci.centos.org/**,*.qcow2', fingerprint: true])
                        }

                        // Set our message topic, properties, and content
                        messageFields = packagepipelineUtils.setMessageFields("complete", artifactOld, parsedMsg)

                        // Send message org.centos.prod.ci.pipeline.allpackages.complete on fedmsg
                        packagepipelineUtils.sendMessage(messageFields['topic'], messageFields['properties'], messageFields['content'])

                        // set the metrics we want
                        // def packageMeasurement = "${ciMetrics.prefix}_${env.fed_repo}"
                        // ciMetrics.setMetricTag(jobMeasurement, 'package_name', env.fed_repo)
                        // ciMetrics.setMetricTag(jobMeasurement, 'build_result', currentBuild.result)
                        // ciMetrics.setMetricField(jobMeasurement, 'build_time', currentBuild.getDuration())
                        // ciMetrics.setMetricField(packageMeasurement, 'build_time', currentBuild.getDuration())
                        // ciMetrics.setMetricTag(packageMeasurement, 'package_name', env.fed_repo)

                    }
                }
            }
        }
    }
}
