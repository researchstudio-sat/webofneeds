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
package won.bot.integrationtest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.listener.EventListener;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CyclicBarrier;

/**
 * Action that trips the specified barrier. Used to synchronize test execution
 * with bot execution.
 */
public class TripBarrierAction extends BaseEventBotAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private CyclicBarrier barrier;

    public TripBarrierAction(final EventListenerContext eventListenerContext, final CyclicBarrier barrier) {
        super(eventListenerContext);
        this.barrier = barrier;
    }

    @Override
    protected void doRun(final Event event, EventListener executingListener) throws Exception {
        try {
            // together with the barrier.await() in the @TestD method, this trips the
            // barrier
            // and both threads continue.
            barrier.await();
        } catch (Exception e) {
            logger.warn("caught exception while waiting on barrier", e);
        }
    }
}
