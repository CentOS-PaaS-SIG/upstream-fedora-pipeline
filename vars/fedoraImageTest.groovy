def call(Map parameters = [:]) {

    def imageType = parameters.get('imageType', 'Cloud')


    imageName = null
    containers = ['fedoraci-runner']

    try {
        deployOpenShiftTemplate(podName: "fedora-image-test-${UUID.randomUUID().toString()}",
                docker_repo_url: '172.30.254.79:5000',
                containers: containers) {
            stage('download image') {
                imageName = downloadCompose(imageType: imageType)
            }

            stage('resize compose') {
                resizeCompose('container': 'fedoraci-runner', imageName: imageName, increase: '10G')
            }

            try {
                stage('test compose') {
                    testCompose('container': 'fedoraci-runner', imageName: imageName)
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
