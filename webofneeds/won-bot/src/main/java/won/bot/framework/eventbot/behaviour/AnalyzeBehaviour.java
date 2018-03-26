package won.bot.framework.eventbot.behaviour;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import won.bot.framework.bot.context.BotContext;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.action.impl.factory.model.Precondition;
import won.bot.framework.eventbot.action.impl.factory.model.Proposal;
import won.bot.framework.eventbot.action.impl.factory.model.ProposalState;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.*;
import won.bot.framework.eventbot.event.impl.analyzation.agreement.AgreementCancellationAcceptedEvent;
import won.bot.framework.eventbot.event.impl.analyzation.agreement.ProposalAcceptedEvent;
import won.bot.framework.eventbot.event.impl.analyzation.precondition.PreconditionMetEvent;
import won.bot.framework.eventbot.event.impl.analyzation.precondition.PreconditionUnmetEvent;
import won.bot.framework.eventbot.event.impl.analyzation.proposal.ProposalReceivedEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandSuccessEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.OpenFromOtherNeedEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.WonMessageReceivedOnConnectionEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.protocol.agreement.AgreementProtocolState;
import won.protocol.agreement.effect.MessageEffect;
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
import java.util.Optional;
import java.util.Set;

public class AnalyzeBehaviour extends BotBehaviour {
    private final BotContext botContext;
    private final String preconditionToProposalListMapName;
    private final String proposalToPreconditionListMapName;
    private final String preconditionConversationStateMapName;
    private final String preconditionMetPending;

    public AnalyzeBehaviour(EventListenerContext context) {
        super(context);
        botContext = context.getBotContext();
        String botName = context.getBotContextWrapper().getBotName();

        this.preconditionToProposalListMapName = botName + ":preconditionToProposalListMap";
        this.proposalToPreconditionListMapName = botName + ":proposalToPreconditionListMap";
        this.preconditionConversationStateMapName = botName + ":preconditionConversationStateMap";
        this.preconditionMetPending = botName + ":preconditionMetPending";
    }

    public AnalyzeBehaviour(EventListenerContext context, String name) {
        super(context, name);
        context.getBotContextWrapper().getBotName();
        botContext = context.getBotContext();
        String botName = context.getBotContextWrapper().getBotName();

        this.preconditionToProposalListMapName = botName + ":" + name + ":preconditionToProposalListMap";
        this.proposalToPreconditionListMapName = botName + ":" + name + ":proposalToPreconditionListMap";
        this.preconditionConversationStateMapName = botName + ":" + name + ":preconditionConversationStateMap";
        this.preconditionMetPending = botName + ":" + name + ":preconditionMetPending";
    }

    @Override
    protected void onActivate(Optional<Object> message) {
        ActionOnEventListener analyzeAction = new ActionOnEventListener(context, new AnalyzeAction(context));

        this.subscribeWithAutoCleanup(MessageFromOtherNeedEvent.class, analyzeAction);
        this.subscribeWithAutoCleanup(OpenFromOtherNeedEvent.class, analyzeAction);
        this.subscribeWithAutoCleanup(ConnectionMessageCommandSuccessEvent.class, analyzeAction);
    }

    private class AnalyzeAction extends BaseEventBotAction {
        public AnalyzeAction(EventListenerContext eventListenerContext) {
            super(eventListenerContext);
        }

        @Override
        protected void doRun(Event event, EventListener executingListener) throws Exception {
            logger.trace("################################## ANALYZING MESSAGE #########################################");
            EventListenerContext ctx = getEventListenerContext();
            EventBus bus = ctx.getEventBus();

            LinkedDataSource linkedDataSource = ctx.getLinkedDataSource();

            boolean receivedMessage;
            Event eventToAnalyze;
            WonMessage wonMessage;

            if(event instanceof ConnectionMessageCommandSuccessEvent) {
                eventToAnalyze = ((ConnectionMessageCommandSuccessEvent) event).getOriginalCommandEvent();
                wonMessage = ((ConnectionMessageCommandSuccessEvent) event).getWonMessage();
                receivedMessage = false;
            } else if(event instanceof WonMessageReceivedOnConnectionEvent) {
                eventToAnalyze = event;
                wonMessage = ((MessageEvent) event).getWonMessage();
                receivedMessage = true;
            } else {
                logger.error("AnalyzeAction can only handle WonMessageReceivedOnConnectionEvent or ConnectionMessageCommandSuccessEvent, was an event of class: " + event.getClass());
                logger.trace("################################## ANALYZING COMPLETE #########################################");
                return;
            }

            URI needUri = ((NeedSpecificEvent) eventToAnalyze).getNeedURI();
            URI remoteNeedUri = ((RemoteNeedSpecificEvent) eventToAnalyze).getRemoteNeedURI();
            URI connectionUri = ((ConnectionSpecificEvent) eventToAnalyze).getConnectionURI();
            Connection connection = makeConnection(needUri, remoteNeedUri, connectionUri);

            logger.trace("Message Information ------");
            logger.trace("Message Type: "+ (receivedMessage ? "RECEIVED" : "SENT"));
            logger.trace("MessageUri: "+ wonMessage.getMessageURI());
            logger.trace("CorrespondingRemoteMessageURI: "+ wonMessage.getCorrespondingRemoteMessageURI());
            logger.trace("NeedUri: " + needUri);
            logger.trace("remoteNeedUri: " + remoteNeedUri);
            logger.trace("connectionUri: " + connectionUri);
            logger.trace("WonMessage Dataset: ");
            logger.trace(getWonMessageString(wonMessage, Lang.TRIG));

            if(connectionUri == null || WonRdfUtils.MessageUtils.isProcessingMessage(wonMessage)){
                logger.debug("AnalyzeAction will not execute on processing messages or messages without a connectionUri (e.g. connect messages)");
                logger.trace("--------------------------");
                logger.trace("################################## ANALYZING COMPLETE #########################################");
                return;
            }

            if(receivedMessage) {
                publishAnalyzingMessage(connection);
            }

            Dataset needDataset = linkedDataSource.getDataForResource(needUri);
            Collection<Resource> goalsInNeed = new NeedModelWrapper(needDataset).getGoals();
            logger.trace("Preconditions in Need: " + goalsInNeed.size());

            AgreementProtocolState agreementProtocolState = AgreementProtocolState.of(connectionUri, getEventListenerContext().getLinkedDataSource()); //Initialize with null, to ensure some form of lazy init for the agreementProtocolState
            Set<MessageEffect> messageEffects = agreementProtocolState.getEffects(wonMessage.getMessageURI());

            logger.trace("MessageEffects in Message: "+messageEffects.size());

            messageEffects.forEach(messageEffect -> {
                switch (messageEffect.getType()) {
                    case ACCEPTS:
                        logger.trace("\tMessageEffect 'Accepts':");

                        if(receivedMessage) {
                            messageEffect.asAccepts().getCancelledAgreementURIs().forEach(cancelledAgreementUri -> {
                                logger.trace("\t\tPublish AgreementCancellationAcceptedEvent for agreementUri: "+cancelledAgreementUri);
                                bus.publish(new AgreementCancellationAcceptedEvent(connection, cancelledAgreementUri));
                            });

                            Model agreementPayload = agreementProtocolState.getAgreement(messageEffect.asAccepts().getAcceptedMessageUri());

                            if (!agreementPayload.isEmpty()) {
                                logger.trace("\t\tPublish ProposalAcceptedEvent for agreementUri: " + messageEffect.asAccepts().getAcceptedMessageUri());
                                bus.publish(new ProposalAcceptedEvent(connection, messageEffect.asAccepts().getAcceptedMessageUri(), agreementPayload));
                            }
                        }
                        break;

                    case PROPOSES:
                        logger.trace("\tMessageEffect 'Proposes':");
                        Proposal proposal = new Proposal(messageEffect.getMessageUri(), ProposalState.SUGGESTED);
                        Model proposalModel = agreementProtocolState.getPendingProposal(proposal.getUri()); //TODO: IT COULD BE THAT WE HAVE TO ADD THIS WHOLE SHABANG FOR AGREEMENTS AS WELL

                        if(!proposalModel.isEmpty()) {
                            logger.trace("\t\tProposal: " + proposal);
                            for(Resource goal : goalsInNeed){
                                String preconditionUri = getUniqueGoalId(goal, needDataset, connectionUri);
                                logger.trace("\t\t\tPreconditionUri: "+preconditionUri);

                                if(!AnalyzeBehaviour.this.hasPreconditionProposalRelation(preconditionUri, proposal.getUri().toString())) {
                                    GoalInstantiationResult result = GoalInstantiationProducer.findInstantiationForGoalInDataset(needDataset, goal, proposalModel);
                                    Precondition precondition = new Precondition(preconditionUri, result.isConform()); //TODO: GOAL INSTANTIATION PRODUCER DUPLICATES THE DATA SOMEHOW AND THUS MAKING THE INITIALLY CONFORM GOAL NOT MET

                                    logger.trace("\t\t\tPrecondition: " + precondition);

                                    //TODO: WE MIGHT NEED TO CHECK WHETHER THE PRECONDITION IS ACTUALLY FULFILLED OR NOT BEFORE WE REMOVE THE TEMP STATUS
                                    boolean preconditionMetPending = AnalyzeBehaviour.this.isPreconditionMetInProposals(precondition.getUri());
                                    logger.trace("\t\t\tRemove PreconditionMetPending Entry: "+preconditionMetPending);

                                    if(preconditionMetPending) {
                                        AnalyzeBehaviour.this.removePreconditionMetPending(precondition.getUri());
                                    }

                                    logger.trace("\t\t\tAdding Precondition/Proposal Relation");
                                    AnalyzeBehaviour.this.addPreconditionProposalRelation(precondition, proposal);
                                }else{
                                    logger.trace("\t\t\tPrecondition/Proposal Relation already present");
                                }
                            }

                            if(receivedMessage) {
                                logger.trace("\t\tSend ProposalReceivedEvent");
                                bus.publish(new ProposalReceivedEvent(connection, (WonMessageReceivedOnConnectionEvent) eventToAnalyze));
                            }
                        } else {
                            logger.trace("\t\tProposal: EMPTY");
                        }
                        break;

                    case REJECTS:
                        logger.trace("\tMessageEffect 'Rejects':");
                        logger.trace("\t\tremove Proposal References for: " + messageEffect.asRejects().getRejectedMessageUri());
                        AnalyzeBehaviour.this.removeProposalReferences(messageEffect.asRejects().getRejectedMessageUri());
                        break;

                    case RETRACTS:
                        logger.trace("\tMessageEffect 'Retracts':");
                        logger.trace("\t\tremove Proposal References for: " + messageEffect.asRetracts().getRetractedMessageUri());
                        AnalyzeBehaviour.this.removeProposalReferences(messageEffect.asRetracts().getRetractedMessageUri());
                        break;

                    default:
                        logger.error("This messageType is not implemented yet: " + messageEffect.getType());
                        break;
                }
            });
            logger.trace("--------------------------");

            //Things to do for each individual message regardless of it being received or sent
            Dataset remoteNeedDataset = ctx.getLinkedDataSource().getDataForResource(remoteNeedUri);
            Dataset conversationDataset = null;  //Initialize with null, to ensure some form of lazy init for the conversationDataset
            GoalInstantiationProducer goalInstantiationProducer = null;
            logger.trace("Conversation Information ------");

            for (Resource goal : goalsInNeed) {
                String preconditionUri = getUniqueGoalId(goal, needDataset, connectionUri);

                logger.trace("\tPreconditionUri: "+preconditionUri);

                if(AnalyzeBehaviour.this.isPreconditionMetInProposals(preconditionUri)){
                    logger.trace("\t\tPrecondition already met in a proposal/agreement");
                } else if(AnalyzeBehaviour.this.isPreconditionMetPending(preconditionUri)){
                    logger.trace("\t\tPrecondition already met by a pending proposal that does not exist yet");
                } else {
                    logger.trace("\t\tPrecondition not yet met in a proposal/agreement");
                    conversationDataset = getConversationDatasetLazyInit(conversationDataset, connectionUri);
                    goalInstantiationProducer = getGoalInstantiationProducerLazyInit(goalInstantiationProducer, needDataset, remoteNeedDataset, conversationDataset);

                    GoalInstantiationResult result = goalInstantiationProducer.findInstantiationForGoal(goal);
                    Boolean oldGoalState = AnalyzeBehaviour.this.getPreconditionConversationState(preconditionUri);
                    boolean newGoalState = result.getShaclReportWrapper().isConform();

                    if(oldGoalState == null || newGoalState != oldGoalState) {
                        logger.trace("\t\t\tState changed");
                        AnalyzeBehaviour.this.addPreconditionConversationState(preconditionUri, newGoalState);
                        if(newGoalState) {
                            logger.trace("\t\t\t\tadding PreconditionMetPending");
                            AnalyzeBehaviour.this.addPreconditionMetPending(preconditionUri);
                            logger.trace("\t\t\t\tsending PreconditionMetEvent");
                            ctx.getEventBus().publish(new PreconditionMetEvent(connection, preconditionUri, result));
                        }else{
                            logger.trace("\t\t\t\tsending PreconditionUnmetEvent");
                            ctx.getEventBus().publish(new PreconditionUnmetEvent(connection, preconditionUri, result));
                        }
                    }else{
                        logger.trace("\t\t\tNo state change");
                    }
                }
            }

            if(receivedMessage){
                publishAnalyzingCompleteMessage(connection);
            }
            logger.trace("################################## ANALYZING COMPLETE #########################################");
        }

        //********* Helper Methods **********
        private Dataset getConversationDatasetLazyInit(Dataset conversationDataset, URI connectionUri) {
            if(conversationDataset == null){
                return WonLinkedDataUtils.getConversationDataset(connectionUri, getEventListenerContext().getLinkedDataSource());
            }else{
                return conversationDataset;
            }
        }

        private GoalInstantiationProducer getGoalInstantiationProducerLazyInit(GoalInstantiationProducer goalInstantiationProducer, Dataset needDataset, Dataset remoteNeedDataset, Dataset conversationDataset){
            if(goalInstantiationProducer == null){
                return new GoalInstantiationProducer(needDataset, remoteNeedDataset, conversationDataset, "http://example.org/", "http://example.org/blended/");
            }else{
                return goalInstantiationProducer;
            }
        }

        private void publishAnalyzingMessage(Connection connection) {
            logger.trace("Publishing AnalyzingMessage for Connection: "+connection.getConnectionURI());
            Model messageModel = WonRdfUtils.MessageUtils.processingMessage(getEventListenerContext().getBotContextWrapper().getBotName() + " - Starting Analyzation");
            getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(connection, messageModel));
        }

        private void publishAnalyzingCompleteMessage(Connection connection) {
            logger.trace("Publishing AnalyzingCompleteMessage for Connection: "+connection.getConnectionURI());
            Model messageModel = WonRdfUtils.MessageUtils.processingMessage(getEventListenerContext().getBotContextWrapper().getBotName() + " - Analyzation complete");
            getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(connection, messageModel));
        }
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

            return connectionURI +"#"+ writer.toString().replaceAll("\\R", " ");
        }
    }

    private static String getWonMessageString(WonMessage wonMessage, Lang lang) {
        StringWriter writer = new StringWriter();
        RDFDataMgr.write(writer, wonMessage.getCompleteDataset(), lang);

        return writer.toString();
    }

    private static Connection makeConnection(URI needURI, URI remoteNeedURI, URI connectionURI){
        Connection con = new Connection();
        con.setConnectionURI(connectionURI);
        con.setNeedURI(needURI);
        con.setRemoteNeedURI(remoteNeedURI);
        return con;
    }

    /**
     * @param preconditionURI to retrieve the state from
     * @return the saved state of the precondition, null if the state was never saved before (undeterminable)
     */
    private Boolean getPreconditionConversationState(String preconditionURI) {
        return (Boolean) botContext.loadFromObjectMap(preconditionConversationStateMapName, preconditionURI);
    }

    /**
     * Saves the state of the precondition
     * @param preconditionURI to save the state of
     * @param state to save
     */
    private void addPreconditionConversationState(String preconditionURI, boolean state) {
        botContext.saveToObjectMap(preconditionConversationStateMapName, preconditionURI, state);
    }

    private void addPreconditionMetPending(String preconditionURI){
        botContext.saveToObjectMap(preconditionMetPending, preconditionURI, true);
    }

    private void removePreconditionMetPending(String preconditionURI){
        botContext.removeFromObjectMap(preconditionMetPending, preconditionURI);
    }

    private boolean isPreconditionMetPending(String preconditionURI) {
        return botContext.loadFromObjectMap(preconditionMetPending, preconditionURI) != null;
    }

    private void addPreconditionProposalRelation(Precondition precondition, Proposal proposal) {
        botContext.addToListMap(preconditionToProposalListMapName, precondition.getUri(), proposal);
        botContext.addToListMap(proposalToPreconditionListMapName, proposal.getUri().toString(), precondition);
    }

    public boolean hasPreconditionProposalRelation(String preconditionURI, String proposalURI) {
        return getPreconditionsForProposalUri(proposalURI).contains(new Precondition(preconditionURI, false)); //Status of the Precondition is irrelevant (equals works on uri alone)
    }

    public List<Proposal> getProposalsForPreconditionUri(String preconditionURI){
        return (List<Proposal>)(List<?>) botContext.loadFromListMap(preconditionToProposalListMapName, preconditionURI);
    }

    /**
     * Returns a List of All saved Precondition URIS for the proposal
     * @param proposalURI string of the proposaluri to retrieve the preconditionList of
     * @return List of all URI-Strings of preconditions save for the given connectionURI
     */
    public List<Precondition> getPreconditionsForProposalUri(String proposalURI) {
        return (List<Precondition>)(List<?>) botContext.loadFromListMap(proposalToPreconditionListMapName, proposalURI);
    }

    /**
     * @param preconditionURI string of the uri of the precondition
     * @return true if the given precondition is met by at least one proposal
     */
    public boolean isPreconditionMetInProposals(String preconditionURI) {
        List<Proposal> proposals = getProposalsForPreconditionUri(preconditionURI);
        for (Proposal p : proposals) {
            List<Precondition> preconditions = getPreconditionsForProposalUri(p.getUri().toString());
            for (Precondition condition : preconditions) {
                if (condition.isMet()) return true;
            }
        }
        return false;
    }

    /**
     * @param proposalUri uri of the proposal to retrieve the preconditionList of
     * @return true if there is at least one Met Precondition for this proposal
     */
    public boolean hasMetPrecondition(URI proposalUri) {
        List<Precondition> preconditions = getPreconditionsForProposalUri(proposalUri.toString());

        for(Precondition p : preconditions) {
            if(p.isMet()) return true;
        }
        return false;
    }

    /**
     * Removes All the stored entries in all Maps Lists or MapList for the given ProposalURI
     * @param proposalURI to be removed
     */
    public void removeProposalReferences(URI proposalURI) {
        removeProposalReferences(proposalURI.toString());
    }

    /**
     * Removes All the stored entries in all Maps Lists or MapList for the given ProposalURI
     * @param proposalURI the string of the proposalURI to be removed
     */
    public void removeProposalReferences(String proposalURI) {
        botContext.removeFromListMap(proposalToPreconditionListMapName, proposalURI);
        botContext.removeLeavesFromListMap(preconditionToProposalListMapName, proposalURI);
    }
}
