package won.bot.framework.bot.context;

import won.bot.framework.eventbot.action.impl.factory.model.Precondition;
import won.bot.framework.eventbot.action.impl.factory.model.Proposal;

import java.net.URI;
import java.util.List;

public class FactoryBotContextWrapper extends BotContextWrapper {
    private final String factoryListName = getBotName() + ":factoryList";
    private final String factoryInternalIdName = getBotName() + ":factoryInternalId";
    private final String factoryOfferToFactoryNeedMapName = getBotName() + ":factoryOfferToFactoryNeedMap";
    private final String connectionToPreconditionListMapName = getBotName() + ":connectionToPreconditionListMap";
    private final String connectionToProposalListMapName = getBotName() + ":connectionToProposalListMap";
    private final String preconditionToConnectionMapName = getBotName() + ":preconditionToConnectionMap";
    private final String preconditionToProposalListMapName = getBotName() + ":preconditionToProposalListMap";
    private final String preconditionConversationStateMapName = getBotName() + ":preconditionConversationStateMap";
    private final String proposalToPreconditionListMapName = getBotName() + ":proposalToPreconditionListMap";
    private final String proposalToConnectionMapName = getBotName() + ":proposalToConnectionMap";
    private final String preconditionMetPending = getBotName() + ":preconditionMetPending";

    public FactoryBotContextWrapper(BotContext botContext, String botName) {
        super(botContext, botName);
    }

    public String getFactoryListName() {
        return factoryListName;
    }

    public boolean isFactoryNeed(URI uri) {
        return getBotContext().isInNamedNeedUriList(uri, factoryListName);
    }

    public URI getURIFromInternal(URI uri) {
        return (URI) getBotContext().loadFromObjectMap(factoryInternalIdName, uri.toString());
    }

    public void addInternalIdToUriReference(URI internalUri, URI uri){
        getBotContext().saveToObjectMap(factoryInternalIdName, internalUri.toString(), uri);
    }

    public URI getFactoryNeedURIFromOffer(URI offerURI) {
        return (URI) getBotContext().loadFromObjectMap(factoryOfferToFactoryNeedMapName, offerURI.toString());
    }

    public void addFactoryNeedURIOfferRelation(URI offerURI, URI factoryNeedURI){
        getBotContext().saveToObjectMap(factoryOfferToFactoryNeedMapName, offerURI.toString(), factoryNeedURI);
    }

    /**
     * @param preconditionURI to retrieve the state from
     * @return the saved state of the precondition, null if the state was never saved before (undeterminable)
     */
    public Boolean getPreconditionConversationState(String preconditionURI) {
        return (Boolean) getBotContext().loadFromObjectMap(preconditionConversationStateMapName, preconditionURI);
    }

    /**
     * Saves the state of the precondition
     * @param preconditionURI to save the state of
     * @param state to save
     */
    public void addPreconditionConversationState(String preconditionURI, boolean state) {
        getBotContext().saveToObjectMap(preconditionConversationStateMapName, preconditionURI, state);
    }

    public void addPreconditionMetPending(String preconditionURI){
        getBotContext().saveToObjectMap(preconditionMetPending, preconditionURI, true);
    }

    public void removePreconditionMetPending(String preconditionURI){
        getBotContext().removeFromObjectMap(preconditionMetPending, preconditionURI);
    }

    public boolean isPreconditionMetPending(String preconditionURI) {
        return getBotContext().loadFromObjectMap(preconditionMetPending, preconditionURI) != null;
    }

    /**
     * Adds one or more preconditions to the given connection Uri ListMap
     * @param connectionURI a single connectionUri to store as the key of the ListMap
     * @param precondition one or more preconditions that should be linked with the connection
     */
    public void addConnectionPrecondition(URI connectionURI, Precondition... precondition) {
        getBotContext().addToListMap(connectionToPreconditionListMapName, connectionURI.toString(), precondition);

        for(Precondition precon : precondition) {
            getBotContext().saveToObjectMap(preconditionToConnectionMapName, precon.getUri(), connectionURI);
        }
    }

    /**
     * Returns a List of All saved Precondition URIS for the connection
     * @param connectionURI uri of the connection to retrieve the preconditionList of
     * @return List of all URI-Strings of preconditions save for the given connectionURI
     */
    public List<String> getPreconditionsForConnectionUri(URI connectionURI) {
        return getPreconditionsForConnectionUri(connectionURI.toString());
    }

    /**
     * Returns a List of All saved Precondition URIS for the connection
     * @param connectionURI string of the uri of the connection to retrieve the preconditionList of
     * @return List of all URI-Strings of preconditions save for the given connectionURI
     */
    public List<String> getPreconditionsForConnectionUri(String connectionURI) {
        return (List<String>)(List<?>) getBotContext().loadFromListMap(connectionToPreconditionListMapName, connectionURI);
    }

    /**
     * @param preconditionURI the uri to retrieve the connection uri for
     * @return Returns the connection uri for a certain precondition uri
     */
    public URI getConnectionURIFromPreconditionURI(String preconditionURI) {
        return (URI) getBotContext().loadFromObjectMap(preconditionToConnectionMapName, preconditionURI);
    }

    public void addPreconditionProposalRelation(Precondition precondition, Proposal proposal) {
        //TODO: WE MIGHT NEED TO CHECK WHETHER THE PRECONDITION IS ACTUALLY FULFILLED OR NOT BEFORE WE REMOVE THE TEMP STATUS
        removePreconditionMetPending(precondition.getUri());
        getBotContext().addToListMap(preconditionToProposalListMapName, precondition.getUri(), proposal);
        getBotContext().addToListMap(proposalToPreconditionListMapName, proposal.getUri().toString(), precondition);
    }

    public boolean hasPreconditionProposalRelation(String preconditionURI, String proposalURI) {
        return getPreconditionsForProposalUri(proposalURI).contains(new Precondition(preconditionURI, false)); //Status of the Precondition is irrelevant (equals works on uri alone)
    }

    public List<Proposal> getProposalsForPreconditionUri(String preconditionURI){
        return (List<Proposal>)(List<?>) getBotContext().loadFromListMap(preconditionToProposalListMapName, preconditionURI);
    }

    public List<Proposal> getProposalsForConnectionUri(String connectionURI){
        return (List<Proposal>)(List<?>) getBotContext().loadFromListMap(connectionToPreconditionListMapName, connectionURI);
    }

    /**
     * Returns a List of All saved Precondition URIS for the proposal
     * @param proposalURI string of the proposaluri to retrieve the preconditionList of
     * @return List of all URI-Strings of preconditions save for the given connectionURI
     */
    public List<Precondition> getPreconditionsForProposalUri(String proposalURI) {
        return (List<Precondition>)(List<?>) getBotContext().loadFromListMap(proposalToPreconditionListMapName, proposalURI.toString());
    }

    /**
     * @param proposalURI uri of the proposal to retrieve the preconditionList of
     * @return true if there is at least one Met Precondition for this proposal
     */
    public boolean hasMetPrecondition(URI proposalURI) {
        return hasMetPrecondition(proposalURI.toString());
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
     * @param proposalURI string of the proposaluri to retrieve the preconditionList of
     * @return true if there is at least one Met Precondition for this proposal
     */
    public boolean hasMetPrecondition(String proposalURI) {
        List<Precondition> preconditions = getPreconditionsForProposalUri(proposalURI);

        for(Precondition p : preconditions) {
            if(p.isMet()) return true;
        }
        return false;
    }

    /**
     * Removes all the stored entries for a given connectionURI
     * @param connectionURI the string of the connection URI that is to be removed from here
     */
    public void removeConnectionReferences(String connectionURI) {
        getPreconditionsForConnectionUri(connectionURI).forEach(this::removePreconditionReferences);
        getProposalsForConnectionUri(connectionURI).forEach(this::removeProposalReferences);

        getBotContext().removeFromListMap(connectionToPreconditionListMapName, connectionURI);
        getBotContext().removeFromListMap(connectionToProposalListMapName, connectionURI);
    }

    /**
     * Removes All the stored entries in all Maps Lists or MapList for the given Proposal
     * @param proposal to be removed
     */
    public void removeProposalReferences(Proposal proposal) {
        removeProposalReferences(proposal.getUri());
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
        getBotContext().removeFromObjectMap(proposalToConnectionMapName, proposalURI);
        getBotContext().removeLeavesFromListMap(connectionToProposalListMapName, proposalURI);
        getBotContext().removeFromListMap(proposalToPreconditionListMapName, proposalURI);
        getBotContext().removeLeavesFromListMap(preconditionToProposalListMapName, proposalURI);
    }

    public void removePreconditionReferences(String preconditionURI) {
        removePreconditionMetPending(preconditionURI);
        getBotContext().removeFromObjectMap(preconditionConversationStateMapName, preconditionURI);
        getBotContext().removeLeavesFromListMap(connectionToPreconditionListMapName, new Precondition(preconditionURI, false)); //Status of the Precondition is irrelevant (equals works on uri alone)
        getBotContext().removeFromObjectMap(preconditionToConnectionMapName, preconditionURI);
        getBotContext().removeFromObjectMap(preconditionToProposalListMapName, preconditionURI);
        getBotContext().removeLeavesFromListMap(proposalToPreconditionListMapName, new Precondition(preconditionURI, false)); //Status of the Precondition is irrelevant (equals works on uri alone)
    }
}
