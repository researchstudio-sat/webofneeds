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
package won.bot.framework.eventbot.event.impl.command.feedback;

import java.net.URI;

import won.bot.framework.eventbot.event.impl.command.MessageCommandEvent;
import won.bot.framework.eventbot.event.impl.command.MessageCommandFailureEvent;
import won.bot.framework.eventbot.event.impl.command.base.AbstractMessageCommandResultEvent;
import won.protocol.model.Connection;

/**
 * Indicates that the bot has successfully sent a connect message, thereby
 * creating a connection.
 */
public class FeedbackCommandFailureEvent extends AbstractMessageCommandResultEvent
                implements MessageCommandFailureEvent, FeedbackCommandResultEvent {
    public FeedbackCommandFailureEvent(MessageCommandEvent originalCommandEvent, Connection con) {
        super(originalCommandEvent, con);
    }

    public FeedbackCommandFailureEvent(MessageCommandEvent originalCommandEvent, URI needURI, URI remoteNeedURI,
                    URI connectionURI) {
        super(originalCommandEvent, needURI, remoteNeedURI, connectionURI);
    }

    public FeedbackCommandFailureEvent(MessageCommandEvent originalCommandEvent, URI needURI, URI remoteNeedURI,
                    URI connectionURI, String message) {
        super(originalCommandEvent, needURI, remoteNeedURI, connectionURI, message);
    }

    public FeedbackCommandFailureEvent(MessageCommandEvent originalCommandEvent, Connection con, String message) {
        super(originalCommandEvent, con, message);
    }

    @Override
    public boolean isSuccess() {
        return false;
    }
}
