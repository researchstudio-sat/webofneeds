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
import won.bot.framework.eventbot.event.MessageEvent;
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

        FactoryBotContextWrapper botContextWrapper = (FactoryBotContextWrapper) ctx.getBotContextWrapper();
        LinkedDataSource linkedDataSource = ctx.getLinkedDataSource();

        URI needUri;
        URI remoteNeedUri;
        URI connectionUri;
        Connection connection;
        boolean publishAnalyzeMessages = false;

        if(event instanceof WonMessageSentOnConnectionEvent) {
            WonMessageSentOnConnectionEvent sentOnConnectionEvent = (WonMessageSentOnConnectionEvent) event;

            needUri = sentOnConnectionEvent.getNeedURI();
            remoteNeedUri = sentOnConnectionEvent.getRemoteNeedURI();
            connectionUri = sentOnConnectionEvent.getConnectionURI();
            connection = makeConnection(needUri, remoteNeedUri, connectionUri);

            Dataset fullConversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSource);
            //TODO: handle Proposals that are sent because we need to add the status of all the other preconditions to the proposed content -> but do we really do that?
            List<URI> proposesEvents = WonRdfUtils.MessageUtils.getProposesEvents(sentOnConnectionEvent.getWonMessage());



            if(!proposesEvents.isEmpty()){ //If you send a message that contains proposal Entries we save the
                URI proposalUri =  sentOnConnectionEvent.getWonMessage().getMessageURI();
                Model proposalModel = HighlevelProtocols.getProposal(fullConversationDataset, proposalUri.toString());

                if(!proposalModel.isEmpty()){
                    //botContextWrapper.addPreconditionProposalRelation();
                }
            }


            /*if(!proposals.isEmpty()) { BLAAAARGH!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                //PUBLISH ALL PROPOSAL GOAL COMBINATIONS THAT WERE NOT CALLED YET
                Iterator<String> proposalIterator = proposals.listNames();

                while(proposalIterator.hasNext()){
                    String proposalUri = proposalIterator.next();
                    Model currentModelToCheck = proposals.getNamedModel(proposalUri);

                    for(Resource goal : goalsInNeed){
                        String preconditionUri = getUniqueGoalId(goal, needDataset, connectionUri);

                        if(!botContextWrapper.hasPreconditionProposalRelation(preconditionUri, proposalUri)) {
                            GoalInstantiationResult result = GoalInstantiationProducer.findInstantiationForGoalInDataset(needDataset, goal, currentModelToCheck);
                            botContextWrapper.addPreconditionProposalRelation(new Precondition(preconditionUri, result.isConform()), new Proposal(proposalUri, ProposalState.SUGGESTED));
                        }
                    }
                }
            }*/



        }else if(event instanceof WonMessageReceivedOnConnectionEvent){
            WonMessageReceivedOnConnectionEvent receivedOnConnectionEvent = (WonMessageReceivedOnConnectionEvent) event;
            needUri = receivedOnConnectionEvent.getNeedURI();
            remoteNeedUri = receivedOnConnectionEvent.getRemoteNeedURI();
            connectionUri = receivedOnConnectionEvent.getConnectionURI();
            connection = receivedOnConnectionEvent.getCon();

            publishAnalyzingMessage(connection);

            List<URI> proposesEvents = WonRdfUtils.MessageUtils.getProposesEvents(receivedOnConnectionEvent.getWonMessage());
            List<URI> proposesToCancelEvents = WonRdfUtils.MessageUtils.getProposesToCancelEvents(receivedOnConnectionEvent.getWonMessage());

            if(!proposesEvents.isEmpty() || !proposesToCancelEvents.isEmpty()){
                //If the message contains proposes or proposesToCancel the event itself is a proposal
                bus.publish(new ProposalReceivedEvent(connection, receivedOnConnectionEvent));
            }

            List<URI> acceptedEventUris = WonRdfUtils.MessageUtils.getAcceptedEvents(receivedOnConnectionEvent.getWonMessage());

            if(!acceptedEventUris.isEmpty()) {
                Dataset fullConversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(receivedOnConnectionEvent.getConnectionURI(), linkedDataSource);

                for(URI acceptedEventURI : acceptedEventUris) {
                    Dataset acceptedEventData = linkedDataSource.getDataForResource(acceptedEventURI, connection.getNeedURI());

                    List<URI> canceledAgreementUris = WonRdfUtils.MessageUtils.getProposesToCancelEvents(acceptedEventData);
                    if(!canceledAgreementUris.isEmpty()){
                        for(URI canceledAgreementUri : canceledAgreementUris){
                            bus.publish(new AgreementCanceledEvent(connection, canceledAgreementUri));
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

        List<URI> rejectEventUris = WonRdfUtils.MessageUtils.getRejectEvents(((MessageEvent) event).getWonMessage());

        if(!rejectEventUris.isEmpty()) {
            Dataset fullConversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSource);

            for(URI rejectEventUri : rejectEventUris) {
                Model agreementPayload = HighlevelProtocols.getAgreement(fullConversationDataset, rejectEventUri);
                if(agreementPayload.isEmpty()) {
                    //if the agreement payload is empty we can be certain that the uri was "just" a proposal before and can be dereferenced from our maps
                    botContextWrapper.removeProposalReferences(rejectEventUri);
                }
            }
        }

        Dataset needDataset = linkedDataSource.getDataForResource(needUri);
        NeedModelWrapper needWrapper = new NeedModelWrapper(needDataset);

        Collection<Resource> goalsInNeed = needWrapper.getGoals();

        if(goalsInNeed.isEmpty()){
            logger.debug("No Goals Present, no need to check agreements/proposals for goal conformity");
            //no need to check for preconditions that are met since the need does not contain any goals anyway
            if(publishAnalyzeMessages) {
                publishAnalyzingCompleteMessage(connection, "No Goals Present, no need to check agreements/proposals for goal conformity");
            }

            return;
        }

        /* FIXME: I AM REALLY NOT SURE ANYMORE IF I EVEN NEED THIS


        Dataset fullConversationDataset = WonLinkedDataUtils.getConversationAndNeedsDataset(connectionUri, linkedDataSource);
        Dataset proposals = HighlevelProtocols.getProposals(fullConversationDataset);
        Dataset agreements = HighlevelProtocols.getAgreements(fullConversationDataset);

        if(!proposals.isEmpty()) {
            //PUBLISH ALL PROPOSAL GOAL COMBINATIONS THAT WERE NOT CALLED YET
            Iterator<String> proposalIterator = proposals.listNames();

            while(proposalIterator.hasNext()){
                String proposalUri = proposalIterator.next();
                Model currentModelToCheck = proposals.getNamedModel(proposalUri);

                for(Resource goal : goalsInNeed){
                    String preconditionUri = getUniqueGoalId(goal, needDataset, connectionUri);

                    if(!botContextWrapper.hasPreconditionProposalRelation(preconditionUri, proposalUri)) {
                        GoalInstantiationResult result = GoalInstantiationProducer.findInstantiationForGoalInDataset(needDataset, goal, currentModelToCheck);
                        botContextWrapper.addPreconditionProposalRelation(new Precondition(preconditionUri, result.isConform()), new Proposal(proposalUri, ProposalState.SUGGESTED));
                    }
                }
            }
        }

        if(!agreements.isEmpty()) {
            //PUBLISH ALL AGREEMENT GOAL COMBINATIONS THAT WERE NOT CALLED YET
            Iterator<String> agreementIterator = agreements.listNames();

            while(agreementIterator.hasNext()){
                String agreementUri = agreementIterator.next();
                Model currentModelToCheck = agreements.getNamedModel(agreementUri);

                for(Resource goal : goalsInNeed){
                    String preconditionUri = getUniqueGoalId(goal, needDataset, connectionUri);

                    if(!botContextWrapper.hasPreconditionProposalRelation(preconditionUri, agreementUri)) {
                        GoalInstantiationResult result = GoalInstantiationProducer.findInstantiationForGoalInDataset(needDataset, goal, currentModelToCheck);
                        botContextWrapper.addPreconditionProposalRelation(new Precondition(preconditionUri, result.isConform()), new Proposal(agreementUri, ProposalState.ACCEPTED));
                    }
                }
            }
        }*/

        Dataset remoteNeedDataset = ctx.getLinkedDataSource().getDataForResource(remoteNeedUri);
        Dataset conversationDataset = WonLinkedDataUtils.getConversationDataset(connectionUri, linkedDataSource);

        GoalInstantiationProducer goalInstantiationProducer = new GoalInstantiationProducer(needDataset, remoteNeedDataset, conversationDataset, "http://example.org/", "http://example.org/blended/");

        for (Resource goal : goalsInNeed) {
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

        if(publishAnalyzeMessages){
            publishAnalyzingCompleteMessage(connection, null);
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