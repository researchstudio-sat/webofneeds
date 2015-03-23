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

package won.node.camel.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.jms.MessagingService;
import won.protocol.jms.NeedProtocolCommunicationService;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.OwnerApplication;
import won.protocol.repository.OwnerApplicationRepository;
import won.protocol.service.QueueManagementService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * User: sbyim
 * Date: 13.11.13
 */
public class NeedProtocolOutgoingMessagesProcessor implements Processor {
    private final org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private NeedProtocolCommunicationService needProtocolCommunicationService;

    @Autowired
    private MessagingService messageService;

    @Override
    public void process(Exchange exchange) throws Exception {
        logger.debug("processing message for sending to remote node");
      WonMessage wonMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.WON_MESSAGE_EXCHANGE_HEADER);
      if (wonMessage.getSenderNodeURI().equals(wonMessage.getReceiverNodeURI())){
        //sending locally, directly put message into the incoming need protocol
        messageService.sendInOnlyMessage(null, null, wonMessage, "seda:NeedProtocolIn");
        return;
      }
      needProtocolCommunicationService.configureCamelEndpoint(wonMessage.getReceiverNodeURI());
      messageService.sendInOnlyMessage(null, null, wonMessage, wonMessage.getReceiverNodeURI().toString());
    }

}
