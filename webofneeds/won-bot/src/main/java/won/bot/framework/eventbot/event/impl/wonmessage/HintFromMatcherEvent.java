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

package won.bot.framework.eventbot.event.impl.wonmessage;

import won.bot.framework.eventbot.event.BaseEvent;
import won.bot.framework.eventbot.event.MessageEvent;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageType;
import won.protocol.model.Match;

public class HintFromMatcherEvent extends BaseEvent  implements MessageEvent {
    private final Match match;
    final WonMessage wonMessage;

    public HintFromMatcherEvent(final Match match, final WonMessage wonMessage) {
        this.match = match;
        this.wonMessage = wonMessage;
    }

    public Match getMatch() {
      return match;
    }

    public WonMessage getWonMessage() {
        return wonMessage;
    }

    @Override
    public WonMessageType getWonMessageType() {
        return this.wonMessage.getMessageType();
    }
}
