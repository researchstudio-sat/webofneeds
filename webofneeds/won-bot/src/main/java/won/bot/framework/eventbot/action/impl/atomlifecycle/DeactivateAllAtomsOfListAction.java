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
package won.bot.framework.eventbot.action.impl.atomlifecycle;

import java.net.URI;
import java.util.Collection;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.atomlifecycle.AtomDeactivatedEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;

/**
 * User: fkleedorfer Date: 28.03.14
 */
public class DeactivateAllAtomsOfListAction extends BaseEventBotAction {
    private String uriListName;

    public DeactivateAllAtomsOfListAction(EventListenerContext eventListenerContext, String uriListName) {
        super(eventListenerContext);
        this.uriListName = uriListName;
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        EventListenerContext ctx = getEventListenerContext();
        Collection<URI> toDeactivate = ctx.getBotContext().getNamedAtomUriList(uriListName);
        for (URI uri : toDeactivate) {
            ctx.getWonMessageSender().sendWonMessage(createWonMessage(uri));
            ctx.getEventBus().publish(new AtomDeactivatedEvent(uri));
        }
    }

    private WonMessage createWonMessage(URI atomURI) throws WonMessageBuilderException {
        WonNodeInformationService wonNodeInformationService = getEventListenerContext().getWonNodeInformationService();
        URI localWonNode = WonRdfUtils.AtomUtils.getWonNodeURIFromAtom(
                        getEventListenerContext().getLinkedDataSource().getDataForResource(atomURI), atomURI);
        return WonMessageBuilder
                        .setMessagePropertiesForDeactivateFromOwner(
                                        wonNodeInformationService.generateEventURI(localWonNode), atomURI, localWonNode)
                        .build();
    }
}
