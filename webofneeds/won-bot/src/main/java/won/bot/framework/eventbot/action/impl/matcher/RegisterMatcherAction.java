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
package won.bot.framework.eventbot.action.impl.matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.EventBotActionUtils;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.matcher.MatcherRegisterFailedEvent;
import won.bot.framework.eventbot.listener.EventListener;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * User: fkleedorfer Date: 28.03.14
 */
public class RegisterMatcherAction extends BaseEventBotAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private List<URI> registeredNodes = new LinkedList<>();

    public RegisterMatcherAction(final EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        final Iterator<?> wonNodeUriIterator = getEventListenerContext().getMatcherNodeURISource().getNodeURIIterator();
        while (wonNodeUriIterator.hasNext()) {
            URI wonNodeUri = (URI) wonNodeUriIterator.next();
            try {
                if (!registeredNodes.contains(wonNodeUri)) {
                    logger.debug("registering matcher on won node {}", wonNodeUri);
                    EventBotActionUtils.rememberInNodeListIfNamePresent(getEventListenerContext(), wonNodeUri);
                    getEventListenerContext().getMatcherProtocolMatcherService().register(wonNodeUri);
                    registeredNodes.add(wonNodeUri);
                    logger.info("matcher registered on won node {}", wonNodeUri);
                }
            } catch (Exception e) {
                logger.warn("Error registering matcher at won node {}. Try again later ... Exception was {}",
                                wonNodeUri, e);
                getEventListenerContext().getEventBus().publish(new MatcherRegisterFailedEvent(wonNodeUri));
            }
        }
    }
}
