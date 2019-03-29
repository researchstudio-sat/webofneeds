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
import won.bot.framework.eventbot.action.impl.needlifecycle.AbstractCreateNeedAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.NeedCreationFailedEvent;
import won.bot.framework.eventbot.event.NeedSpecificEvent;
import won.bot.framework.eventbot.event.impl.command.replace.ReplaceCommandEvent;
import won.bot.framework.eventbot.event.impl.debugbot.ConnectDebugCommandEvent;
import won.bot.framework.eventbot.event.impl.debugbot.HintDebugCommandEvent;
import won.bot.framework.eventbot.event.impl.debugbot.NeedCreatedEventForDebugConnect;
import won.bot.framework.eventbot.event.impl.debugbot.NeedCreatedEventForDebugHint;
import won.bot.framework.eventbot.event.impl.debugbot.ReplaceDebugNeedContentCommandEvent;
import won.bot.framework.eventbot.event.impl.matcher.NeedCreatedEventForMatcher;
import won.bot.framework.eventbot.event.impl.needlifecycle.NeedCreatedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.FailureResponseEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.message.WonMessage;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.DefaultNeedModelWrapper;
import won.protocol.util.NeedModelWrapper;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WON;

/**
 * Creates a need with the specified facets. If no facet is specified, the
 * chatFacet will be used.
 */
public class ReplaceDebugNeedContentAction extends BaseEventBotAction {
    private Counter counter = new CounterImpl("DebugNeedsReplaceCounter");

    public ReplaceDebugNeedContentAction(final EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        if (!(event instanceof ReplaceDebugNeedContentCommandEvent)) {
            logger.warn("could not process event {}, expecting only ReplaceDebugCommandEvent events");
            return;
        }
        ReplaceDebugNeedContentCommandEvent replaceDebugCommandEvent = (ReplaceDebugNeedContentCommandEvent) event;
        URI myNeedUri = replaceDebugCommandEvent.getCon().getNeedURI();
        String replyText = "";
        Dataset needDataset = getEventListenerContext().getLinkedDataSource().getDataForResource(myNeedUri);
        String titleString = null;
        if (needDataset == null) {
            throw new IllegalStateException("Cannot edit my need " + myNeedUri + " : retrieved dataset is null");
        }
        DefaultNeedModelWrapper needModelWrapper = new DefaultNeedModelWrapper(needDataset);
        titleString = needModelWrapper.getSomeTitleFromIsOrAll("en", "de");
        if (titleString != null) {
            titleString = titleString.replaceFirst("( \\(edit #\\d+\\)|$)", " (edit #" + counter.increment() + ")");
        } else {
            titleString = "Debug Need (edit #1)";
        }
        needModelWrapper.setTitle(titleString);
        Dataset onlyContentGraphDataset = DatasetFactory.createGeneral();
        onlyContentGraphDataset.setDefaultModel(needModelWrapper.getNeedModel());
        EventListenerContext ctx = getEventListenerContext();
        EventBus bus = ctx.getEventBus();
        final URI wonNodeUri = URI.create(needModelWrapper.getWonNodeUri());
        logger.debug("replacing need on won node {} with content {} ", wonNodeUri,
                        StringUtils.abbreviate(RdfUtils.toString(onlyContentGraphDataset), 150));
        bus.publish(new ReplaceCommandEvent(onlyContentGraphDataset));
    }
}
