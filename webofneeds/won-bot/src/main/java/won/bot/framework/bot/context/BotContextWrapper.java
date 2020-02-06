package won.bot.framework.bot.context;

import java.net.URI;
import java.util.List;

public class BotContextWrapper {
    private final String botName;
    private final String atomCreateListName;
    private final String atomUriAssociation;
    private BotContext botContext;

    public BotContextWrapper(BotContext botContext, String botName) {
        this.botContext = botContext;
        this.botName = botName;
        this.atomCreateListName = botName + ":atomList";
        this.atomUriAssociation = botName + ":atom_target_atom";
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
        return (URI) getBotContext().loadFromObjectMap(atomUriAssociation, uri.toString());
    }

    public void addUriAssociation(URI uri, URI uri2) {
        // save the mapping between the original and the reaction in to the context.
        getBotContext().saveToObjectMap(atomUriAssociation, uri.toString(), uri2);
        getBotContext().saveToObjectMap(atomUriAssociation, uri2.toString(), uri);
    }

    public List<URI> getAtomCreateList() {
        return getBotContext().getNamedAtomUriList(atomCreateListName);
    }

    public boolean isAtomKnown(URI atomUri) {
        return getBotContext().isInNamedAtomUriList(atomUri, atomCreateListName);
    }
}
