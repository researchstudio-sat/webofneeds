package won.bot.framework.bot.context;

import java.net.URI;
import java.util.List;

public class BotContextWrapper {
    public static final String KEY_ATOM_TARGET_ATOM_ASSOCIATION = "atom_target_atom";
    private final String botName;
    private final String atomCreateListName = getBotName() + ":atomList";
    private BotContext botContext;

    public BotContextWrapper(BotContext botContext, String botName) {
        this.botContext = botContext;
        this.botName = botName;
    }

    public String getBotName() {
        return botName;
    }

    public String getAtomCreateListName() {
        return atomCreateListName;
    }

    public BotContext getBotContext() {
        return botContext;
    }

    public URI getUriAssociation(URI uri) {
        return (URI) getBotContext().loadFromObjectMap(KEY_ATOM_TARGET_ATOM_ASSOCIATION, uri.toString());
    }

    public void addUriAssociation(URI uri, URI uri2) {
        // save the mapping between the original and the reaction in to the context.
        getBotContext().saveToObjectMap(KEY_ATOM_TARGET_ATOM_ASSOCIATION, uri.toString(), uri2);
        getBotContext().saveToObjectMap(KEY_ATOM_TARGET_ATOM_ASSOCIATION, uri2.toString(), uri);
    }

    public List<URI> getAtomCreateList() {
        return getBotContext().getNamedAtomUriList(atomCreateListName);
    }
}
