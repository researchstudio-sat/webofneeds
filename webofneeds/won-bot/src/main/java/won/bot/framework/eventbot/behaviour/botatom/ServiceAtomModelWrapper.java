package won.bot.framework.eventbot.behaviour.botatom;

import org.apache.jena.query.Dataset;
import won.protocol.util.DefaultAtomModelWrapper;
import won.protocol.vocabulary.WXCHAT;
import won.protocol.vocabulary.WXHOLD;
import won.protocol.vocabulary.WXREVIEW;

import java.net.URI;

public class ServiceAtomModelWrapper extends DefaultAtomModelWrapper {
    // TODO: ADD MORE SERVICE BOT ATOM CONTENT MAKE SOCKETS CONFIGURABLE
    private ServiceAtomContent serviceAtomContent;

    public ServiceAtomModelWrapper(URI atomUri, ServiceAtomContent serviceAtomContent) {
        this(atomUri.toString(), serviceAtomContent);
    }

    public ServiceAtomModelWrapper(String atomUri, ServiceAtomContent serviceAtomContent) {
        super(atomUri);
        // SET CONTENT OBJECT
        this.serviceAtomContent = serviceAtomContent;
        // SET RDF STRUCTURE
        this.setTitle(serviceAtomContent.getName());
        this.setDescription(serviceAtomContent.getDescription());
        this.addSocket("#HolderSocket", WXHOLD.HolderSocketString);
        this.addSocket("#ChatSocket", WXCHAT.ChatSocketString);
        this.addSocket("#ReviewSocket", WXREVIEW.ReviewSocketString);
    }

    public ServiceAtomModelWrapper(Dataset atomDataset) {
        super(atomDataset);

        serviceAtomContent = new ServiceAtomContent(this.getSomeTitleFromIsOrAll());
        serviceAtomContent.setDescription(this.getSomeDescription());
    }

    public ServiceAtomContent getServiceAtomContent() {
        return serviceAtomContent;
    }
}
