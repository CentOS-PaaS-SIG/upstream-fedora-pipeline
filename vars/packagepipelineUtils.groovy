import org.centos.pipeline.PackagePipelineUtils
import org.jenkinsci.plugins.pipeline.modeldefinition.Utils

/**
 * A class of methods used in the Jenkinsfile pipeline.
 * These methods are wrappers around methods in the packagepipelineUtils library.
 */
class packagepipelineUtils implements Serializable {

    def packagePipelineUtils = new PackagePipelineUtils()

    /**
     * Method to get the current release number used by rawhide
     * @return integer
     */
    def getRawhideRelease() {
        packagePipelineUtils.getRawhideRelease()
    }
    /**
     * Method to to find DIST_BRANCH to use for rpm NVRs
     * @param String branch - the branch value from the CI_MESSAGE
     * @return
     */
    def setDistBranch(String branch) {
        return packagePipelineUtils.setDistBranch(branch)
    }

    /**
     * Method that uses contra-lib shared library
     * to create the ci.artifact.test.messageType messages
     * @param messageType: queued, running, complete, error
     * @param artifact: dist-git-pr, koji-build
     * @param parsedMsg: The parsed fedmsg
     * @return
     */
    def setMessageFields(String messageType, String artifact, Map parsedMsg) {
        packagePipelineUtils.setMessageFields(messageType, artifact, parsedMsg)
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
     * Function to check if branch is master or fXX, XX > 19
     * @param branch - The branch to check
     * @return bool
     */
    def checkBranch(String branch) {
        return packagePipelineUtils.checkBranch(branch)
    }

    def influxDBPrefix() {
        return "Fedora_All_Packages_Pipeline"
    }

    /**
     * Mark stage stageName as skipped
     * @param stageName
     * @return
     */
    def skip(String stageName) {
        org.jenkinsci.plugins.pipeline.modeldefinition.Utils.markStageSkippedForConditional(stageName)
    }

    /*
     * Wrapper method to execute a specified script in a specified container
     * @param parameters
     * @return
     */
    def executeInContainer(Map parameters) {
        packagePipelineUtils.executeInContainer(parameters)
    }

    /**
     * Method to prepend 'env.' to the keys in source file and write them in a format of env.key=value in the destination file.
     * @param sourceFile The file to read from
     * @param destinationFile The file to write to
     */
     def convertProps(String sourceFile, String destinationFile) {
         packagePipelineUtils.convertProps(sourceFile, destinationFile)
     }

    /**
     * Function to set env.isScratch, env.request_0, and
     * env.request_1 based on the parsedMsg key structure
     * @param parsedMsg - The parsed fedmsg
     * @return
     */
    def setScratchVars(Map parsedMsg) {
        packagePipelineUtils.setScratchVars(parsedMsg)
    }

    /**
    * Based on tagMap, add comment to GH with
    * instructions to manual commands
    *
    * @param map of tags
    * @return
    */
    def sendPRCommentforTags(imageOperationsList) {
        packagePipelineUtils.sendPRCommentforTags(imageOperationsList)
    }

    /**
     * info about tags to be used
     * @param map
     */
    def printLabelMap(map) {
        packagePipelineUtils.printLabelMap(map)
    }

    /**
     * Setup container templates in openshift
     * @param openshiftProject Openshift Project
     * @return
     */
    def setupContainerTemplates(String openshiftProject) {
        packagePipelineUtils.setupContainerTemplates(openshiftProject)
    }

    def downloadCompose(Map parameters = [:]) {
        packagePipelineUtils.downloadCompose(parameters)
    }

    def resizeCompose(Map parameters = [:]) {
        packagePipelineUtils.resizeCompose(parameters)
    }

    def testCompose(Map parameters = [:]) {
        packagePipelineUtils.testCompose(parameters)
    }
    /**
     * Set environment variables parsing a CI message from koji build
     * @return
     */
    def processBuildCIMessage() {
        packagePipelineUtils.processBuildCIMessage()
    }
}
