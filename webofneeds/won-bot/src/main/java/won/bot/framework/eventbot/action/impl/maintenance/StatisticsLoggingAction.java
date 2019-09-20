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
package won.bot.framework.eventbot.action.impl.maintenance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.bus.impl.EventBusStatistics;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.listener.EventListener;

import java.lang.invoke.MethodHandles;
import java.util.Comparator;

/*
 * Collects the EventBusStatistics and logs them.
 */
public class StatisticsLoggingAction extends BaseEventBotAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public StatisticsLoggingAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        EventBus bus = getEventListenerContext().getEventBus();
        EventBusStatistics statistics = bus.generateEventBusStatistics();
        StringBuilder sb = new StringBuilder();
        sb.append("\nEvent bus statistics: \n").append("number of listeners: ").append(statistics.getListenerCount())
                        .append("\n").append("number of listeners per listener class:\n");
        statistics.getListenerCountPerListenerClass().entrySet().stream()
                        .sorted(Comparator.comparing(e -> e.getKey().getName()))
                        .forEach(e -> sb.append(e.getKey().getName()).append(": ").append(e.getValue()).append("\n"));
        sb.append("number of listeners per event class:\n");
        statistics.getListenerCountPerEvent().entrySet().stream()
                        .sorted(Comparator.comparing(e -> e.getKey().getName()))
                        .forEach(e -> sb.append(e.getKey()).append(": ").append(e.getValue()).append("\n"));
        logger.info(sb.toString());
    }
}
