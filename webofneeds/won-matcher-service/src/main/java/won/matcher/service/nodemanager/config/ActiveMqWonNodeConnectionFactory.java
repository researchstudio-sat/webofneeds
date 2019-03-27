package won.matcher.service.nodemanager.config;

import java.util.UUID;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQSslConnectionFactory;
import org.apache.camel.FailedToCreateConsumerException;
import org.apache.camel.component.jms.JmsComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActorContext;
import akka.camel.Camel;
import akka.camel.CamelExtension;
import won.cryptography.ssl.MessagingContext;
import won.matcher.service.common.spring.SpringExtension;
import won.matcher.service.nodemanager.actor.HintProducerProtocolActor;
import won.matcher.service.nodemanager.actor.NeedConsumerProtocolActor;
import won.matcher.service.nodemanager.pojo.WonNodeConnection;
import won.protocol.service.WonNodeInfo;
import won.protocol.vocabulary.WON;

/**
 * Factory for creating a
 * {@link won.matcher.service.nodemanager.pojo.WonNodeConnection} with ActiveMq
 * endpoint of a won node
 * <p>
 * User: hfriedrich Date: 18.05.2015
 */
// TODO reuse BrokerComponentFactory
public class ActiveMqWonNodeConnectionFactory {
    protected static final Logger log = LoggerFactory.getLogger(ActiveMqWonNodeConnectionFactory.class);

    /**
     * Create a {@link won.matcher.service.nodemanager.pojo.WonNodeConnection} for
     * active mq
     *
     * @param context actor context to create the message consuming actors in
     * @param wonNodeInfo info about the won node (e.g. topics to subscribe)
     * @return the connection
     * @throws FailedToCreateConsumerException
     */
    public static WonNodeConnection createWonNodeConnection(UntypedActorContext context, WonNodeInfo wonNodeInfo,
                    MessagingContext messagingContext) {
        // read won node info
        String activeMq = WON.WON_OVER_ACTIVE_MQ.toString();
        String brokerUri = wonNodeInfo.getSupportedProtocolImplParamValue(activeMq, WON.HAS_BROKER_URI.toString());
        String createdTopic = wonNodeInfo.getSupportedProtocolImplParamValue(activeMq,
                        WON.HAS_ACTIVEMQ_MATCHER_PROTOCOL_OUT_NEED_CREATED_TOPIC_NAME.toString());
        String activatedTopic = wonNodeInfo.getSupportedProtocolImplParamValue(activeMq,
                        WON.HAS_ACTIVEMQ_MATCHER_PROTOCOL_OUT_NEED_ACTIVATED_TOPIC_NAME.toString());
        String deactivatedTopic = wonNodeInfo.getSupportedProtocolImplParamValue(activeMq,
                        WON.HAS_ACTIVEMQ_MATCHER_PROTOCOL_OUT_NEED_DEACTIVATED_TOPIC_NAME.toString());
        String hintQueue = wonNodeInfo.getSupportedProtocolImplParamValue(activeMq,
                        WON.HAS_ACTIVEMQ_MATCHER_PROTOCOL_QUEUE_NAME.toString());
        // create the activemq component for this won node
        String uuid = UUID.randomUUID().toString();
        String componentName = "activemq-" + uuid;
        ActiveMQConnectionFactory connectionFactory = createConnectionFactory(brokerUri, messagingContext);
        // connectionFactory.setExceptionListener( ... )
        Camel camel = CamelExtension.get(context.system());
        camel.context().addComponent(componentName, JmsComponent.jmsComponent(connectionFactory));
        // create the actors that receive the messages (need events)
        String createdComponent = componentName + ":topic:" + createdTopic + "?testConnectionOnStartup=false";
        Props createdProps = SpringExtension.SpringExtProvider.get(context.system())
                        .props(NeedConsumerProtocolActor.class, createdComponent);
        ActorRef created = context.actorOf(createdProps, "ActiveMqNeedCreatedConsumerProtocolActor-" + uuid);
        log.info("Create camel component JMS listener {} for won node {}", createdComponent,
                        wonNodeInfo.getWonNodeURI());
        ActorRef activated = created;
        if (!activatedTopic.equals(createdTopic)) {
            String activatedComponent = componentName + ":topic:" + activatedTopic + "?testConnectionOnStartup=false";
            Props activatedProps = SpringExtension.SpringExtProvider.get(context.system())
                            .props(NeedConsumerProtocolActor.class, activatedComponent);
            activated = context.actorOf(activatedProps, "ActiveMqNeedActivatedConsumerProtocolActor-" + uuid);
            log.info("Create camel component JMS listener {} for won node {}", activatedComponent,
                            wonNodeInfo.getWonNodeURI());
        }
        ActorRef deactivated;
        if (deactivatedTopic.equals(createdTopic)) {
            deactivated = created;
        } else if (deactivatedTopic.equals(activatedTopic)) {
            deactivated = activated;
        } else {
            String deactivatedComponent = componentName + ":topic:" + deactivatedTopic
                            + "?testConnectionOnStartup=false";
            Props deactivatedProps = SpringExtension.SpringExtProvider.get(context.system())
                            .props(NeedConsumerProtocolActor.class, deactivatedComponent);
            deactivated = context.actorOf(deactivatedProps, "ActiveMqNeedDeactivatedConsumerProtocolActor-" + uuid);
            log.info("Create camel component JMS listener {} for won node {}", deactivatedComponent,
                            wonNodeInfo.getWonNodeURI());
        }
        // create the actor that sends messages (hint events)
        String hintComponent = componentName + ":queue:" + hintQueue;
        Props hintProps = SpringExtension.SpringExtProvider.get(context.system()).props(HintProducerProtocolActor.class,
                        hintComponent, null);
        ActorRef hintProducer = context.actorOf(hintProps, "ActiveMqHintProducerProtocolActor-" + uuid);
        log.info("Create camel component JMS listener {} for won node {}", hintComponent, wonNodeInfo.getWonNodeURI());
        // watch the created consumers from the context to get informed when they are
        // terminated
        context.watch(created);
        context.watch(activated);
        context.watch(deactivated);
        context.watch(hintProducer);
        // create the connection
        WonNodeConnection jmsConnection = new WonNodeConnection(wonNodeInfo, created, activated, deactivated,
                        hintProducer);
        return jmsConnection;
    }

    private static ActiveMQConnectionFactory createConnectionFactory(final String brokerUri,
                    final MessagingContext messagingContext) {
        KeyManager keyManager = null;
        TrustManager trustManager = null;
        if (messagingContext != null) {
            try {
                keyManager = messagingContext.getClientKeyManager();
                trustManager = messagingContext.getClientTrustManager();
            } catch (Exception e) {
                log.error("Key- or Trust- manager initialization problem", e);
            }
        }
        // jms.prefetchPolicy parameter is added to prevent matcher-consumer death due
        // to overflowing with messages,
        // see http://activemq.apache.org/what-is-the-prefetch-limit-for.html
        ActiveMQSslConnectionFactory connectionFactory = new ActiveMQSslConnectionFactory(
                        brokerUri + "?jms.prefetchPolicy.all=10?");
        // for non-persistent messages setting "AlwaysSyncSend" to true makes it slow,
        // but ensures that a producer is immediately informed
        // about the memory issues on broker (is blocked or gets exception depending on
        // <systemUsage> config)
        // see more info http://activemq.apache.org/producer-flow-control.html
        connectionFactory.setAlwaysSyncSend(false);
        connectionFactory.setUseAsyncSend(true);
        connectionFactory.setDispatchAsync(true);
        if (keyManager != null && trustManager != null) {
            connectionFactory.setKeyAndTrustManagers(new KeyManager[] { keyManager },
                            new TrustManager[] { trustManager }, null);
        } else {
            log.warn("key or trust manager was null, therefore do not set them in connection factory");
        }
        return connectionFactory;
    }
}
