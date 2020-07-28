package won.bot.framework.extensions.serviceatom;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import won.bot.vocabulary.WXBOT;
import won.protocol.util.DefaultAtomModelWrapper;
import won.protocol.vocabulary.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ServiceAtomModelWrapper extends DefaultAtomModelWrapper {
    private final ServiceAtomContent serviceAtomContent;

    public ServiceAtomModelWrapper(URI atomUri, ServiceAtomContent serviceAtomContent) {
        this(atomUri.toString(), serviceAtomContent);
    }

    public ServiceAtomModelWrapper(String atomUri, ServiceAtomContent serviceAtomContent) {
        super(atomUri);
        // SET CONTENT OBJECT
        this.serviceAtomContent = serviceAtomContent;
        // SET RDF STRUCTURE
        Resource atom = this.getAtomModel().createResource(atomUri);
        atom.addProperty(RDF.type, WXBOT.ServiceAtom);
        if (Objects.nonNull(serviceAtomContent.getSockets())) {
            serviceAtomContent.getSockets().forEach(this::addSocket);
        }
        if (Objects.nonNull(serviceAtomContent.getName())) {
            this.setName(serviceAtomContent.getName());
        }
        if (Objects.nonNull(serviceAtomContent.getDescription())) {
            atom.addProperty(SCHEMA.DESCRIPTION, serviceAtomContent.getDescription());
        }
        if (Objects.nonNull(serviceAtomContent.getTermsOfService())) {
            atom.addProperty(SCHEMA.TERMS_OF_SERVICE, serviceAtomContent.getTermsOfService());
        }
        if (Objects.nonNull(serviceAtomContent.getTags())) {
            serviceAtomContent.getTags().forEach(tag -> atom.addProperty(WONCON.tag, tag));
        }
    }

    public ServiceAtomModelWrapper(Dataset atomDataset) {
        super(atomDataset);
        serviceAtomContent = new ServiceAtomContent(this.getSomeName());
        serviceAtomContent.setDescription(this.getSomeDescription());
        serviceAtomContent.setTermsOfService(getSomeContentPropertyStringValue(SCHEMA.TERMS_OF_SERVICE));
        serviceAtomContent.setTags(this.getTags(this.getAtomContentNode()));
        Map<String, String> sockets = new HashMap<>();
        this.getSocketTypeUriMap().forEach(
                        (socketUri, socketTypeUri) -> sockets.put(socketUri.toString(), socketTypeUri.toString()));
        serviceAtomContent.setSockets(sockets);
    }

    public ServiceAtomContent getServiceAtomContent() {
        return serviceAtomContent;
    }
}
