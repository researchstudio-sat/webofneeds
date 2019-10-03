package won.bot.framework.eventbot.behaviour.botatom;

import org.apache.jena.query.Dataset;
import won.protocol.util.DefaultAtomModelWrapper;
import won.protocol.vocabulary.WXCHAT;
import won.protocol.vocabulary.WXHOLD;
import won.protocol.vocabulary.WXREVIEW;

import java.net.URI;

public class BotServiceAtomModelWrapper extends DefaultAtomModelWrapper {
    // TODO: ADD MORE SERVICE BOT ATOM CONTENT MAKE SOCKETS CONFIGURABLE
    private BotServiceAtomContent botServiceAtomContent;

    public BotServiceAtomModelWrapper(URI atomUri, BotServiceAtomContent botServiceAtomContent) {
        this(atomUri.toString(), botServiceAtomContent);
    }

    public BotServiceAtomModelWrapper(String atomUri, BotServiceAtomContent botServiceAtomContent) {
        super(atomUri);
        // SET CONTENT OBJECT
        this.botServiceAtomContent = botServiceAtomContent;
        // SET RDF STRUCTURE
        this.setTitle(botServiceAtomContent.getName());
        this.addSocket("#HolderSocket", WXHOLD.HolderSocketString);
        this.addSocket("#ChatSocket", WXCHAT.ChatSocketString);
        this.addSocket("#ReviewSocket", WXREVIEW.ReviewSocketString);
    }

    public BotServiceAtomModelWrapper(Dataset atomDataset) {
        super(atomDataset);
        // TODO: EXTRACT botServiceAtomContent
    }

    public BotServiceAtomContent getBotServiceAtomContent() {
        return botServiceAtomContent;
    }
}
