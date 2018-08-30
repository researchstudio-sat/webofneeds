package won.protocol.jms;

import javax.jms.ConnectionFactory;

import org.apache.camel.component.jms.JmsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: LEIH-NB
 * Date: 28.04.14
 */
public class WonJmsConfiguration extends JmsConfiguration
{
  private final Logger logger = LoggerFactory.getLogger(getClass());
  
  public WonJmsConfiguration(ConnectionFactory connectionFactory){
    super(connectionFactory);
    configureDefaultJmsConfiguration();
    
  }
  public void configureDefaultJmsConfiguration(){
    setTimeToLive(0);
    setDisableTimeToLive(true);
    setRequestTimeout(0);
    setAsyncConsumer(true);
    setDeliveryPersistent(false);
    setDisableReplyTo(true);
    setTimeToLive(5000);
    setTransacted(false);
    logger.info("default jms configuration setup done");
  }

  public void configureJmsConfigurationForQueues(){
    setConcurrentConsumers(5);
  }

  public void configureJmsConfigurationForTopics(){

  }

}
