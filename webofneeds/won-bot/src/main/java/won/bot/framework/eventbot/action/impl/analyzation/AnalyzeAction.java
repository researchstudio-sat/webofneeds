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
import won.bot.framework.bot.context.FactoryBotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.analyzation.agreement.AgreementAcceptedEvent;
import won.bot.framework.eventbot.event.impl.analyzation.agreement.AgreementCanceledEvent;
import won.bot.framework.eventbot.event.impl.analyzation.precondition.PreconditionMetEvent;
import won.bot.framework.eventbot.event.impl.analyzation.precondition.PreconditionUnmetEvent;
import won.bot.framework.eventbot.event.impl.analyzation.proposal.ProposalReceivedEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.WonMessageReceivedOnConnectionEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.WonMessageSentOnConnectionEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.highlevel.AgreementProtocol;
import won.protocol.model.Connection;
import won.protocol.util.NeedModelWrapper;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;
import won.utils.goals.GoalInstantiationProducer;
import won.utils.goals.GoalInstantiationResult;

import java.io.StringWriter;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class AnalyzeAction extends BaseEventBotAction {

    public AnalyzeAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        EventListenerContext ctx = getEventListenerContext();
        EventBus bus = ctx.getEventBus();

        if(!(ctx.getBotContextWrapper() instanceof FactoryBotContextWrapper)) {
            logger.error("AnalyzeAction can only work with a FactoryBotContextWrapper, but was an instance of class: " + ctx.getBotContextWrapper().getClass());
            return ;
        }

        if(event instanceof WonMessageSentOnConnectionEvent) {
            logger.debug("AnalyzeAction was called for a WonMessageSentOnConnectionEvent, this handling is not implemented yet");
        }else if(event instanceof WonMessageReceivedOnConnectionEvent){
            LinkedDataSource linkedDataSource = ctx.getLinkedDataSource();

            WonMessageReceivedOnConnectionEvent receivedOnConnectionEvent = (WonMessageReceivedOnConnectionEvent) event;
            Connection con = receivedOnConnectionEvent.getCon();
            publishAnalyzingMessage(con);

            List<URI> proposesEvents = WonRdfUtils.MessageUtils.getProposesEvents(receivedOnConnectionEvent.getWonMessage());
            List<URI> proposesToCancelEvents = WonRdfUtils.MessageUtils.getProposesToCancelEvents(receivedOnConnectionEvent.getWonMessage());

            if(!proposesEvents.isEmpty() || !proposesToCancelEvents.isEmpty()){
                //If the message contains proposes or proposesToCancel the event itself is a proposal
                bus.publish(new ProposalReceivedEvent(con, receivedOnConnectionEvent));
            }

            List<URI> acceptedEvents = WonRdfUtils.MessageUtils.getAcceptedEvents(receivedOnConnectionEvent.getWonMessage());

            if(!acceptedEvents.isEmpty()) {
                //IF ACCEPTS MESSAGE -> ACCEPT AGREEMENT
                Dataset fullConversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(receivedOnConnectionEvent.getConnectionURI(), linkedDataSource);
                URI agreementUri = receivedOnConnectionEvent.getWonMessage().getCorrespondingRemoteMessageURI();
                Model agreementPayload = AgreementProtocol.getAgreement(fullConversationDataset, agreementUri);

                if(!agreementPayload.isEmpty()){
                    bus.publish(new AgreementAcceptedEvent(con, agreementUri, agreementPayload));
                }else{
                    for (URI acceptedEvent : acceptedEvents) {
                        Dataset acceptedEventData = linkedDataSource.getDataForResource(acceptedEvent, con.getNeedURI());

                        if(!acceptedEventData.isEmpty()) {
                            List<URI> canceledAgreementUris = WonRdfUtils.MessageUtils.getProposesToCancelEvents(acceptedEventData);

                            for (URI canceledAgreementUri : canceledAgreementUris) {
                                bus.publish(new AgreementCanceledEvent(con, canceledAgreementUri));
                            }
                        }
                    }
                }
                publishAnalyzingCompleteMessage(con, "Accept Message Parsing complete");
            }

            if(acceptedEvents.isEmpty() && proposesToCancelEvents.isEmpty()) { //TODO: REMOVE THIS CHECK WE NEED TO ANALYZE THE CONVERSATION ON EVERY MESSAGE ANYWAY
                Dataset needDataset = linkedDataSource.getDataForResource(receivedOnConnectionEvent.getNeedURI());
                NeedModelWrapper needWrapper = new NeedModelWrapper(needDataset);

                Collection<Resource> goalsInNeed = needWrapper.getGoals();

                if(goalsInNeed.isEmpty()){
                    logger.debug("No Goals Present, no need to check agreements/proposals for goal conformity");
                    //no need to check for preconditions that are met since the need does not contain any goals anyway
                    publishAnalyzingCompleteMessage(con, "No Goals Present, no need to check agreements/proposals for goal conformity");
                    return;
                }
                Dataset fullConversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(receivedOnConnectionEvent.getConnectionURI(), linkedDataSource);
                Dataset proposals = AgreementProtocol.getProposals(fullConversationDataset);

                if (!proposals.isEmpty()) {
                    goalsInNeed.removeAll(this.getGoalsWithPreconditionMet(needDataset, proposals, goalsInNeed));

                    if(goalsInNeed.isEmpty()){
                        logger.debug("No Goals that are unmet anymore, do not validate any further");
                        publishAnalyzingCompleteMessage(con, "No Goals that are unmet anymore, do not validate any further");
                        return;
                    }
                }

                Dataset agreements = AgreementProtocol.getAgreements(fullConversationDataset);
                if (!agreements.isEmpty()) {
                    goalsInNeed.removeAll(this.getGoalsWithPreconditionMet(needDataset, agreements, goalsInNeed));

                    if (goalsInNeed.isEmpty()) {
                        logger.debug("No Goals that are unmet anymore, do not validate any further");
                        publishAnalyzingCompleteMessage(con, "No Goals that are unmet anymore, do not validate any further");
                        return;
                    }
                }

                checkChangedPreconditionsOfRemainingGoals(ctx, receivedOnConnectionEvent, needDataset, goalsInNeed);
                publishAnalyzingCompleteMessage(con, null);
            }
        } else {
            logger.error("AnalyzeAction can only handle WonMessageReceivedOnConnectionEvent or WonMessageSentOnConnectionEvent, was an event of class: " + event.getClass());
        }
    }

    private void publishAnalyzingMessage(Connection connection) {
        Model messageModel = WonRdfUtils.MessageUtils.textMessage(getEventListenerContext().getBotContextWrapper().getBotName() + " - Starting Analyzation");
        getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(connection, messageModel)); //TODO: REMOVE THIS OR CHANGE IT TO A SORT-OF PROCESSING MESSAGE TYPE
    }

    private void publishAnalyzingCompleteMessage(Connection connection, String detailMessage) {
        Model messageModel = WonRdfUtils.MessageUtils.textMessage(getEventListenerContext().getBotContextWrapper().getBotName() + " - Analyzation complete" + (detailMessage!= null? (", DetailMessage: "+detailMessage): ""));
        getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(connection, messageModel)); //TODO: REMOVE THIS OR CHANGE IT TO A SORT-OF PROCESSING MESSAGE TYPE
    }

    private Collection<Resource> getGoalsWithPreconditionMet(Dataset needDataset, Dataset datasetToCheck, Collection<Resource> goals) {
        logger.debug("Checking Dataset with goals");
        Collection<Resource> metGoals = new LinkedList<>();
        Iterator<String> datasetIterator = datasetToCheck.listNames();

        while(datasetIterator.hasNext()){
            Model currentModelToCheck = datasetToCheck.getNamedModel(datasetIterator.next());

            for(Resource goal : goals){
                GoalInstantiationResult result = GoalInstantiationProducer.findInstantiationForGoalInDataset(needDataset, goal, currentModelToCheck);
                if(result.isConform()){
                    logger.debug("Goal Precondition is met by a part of the dataset removing further validationcheck for this goal");
                    metGoals.add(goal);
                }else{
                    logger.debug("Goal Precondition is unmet by a part of the dataset");
                }
            }
        }

        return metGoals;
    }

    private void checkChangedPreconditionsOfRemainingGoals(EventListenerContext ctx, WonMessageReceivedOnConnectionEvent receivedOnConnectionEvent, Dataset needDataset, Collection<Resource> goalsInNeed) {
        FactoryBotContextWrapper botContextWrapper = (FactoryBotContextWrapper) ctx.getBotContextWrapper();

        Dataset remoteNeedDataset = ctx.getLinkedDataSource().getDataForResource(receivedOnConnectionEvent.getRemoteNeedURI());
        Dataset conversationDataset = WonLinkedDataUtils.getConversationDataset(receivedOnConnectionEvent.getConnectionURI(), ctx.getLinkedDataSource());

        GoalInstantiationProducer goalInstantiationProducer = new GoalInstantiationProducer(needDataset, remoteNeedDataset, conversationDataset, "http://example.org/", "http://example.org/blended/");

        for (Resource goal : goalsInNeed) {
            GoalInstantiationResult result = goalInstantiationProducer.findInstantiationForGoal(goal);
            Boolean oldGoalState = botContextWrapper.getGoalPreconditionState(getUniqueGoalId(goal, needDataset, receivedOnConnectionEvent.getCon()));
            boolean newGoalState = result.getShaclReportWrapper().isConform();

            if(oldGoalState == null || newGoalState != oldGoalState) {
                if(newGoalState) {
                    ctx.getEventBus().publish(new PreconditionMetEvent(receivedOnConnectionEvent.getCon(), result));
                }else{
                    ctx.getEventBus().publish(new PreconditionUnmetEvent(receivedOnConnectionEvent.getCon(), result));
                }
                botContextWrapper.addGoalPreconditionState(getUniqueGoalId(goal, needDataset, receivedOnConnectionEvent.getCon()), newGoalState);
            }
        }
    }

    private static String getUniqueGoalId(Resource goal, Dataset needDataset, Connection con) { //TODO: GOAL STATE RETRIEVAL IS NOT BASED ON THE CORRECT URI SO FAR
        if(goal.getURI() != null) {
            return goal.getURI();
        }else{
            NeedModelWrapper needWrapper = new NeedModelWrapper(needDataset);

            StringWriter writer = new StringWriter();
            Model shapesModel = needWrapper.getShapesGraph(goal);
            if(shapesModel != null) {
                shapesModel.write(writer, "TRIG");
            }
            Model dataModel = needWrapper.getDataGraph(goal);
            if(dataModel != null) {
                dataModel.write(writer, "TRIG");
            }

            return con.getConnectionURI() +"#"+ writer.toString();
        }
    }
}