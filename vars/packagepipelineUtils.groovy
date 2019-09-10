import org.centos.pipeline.PackagePipelineUtils
import org.centos.contra.pipeline.Utils
import org.centos.Utils
import org.jenkinsci.plugins.pipeline.modeldefinition.Utils

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
     * @param String branch - the branch value from the CI_MESSAGE
     * @return
     */
    def setDistBranch(String branch) {
        return packagePipelineUtils.setDistBranch(branch)
    }

    /**
     * Method to set message fields to be published
     * @param messageType ${MAIN_TOPIC}.ci.pipeline.<defined-in-README>
     * @param artifact ${MAIN_TOPIC}.ci.pipeline.allpackages-${artifact}.<defined-in-README>
     * @param parsedMsg: The parsed fedmsg
     * @return
     */
    def setMessageFields(String messageType, String artifact, Map parsedMsg) {
        packagePipelineUtils.setMessageFields(messageType, artifact, parsedMsg)
    }

    /**
     * Method that uses contra-lib shared library
     * to create the ci.artifact.test.messageType messages
     * @param messageType: queued, running, complete, error
     * @param artifact: dist-git-pr, koji-build
     * @param parsedMsg: The parsed fedmsg
     * @return
     */
    def setTestMessageFields(String messageType, String artifact, Map parsedMsg) {
        packagePipelineUtils.setTestMessageFields(messageType, artifact, parsedMsg)
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
     * Test if $tag tests exist for $mypackage on $mybranch in fedora dist-git
     * For mybranch, use fXX or master and pr_id is PR number (digits only)
     * @param mypackage
     * @param mybranch - Fedora branch
     * @param tag
     * @param pr_id    - PR number
     * @param namespace - rpms (default) or container
     * @return
     */
    def checkTests(String mypackage, String mybranch, String tag, String pr_id=null, String namespace='rpms') {
        contraUtils.checkTests(mypackage, mybranch, tag, pr_id, namespace)
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
        return contraUtils.setBuildBranch(tag)
    }

    /**
     * @param openshiftProject name of openshift namespace/project.
     * @param nodeName podName we are going to get container logs from.
     * @return
     */
    def getContainerLogsFromPod(String openshiftProject, String nodeName=env.NODE_NAME) {
        contraUtils.getContainerLogsFromPod(openshiftProject, nodeName)
    }

    /**
     * Mark stage stageName as skipped
     * @param stageName
     * @return
     */
    def skip(String stageName) {
        org.jenkinsci.plugins.pipeline.modeldefinition.Utils.markStageSkippedForConditional(stageName)
    }

    /**
     * Method to prepend 'env.' to the keys in source file and write them in a format of env.key=value in the destination file.
     * @param sourceFile The file to read from
     * @param destinationFile The file to write to
     */
     def convertProps(String sourceFile, String destinationFile) {
         centosUtils.convertProps(sourceFile, destinationFile)
     }

    /**
     * Library to parse Pagure PR CI_MESSAGE and check if
     * it is for a new commit added, the comment contains
     * some keyword, or if the PR was rebased
     * If notification = true, commit was added or it was rebased
     * @param message - The CI_MESSAGE
     * @param keyword - The keyword we care about
     * @return bool
     */
    def checkUpdatedPR(String message, String keyword) {
        return contraUtils.checkUpdatedPR(message, keyword)
    }

    /**
     * Using the currentBuild, get a string representation
     * of the changelog.
     * @return String of changelog
     */
    def getChangeLogFromCurrentBuild() {
        return contraUtils.getChangeLogFromCurrentBuild()
    }

    /**
     *
     * @param nick nickname to connect to IRC with
     * @param channel channel to connect to
     * @param message message to send
     * @param ircServer optional IRC server defaults to irc.freenode.net:6697
     * @return
     */
    def sendIRCNotification(String nick, String channel, String message, String ircServer="irc.freenode.net:6697") {
        contraUtils.sendIRCNotification(nick, channel, message, ircServer)
    }

    /**
     * Library to send message
     * @param msgTopic - The topic to send the message on
     * @param msgProps - The message properties in key=value form, one key/value per line ending in '\n'
     * @param msgContent - Message content
     * @param provider - Provider to send message on. If not passed, will default to env.MSG_PROVIDER
     * @return
     */
    def sendMessage(String msgTopic, String msgProps, String msgContent, def provider=null) {
        contraUtils.sendMessage(msgTopic, msgProps, msgContent, provider)
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
}
