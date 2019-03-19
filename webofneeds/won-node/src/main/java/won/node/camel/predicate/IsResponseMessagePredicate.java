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
import won.protocol.message.WonMessageType;
import won.protocol.message.processor.camel.WonCamelConstants;

/**
 * Predicate to check if the wonMessage is a response message.
 */
public class IsResponseMessagePredicate implements Predicate {
    @Override
    public boolean matches(final Exchange exchange) {
        WonMessage wonMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
        WonMessageType messageType = wonMessage.getMessageType();
        switch (messageType) {
        case SUCCESS_RESPONSE:
            return true;
        case FAILURE_RESPONSE:
            return true;
        }
        return false;
    }
}
