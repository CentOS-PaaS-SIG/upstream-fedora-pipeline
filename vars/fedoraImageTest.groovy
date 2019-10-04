def call(Map parameters = [:]) {

    def imageType = parameters.get('imageType', 'Cloud')


    imageName = null
    containers = ['fedoraci-runner']

    try {
        deployOpenShiftTemplate(podName: "fedora-image-test-${UUID.randomUUID().toString()}",
                docker_repo_url: '172.30.254.79:5000',
                containers: containers) {
            stage('download image') {
                imageName = packagepipelineUtils.downloadCompose(imageType: imageType)
            }

            stage('resize compose') {
                packagepipelineUtils.resizeCompose('container': 'fedoraci-runner', imageName: imageName, increase: '10G')
            }

            try {
                stage('test compose') {
                    packagepipelineUtils.testCompose('container': 'fedoraci-runner', imageName: imageName, interactions: env.INTERACTIONS ?: 10)
                }

            } catch (e) {
                throw e
            } finally {
                stage('archive VM logs') {
                    handlePipelineStep {
                        step([$class   : 'ArtifactArchiver', allowEmptyArchive: true,
                                      artifacts: '**/artifacts/*.log'])
                    }
                }
            }
            stage('archive image') {
                handlePipelineStep {
                    step([$class   : 'ArtifactArchiver', allowEmptyArchive: true,
                          artifacts: '*.qcow2', fingerprint: true])
                }
            }
        }
    } catch(e) {
        echo e.getMessage()
        throw e
    }

}
