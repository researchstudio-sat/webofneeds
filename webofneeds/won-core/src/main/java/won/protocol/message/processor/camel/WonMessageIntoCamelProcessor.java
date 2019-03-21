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

package won.protocol.message.processor.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.jena.riot.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDecoder;
import won.protocol.message.processor.exception.WonMessageProcessingException;

import java.net.URI;
import java.util.Map;

/**
 * First processor for incoming messages. It expects a serialized WonMessage in
 * TriG format in the exchange's in, in the body or a WonMessage object in the
 * in header 'wonMessgage'. If that header is empty, the WonMessage found in the
 * body is deserialized and put into the in header 'wonMessage'. Moreover, the
 * 'messageType' header is set.
 * <p>
 * To avoid confusions, the body of the exchange's in is deleted.
 */
public class WonMessageIntoCamelProcessor implements Processor {
  Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public void process(final Exchange exchange) throws Exception {
    logger.debug("processing won message");
    Map headers = exchange.getIn().getHeaders();
    // if the wonMessage header is there, don't change it - that way we can re-route
    // internal messages
    WonMessage wonMessage = (WonMessage) headers.get(WonCamelConstants.MESSAGE_HEADER);
    if (wonMessage == null) {
      try {
        wonMessage = WonMessageDecoder.decode(Lang.TRIG, exchange.getIn().getBody().toString());
      } catch (Exception e) {
        // stop the exchange in this case - maybe at some point we can return a failure
        // response but
        // currently, we would have to look into the message for doing that, and looking
        // into
        // the message is not possible if we cannot decode it.
        logger.info(
            "could not decode message as TriG, ignoring it (the offending message is logged at loglevel 'DEBUG')", e);
        if (logger.isDebugEnabled()) {
          logger.debug("offending message: {}", exchange.getIn().getBody().toString());
        }

        exchange.setProperty(Exchange.ROUTE_STOP, Boolean.TRUE);

        throw new WonMessageProcessingException("Could not decode message", e);
      }
    }
    if (wonMessage == null) {
      throw new WonMessageProcessingException(
          "No WonMessage found in header '" + WonCamelConstants.MESSAGE_HEADER + "' or in the body");
    }
    exchange.getIn().setHeader(WonCamelConstants.MESSAGE_TYPE_HEADER,
        URI.create(wonMessage.getMessageType().getResource().getURI()));
    exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, wonMessage);
    exchange.getIn().setBody(null);
  }

}
