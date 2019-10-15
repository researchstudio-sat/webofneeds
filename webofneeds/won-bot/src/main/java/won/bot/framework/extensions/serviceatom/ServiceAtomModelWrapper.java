package won.bot.framework.extensions.serviceatom;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import won.bot.vocabulary.WXBOT;
import won.protocol.util.DefaultAtomModelWrapper;
import won.protocol.vocabulary.WXCHAT;
import won.protocol.vocabulary.WXHOLD;
import won.protocol.vocabulary.WXREVIEW;

import java.net.URI;
import java.util.Objects;

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
        Resource atom = this.getAtomModel().createResource(atomUri.toString());
        atom.addProperty(RDF.type, WXBOT.ServiceAtom);
        this.addSocket("#HolderSocket", WXHOLD.HolderSocketString);
        this.addSocket("#ChatSocket", WXCHAT.ChatSocketString);
        this.addSocket("#ReviewSocket", WXREVIEW.ReviewSocketString);
        this.setTitle(serviceAtomContent.getName());
        if (Objects.nonNull(serviceAtomContent.getDescription())) {
            this.setDescription(serviceAtomContent.getDescription());
        }
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
