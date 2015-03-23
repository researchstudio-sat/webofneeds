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
import won.protocol.util.WonRdfUtils;

import java.net.URI;
import java.util.Map;

/**
 * First processor for incoming messages. It expects a serialized WonMessage
 * in TriG format in the exchange's in, in the body.
 * The WonMessage is deserialized and put into the in header 'wonMessage'.
 * Moreover, the headers 'facetType' and 'messageType' are set.
 */
public class WonMessageIntoCamelProcessor implements Processor
{
  Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public void process(final Exchange exchange) throws Exception {
    logger.debug("processing won message");
    Map headers = exchange.getIn().getHeaders();
    WonMessage wonMessage = WonMessageDecoder.decode(Lang.TRIG, exchange.getIn().getBody().toString());
    exchange.getIn().setHeader("messageType", URI.create(wonMessage.getMessageType().getResource().getURI()));
    exchange.getIn().setHeader("facetType", WonRdfUtils.FacetUtils.getFacet(wonMessage));
    exchange.getIn().setHeader("wonMessage", wonMessage);
  }

}
