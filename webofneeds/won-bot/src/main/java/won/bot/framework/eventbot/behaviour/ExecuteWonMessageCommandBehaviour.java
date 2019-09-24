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
package won.bot.framework.eventbot.behaviour;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.impl.wonmessage.execCommand.*;
import won.bot.framework.eventbot.event.impl.command.MessageCommandFailureEvent;
import won.bot.framework.eventbot.event.impl.command.close.CloseCommandEvent;
import won.bot.framework.eventbot.event.impl.command.connect.ConnectCommandEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.event.impl.command.create.CreateAtomCommandEvent;
import won.bot.framework.eventbot.event.impl.command.deactivate.DeactivateAtomCommandEvent;
import won.bot.framework.eventbot.event.impl.command.feedback.FeedbackCommandEvent;
import won.bot.framework.eventbot.event.impl.command.open.OpenCommandEvent;
import won.bot.framework.eventbot.event.impl.command.replace.ReplaceCommandEvent;

import java.util.Optional;

/**
 * Behaviour that causes all WonMessageCommand events to be executed.
 */
public class ExecuteWonMessageCommandBehaviour extends BotBehaviour {
    public ExecuteWonMessageCommandBehaviour(EventListenerContext context) {
        super(context, "ExecuteWonMessageCommandBehaviour");
    }

    public ExecuteWonMessageCommandBehaviour(EventListenerContext context, String name) {
        super(context, name);
    }

    @Override
    protected void onActivate(Optional<Object> message) {
        this.subscribeWithAutoCleanup(CreateAtomCommandEvent.class, new ExecuteCreateAtomCommandAction(context));
        this.subscribeWithAutoCleanup(ReplaceCommandEvent.class, new ExecuteReplaceCommandAction(context));
        this.subscribeWithAutoCleanup(ConnectCommandEvent.class, new ExecuteConnectCommandAction(context));
        this.subscribeWithAutoCleanup(OpenCommandEvent.class, new ExecuteOpenCommandAction(context));
        this.subscribeWithAutoCleanup(ConnectionMessageCommandEvent.class,
                        new ExecuteConnectionMessageCommandAction(context));
        this.subscribeWithAutoCleanup(CloseCommandEvent.class, new ExecuteCloseCommandAction(context));
        this.subscribeWithAutoCleanup(DeactivateAtomCommandEvent.class,
                        new ExecuteDeactivateAtomCommandAction(context));
        this.subscribeWithAutoCleanup(FeedbackCommandEvent.class, new ExecuteFeedbackCommandAction(context));
        // if we receive a message command failure, log it
        this.subscribeWithAutoCleanup(MessageCommandFailureEvent.class, new LogMessageCommandFailureAction(context));
    }
}
