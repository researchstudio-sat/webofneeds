/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.protocol.jms;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQSslConnectionFactory;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import won.cryptography.ssl.MessagingContext;
import won.protocol.model.MessagingType;

import javax.jms.ConnectionFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import java.net.URI;

/**
 * Adds a camel component that is used to contact a remote ActiveMQ broker and
 * configures its connectionFactory using the {@link WonJmsConfiguration}. User:
 * LEIH-NB Date: 28.01.14
 */
public class BrokerComponentFactory {
  Logger logger = LoggerFactory.getLogger(this.getClass());

  private synchronized Component getBrokerComponent(URI brokerURI, MessagingType type) {
    // TODO: make this configurable for different broker implementations.
    logger.info("establishing activemq connection for brokerUri {} (with specified type)", brokerURI);
    ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory(
        brokerURI + "?jms.prefetchPolicy.all=50");
    return getBrokerComponent(type, activeMQConnectionFactory);

  }

  private synchronized Component getBrokerComponent(URI brokerURI, MessagingType type, KeyManager keyManager,
      TrustManager trustManager) {
    // TODO: make this configurable for different broker implementations.
    logger.info(
        "establishing activemq ssl connection for brokerUri {} (with specified type, keyManager, and TrustManager)",
        brokerURI);
    // jms.prefetchPolicy parameter is added to prevent matcher-consumer death due
    // to overflowing with messages,
    // see http://activemq.apache.org/what-is-the-prefetch-limit-for.html
    ActiveMQSslConnectionFactory activeMQConnectionFactory = new ActiveMQSslConnectionFactory(
        brokerURI + "?jms.prefetchPolicy.all=50");

    activeMQConnectionFactory.setKeyAndTrustManagers(new KeyManager[] { keyManager },
        new TrustManager[] { trustManager }, null);

    return getBrokerComponent(type, activeMQConnectionFactory);

  }

  public synchronized Component getBrokerComponent(URI brokerURI, MessagingType type,
      MessagingContext messagingContext) {
    // TODO: make this configurable for different broker implementations.
    logger.info("establishing activemq connection for brokerUri {}", brokerURI);
    KeyManager keyManager = null;
    TrustManager trustManager = null;
    try {
      keyManager = messagingContext.getClientKeyManager();
      trustManager = messagingContext.getClientTrustManager();
    } catch (Exception e) {
      logger.error("Key- or Trust- manager initialization problem");
    }

    if (keyManager == null || trustManager == null) {
      return getBrokerComponent(brokerURI, type);
    } else {
      return getBrokerComponent(brokerURI, type, keyManager, trustManager);
    }
  }

  private synchronized Component getBrokerComponent(MessagingType type, ActiveMQConnectionFactory connectionFactory) {

    CachingConnectionFactory cachingConnectionFactory = (CachingConnectionFactory) configureCachingConnectionFactory(
        connectionFactory);

    WonJmsConfiguration jmsConfiguration = new WonJmsConfiguration(cachingConnectionFactory);

    switch (type) {
    case Queue:
      jmsConfiguration.configureJmsConfigurationForQueues();
      break;
    case Topic:
      jmsConfiguration.configureJmsConfigurationForTopics();
      break;
    }

    ActiveMQComponent activeMQComponent = ActiveMQComponent.activeMQComponent();

    activeMQComponent.setConfiguration(jmsConfiguration);

    return activeMQComponent;

  }

  public synchronized ConnectionFactory configureCachingConnectionFactory(ActiveMQConnectionFactory connectionFactory) {

    // for non-persistent messages setting "AlwaysSyncSend" to true makes it slow,
    // but ensures that a producer is immediately informed
    // about the memory issues on broker (is blocked or gets exception depending on
    // <systemUsage> config)
    // see more info http://activemq.apache.org/producer-flow-control.html
    connectionFactory.setAlwaysSyncSend(false);

    // disable timestamps by default so that ttl of messages is not checked
    connectionFactory.setDisableTimeStampsByDefault(true);

    CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(connectionFactory);
    cachingConnectionFactory.setCacheConsumers(true);
    cachingConnectionFactory.setCacheProducers(true);
    return cachingConnectionFactory;
  }

}
