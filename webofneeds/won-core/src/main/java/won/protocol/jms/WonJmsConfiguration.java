package won.protocol.jms;

import org.apache.camel.component.jms.JmsConfiguration;

import javax.jms.ConnectionFactory;

/**
 * User: LEIH-NB
 * Date: 28.04.14
 */
public class WonJmsConfiguration extends JmsConfiguration
{
  public WonJmsConfiguration(ConnectionFactory connectionFactory){
    super(connectionFactory);
    configureDefaultJmsConfiguration();
  }
  public void configureDefaultJmsConfiguration(){
    setTimeToLive(0);
    setDisableTimeToLive(true);
    setRequestTimeout(0);
  }

  public void configureJmsConfigurationForQueues(){
    setConcurrentConsumers(5);
  }

  public void configureJmsConfigurationForTopics(){

  }

}
