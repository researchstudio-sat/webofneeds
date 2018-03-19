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
import won.bot.framework.eventbot.action.impl.factory.model.Precondition;
import won.bot.framework.eventbot.action.impl.factory.model.Proposal;
import won.bot.framework.eventbot.action.impl.factory.model.ProposalState;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.*;
import won.bot.framework.eventbot.event.impl.analyzation.agreement.ProposalAcceptedEvent;
import won.bot.framework.eventbot.event.impl.analyzation.agreement.AgreementCanceledEvent;
import won.bot.framework.eventbot.event.impl.analyzation.precondition.PreconditionMetEvent;
import won.bot.framework.eventbot.event.impl.analyzation.precondition.PreconditionUnmetEvent;
import won.bot.framework.eventbot.event.impl.analyzation.proposal.ProposalReceivedEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.WonMessageReceivedOnConnectionEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.WonMessageSentOnConnectionEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.protocol.highlevel.HighlevelProtocols;
import won.protocol.message.WonMessage;
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
import java.util.List;
import java.util.Set;

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

        FactoryBotContextWrapper botContextWrapper = (FactoryBotContextWrapper) ctx.getBotContextWrapper();
        LinkedDataSource linkedDataSource = ctx.getLinkedDataSource();

        boolean receivedMessage;

        if(event instanceof WonMessageSentOnConnectionEvent) {
            receivedMessage = false;
        } else if(event instanceof WonMessageReceivedOnConnectionEvent) {
            receivedMessage = true;
        } else {
            logger.error("AnalyzeAction can only handle WonMessageReceivedOnConnectionEvent or WonMessageSentOnConnectionEvent, was an event of class: " + event.getClass());
            return;
        }

        URI needUri = ((NeedSpecificEvent) event).getNeedURI();
        URI remoteNeedUri = ((RemoteNeedSpecificEvent) event).getRemoteNeedURI();
        URI connectionUri = ((ConnectionSpecificEvent) event).getConnectionURI();
        Connection connection = makeConnection(needUri, remoteNeedUri, connectionUri);
        WonMessage wonMessage = ((MessageEvent) event).getWonMessage();

        if(WonRdfUtils.MessageUtils.isProcessingMessage(wonMessage)){
            logger.debug("AnalyzeAction will not execute on processing messages");
            return;
        }

        if(receivedMessage) {
            publishAnalyzingMessage(connection);
        }

        List<URI> proposesEvents = WonRdfUtils.MessageUtils.getProposesEvents(wonMessage);
        List<URI> rejectEventUris = WonRdfUtils.MessageUtils.getRejectEvents(wonMessage);
        List<URI> retractEventUris = WonRdfUtils.MessageUtils.getRetractEvents(wonMessage);
        List<URI> proposesToCancelEvents = WonRdfUtils.MessageUtils.getProposesToCancelEvents(wonMessage);
        List<URI> acceptedEventUris = WonRdfUtils.MessageUtils.getAcceptedEvents(wonMessage);

        Dataset conversationAndNeedsDataset = null; //Initialize with null, to ensure some form of lazy init for the conversationAndNeedsDataset

        if(receivedMessage) {
            WonMessageReceivedOnConnectionEvent receivedOnConnectionEvent = (WonMessageReceivedOnConnectionEvent) event;

            if(!proposesEvents.isEmpty() || !proposesToCancelEvents.isEmpty()) {
                //If the message contains proposes or proposesToCancel the event itself is a proposal
                bus.publish(new ProposalReceivedEvent(connection, receivedOnConnectionEvent));
            }

            if(!acceptedEventUris.isEmpty()) {
                for(URI acceptedEventURI : acceptedEventUris) {
                    Dataset acceptedEventData = linkedDataSource.getDataForResource(acceptedEventURI, connection.getNeedURI());

                    List<URI> proposedToCancelAgreementUris = WonRdfUtils.MessageUtils.getProposesToCancelEvents(acceptedEventData);
                    if(!proposedToCancelAgreementUris.isEmpty()){
                        for(URI proposedToCancelAgreementUri : proposedToCancelAgreementUris){
                            bus.publish(new AgreementCanceledEvent(connection, proposedToCancelAgreementUri));
                        }
                    } else {
                        conversationAndNeedsDataset = getFullConversationDatasetLazyInit(conversationAndNeedsDataset, connectionUri);
                        Model agreementPayload = HighlevelProtocols.getAgreement(conversationAndNeedsDataset, acceptedEventURI);
                        if(!agreementPayload.isEmpty()) {
                            bus.publish(new ProposalAcceptedEvent(connection, acceptedEventURI, agreementPayload));
                        }
                    }
                }
            }
        }

        //Things to do for each individual message regardless of it being received or sent
        Dataset needDataset = linkedDataSource.getDataForResource(needUri);
        Collection<Resource> goalsInNeed = new NeedModelWrapper(needDataset).getGoals();

        if(!proposesEvents.isEmpty() || !proposesToCancelEvents.isEmpty()){
            //If the message contains proposesEvents or proposesToCancel events it is considered a proposal so we save the status of it in the botContext
            Proposal proposal = new Proposal(receivedMessage? wonMessage.getCorrespondingRemoteMessageURI() : wonMessage.getMessageURI(), ProposalState.SUGGESTED);
            conversationAndNeedsDataset = getFullConversationDatasetLazyInit(conversationAndNeedsDataset, connectionUri);
            Model proposalModel = HighlevelProtocols.getProposal(conversationAndNeedsDataset, proposal.getUri().toString());

            if(!proposalModel.isEmpty()) {
                for(Resource goal : goalsInNeed){
                    String preconditionUri = getUniqueGoalId(goal, needDataset, connectionUri);

                    if(!botContextWrapper.hasPreconditionProposalRelation(preconditionUri, proposal.getUri().toString())) {
                        GoalInstantiationResult result = GoalInstantiationProducer.findInstantiationForGoalInDataset(needDataset, goal, proposalModel);
                        Precondition precondition = new Precondition(preconditionUri, result.isConform());

                        logger.debug("Adding Precondition/Proposal Relation: " + precondition + "/" + proposal);
                        botContextWrapper.addPreconditionProposalRelation(precondition, proposal);
                    }
                }
            } else {
                logger.debug("No PreconditionProposalRelations to add... proposalModel is empty");
            }
        }

        if(!rejectEventUris.isEmpty() || !retractEventUris.isEmpty()) {
            conversationAndNeedsDataset = getFullConversationDatasetLazyInit(conversationAndNeedsDataset, connectionUri);
            Set<URI> agreementUris = HighlevelProtocols.getAgreementUris(conversationAndNeedsDataset);

            retractEventUris.addAll(rejectEventUris);
            retractEventUris.forEach(eventUri -> {
                if(!agreementUris.contains(eventUri)) {
                    //if the agreement payload is empty we can be certain that the uri was "just" a proposal before and can be dereferenced from our maps
                    botContextWrapper.removeProposalReferences(eventUri);
                }
            });
        }

        Dataset remoteNeedDataset = ctx.getLinkedDataSource().getDataForResource(remoteNeedUri);
        Dataset conversationDataset = null;  //Initialize with null, to ensure some form of lazy init for the conversationDataset
        GoalInstantiationProducer goalInstantiationProducer = null;

        for (Resource goal : goalsInNeed) {
            String preconditionUri = getUniqueGoalId(goal, needDataset, connection);

            if(!botContextWrapper.isPreconditionMetInProposals(preconditionUri)) { //ONLY HANDLE PRECONDITIONS THAT ARE NOT YET MET WITHIN THE PROPOSALS
                logger.debug("Goal/Precondition not yet met in a proposal/agreement, " + preconditionUri);
                conversationDataset = getConversationDatasetLazyInit(conversationDataset, connectionUri);
                goalInstantiationProducer = getGoalInstantiationProducerLazyInit(goalInstantiationProducer, needDataset, remoteNeedDataset, conversationDataset);

                GoalInstantiationResult result = goalInstantiationProducer.findInstantiationForGoal(goal);
                Boolean oldGoalState = botContextWrapper.getPreconditionConversationState(getUniqueGoalId(goal, needDataset, connection));
                boolean newGoalState = result.getShaclReportWrapper().isConform();

                if(oldGoalState == null || newGoalState != oldGoalState) {
                    if(newGoalState) {
                        ctx.getEventBus().publish(new PreconditionMetEvent(connection, preconditionUri, result));
                    }else{
                        ctx.getEventBus().publish(new PreconditionUnmetEvent(connection, preconditionUri, result));
                    }
                    botContextWrapper.addPreconditionConversationState(preconditionUri, newGoalState);
                }
            } else {
                logger.debug("Goal/Precondition already met in a proposal/agreement, " + preconditionUri);
            }
        }

        if(receivedMessage){
            publishAnalyzingCompleteMessage(connection, null);
        }
    }

    //********* Helper Methods **********
    private Dataset getFullConversationDatasetLazyInit(Dataset conversationAndNeedsDataset, URI connectionUri){
        if(conversationAndNeedsDataset == null) {
            logger.debug("Retrieving FullConversation Of Connection");
            return WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, getEventListenerContext().getLinkedDataSource());
        }else{
            logger.debug("Already retrieved FullConversation Of Connection");
            return conversationAndNeedsDataset;
        }
    }

    private Dataset getConversationDatasetLazyInit(Dataset conversationDataset, URI connectionUri) {
        if(conversationDataset == null){
            logger.debug("Retrieving Conversation Of Connection");
            return WonLinkedDataUtils.getConversationDataset(connectionUri, getEventListenerContext().getLinkedDataSource());
        }else{
            logger.debug("Already retrieved FullConversation Of Connection");
            return conversationDataset;
        }
    }

    private GoalInstantiationProducer getGoalInstantiationProducerLazyInit(GoalInstantiationProducer goalInstantiationProducer, Dataset needDataset, Dataset remoteNeedDataset, Dataset conversationDataset){
        if(goalInstantiationProducer == null){
            logger.debug("Instantiating GoalInstantiationProducer");
            return new GoalInstantiationProducer(needDataset, remoteNeedDataset, conversationDataset, "http://example.org/", "http://example.org/blended/");
        }else{
            logger.debug("Already instantiated GoalInstantiationProducer");
            return goalInstantiationProducer;
        }
    }

    private void publishAnalyzingMessage(Connection connection) {
        Model messageModel = WonRdfUtils.MessageUtils.processingMessage(getEventListenerContext().getBotContextWrapper().getBotName() + " - Starting Analyzation");
        getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(connection, messageModel));
    }

    private void publishAnalyzingCompleteMessage(Connection connection, String detailMessage) {
        Model messageModel = WonRdfUtils.MessageUtils.processingMessage(getEventListenerContext().getBotContextWrapper().getBotName() + " - Analyzation complete" + (detailMessage!= null? (", DetailMessage: "+detailMessage): ""));
        getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(connection, messageModel));
    }

    private static String getUniqueGoalId(Resource goal, Dataset needDataset, Connection con) {
        return getUniqueGoalId(goal, needDataset, con.getConnectionURI());
    }

    private static String getUniqueGoalId(Resource goal, Dataset needDataset, URI connectionURI) { //TODO: GOAL STATE RETRIEVAL IS NOT BASED ON THE CORRECT URI SO FAR
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

            return connectionURI +"#"+ writer.toString();
        }
    }

    private static Connection makeConnection(URI needURI, URI remoteNeedURI, URI connectionURI){
        Connection con = new Connection();
        con.setConnectionURI(connectionURI);
        con.setNeedURI(needURI);
        con.setRemoteNeedURI(remoteNeedURI);
        return con;
    }
}