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

import com.hp.hpl.jena.rdf.model.Model;
import org.apache.camel.Exchange;
import won.node.camel.processor.AbstractCamelProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;

import java.net.URI;

/**
 * Sends a error response message back to the sender of the original message, if that message was sent on
 * behalf of a specified need (i.e. its senderNeedURI is set).
 */
public class FailResponder extends AbstractCamelProcessor
{
  @Override
  public void process(final Exchange exchange) throws Exception {
    WonMessage originalMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.ORIGINAL_MESSAGE_HEADER);
    if (originalMessage == null){
      originalMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
    }

    logger.error("an error occurred while processing WON message");
    Exception e = (Exception) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
    String errormessage = null;
    if (e != null){
      errormessage = e.getMessage();
    } else {
      errormessage = String.format("An error occurred while processing message %s", originalMessage.getMessageURI());
    }
    if (originalMessage == null) return;
    //if (originalMessage == null) throw new WonMessageProcessingException("did not find the original message in the " +
    //  "exchange header '" + WonCamelConstants.WON_MESSAGE_HEADER +"'");
    //only send success message if the original message was sent on behalf of a need (otherwise we have to find out
    // with other means which ownerapplications to send the response to.
    //originalMessage.getCompleteDataset();
    //if (originalMessage.getSenderURI() == null) return;
    URI newMessageURI = this.wonNodeInformationService.generateEventURI();
    Model errorMessageContent = WonRdfUtils.MessageUtils.textMessage(errormessage);
    RdfUtils.replaceBaseURI(errorMessageContent, newMessageURI.toString());
    WonMessage responseMessage = new WonMessageBuilder()
            .setPropertiesForNodeResponse(originalMessage, false,newMessageURI)
            .addContent(
              errorMessageContent,
                    null)
            .build();

    if (WonMessageDirection.FROM_OWNER == originalMessage.getEnvelopeType()){
      sendSystemMessageToOwner(responseMessage);
    } else if (WonMessageDirection.FROM_EXTERNAL == originalMessage.getEnvelopeType()){
      sendSystemMessageToRemoteNode(responseMessage);
    }
  }
}
