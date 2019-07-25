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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.atomlifecycle.AtomCreatedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.AtomHintFromMatcherEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.SocketHintFromMatcherEvent;
import won.bot.framework.eventbot.listener.EventListener;

/**
 * Created by hfriedrich on 02.10.2015.
 */
public class MatchingLoadTestMonitorAction extends BaseEventBotAction {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    Map<String, Long> atomEventStartTime = Collections.synchronizedMap(new HashMap<>());
    Map<String, List<Long>> hintEventReceivedTime = Collections.synchronizedMap(new HashMap<>());
    Map<String, Split> atomSplits = Collections.synchronizedMap(new HashMap<>());
    private long startTestTime = -1;

    public MatchingLoadTestMonitorAction(final EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(final Event event, EventListener executingListener) throws Exception {
        Stopwatch stopwatch = SimonManager.getStopwatch("atomHintFullRoundtrip");
        if (event instanceof AtomCreatedEvent) {
            Split split = stopwatch.start();
            atomSplits.put(((AtomCreatedEvent) event).getAtomURI().toString(), split);
            logger.info("RECEIVED EVENT {} for uri {}", event, ((AtomCreatedEvent) event).getAtomURI().toString());
            long startTime = System.currentTimeMillis();
            String atomUri = ((AtomCreatedEvent) event).getAtomURI().toString();
            atomEventStartTime.put(atomUri, startTime);
        } else if (event instanceof AtomHintFromMatcherEvent) {
            String atomUri = ((AtomHintFromMatcherEvent) event).getRecipientAtom().toString();
            logger.info("RECEIVED EVENT {} for uri {}", event, atomUri);
            long hintReceivedTime = System.currentTimeMillis();
            atomSplits.get(atomUri).stop();
            if (hintEventReceivedTime.get(atomUri) == null) {
                hintEventReceivedTime.put(atomUri, new LinkedList<Long>());
            }
            hintEventReceivedTime.get(atomUri).add(hintReceivedTime);
        } else if (event instanceof SocketHintFromMatcherEvent) {
            String atomUri = ((SocketHintFromMatcherEvent) event).getRecipientSocket().toString();
            logger.info("RECEIVED EVENT {} for uri {}", event, atomUri);
            long hintReceivedTime = System.currentTimeMillis();
            atomSplits.get(atomUri).stop();
            if (hintEventReceivedTime.get(atomUri) == null) {
                hintEventReceivedTime.put(atomUri, new LinkedList<Long>());
            }
            hintEventReceivedTime.get(atomUri).add(hintReceivedTime);
        }
        if (startTestTime == -1) {
            startTestTime = System.currentTimeMillis();
        }
        logger.info("Number of Atoms: {}", atomEventStartTime.size());
        logger.info("Number of Hints: {}", getTotalHints());
        logger.info("Number of Atoms with Hints: {}", getAtomsWithHints());
        logger.info("Average Duration: {}", getAverageHintDuration());
        logger.info("Minimum Duration: {}", getMinHintDuration());
        logger.info("Maximum Duration: {}", getMaxHintDuration());
        logger.info("Atoms with Hints per Second: {}", getAtomsWithAtomsPerSecond(startTestTime));
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
        for (String atomUri : hintEventReceivedTime.keySet()) {
            long started = atomEventStartTime.get(atomUri);
            for (Long received : hintEventReceivedTime.get(atomUri)) {
                num++;
                duration += (received - started);
            }
        }
        return (num != 0) ? duration / num : 0;
    }

    private long getMinHintDuration() {
        long min = Long.MAX_VALUE;
        for (String atomUri : hintEventReceivedTime.keySet()) {
            long started = atomEventStartTime.get(atomUri);
            for (Long received : hintEventReceivedTime.get(atomUri)) {
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
        for (String atomUri : hintEventReceivedTime.keySet()) {
            long started = atomEventStartTime.get(atomUri);
            for (Long received : hintEventReceivedTime.get(atomUri)) {
                long duration = received - started;
                if (duration > max) {
                    max = duration;
                }
            }
        }
        return max;
    }

    private long getAtomsWithHints() {
        long numAtomsWithHints = 0;
        for (String atomUri : hintEventReceivedTime.keySet()) {
            if (hintEventReceivedTime.get(atomUri).size() != 0) {
                numAtomsWithHints++;
            }
        }
        return numAtomsWithHints;
    }

    private float getHintsPerSecondThroughput(long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        return ((float) getTotalHints() * 1000) / duration;
    }

    private float getAtomsWithAtomsPerSecond(long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        return ((float) getAtomsWithHints() * 1000) / duration;
    }
}
