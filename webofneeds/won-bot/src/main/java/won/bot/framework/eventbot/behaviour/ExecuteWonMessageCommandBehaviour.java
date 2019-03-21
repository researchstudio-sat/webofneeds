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

package won.bot.framework.eventbot.behaviour;

import java.util.Optional;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.EventBotAction;
import won.bot.framework.eventbot.action.impl.wonmessage.execCommand.ExecuteCloseCommandAction;
import won.bot.framework.eventbot.action.impl.wonmessage.execCommand.ExecuteConnectCommandAction;
import won.bot.framework.eventbot.action.impl.wonmessage.execCommand.ExecuteConnectionMessageCommandAction;
import won.bot.framework.eventbot.action.impl.wonmessage.execCommand.ExecuteCreateNeedCommandAction;
import won.bot.framework.eventbot.action.impl.wonmessage.execCommand.ExecuteDeactivateNeedCommandAction;
import won.bot.framework.eventbot.action.impl.wonmessage.execCommand.ExecuteFeedbackCommandAction;
import won.bot.framework.eventbot.action.impl.wonmessage.execCommand.ExecuteOpenCommandAction;
import won.bot.framework.eventbot.action.impl.wonmessage.execCommand.LogMessageCommandFailureAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.MessageCommandFailureEvent;
import won.bot.framework.eventbot.event.impl.command.close.CloseCommandEvent;
import won.bot.framework.eventbot.event.impl.command.connect.ConnectCommandEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.event.impl.command.create.CreateNeedCommandEvent;
import won.bot.framework.eventbot.event.impl.command.deactivate.DeactivateNeedCommandEvent;
import won.bot.framework.eventbot.event.impl.command.feedback.FeedbackCommandEvent;
import won.bot.framework.eventbot.event.impl.command.open.OpenCommandEvent;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;

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
        linkEventToActionWithAutoCleanup(CreateNeedCommandEvent.class, new ExecuteCreateNeedCommandAction(context));
        linkEventToActionWithAutoCleanup(ConnectCommandEvent.class, new ExecuteConnectCommandAction(context));
        linkEventToActionWithAutoCleanup(OpenCommandEvent.class, new ExecuteOpenCommandAction(context));
        linkEventToActionWithAutoCleanup(ConnectionMessageCommandEvent.class, new ExecuteConnectionMessageCommandAction(context));
        linkEventToActionWithAutoCleanup(CloseCommandEvent.class, new ExecuteCloseCommandAction(context));
        linkEventToActionWithAutoCleanup(DeactivateNeedCommandEvent.class, new ExecuteDeactivateNeedCommandAction(context));
        linkEventToActionWithAutoCleanup(FeedbackCommandEvent.class, new ExecuteFeedbackCommandAction(context));
        //if we receive a message command failure, log it
        linkEventToActionWithAutoCleanup(MessageCommandFailureEvent.class, new LogMessageCommandFailureAction(context));
    }

    private void linkEventToActionWithAutoCleanup(Class<? extends Event> clazz, EventBotAction action) {
        this.subscribeWithAutoCleanup(clazz,
                new ActionOnEventListener(
                        context,
                        action
                )
        );
    }
}

