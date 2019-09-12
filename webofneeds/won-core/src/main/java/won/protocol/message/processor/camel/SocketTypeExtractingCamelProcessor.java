/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.protocol.message.processor.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.exception.MissingMessagePropertyException;
import won.protocol.message.processor.exception.WonMessageProcessingException;
import won.protocol.model.Connection;
import won.protocol.repository.ConnectionRepository;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;

/**
 * Extracts the socket looking into the 'wonConnectionURI' header and looking up
 * the connection in the db
 */
public class SocketTypeExtractingCamelProcessor implements Processor {
    @Autowired
    ConnectionRepository connectionRepository;

    @Override
    public void process(Exchange exchange) throws Exception {
        URI socketType;
        WonMessage wonMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
        URI conUri = (URI) exchange.getIn().getHeader(WonCamelConstants.CONNECTION_URI_HEADER);
        if (conUri == null) {
            throw new MissingMessagePropertyException(URI.create(WONMSG.recipient.getURI()));
        }
        Connection con = connectionRepository.findOneByConnectionURI(conUri);
        socketType = con.getTypeURI();
        if (socketType == null) {
            throw new WonMessageProcessingException(String.format(
                            "Failed to determine connection " + "socket for message %s", wonMessage.getMessageURI()));
        }
        exchange.getIn().setHeader(WonCamelConstants.SOCKET_TYPE_HEADER, socketType);
    }
}
