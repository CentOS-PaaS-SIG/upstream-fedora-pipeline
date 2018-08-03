def call(Map parameters = [:]) {

    def imageType = parameters.get('imageType', 'Cloud')


    imageName = null
    containers = ['singlehost-test']

    try {
        deployOpenShiftTemplate(podName: "fedora-image-test-${UUID.randomUUID().toString()}",
                docker_repo_url: '172.30.254.79:5000',
                containers: containers) {
            stage('download image') {
                imageName = downloadCompose(imageType: imageType)
            }

            stage('test compose') {
                testCompose(imageName: imageName)
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
