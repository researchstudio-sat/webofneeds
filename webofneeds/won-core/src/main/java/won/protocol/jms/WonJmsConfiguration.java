package won.protocol.jms;

import java.lang.invoke.MethodHandles;

import javax.jms.ConnectionFactory;

import org.apache.camel.component.jms.JmsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: LEIH-NB Date: 28.04.14
 */
public class WonJmsConfiguration extends JmsConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public WonJmsConfiguration(ConnectionFactory connectionFactory) {
        super(connectionFactory);
        configureDefaultJmsConfiguration();
    }

    public void configureDefaultJmsConfiguration() {
        // setDisableTimeToLive(true);
        // setRequestTimeout(0);
        setDeliveryPersistent(false);
        setAcknowledgementModeName("DUPS_OK_ACKNOWLEDGE"); // make each consumer explicitly acknowledge each message
        setDisableReplyTo(true);
        setExplicitQosEnabled(true); // required for the TTL to have an effect
        setTimeToLive(10);
        setTransacted(false);
        setAsyncConsumer(true); // this may improve performance for most cases, but it bites us in the
        // registration situation, when we actually want to get a result from the
        // wonnode.
        logger.info("default jms configuration setup done");
    }

    public void configureJmsConfigurationForQueues() {
        setConcurrentConsumers(1);
    }

    public void configureJmsConfigurationForTopics() {
        setConcurrentConsumers(1);
    }
}
