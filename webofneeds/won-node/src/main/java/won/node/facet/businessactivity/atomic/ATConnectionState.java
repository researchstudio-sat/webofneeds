package won.node.facet.businessactivity.atomic;

import java.net.URI;

import won.protocol.model.Connection;

/**
 * User: Danijel Date: 20.3.14.
 */
public class ATConnectionState {
    private Connection con;
    private URI ownerStateUri;

    public ATConnectionState(Connection con, URI coordinatorStateUri) {
        this.con = con;
        this.ownerStateUri = coordinatorStateUri;
    }

    public Connection getCon() {
        return con;
    }

    public void setCon(Connection con) {
        this.con = con;
    }

    public URI getOwnerStateUri() {
        return ownerStateUri;
    }

    public void setOwnerStateUri(URI coordinatorStateUri) {
        this.ownerStateUri = coordinatorStateUri;
    }
}
