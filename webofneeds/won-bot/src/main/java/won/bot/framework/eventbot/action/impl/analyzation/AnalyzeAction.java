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

        URI needUri;
        URI remoteNeedUri;
        URI connectionUri;
        Connection connection;
        WonMessage wonMessage;
        boolean receivedMessage;

        try {
            needUri = ((NeedSpecificEvent) event).getNeedURI();
            remoteNeedUri = ((RemoteNeedSpecificEvent) event).getRemoteNeedURI();
            connectionUri = ((ConnectionSpecificEvent) event).getConnectionURI();
            connection = makeConnection(needUri, remoteNeedUri, connectionUri);
            wonMessage = ((MessageEvent) event).getWonMessage();
        }catch(ClassCastException e){
            logger.error("AnalyzeAction can only handle WonMessageReceivedOnConnectionEvent or WonMessageSentOnConnectionEvent, was an event of class: " + event.getClass());
            return;
        }

        Dataset fullConversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSource);
        List<URI> proposesEvents = WonRdfUtils.MessageUtils.getProposesEvents(wonMessage);
        List<URI> rejectEventUris = WonRdfUtils.MessageUtils.getRejectEvents(wonMessage);
        List<URI> proposesToCancelEvents = WonRdfUtils.MessageUtils.getProposesToCancelEvents(wonMessage);

        if(event instanceof WonMessageSentOnConnectionEvent) {
            receivedMessage = false;
            //DO NOTHING SPECIAL NOW
        }else if(event instanceof WonMessageReceivedOnConnectionEvent){
            receivedMessage = true;
            publishAnalyzingMessage(connection);
            WonMessageReceivedOnConnectionEvent receivedOnConnectionEvent = (WonMessageReceivedOnConnectionEvent) event;

            if(!proposesEvents.isEmpty() || !proposesToCancelEvents.isEmpty()){
                //If the message contains proposes or proposesToCancel the event itself is a proposal
                bus.publish(new ProposalReceivedEvent(connection, receivedOnConnectionEvent));
            }

            List<URI> acceptedEventUris = WonRdfUtils.MessageUtils.getAcceptedEvents(wonMessage);

            if(!acceptedEventUris.isEmpty()) {
                for(URI acceptedEventURI : acceptedEventUris) {
                    Dataset acceptedEventData = linkedDataSource.getDataForResource(acceptedEventURI, connection.getNeedURI());

                    List<URI> proposedToCancelAgreementUris = WonRdfUtils.MessageUtils.getProposesToCancelEvents(acceptedEventData);
                    if(!proposedToCancelAgreementUris.isEmpty()){
                        for(URI proposedToCancelAgreementUri : proposedToCancelAgreementUris){
                            bus.publish(new AgreementCanceledEvent(connection, proposedToCancelAgreementUri));
                        }
                    } else {
                        Model agreementPayload = HighlevelProtocols.getAgreement(fullConversationDataset, acceptedEventURI);
                        if(!agreementPayload.isEmpty()) {
                            bus.publish(new ProposalAcceptedEvent(connection, acceptedEventURI, agreementPayload));
                        }
                    }
                }
            }
        } else {
            logger.error("AnalyzeAction can only handle WonMessageReceivedOnConnectionEvent or WonMessageSentOnConnectionEvent, was an event of class: " + event.getClass());
            return;
        }

        //Things to do for each individual message regardless of it being received or sent
        Dataset needDataset = linkedDataSource.getDataForResource(needUri);
        Collection<Resource> goalsInNeed = new NeedModelWrapper(needDataset).getGoals();

        if(!proposesEvents.isEmpty() || !proposesToCancelEvents.isEmpty()){
            //If the message contains proposesEvents or proposesToCancel events it is considered a proposal so we save the status of it in the botContext
            Proposal proposal = new Proposal(receivedMessage? wonMessage.getCorrespondingRemoteMessageURI() : wonMessage.getMessageURI(), ProposalState.SUGGESTED);
            Model proposalModel = HighlevelProtocols.getProposal(fullConversationDataset, proposal.getUri().toString());

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

        if(!rejectEventUris.isEmpty()) {
            Set<URI> agreementUris = HighlevelProtocols.getAgreementUris(fullConversationDataset);

            rejectEventUris.forEach(rejectEventUri -> {
                if(!agreementUris.contains(rejectEventUri)) {
                    //if the agreement payload is empty we can be certain that the uri was "just" a proposal before and can be dereferenced from our maps
                    botContextWrapper.removeProposalReferences(rejectEventUri);
                }
            });
        }

        Dataset remoteNeedDataset = ctx.getLinkedDataSource().getDataForResource(remoteNeedUri);
        Dataset conversationDataset = WonLinkedDataUtils.getConversationDataset(connectionUri, linkedDataSource);

        GoalInstantiationProducer goalInstantiationProducer = new GoalInstantiationProducer(needDataset, remoteNeedDataset, conversationDataset, "http://example.org/", "http://example.org/blended/");

        for (Resource goal : goalsInNeed) {
            String preconditionUri = getUniqueGoalId(goal, needDataset, connection);

            if(!botContextWrapper.isPreconditionMetInProposals(preconditionUri)){ //ONLY HANDLE PRECONDITIONS THAT ARE NOT YET MET WITHIN THE PROPOSALS
                GoalInstantiationResult result = goalInstantiationProducer.findInstantiationForGoal(goal);
                Boolean oldGoalState = botContextWrapper.getPreconditionConversationState(getUniqueGoalId(goal, needDataset, connection));
                boolean newGoalState = result.getShaclReportWrapper().isConform();

                if(oldGoalState == null || newGoalState != oldGoalState) {
                    if(newGoalState) {
                        ctx.getEventBus().publish(new PreconditionMetEvent(connection, result));
                    }else{
                        ctx.getEventBus().publish(new PreconditionUnmetEvent(connection, result));
                    }
                    botContextWrapper.addPreconditionConversationState(getUniqueGoalId(goal, needDataset, connection), newGoalState);
                }
            }
        }

        if(receivedMessage){
            publishAnalyzingCompleteMessage(connection, null);
        }
    }

    //********* Helper Methods **********
    private void publishAnalyzingMessage(Connection connection) {
        Model messageModel = WonRdfUtils.MessageUtils.textMessage(getEventListenerContext().getBotContextWrapper().getBotName() + " - Starting Analyzation");
        getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(connection, messageModel)); //TODO: REMOVE THIS OR CHANGE IT TO A SORT-OF PROCESSING MESSAGE TYPE
    }

    private void publishAnalyzingCompleteMessage(Connection connection, String detailMessage) {
        Model messageModel = WonRdfUtils.MessageUtils.textMessage(getEventListenerContext().getBotContextWrapper().getBotName() + " - Analyzation complete" + (detailMessage!= null? (", DetailMessage: "+detailMessage): ""));
        getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(connection, messageModel)); //TODO: REMOVE THIS OR CHANGE IT TO A SORT-OF PROCESSING MESSAGE TYPE
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