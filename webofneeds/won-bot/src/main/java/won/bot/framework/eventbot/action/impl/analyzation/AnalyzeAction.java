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
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import won.bot.framework.bot.context.BotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.analyzation.agreement.AgreementAcceptedEvent;
import won.bot.framework.eventbot.event.impl.analyzation.agreement.AgreementCanceledEvent;
import won.bot.framework.eventbot.event.impl.analyzation.precondition.PreconditionMetEvent;
import won.bot.framework.eventbot.event.impl.analyzation.precondition.PreconditionUnmetEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.WonMessageReceivedOnConnectionEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.highlevel.HighlevelProtocols;
import won.protocol.model.Connection;
import won.protocol.util.NeedModelWrapper;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;
import won.utils.goals.GoalInstantiationProducer;
import won.utils.goals.GoalInstantiationResult;

import java.net.URI;
import java.util.Collection;
import java.util.List;

/**
 * Checks if the received hint is for a factoryURI
 */
public class AnalyzeAction extends BaseEventBotAction {
    public AnalyzeAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        if(!(event instanceof WonMessageReceivedOnConnectionEvent)){
            logger.error("AnalyzeAction can only handle WonMessageReceivedOnConnectionEvent, was an event of class: " + event.getClass());
            return;
        }

        EventListenerContext ctx = getEventListenerContext();
        EventBus bus = ctx.getEventBus();
        LinkedDataSource linkedDataSource = ctx.getLinkedDataSource();

        WonMessageReceivedOnConnectionEvent receivedOnConnectionEvent = (WonMessageReceivedOnConnectionEvent) event;
        Connection con = receivedOnConnectionEvent.getCon();

        List<URI> acceptedEvents = WonRdfUtils.MessageUtils.getAcceptedEvents(receivedOnConnectionEvent.getWonMessage());

        if(!acceptedEvents.isEmpty()) {
            publishAnalyzingMessage(con);
            //IF ACCEPTS MESSAGE -> ACCEPT AGREEMENT
            Dataset fullConversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(receivedOnConnectionEvent.getConnectionURI(), linkedDataSource);
            URI agreementUri = receivedOnConnectionEvent.getWonMessage().getCorrespondingRemoteMessageURI();
            Model agreementPayload = HighlevelProtocols.getAgreement(fullConversationDataset, agreementUri.toString());

            if(!agreementPayload.isEmpty()){
                bus.publish(new AgreementAcceptedEvent(con, agreementUri, agreementPayload));
            }else{
                for (URI acceptedEvent : acceptedEvents) {
                    Dataset acceptedEventData = linkedDataSource.getDataForResource(acceptedEvent);

                    List<URI> proposeToCancelEvents = WonRdfUtils.MessageUtils.getProposeToCancelEvents(acceptedEventData);

                    for(URI proposeToCancelEvent : proposeToCancelEvents) {
                        Model agreementToCancel = HighlevelProtocols.getAgreement(fullConversationDataset, proposeToCancelEvent.toString());
                        bus.publish(new AgreementCanceledEvent(con, proposeToCancelEvent, agreementToCancel));
                    }
                }
            }
        } else {
            Dataset needDataset = linkedDataSource.getDataForResource(receivedOnConnectionEvent.getNeedURI());
            NeedModelWrapper needWrapper = new NeedModelWrapper(needDataset);

            Collection<Resource> goalsInNeed = needWrapper.getGoals();

            Dataset fullConversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(receivedOnConnectionEvent.getConnectionURI(), linkedDataSource);

            if(goalsInNeed.isEmpty() || !HighlevelProtocols.getAgreements(fullConversationDataset).isEmpty() || !HighlevelProtocols.getProposals(fullConversationDataset).isEmpty()) { //TODO: do not do this like that but for every goal individually
                return; //If there are no goals present we do not have to do anything (also we do not have to do anything if there are already proposals or agreements present
            }
            publishAnalyzingMessage(con);

            Dataset remoteNeedDataset = linkedDataSource.getDataForResource(receivedOnConnectionEvent.getRemoteNeedURI());
            Dataset conversationDataset = WonLinkedDataUtils.getConversationDataset(receivedOnConnectionEvent.getConnectionURI(), linkedDataSource);

            GoalInstantiationProducer goalInstantiationProducer = new GoalInstantiationProducer(needDataset, remoteNeedDataset, conversationDataset, "http://example.org/", "http://example.org/blended/");

            for(Resource goal : goalsInNeed){
                GoalInstantiationResult result = goalInstantiationProducer.findInstantiationForGoal(goal);

                if (result.getShaclReportWrapper().isConform()) {
                    bus.publish(new PreconditionMetEvent(con, result));
                } else {
                    bus.publish(new PreconditionUnmetEvent(con, result));
                }
            }
        }
    }

    private void publishAnalyzingMessage(Connection connection) {
        Model messageModel = WonRdfUtils.MessageUtils.textMessage("Checking Taxi availability...");
        getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(connection, messageModel)); //TODO: REMOVE THIS OR CHANGE IT TO A SORT-OF PROCESSING MESSAGE TYPE
    }
}
