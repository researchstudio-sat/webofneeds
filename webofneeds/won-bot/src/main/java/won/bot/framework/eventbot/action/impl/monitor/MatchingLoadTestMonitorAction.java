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
package won.bot.framework.eventbot.action.impl.monitor;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.javasimon.Stopwatch;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.needlifecycle.NeedCreatedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.HintFromMatcherEvent;
import won.bot.framework.eventbot.listener.EventListener;

/**
 * Created by hfriedrich on 02.10.2015.
 */
public class MatchingLoadTestMonitorAction extends BaseEventBotAction {
    Map<String, Long> needEventStartTime = Collections.synchronizedMap(new HashMap<>());
    Map<String, List<Long>> hintEventReceivedTime = Collections.synchronizedMap(new HashMap<>());
    Map<String, Split> needSplits = Collections.synchronizedMap(new HashMap<>());
    private long startTestTime = -1;

    public MatchingLoadTestMonitorAction(final EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(final Event event, EventListener executingListener) throws Exception {
        Stopwatch stopwatch = SimonManager.getStopwatch("needHintFullRoundtrip");
        if (event instanceof NeedCreatedEvent) {
            Split split = stopwatch.start();
            needSplits.put(((NeedCreatedEvent) event).getNeedURI().toString(), split);
            logger.info("RECEIVED EVENT {} for uri {}", event, ((NeedCreatedEvent) event).getNeedURI().toString());
            long startTime = System.currentTimeMillis();
            String needUri = ((NeedCreatedEvent) event).getNeedURI().toString();
            needEventStartTime.put(needUri, startTime);
        } else if (event instanceof HintFromMatcherEvent) {
            logger.info("RECEIVED EVENT {} for uri {}", event,
                            ((HintFromMatcherEvent) event).getMatch().getFromNeed().toString());
            long hintReceivedTime = System.currentTimeMillis();
            String needUri = ((HintFromMatcherEvent) event).getMatch().getFromNeed().toString();
            needSplits.get(((HintFromMatcherEvent) event).getMatch().getFromNeed().toString()).stop();
            if (hintEventReceivedTime.get(needUri) == null) {
                hintEventReceivedTime.put(needUri, new LinkedList<Long>());
            }
            hintEventReceivedTime.get(needUri).add(hintReceivedTime);
        }
        if (startTestTime == -1) {
            startTestTime = System.currentTimeMillis();
        }
        logger.info("Number of Needs: {}", needEventStartTime.size());
        logger.info("Number of Hints: {}", getTotalHints());
        logger.info("Number of Needs with Hints: {}", getNeedsWithHints());
        logger.info("Average Duration: {}", getAverageHintDuration());
        logger.info("Minimum Duration: {}", getMinHintDuration());
        logger.info("Maximum Duration: {}", getMaxHintDuration());
        logger.info("Needs with Hints per Second: {}", getNeedsWithNeedsPerSecond(startTestTime));
        logger.info("Hints per Second: {}", getHintsPerSecondThroughput(startTestTime));
    }

    private int getTotalHints() {
        int total = 0;
        for (List<Long> hintList : hintEventReceivedTime.values()) {
            total += hintList.size();
        }
        return total;
    }

    private long getAverageHintDuration() {
        long num = 0;
        long duration = 0;
        for (String needUri : hintEventReceivedTime.keySet()) {
            long started = needEventStartTime.get(needUri);
            for (Long received : hintEventReceivedTime.get(needUri)) {
                num++;
                duration += (received - started);
            }
        }
        return (num != 0) ? duration / num : 0;
    }

    private long getMinHintDuration() {
        long min = Long.MAX_VALUE;
        for (String needUri : hintEventReceivedTime.keySet()) {
            long started = needEventStartTime.get(needUri);
            for (Long received : hintEventReceivedTime.get(needUri)) {
                long duration = received - started;
                if (duration < min) {
                    min = duration;
                }
            }
        }
        return min;
    }

    private long getMaxHintDuration() {
        long max = 0;
        for (String needUri : hintEventReceivedTime.keySet()) {
            long started = needEventStartTime.get(needUri);
            for (Long received : hintEventReceivedTime.get(needUri)) {
                long duration = received - started;
                if (duration > max) {
                    max = duration;
                }
            }
        }
        return max;
    }

    private long getNeedsWithHints() {
        long numNeedsWithHints = 0;
        for (String needUri : hintEventReceivedTime.keySet()) {
            if (hintEventReceivedTime.get(needUri).size() != 0) {
                numNeedsWithHints++;
            }
        }
        return numNeedsWithHints;
    }

    private float getHintsPerSecondThroughput(long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        return ((float) getTotalHints() * 1000) / duration;
    }

    private float getNeedsWithNeedsPerSecond(long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        return ((float) getNeedsWithHints() * 1000) / duration;
    }
}
