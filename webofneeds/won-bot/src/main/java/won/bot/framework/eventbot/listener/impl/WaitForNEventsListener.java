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
package won.bot.framework.eventbot.listener.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.filter.EventFilter;
import won.bot.framework.eventbot.listener.AbstractDoOnceAfterNEventsListener;

import java.lang.invoke.MethodHandles;

/**
 * Listener that waits for N events, then publishes a FinishedEvent.
 */
public class WaitForNEventsListener extends AbstractDoOnceAfterNEventsListener {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public WaitForNEventsListener(final EventListenerContext context, final int targetCount) {
        super(context, targetCount);
    }

    public WaitForNEventsListener(final EventListenerContext context, final EventFilter eventFilter,
                    final int targetCount) {
        super(context, eventFilter, targetCount);
    }

    public WaitForNEventsListener(final EventListenerContext context, final String name, final int targetCount) {
        super(context, name, targetCount);
    }

    public WaitForNEventsListener(final EventListenerContext context, final String name, final EventFilter eventFilter,
                    final int targetCount) {
        super(context, name, eventFilter, targetCount);
    }

    @Override
    protected void unsubscribe() {
        getEventListenerContext().getEventBus().unsubscribe(this);
    }

    @Override
    protected void doOnce(final Event event) throws Exception {
        logger.debug("Finished waiting for {} events", getTargetCount());
    }
}
