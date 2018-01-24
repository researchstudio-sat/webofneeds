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

import org.apache.jena.query.Dataset;
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
import won.bot.framework.eventbot.event.impl.wonmessage.WonMessageReceivedOnConnectionEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.utils.goals.GoalInstantiationProducer;
import won.utils.goals.GoalInstantiationResult;

import java.util.Collection;

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
        EventListenerContext ctx = getEventListenerContext();
        BotContextWrapper botContextWrapper = ctx.getBotContextWrapper();
        EventBus bus = ctx.getEventBus();
        LinkedDataSource linkedDataSource = ctx.getLinkedDataSource();

        if(event instanceof WonMessageReceivedOnConnectionEvent){
            WonMessageReceivedOnConnectionEvent receivedOnConnectionEvent = (WonMessageReceivedOnConnectionEvent) event;
            Dataset needDataset = linkedDataSource.getDataForResource(receivedOnConnectionEvent.getNeedURI());
            Dataset remoteNeedDataset = linkedDataSource.getDataForResource(receivedOnConnectionEvent.getRemoteNeedURI());
            Dataset connectionDataset = linkedDataSource.getDataForResource(receivedOnConnectionEvent.getConnectionURI());

            GoalInstantiationProducer goalInstantiationProducer = new GoalInstantiationProducer(needDataset, remoteNeedDataset, connectionDataset, "http://example.org/","http://example.org/blended/");
            Collection<GoalInstantiationResult> results = goalInstantiationProducer.createAllGoalCombinationInstantiationResults();
            Connection con = ((WonMessageReceivedOnConnectionEvent) event).getCon();

            for(GoalInstantiationResult result : results) {
                if(result.getShaclReportWrapper().isConform()){
                    bus.publish(new PreconditionMetEvent(con, result));
                }else{
                    bus.publish(new PreconditionUnmetEvent(con, result));
                }
            }
            //TODO: THIS IS SOLELY FOR DEBUG PURPOSES NOW
            if(event instanceof MessageFromOtherNeedEvent) {
                logger.info("Analyzing MessageFromOtherNeedEvent");
                MessageFromOtherNeedEvent messageFromOtherNeedEvent = (MessageFromOtherNeedEvent) event;

                String textMessage = WonRdfUtils.MessageUtils.getTextMessage(messageFromOtherNeedEvent.getWonMessage());

                if("PreconditionMetEvent".equals(textMessage)){
                    bus.publish(new PreconditionMetEvent(con, null));
                }else if("PrecondtionUnmetEvent".equals(textMessage)){
                    bus.publish(new PreconditionUnmetEvent(con, null));
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
        } else {
            logger.error("AnalyzeAction can only handle WonMessageReceivedOnConnectionEvent");
            return;
        }
    }
}
