package won.bot.framework.bot.context;


import won.bot.framework.component.factory.FactoryConstraintService;

import java.net.URI;

public class FactoryBotContextWrapper extends BotContextWrapper {
    private String factoryListName;
    private String factoryInternalIdName;
    private String factoryOfferToFactoryNeedMapName;

    private FactoryConstraintService factoryConstraintService;

    public FactoryBotContextWrapper(BotContext botContext, String needCreateListName, String factoryListName, String factoryInternalIdName, String factoryOfferToFactoryNeedMapName) {
        super(botContext, needCreateListName);
        this.factoryListName = factoryListName;
        this.factoryInternalIdName = factoryInternalIdName;
        this.factoryOfferToFactoryNeedMapName = factoryOfferToFactoryNeedMapName;
    }

    public String getFactoryListName() {
        return factoryListName;
    }

    public String getFactoryInternalIdName() {
        return factoryInternalIdName;
    }

    public String getFactoryOfferToFactoryNeedMapName() {
        return factoryOfferToFactoryNeedMapName;
    }

    public void setFactoryOfferToFactoryNeedMapName(String factoryOfferToFactoryNeedMapName) {
        this.factoryOfferToFactoryNeedMapName = factoryOfferToFactoryNeedMapName;
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
