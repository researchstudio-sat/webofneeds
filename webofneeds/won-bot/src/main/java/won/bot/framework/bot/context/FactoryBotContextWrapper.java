package won.bot.framework.bot.context;


import java.net.URI;

public class FactoryBotContextWrapper extends BotContextWrapper {
    private String factoryListName;
    private String factoryInternalIdName;

    public FactoryBotContextWrapper(BotContext botContext, String needCreateListName, String factoryListName, String factoryInternalIdName) {
        super(botContext, needCreateListName);
        this.factoryListName = factoryListName;
        this.factoryInternalIdName = factoryInternalIdName;
    }

    public String getFactoryListName() {
        return factoryListName;
    }

    public String getFactoryInternalIdName() {
        return factoryInternalIdName;
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
}
