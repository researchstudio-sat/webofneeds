package won.bot.framework.bot.context;

import java.net.URI;
import java.util.*;

public class BotContextWrapper {
    private final String botName;
    private final String atomCreateListName;
    private final String nodeListName;
    private final String atomUriAssociation;
    private final Set<String> atomListNames;
    private BotContext botContext;

    public BotContextWrapper(BotContext botContext, String botName) {
        this(botContext, botName, (String[]) null);
    }

    public BotContextWrapper(BotContext botContext, String botName, String... additionalAtomListNames) {
        this.botContext = botContext;
        this.botName = botName;
        this.atomCreateListName = botName + ":atomList";
        this.nodeListName = botName + ":nodeList";
        this.atomUriAssociation = botName + ":atom_target_atom";
        if (additionalAtomListNames != null) {
            HashSet<String> atomListNames = new HashSet<>(Arrays.asList(additionalAtomListNames));
            atomListNames.add(this.atomCreateListName);
            this.atomListNames = atomListNames;
        } else {
            this.atomListNames = Collections.singleton(this.getAtomCreateListName());
        }
    }

    public String getBotName() {
        return botName;
    }

    public String getAtomCreateListName() {
        return atomCreateListName;
    }

    public String getNodeListName() {
        return nodeListName;
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

    /**
     * Retrieves all atomUris stored by this Bot (as long as they are also added
     * with the {@link this#rememberAtomUri(URI)} method or stored in any list with
     * a name within {@link this#atomListNames} )
     *
     * @return atomUris within all collections with the names stored in
     * {@link this#atomListNames}
     */
    public Set<URI> retrieveAllAtomUris() {
        HashSet<URI> allAtomUris = new HashSet<>();
        for (String atomListName : atomListNames) {
            allAtomUris.addAll(getBotContext().getUriList(atomListName));
        }
        return allAtomUris;
    }

    public boolean isAtomKnown(URI atomUri) {
        return retrieveAllAtomUris().contains(atomUri);
    }

    public void rememberAtomUri(URI atomUri) {
        if (!isAtomKnown(atomUri)) {
            getBotContext().appendToUriList(atomUri, atomCreateListName);
        }
    }

    public void removeAtomUri(URI atomUri) {
        getBotContext().removeFromUriList(atomUri, atomCreateListName);
    }

    public boolean isNodeKnown(URI nodeUri) {
        return getBotContext().isInUriList(nodeUri, nodeListName);
    }

    public void rememberNodeUri(URI nodeUri) {
        if (!isNodeKnown(nodeUri)) {
            getBotContext().appendToUriList(nodeUri, nodeListName);
        }
    }

    public void removeNodeUri(URI nodeUri) {
        getBotContext().removeFromUriList(nodeUri, nodeListName);
    }
}
