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

package won.node.camel.processor.general;

import java.net.URI;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;

import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Connection;
import won.protocol.repository.ConnectionRepository;

/**
 * Extracts the connection state and creates a ConnectionStateChangeBuilder. The connection state builder is set as a
 * header in the in message.
 * 
 */
public class ConnectionStateChangeBuilderCamelProcessor implements Processor {

    @Autowired
    ConnectionRepository connectionRepository;

    public ConnectionStateChangeBuilderCamelProcessor() {
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        ConnectionStateChangeBuilder stateChangeBuilder = new ConnectionStateChangeBuilder();
        // first, try to find the connection uri in the header:
        URI conUri = (URI) exchange.getIn().getHeader(WonCamelConstants.CONNECTION_URI_HEADER);
        if (conUri == null) {
            // not found. get it from the message and put it in the header
            WonMessage wonMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
            conUri = wonMessage.getEnvelopeType() == WonMessageDirection.FROM_EXTERNAL ? wonMessage.getReceiverURI()
                    : wonMessage.getSenderURI();
        }
        if (conUri != null) {
            // found a connection. Put its URI in the header and load it
            Connection con = connectionRepository.findOneByConnectionURI(conUri);
            stateChangeBuilder.oldState(con.getState());
        } else {
            // found no connection. don't modify the builder
        }

        // put the state change builder in the header
        exchange.getIn().setHeader(WonCamelConstants.CONNECTION_STATE_CHANGE_BUILDER_HEADER, stateChangeBuilder);
    }
}