package won.bot.framework.bot.context;

import won.bot.framework.eventbot.action.impl.factory.model.Proposal;

import java.net.URI;
import java.util.List;

public class FactoryBotContextWrapper extends BotContextWrapper {
    private final String factoryListName = getBotName() + ":factoryList";
    private final String factoryInternalIdName = getBotName() + ":factoryInternalId";
    private final String factoryOfferToFactoryNeedMapName = getBotName() + ":factoryOfferToFactoryNeedMap";
    private final String preconditionStateMapName = getBotName() + ":preconditionStateMap";
    private final String connectionToPreconditionListMapName = getBotName() + ":connectionToPreconditionListMap";
    private final String preconditionToConnectionMapName = getBotName() + ":preconditionToConnectionMap";
    private final String precondtionToProposalListMapName = getBotName() + ":precondtionToProposalListMap";
    private final String proposalToPreconditionListMapName = getBotName() + ":proposalToPreconditionListMap";

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
    public Boolean getPreconditionState(String preconditionURI) {
        return (Boolean) getBotContext().loadFromObjectMap(preconditionStateMapName, preconditionURI);
    }

    /**
     * Saves the state of the precondition
     * @param preconditionURI to save the state of
     * @param state to save
     */
    public void addPreconditionState(String preconditionURI, boolean state) {
        getBotContext().saveToObjectMap(preconditionStateMapName, preconditionURI, state);
    }

    /**
     * Adds one or more preconditions to the given connection Uri ListMap
     * @param connectionURI a single connectionUri to store as the key of the ListMap
     * @param preconditionURI one or more preconditionUris that should be linked with the connection
     */
    public void addConnectionPrecondition(URI connectionURI, String... preconditionURI) {
        getBotContext().addToListMap(connectionToPreconditionListMapName, connectionURI.toString(), preconditionURI);

        for(String preconUri : preconditionURI) {
            getBotContext().saveToObjectMap(preconditionToConnectionMapName, preconUri, connectionURI);
        }
    }

    /**
     * Returns a List of All saved Precondition URIS for the connection
     * @param connectionURI uri of the connection to retrieve the preconditionList of
     * @return List of all URI-Strings of preconditions save for the given connectionURI
     */
    public List<String> getPreconditionsForConnectionUri(URI connectionURI) {
        return (List<String>)(List<?>) getBotContext().loadFromListMap(connectionToPreconditionListMapName, connectionURI.toString());
    }

    /**
     * @param preconditionURI the uri to retrieve the connection uri for
     * @return Returns the connection uri for a certain precondition uri
     */
    public URI getConnectionURIFromPreconditionURI(String preconditionURI) {
        return (URI) getBotContext().loadFromObjectMap(preconditionToConnectionMapName, preconditionURI);
    }

    public void addPreconditionProposalRelation(String preconditionURI, Proposal proposal) {
        getBotContext().addToListMap(precondtionToProposalListMapName, preconditionURI, proposal);
        getBotContext().addToListMap(proposalToPreconditionListMapName, proposal.getUri().toString(), preconditionURI);
    }

    public List<Proposal> getProposalsForPreconditionUri(String preconditionURI){
        return (List<Proposal>)(List<?>) getBotContext().loadFromListMap(precondtionToProposalListMapName, preconditionURI);
    }

    /**
     * Returns a List of All saved Precondition URIS for the connection
     * @param proposalURI uri of the proposal to retrieve the preconditionList of
     * @return List of all URI-Strings of preconditions save for the given connectionURI
     */
    public List<String> getPreconditionsForProposalUri(URI proposalURI) {
        return (List<String>)(List<?>) getBotContext().loadFromListMap(connectionToPreconditionListMapName, proposalURI.toString());
    }

    public void removeAllConnectionData(URI connectionURI) {
        //TODO: REMOVE ALL UNUSED VALUES (TO BE DETERMINED)
    }
}
