def call(Map parameters = [:]) {
    def imageName = parameters.get('imageName')
    def cmd = """
              curl -O https://pagure.io/upstream-fedora-ci/raw/master/f/fedora-ci-monitor/validate-test-subject.py && \
              rm -rf /tmp/artifacts && \
              python validate-test-subject.py -s ${imageName} && \
              test /tmp/artifacts && false || true
              """
    executeInContainer(containerName: 'singlehost-test', containerScript: cmd)

}
