import java.util.logging.Logger
import jenkins.security.s2m.*
import jenkins.model.*;
import hudson.markup.RawHtmlMarkupFormatter
import hudson.model.*
import hudson.security.*
import com.redhat.jenkins.plugins.ci.*
import com.redhat.jenkins.plugins.ci.messaging.*

def logger = Logger.getLogger("")
logger.info("Disabling CLI over remoting")
jenkins.CLI.get().setEnabled(false);
logger.info("Enable Slave -> Master Access Control")
Jenkins.instance.injector.getInstance(AdminWhitelistRule.class).setMasterKillSwitch(false);
// Set global and job read permissions
def strategy = Jenkins.instance.getAuthorizationStrategy()
strategy.add(hudson.model.Hudson.READ,'anonymous')
strategy.add(hudson.model.Item.READ,'anonymous')
Jenkins.instance.setAuthorizationStrategy(strategy)
// Set Markup Formatter to Safe HTML so PR hyperlinks work
Jenkins.instance.setMarkupFormatter(new RawHtmlMarkupFormatter(false))
Jenkins.instance.save()

logger.info("Setup fedora-fedmsg Messaging Provider")
FedMsgMessagingProvider fedmsg = new FedMsgMessagingProvider("fedora-fedmsg", "tcp://hub.fedoraproject.org:9940", "tcp://172.19.4.24:9941", "org.fedoraproject");
GlobalCIConfiguration.get().addMessageProvider(fedmsg)

logger.info("Setup fedora-fedmsg-stage Messaging Provider")
FedMsgMessagingProvider fedmsgStage = new FedMsgMessagingProvider("fedora-fedmsg-stage", "tcp://stg.fedoraproject.org:9940", "tcp://172.19.4.36:9941", "org.fedoraproject");
GlobalCIConfiguration.get().addMessageProvider(fedmsgStage)

logger.info("Setup fedora-fedmsg-devel Messaging Provider")
FedMsgMessagingProvider fedmsgDevel = new FedMsgMessagingProvider("fedora-fedmsg-devel", "tcp://fedmsg-relay.continuous-infra.svc:4001", "tcp://fedmsg-relay.continuous-infra.svc:2003", "org.fedoraproject");
GlobalCIConfiguration.get().addMessageProvider(fedmsgDevel)

logger.info("Setup FedoraMessaging Provider")
RabbitMQMessagingProvider fedoraMessaging = new RabbitMQMessagingProvider("FedoraMessaging", "rabbitmq.fedoraproject.org", "5671", "/pubsub", "org.centos.ci", "amq.topic", "centos-ci");
GlobalCIConfiguration.get().addMessageProvider(fedoraMessaging)

logger.info("Setup FedoraMessagingStage Provider")
RabbitMQMessagingProvider fedoraMessagingStage = new RabbitMQMessagingProvider("FedoraMessagingStage", "rabbitmq.stg.fedoraproject.org", "5671", "/pubsub", "org.centos.ci", "amq.topic", "centos-ci");
GlobalCIConfiguration.get().addMessageProvider(fedoraMessagingStage)

logger.info("Setting Time Zone to be EST")
System.setProperty('org.apache.commons.jelly.tags.fmt.timeZone', 'America/New_York')
