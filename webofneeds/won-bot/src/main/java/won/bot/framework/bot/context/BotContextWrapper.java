package won.bot.framework.bot.context;

import java.net.URI;
import java.util.List;

public class BotContextWrapper {
    public static final String KEY_NEED_REMOTE_NEED_ASSOCIATION = "need_remote_need";
    private final String botName;
    private final String needCreateListName = getBotName() + ":needList";
    private BotContext botContext;

    public BotContextWrapper(BotContext botContext, String botName) {
        this.botContext = botContext;
        this.botName = botName;
    }

    public String getBotName() {
        return botName;
    }

    public String getNeedCreateListName() {
        return needCreateListName;
    }

    public BotContext getBotContext() {
        return botContext;
    }

    public URI getUriAssociation(URI uri) {
        return (URI) getBotContext().loadFromObjectMap(KEY_NEED_REMOTE_NEED_ASSOCIATION, uri.toString());
    }

    public void addUriAssociation(URI uri, URI uri2) {
        // save the mapping between the original and the reaction in to the context.
        getBotContext().saveToObjectMap(KEY_NEED_REMOTE_NEED_ASSOCIATION, uri.toString(), uri2);
        getBotContext().saveToObjectMap(KEY_NEED_REMOTE_NEED_ASSOCIATION, uri2.toString(), uri);
    }

    public List<URI> getNeedCreateList() {
        return getBotContext().getNamedNeedUriList(needCreateListName);
    }
}
