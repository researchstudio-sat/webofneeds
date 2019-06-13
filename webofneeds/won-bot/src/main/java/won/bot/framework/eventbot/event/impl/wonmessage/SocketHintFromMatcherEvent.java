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
package won.bot.framework.eventbot.event.impl.wonmessage;

import java.net.URI;

import won.bot.framework.eventbot.event.BaseEvent;
import won.bot.framework.eventbot.event.MessageEvent;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageType;

public class SocketHintFromMatcherEvent extends BaseEvent implements MessageEvent {
    final WonMessage wonMessage;
    double hintScore = 0;
    URI hintTargetSocket = null;
    URI recipientSocket = null;

    public SocketHintFromMatcherEvent(final WonMessage wonMessage) {
        this.wonMessage = wonMessage;
        this.hintScore = wonMessage.getHintScore();
        this.hintTargetSocket = wonMessage.getHintTargetSocketURI();
        this.recipientSocket = wonMessage.getRecipientSocketURI();
    }

    public double getHintScore() {
        return hintScore;
    }

    public URI getHintTargetSocket() {
        return hintTargetSocket;
    }

    public URI getRecipientSocket() {
        return recipientSocket;
    }

    public WonMessage getWonMessage() {
        return wonMessage;
    }

    @Override
    public WonMessageType getWonMessageType() {
        return this.wonMessage.getMessageType();
    }
}
