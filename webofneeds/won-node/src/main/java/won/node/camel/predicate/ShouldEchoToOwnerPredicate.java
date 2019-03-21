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

package won.node.camel.predicate;


import org.apache.camel.Exchange;
import org.apache.camel.Predicate;

import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageType;
import won.protocol.message.processor.camel.WonCamelConstants;

/**
 * True if the WonMessage in the wonOriginalMessage header meets the criteria for echoing it to the owner:
 * - sent FROM_SYSTEM or FROM_OWNER
 * - if it is a response, it is not directed at the remote need
 */
public class ShouldEchoToOwnerPredicate implements Predicate {

    @Override
    public boolean matches(Exchange exchange) {
        WonMessage message = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.ORIGINAL_MESSAGE_HEADER);
        if (message == null) return false;
        if (message.getEnvelopeType() == WonMessageDirection.FROM_EXTERNAL) return false;        
        if (
            (message.getMessageType() == WonMessageType.SUCCESS_RESPONSE || message.getMessageType() == WonMessageType.FAILURE_RESPONSE) 
            && (message.getSenderNeedURI() != null && ! message.getSenderNeedURI().equals(message.getReceiverNeedURI()))) {
            //responses directed at remote needs are not to be echoed to the owner
            return false;
        }
        return true;
    }
}
