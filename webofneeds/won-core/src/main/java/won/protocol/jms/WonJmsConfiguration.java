package won.protocol.jms;

import javax.jms.ConnectionFactory;

import org.apache.camel.component.jms.JmsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: LEIH-NB Date: 28.04.14
 */
public class WonJmsConfiguration extends JmsConfiguration {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public WonJmsConfiguration(ConnectionFactory connectionFactory) {
        super(connectionFactory);
        configureDefaultJmsConfiguration();
    }

    public void configureDefaultJmsConfiguration() {
        // setDisableTimeToLive(true);
        // setRequestTimeout(0);
        setAsyncConsumer(true); // this may improve performance for most cases, but it bites us in the
                                // registration situation, when we actually want to get a result from the
                                // wonnode.
        setDeliveryPersistent(false);
        setDisableReplyTo(true);
        setExplicitQosEnabled(true); // required for the TTL to have an effect
        setTimeToLive(10);
        setTransacted(false);
        logger.info("default jms configuration setup done");
    }

    public void configureJmsConfigurationForQueues() {
        setConcurrentConsumers(5);
    }

    public void configureJmsConfigurationForTopics() {
    }
}
