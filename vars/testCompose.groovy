def call(Map parameters = [:]) {
    def imageName = parameters.get('imageName')
    sh("curl -O https://pagure.io/upstream-fedora-ci/raw/master/f/fedora-ci-monitor/validate-test-subject.py")
    sh("rm -rf /tmp/artifacts")
    sh("python validate-test-subject.py -s ${imageName}")
    sh("test /tmp/artifacts && false || true")
}
