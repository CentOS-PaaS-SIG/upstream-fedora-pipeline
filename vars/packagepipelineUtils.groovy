import org.centos.pipeline.PackagePipelineUtils
import org.centos.contra.pipeline.Utils
import org.centos.Utils

/**
 * A class of methods used in the Jenkinsfile pipeline.
 * These methods are wrappers around methods in the packagepipelineUtils library.
 */
class packagepipelineUtils implements Serializable {

    def packagePipelineUtils = new PackagePipelineUtils()
    def contraUtils = new org.centos.contra.pipeline.Utils()
    def centosUtils = new org.centos.Utils()

    // pass in from the jenkinsfile
    // def cimetrics

    /**
     * Method to to find DIST_BRANCH to use for rpm NVRs
     * @return
     */
    def setDistBranch() {
        return packagePipelineUtils.setDistBranch()
    }

    /**
     * Method to set message fields to be published
     * @param messageType ${MAIN_TOPIC}.ci.pipeline.<defined-in-README>
     * @param artifact ${MAIN_TOPIC}.ci.pipeline.allpackages-${artifact}.<defined-in-README>
     * @return
     */
    def setMessageFields(String messageType, String artifact) {
        packagePipelineUtils.setMessageFields(messageType, artifact)
    }

    /**
     * Method that uses contra-lib shared library
     * to create the ci.artifact.test.messageType messages
     * @param messageType: queued, running, complete, error
     * @param artifact: dist-git-pr, koji-build
     * @return
     */
    def setTestMessageFields(String messageType, String artifact) {
        packagePipelineUtils.setTestMessageFields(messageType, artifact)
    }

    /**
     * Method to set default environmental variables. Performed once at start of Jenkinsfile
     * @param envMap Key/value pairs which will be set as environmental variables.
     * @return
     */
    def setDefaultEnvVars(Map envMap = null) {
        packagePipelineUtils.setDefaultEnvVars(envMap)
    }

    /**
     * Method to set stage specific environmental variables.
     * @param stage Current stage
     * @return
     */
    def setStageEnvVars(String stage) {
        packagePipelineUtils.setStageEnvVars(stage)
    }

    def prepareCredentials(String credentials) {
        packagePipelineUtils.prepareCredentials(credentials)
    }

    /**
     * Watch for messages
     * @param msg_provider jms-messaging message provider
     * @param message trigger message
     */
    def watchForMessages(String msg_provider, String message) {
        packagePipelineUtils.watchForMessages(msg_provider, message)
    }

    def ciPipeline(Closure body) {
        try {
            packagePipelineUtils.ciPipeline(body)
        } catch(e) {
            throw e
        } // finally {
          //  cimetrics.writeToInflux()
        // }
    }

    def handlePipelineStep(Map config, Closure body) {
        packagePipelineUtils.handlePipelineStep(config, body)
    }

    // def timedPipelineStep(Map config, Closure body) {
    //    def measurement = timedMeasurement()
    //    cimetrics.timed measurement, config.stepName, {
    //        packagePipelineUtils.handlePipelineStep(config, body)
    //    }
    // }

    def timedMeasurement() {
        return "${influxDBPrefix()}_${packagePipelineUtils.timedMeasurement()}"
    }

    /**
     * Function to check if fed_branch is master or fXX, XX > 19
     * @return bool
     */
    def checkBranch() {
        return packagePipelineUtils.checkBranch()
    }

    def influxDBPrefix() {
        return "Fedora_All_Packages_Pipeline"
    }

    /**
     * Test if $tag tests exist for $mypackage on $mybranch in fedora dist-git
     * For mybranch, use fXX or master, or PR number (digits only)
     * @param mypackage
     * @param mybranch - Fedora branch or PR number
     * @param tag
     * @return
     */
    def checkTests(String mypackage, String mybranch, String tag) {
        contraUtils.checkTests(mypackage, mybranch, tag)
    }

    /**
     *
     * @param openshiftProject name of openshift namespace/project.
     * @param nodeName podName we are going to verify.
     * @return
     */
    def verifyPod(String openshiftProject, String nodeName=env.NODE_NAME) {
        contraUtils.verifyPod(openshiftProject, nodeName)
    }

    /**
     * @param request - the url that refers to the package
     * @return
     */
    def repoFromRequest(String request) {
        contraUtils.repoFromRequest(request)
    }

    /**
     * Set branch and repo_branch based on the candidate branch
     * This is meant to be run with a CI_MESSAGE from a build task
     * @param tag - The tag from the request field e.g. f27-candidate
     * @return
     */
    def setBuildBranch(String tag) {
        contraUtils.setBuildBranch(tag)
    }

}
