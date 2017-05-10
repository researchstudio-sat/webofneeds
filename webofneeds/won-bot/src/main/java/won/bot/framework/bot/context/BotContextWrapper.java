package won.bot.framework.bot.context;

import won.bot.framework.eventbot.action.impl.needlifecycle.AbstractCreateNeedAction;

import java.net.URI;
import java.util.List;

public class BotContextWrapper {
    public static final String KEY_NEED_REMOTE_NEED_ASSOCIATION = "need_remote_need";

    private String needCreateListName;
    private BotContext botContext;

    public BotContextWrapper(BotContext botContext, String needCreateListName) {
        this.botContext = botContext;
        this.needCreateListName = needCreateListName;
    }

    public String getNeedCreateListName() {
        return needCreateListName;
    }

    public BotContext getBotContext() {
        return botContext;
    }

    public URI getUriAssociation(URI uri){
        return (URI) getBotContext().loadFromObjectMap(KEY_NEED_REMOTE_NEED_ASSOCIATION, uri.toString());
    }

    public void addUriAssociation(URI uri, URI uri2){
        // save the mapping between the original and the reaction in to the context.
        getBotContext().saveToObjectMap(KEY_NEED_REMOTE_NEED_ASSOCIATION, uri.toString(), uri2);
        getBotContext().saveToObjectMap(KEY_NEED_REMOTE_NEED_ASSOCIATION, uri2.toString(), uri);
    }

    public List<URI> getNeedCreateList(){
        return getBotContext().getNamedNeedUriList(needCreateListName);
    }
}
