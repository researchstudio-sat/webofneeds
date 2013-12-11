package won.owner.protocol.impl;

import com.hp.hpl.jena.rdf.model.Model;

import javax.jms.MapMessage;
import java.net.URI;

/**
 * User: LEIH-NB
 * Date: 21.10.13
 */
public class NeedCreationMessage {
    URI ownerURI;
    Model content;
    boolean activation;

    public URI getOwnerURI() {
        return ownerURI;
    }

    public void setOwnerURI(URI ownerURI) {
        this.ownerURI = ownerURI;
    }

    public Model getContent() {
        return content;
    }

    public void setContent(Model content) {
        this.content = content;
    }

    public boolean isActivation() {
        return activation;
    }

    public void setActivation(boolean activation) {
        this.activation = activation;
    }
}
