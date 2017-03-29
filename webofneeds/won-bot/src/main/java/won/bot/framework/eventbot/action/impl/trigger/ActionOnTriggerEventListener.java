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

package won.bot.framework.eventbot.action.impl.trigger;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.EventBotAction;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;

/**
 * Executes the action each time the specified trigger publishes its BotTriggerEvent.
 */
public class ActionOnTriggerEventListener extends ActionOnEventListener {
    public ActionOnTriggerEventListener(EventListenerContext context, BotTrigger botTrigger, EventBotAction task) {
        super(context, new BotTriggerFilter(botTrigger), task);
    }

    public ActionOnTriggerEventListener(EventListenerContext context, String name, BotTrigger botTrigger, EventBotAction task) {
        super(context, name, new BotTriggerFilter(botTrigger), task);
    }

    public ActionOnTriggerEventListener(EventListenerContext context, BotTrigger botTrigger, EventBotAction task, int timesToRun) {
        super(context, new BotTriggerFilter(botTrigger), task, timesToRun);
    }

    public ActionOnTriggerEventListener(EventListenerContext context, String name, BotTrigger botTrigger, EventBotAction task, int timesToRun) {
        super(context, name, new BotTriggerFilter(botTrigger), task, timesToRun);
    }
}
