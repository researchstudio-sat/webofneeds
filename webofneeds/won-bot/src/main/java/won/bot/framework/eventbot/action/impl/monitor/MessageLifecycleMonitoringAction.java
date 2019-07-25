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

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.Charsets;
import org.apache.jena.query.Dataset;
import org.apache.jena.riot.Lang;
import org.apache.jena.sparql.core.Quad;
import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.javasimon.Stopwatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.monitor.CrawlDoneEvent;
import won.bot.framework.eventbot.event.impl.monitor.CrawlReadyEvent;
import won.bot.framework.eventbot.event.impl.monitor.MessageDispatchStartedEvent;
import won.bot.framework.eventbot.event.impl.monitor.MessageDispatchedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.DeliveryResponseEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageSpecificEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.SuccessResponseEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.message.WonMessage;
import won.protocol.util.RdfUtils;

public class MessageLifecycleMonitoringAction extends BaseEventBotAction {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    Map<String, Split> msgSplitsB = Collections.synchronizedMap(new HashMap<>());
    Map<String, Split> msgSplitsBC = Collections.synchronizedMap(new HashMap<>());
    Map<String, Split> msgSplitsBCD = Collections.synchronizedMap(new HashMap<>());
    Map<String, Split> msgSplitsBCDE = Collections.synchronizedMap(new HashMap<>());
    Map<URI, URI> connectionMsgUris = Collections.synchronizedMap(new HashMap<>());
    Map<URI, URI> responseMsgUris = Collections.synchronizedMap(new HashMap<>());
    private long startTestTime = -1;

    public MessageLifecycleMonitoringAction(final EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(final Event event, EventListener executingListener) throws Exception {
        Stopwatch stopwatchB = SimonManager.getStopwatch("messageTripB");
        Stopwatch stopwatchBC = SimonManager.getStopwatch("messageTripBC");
        Stopwatch stopwatchBCD = SimonManager.getStopwatch("messageTripBCD");
        Stopwatch stopwatchBCDE = SimonManager.getStopwatch("messageTripBCDE");
        if (event instanceof MessageSpecificEvent) {
            MessageSpecificEvent msgEvent = (MessageSpecificEvent) event;
            URI msgURI = msgEvent.getMessageURI();
            logger.debug("RECEIVED EVENT {} for uri {}", event, msgURI);
            if (event instanceof MessageDispatchStartedEvent) {
                Split splitB = stopwatchB.start();
                Split splitBC = stopwatchBC.start();
                Split splitBCD = stopwatchBCD.start();
                Split splitBCDE = stopwatchBCDE.start();
                msgSplitsB.put(msgURI.toString(), splitB);
                msgSplitsBC.put(msgURI.toString(), splitBC);
                msgSplitsBCD.put(msgURI.toString(), splitBCD);
                msgSplitsBCDE.put(msgURI.toString(), splitBCDE);
                connectionMsgUris.put(msgURI, msgEvent.getAtomURI());
            } else if (event instanceof MessageDispatchedEvent) {
                msgSplitsB.get(msgURI.toString()).stop();
            }
        } else if (event instanceof SuccessResponseEvent || event instanceof FailureResponseEvent) {
            DeliveryResponseEvent responseEvent = (DeliveryResponseEvent) event;
            if (connectionMsgUris.keySet().contains(responseEvent.getOriginalMessageURI())
                            || connectionMsgUris.keySet().contains(responseEvent.getRemoteResponseToMessageURI())) {
                responseMsgUris.put(responseEvent.getMessage().getMessageURI(), responseEvent.getAtomURI());
                if (responseEvent.isRemoteResponse()) {
                    responseMsgUris.put(responseEvent.getMessage().getCorrespondingRemoteMessageURI(),
                                    responseEvent.getTargetAtomURI());
                }
            }
            if (responseEvent.isRemoteResponse()) {
                if (msgSplitsBC.get(responseEvent.getRemoteResponseToMessageURI().toString()) != null) {
                    logger.debug("RECEIVED REMOTE RESPONSE EVENT {} for uri {}", event,
                                    responseEvent.getRemoteResponseToMessageURI());
                    msgSplitsBCDE.get(responseEvent.getRemoteResponseToMessageURI().toString()).stop();
                }
            } else if (msgSplitsBC.get(responseEvent.getOriginalMessageURI().toString()) != null) {
                logger.debug("RECEIVED RESPONSE EVENT {} for uri {}", event, responseEvent.getOriginalMessageURI());
                msgSplitsBC.get(responseEvent.getOriginalMessageURI().toString()).stop();
            }
        } else if (event instanceof MessageFromOtherAtomEvent) {
            WonMessage msg = ((MessageFromOtherAtomEvent) event).getWonMessage();
            URI remoteMessageURI = msg.getCorrespondingRemoteMessageURI();
            msgSplitsBCD.get(remoteMessageURI.toString()).stop();
            connectionMsgUris.put(msg.getMessageURI(), msg.getRecipientAtomURI());
        } else if (event instanceof CrawlReadyEvent) {
            reportMessageSizes(connectionMsgUris, "Connection Messages");
            reportMessageSizes(responseMsgUris, "Delivery Responses");
            getEventListenerContext().getEventBus().publish(new CrawlDoneEvent());
        }
    }

    private void reportMessageSizes(final Map<URI, URI> msgUris, String name) {
        int[] counter = new int[4];
        Set<URI> keys = msgUris.keySet();
        for (URI uri : keys) {
            Dataset dataset = getEventListenerContext().getLinkedDataSource().getDataForResource(uri, msgUris.get(uri));
            record(dataset, counter);
        }
        String sizeInfo = "\nSIZES for " + name + ":\n" + "messages=" + counter[0] + ", named-graphs=" + counter[1]
                        + ", " + "quads=" + counter[2] + ", bytes-in-Trig-UTF8=" + counter[3];
        logger.info(sizeInfo);
    }

    private void record(final Dataset dataset, final int[] counter) {
        counter[0]++;
        counter[1] = counter[1] + RdfUtils.getModelNames(dataset).size();
        Iterator<Quad> quadsIterator = dataset.asDatasetGraph().find();
        while (quadsIterator.hasNext()) {
            quadsIterator.next();
            counter[2]++;
        }
        counter[3] = counter[3] + RdfUtils.writeDatasetToString(dataset, Lang.TRIG).getBytes(Charsets.UTF_8).length;
    }
}
