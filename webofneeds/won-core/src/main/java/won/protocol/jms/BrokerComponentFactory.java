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
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.Component;
import org.apache.camel.component.jms.JmsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;

import java.net.URI;

import static org.apache.activemq.camel.component.ActiveMQComponent.activeMQComponent;

/**
 * User: LEIH-NB
 * Date: 28.01.14
 */
public class BrokerComponentFactory {
    Logger logger = LoggerFactory.getLogger(this.getClass());
    public synchronized Component getBrokerComponent(URI brokerURI){
        //TODO: make this configurable for different broker implementations.
        logger.info("establishing activemq connection for brokerUri {}",brokerURI);
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory(brokerURI);
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(activeMQConnectionFactory);
        JmsConfiguration jmsConfiguration = new JmsConfiguration(cachingConnectionFactory);
        jmsConfiguration.setTimeToLive(0);
        jmsConfiguration.setDisableTimeToLive(true);
        jmsConfiguration.setRequestTimeout(0);

        ActiveMQComponent activeMQComponent = ActiveMQComponent.activeMQComponent();

        activeMQComponent.setConfiguration(jmsConfiguration);

        return activeMQComponent;

    }

}
