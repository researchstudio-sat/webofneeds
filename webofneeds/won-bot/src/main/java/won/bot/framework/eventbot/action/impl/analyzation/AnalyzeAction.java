/*
 * Copyright 2017  Research Studios Austria Forschungsges.m.b.H.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */

package won.bot.framework.eventbot.action.impl.analyzation;

import won.bot.framework.bot.context.BotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.analyzation.agreement.AgreementAcceptedEvent;
import won.bot.framework.eventbot.event.impl.analyzation.agreement.AgreementCanceledEvent;
import won.bot.framework.eventbot.event.impl.analyzation.agreement.AgreementErrorEvent;
import won.bot.framework.eventbot.event.impl.analyzation.precondition.PreconditionMetEvent;
import won.bot.framework.eventbot.event.impl.analyzation.precondition.PreconditionUnmetEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.OpenFromOtherNeedEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;

/**
 * Checks if the received hint is for a factoryURI
 */
public class AnalyzeAction extends BaseEventBotAction {
    public AnalyzeAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        //TODO: Implement the analyzation accordingly, currently we will push the events by sending the event givien as a textmessage (for debug purposes)
        BotContextWrapper botContextWrapper = getEventListenerContext().getBotContextWrapper();
        EventBus bus = getEventListenerContext().getEventBus();

        if(event instanceof OpenFromOtherNeedEvent){
            logger.info("Analyzing OpenFromOtherNeedEvent");
            OpenFromOtherNeedEvent openFromOtherNeedEvent = (OpenFromOtherNeedEvent) event;
            Connection con = openFromOtherNeedEvent.getCon();
            /*TODO: currently dont do anything special for OpenFromOtherNeedEvent, when the analyzer is finished there will not be a need for splitting those two events anymore*/
        }else if(event instanceof MessageFromOtherNeedEvent) {
            logger.info("Analyzing MessageFromOtherNeedEvent");
            MessageFromOtherNeedEvent messageFromOtherNeedEvent = (MessageFromOtherNeedEvent) event;

            String textMessage = WonRdfUtils.MessageUtils.getTextMessage(messageFromOtherNeedEvent.getWonMessage());
            Connection con = messageFromOtherNeedEvent.getCon();

            if("PreconditionMetEvent".equals(textMessage)){
                bus.publish(new PreconditionMetEvent(con, new Object()));
            }else if("PrecondtionUnmetEvent".equals(textMessage)){
                bus.publish(new PreconditionUnmetEvent(con, new Object()));
            } else if("AgreementCanceledEvent".equals(textMessage)){
                bus.publish(new AgreementCanceledEvent(con, new Object()));
            }else if("AgreementAcceptedEvent".equals(textMessage)){
                bus.publish(new AgreementAcceptedEvent(con, new Object()));
            }else if("AgreementErrorEvent".equals(textMessage)){
                bus.publish(new AgreementErrorEvent(con, new Object()));
            }
        } else {
            logger.error("AnalyzeAction can only handle MessageFromOtherNeedEvent and OpenFromOtherNeedEvent");
            return;
        }
    }
}
