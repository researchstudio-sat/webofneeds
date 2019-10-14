/*
 * Copyright 2017 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.bot.framework.eventbot.behaviour;

import java.util.Optional;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.impl.wonmessage.execCommand.ExecuteCloseCommandAction;
import won.bot.framework.eventbot.event.impl.command.close.CloseCommandEvent;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;

/**
 * Behaviour that responds to a CloseCommand by executing the
 * ExecuteCommandAction
 */
public class CloseBehaviour extends BotBehaviour {
    public CloseBehaviour(EventListenerContext context) {
        super(context);
    }

    public CloseBehaviour(EventListenerContext context, String name) {
        super(context, name);
    }

    @Override
    protected void onActivate(Optional<Object> message) {
        this.subscribeWithAutoCleanup(CloseCommandEvent.class,
                        new ActionOnEventListener(context, new ExecuteCloseCommandAction(context)));
    }
}
