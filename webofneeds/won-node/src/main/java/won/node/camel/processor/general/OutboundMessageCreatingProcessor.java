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

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;

/**
 * Processor that expects an outgoingMessageFactory in the respecitve in header, uses it to
 * generate the outgoing message and places the new message in the WonOutgoingMessage header
 */
public class OutboundMessageCreatingProcessor implements Processor {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Override public void process(Exchange exchange) throws Exception {
    WonMessage wonMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);

    if (wonMessage == null) {
      logger.debug("did not find a WonMessage in header {}, this is unexpected ", WonCamelConstants.MESSAGE_HEADER);
      return;
    }
    //remove the factory from the camel message so it does not slow down the rest of the processing chain
    Object factory = exchange.getIn().removeHeader(WonCamelConstants.OUTBOUND_MESSAGE_FACTORY_HEADER);
    if (factory == null) {
      logger.debug("did not find an outbound message for message {} in header {}, this is unexpected ",
          wonMessage.getMessageURI(), WonCamelConstants.OUTBOUND_MESSAGE_FACTORY_HEADER);
      return;
    }
    OutboundMessageFactoryProcessor factoryProcessor = (OutboundMessageFactoryProcessor) factory;
    WonMessage outboundMessage = factoryProcessor.process(wonMessage);
    if (outboundMessage == null) {
      logger.debug("factory did not produce an outgoing WonMessage based on WonMessage {}, this is unexpected",
          wonMessage.getMessageURI());
    }
    exchange.getIn().setHeader(WonCamelConstants.OUTBOUND_MESSAGE_HEADER, outboundMessage);

  }
}
