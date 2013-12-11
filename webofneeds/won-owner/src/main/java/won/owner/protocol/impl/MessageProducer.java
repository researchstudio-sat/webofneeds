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

package won.owner.protocol.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.Date;
import java.util.logging.Logger;
import java.net.URI;
/**
 * User: S.B. Yim
 * Date: 14.10.13
 */
public class MessageProducer {
    private static final Logger LOG = Logger.getLogger(MessageProducer.class.toString());

    @Autowired
    protected JmsTemplate jmsTemplate;

    protected  int numberOfMessages = 100;

    public void textMessage(URI brokerURI, final String message) throws JMSException{
        StringBuilder payload = new StringBuilder();
        payload.append(message);


        jmsTemplate.send(new MessageCreator(){
           @Override
           public Message createMessage(Session session) throws JMSException {
               return session.createTextMessage(message);  //To change body of implemented methods use File | Settings | File Templates.
           }
        });

    }
}
