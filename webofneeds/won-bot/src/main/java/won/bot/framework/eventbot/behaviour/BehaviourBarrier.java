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

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.filter.EventFilter;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;

/**
 * User: fkleedorfer Date: 29.08.2017
 */
public class BehaviourBarrier extends BotBehaviour {
    private Set<BotBehaviour> behavioursToWaitFor = Collections.synchronizedSet(new HashSet<>());
    private Set<BotBehaviour> behavioursToStart = Collections.synchronizedSet(new HashSet<>());

    public BehaviourBarrier(EventListenerContext context) {
        super(context);
    }

    public BehaviourBarrier(EventListenerContext context, String name) {
        super(context, name);
    }

    public void waitFor(BotBehaviour botBehaviour) {
        this.behavioursToWaitFor.add(botBehaviour);
    }

    public void thenStart(BotBehaviour botBehaviour) {
        this.behavioursToStart.add(botBehaviour);
    }

    @Override
    protected void onActivate(Optional<Object> message) {
        Set<BotBehaviour> deactivatedBehaviours = Collections.synchronizedSet(new HashSet<>());
        subscribeWithAutoCleanup(BotBehaviourDeactivatedEvent.class,
                new ActionOnEventListener(context, new EventFilter() {
                    @Override
                    public boolean accept(Event event) {
                        if (!(event instanceof BotBehaviourDeactivatedEvent))
                            return false;
                        return behavioursToWaitFor.contains(((BotBehaviourDeactivatedEvent) event).getBehaviour());
                    }
                }, new BaseEventBotAction(context) {
                    @Override
                    protected void doRun(Event event, EventListener executingListener) throws Exception {
                        synchronized (behavioursToWaitFor) {
                            deactivatedBehaviours.add(((BotBehaviourDeactivatedEvent) event).getBehaviour());
                            if (deactivatedBehaviours.containsAll(behavioursToWaitFor)) {
                                behavioursToStart.forEach(behaviour -> behaviour.activate());
                                deactivate();
                            }
                        }
                    }
                }));
    }
}
