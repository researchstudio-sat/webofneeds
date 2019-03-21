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
import won.node.camel.processor.AbstractCamelProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;

/**
 * Sends the WonMessage found in the exchange's in (in the 'wonMessage' header) to
 * the respective remote WoN node or delivers it locally.
 */
public class ToNodeSender extends AbstractCamelProcessor {
  @Override public void process(final Exchange exchange) throws Exception {
    WonMessage message = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
    sendMessageToNode(message);
  }
}
