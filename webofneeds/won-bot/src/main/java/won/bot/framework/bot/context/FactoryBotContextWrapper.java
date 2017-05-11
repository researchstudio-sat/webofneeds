package won.bot.framework.bot.context;

import java.net.URI;

public class FactoryBotContextWrapper extends BotContextWrapper {
    private String factoryListName = getBotName() + ":factoryList";
    private String factoryInternalIdName = getBotName() + ":factoryInternalId";
    private String factoryOfferToFactoryNeedMapName = getBotName() + ":factoryOfferToFactoryNeedMap";

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
        getBotContext().saveToObjectMap(factoryInternalIdName, offerURI.toString(), factoryNeedURI);
    }
}
