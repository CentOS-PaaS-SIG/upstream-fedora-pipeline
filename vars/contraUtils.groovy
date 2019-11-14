import org.centos.contra.pipeline.Utils
import org.jenkinsci.plugins.pipeline.modeldefinition.Utils

/**
 * A class of methods used in the Jenkinsfile pipeline.
 * These methods are wrappers around methods in the packagepipelineUtils library.
 */
class contraUtils implements Serializable {

    def contraUtils = new org.centos.contra.pipeline.Utils()

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
}
