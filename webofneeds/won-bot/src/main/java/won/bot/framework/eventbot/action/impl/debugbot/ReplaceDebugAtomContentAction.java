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
package won.bot.framework.eventbot.action.impl.debugbot;

import java.net.URI;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.action.impl.counter.Counter;
import won.bot.framework.eventbot.action.impl.counter.CounterImpl;
import won.bot.framework.eventbot.action.impl.atomlifecycle.AbstractCreateAtomAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.AtomCreationFailedEvent;
import won.bot.framework.eventbot.event.AtomSpecificEvent;
import won.bot.framework.eventbot.event.impl.command.replace.ReplaceCommandEvent;
import won.bot.framework.eventbot.event.impl.debugbot.ConnectDebugCommandEvent;
import won.bot.framework.eventbot.event.impl.debugbot.HintDebugCommandEvent;
import won.bot.framework.eventbot.event.impl.debugbot.AtomCreatedEventForDebugConnect;
import won.bot.framework.eventbot.event.impl.debugbot.AtomCreatedEventForDebugHint;
import won.bot.framework.eventbot.event.impl.debugbot.ReplaceDebugAtomContentCommandEvent;
import won.bot.framework.eventbot.event.impl.matcher.AtomCreatedEventForMatcher;
import won.bot.framework.eventbot.event.impl.atomlifecycle.AtomCreatedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.message.WonMessage;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.DefaultAtomModelWrapper;
import won.protocol.util.AtomModelWrapper;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WON;

/**
 * Creates an atom with the specified sockets. If no socket is specified, the
 * chatSocket will be used.
 */
public class ReplaceDebugAtomContentAction extends BaseEventBotAction {
    private Counter counter = new CounterImpl("DebugAtomsReplaceCounter");

    public ReplaceDebugAtomContentAction(final EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        if (!(event instanceof ReplaceDebugAtomContentCommandEvent)) {
            logger.warn("could not process event {}, expecting only ReplaceDebugCommandEvent events");
            return;
        }
        ReplaceDebugAtomContentCommandEvent replaceDebugCommandEvent = (ReplaceDebugAtomContentCommandEvent) event;
        URI myAtomUri = replaceDebugCommandEvent.getCon().getAtomURI();
        String replyText = "";
        Dataset atomDataset = getEventListenerContext().getLinkedDataSource().getDataForResource(myAtomUri);
        String titleString = null;
        if (atomDataset == null) {
            throw new IllegalStateException("Cannot edit my atom " + myAtomUri + " : retrieved dataset is null");
        }
        DefaultAtomModelWrapper atomModelWrapper = new DefaultAtomModelWrapper(atomDataset);
        titleString = atomModelWrapper.getSomeTitleFromIsOrAll("en", "de");
        if (titleString != null) {
            titleString = titleString.replaceFirst("( \\(edit #\\d+\\)|$)", " (edit #" + counter.increment() + ")");
        } else {
            titleString = "Debug Atom (edit #1)";
        }
        atomModelWrapper.setTitle(titleString);
        Dataset onlyContentGraphDataset = DatasetFactory.createGeneral();
        onlyContentGraphDataset.setDefaultModel(atomModelWrapper.getAtomModel());
        EventListenerContext ctx = getEventListenerContext();
        EventBus bus = ctx.getEventBus();
        final URI wonNodeUri = URI.create(atomModelWrapper.getWonNodeUri());
        logger.debug("replacing atom on won node {} with content {} ", wonNodeUri,
                        StringUtils.abbreviate(RdfUtils.toString(onlyContentGraphDataset), 150));
        bus.publish(new ReplaceCommandEvent(onlyContentGraphDataset));
    }
}
