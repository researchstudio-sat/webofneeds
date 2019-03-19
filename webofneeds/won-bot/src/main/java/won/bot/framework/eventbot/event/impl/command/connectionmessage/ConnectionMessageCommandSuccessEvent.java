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

package won.bot.framework.eventbot.event.impl.command.connectionmessage;

import won.bot.framework.eventbot.event.impl.command.MessageCommandSuccessEvent;
import won.bot.framework.eventbot.event.impl.command.base.AbstractMessageCommandResultEvent;
import won.protocol.message.WonMessage;

/**
 * Indicates that the bot has successfully sent a connection message.
 */
public class ConnectionMessageCommandSuccessEvent extends AbstractMessageCommandResultEvent
        implements MessageCommandSuccessEvent, ConnectionMessageCommandResultEvent {
    private WonMessage wonMessage;

    public ConnectionMessageCommandSuccessEvent(ConnectionMessageCommandEvent originalCommandEvent,
            WonMessage wonMessage) {
        super(originalCommandEvent, originalCommandEvent.getCon());
        this.wonMessage = wonMessage;
    }

    @Override
    public boolean isSuccess() {
        return true;
    }

    public WonMessage getWonMessage() {
        return wonMessage;
    }
}
