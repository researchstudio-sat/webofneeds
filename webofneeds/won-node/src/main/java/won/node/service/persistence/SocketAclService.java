package won.node.service.persistence;

import org.apache.jena.graph.Graph;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.sparql.graph.GraphFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.auth.socket.SocketAuthorizationSource;
import won.auth.socket.SocketAuthorizations;
import won.auth.socket.support.SocketAclAlgorithms;
import won.protocol.model.Atom;
import won.protocol.model.Connection;
import won.protocol.model.Socket;
import won.protocol.util.linkeddata.uriresolver.WonRelativeUriHelper;

import java.net.URI;
import java.util.Optional;
import java.util.Set;

@Component
public class SocketAclService {
    @Autowired
    SocketService socketService;
    @Autowired
    AtomService atomService;
    @Autowired
    ConnectionService connectionService;
    @Autowired
    SocketAuthorizationSource socketAuthorizationSource;
    SocketAclAlgorithms socketAuthorizationAclModifierAlgorithms = new SocketAclAlgorithms();

    /**
     * Adds the socket's <code>localAuth</code> authorizations to the atom's acl,
     * i.e., the authorizations that the socket of an atom specifies for the atom's
     * ACL.
     * <p>
     * This operation should be executed when the socket is added to the atom, ie.
     * upon atom creation or modification.
     * </p>
     */
    public void addLocalSocketAcls(URI atomURI, Set<Socket> socketEntities, Dataset atomDataset) {
        URI socketAclGraphUri = WonRelativeUriHelper.createSocketAclGraphURIForAtomURI(atomURI);
        Graph socketAcls = atomDataset.containsNamedModel(socketAclGraphUri.toString())
                        ? atomDataset.getNamedModel(socketAclGraphUri.toString()).getGraph()
                        : GraphFactory.createDefaultGraph();
        for (Socket socketEntity : socketEntities) {
            Optional<SocketAuthorizations> sa = socketAuthorizationSource
                            .getSocketAuthorizations(socketEntity.getTypeURI());
            if (sa.isPresent()) {
                socketAcls = socketAuthorizationAclModifierAlgorithms
                                .addAuthorizationsForSocket(socketAcls, sa.get().getLocalAuths(),
                                                socketEntity.getSocketURI(),
                                                atomURI);
            }
        }
        atomDataset.addNamedModel(socketAclGraphUri.toString(), ModelFactory.createModelForGraph(socketAcls));
    }

    /**
     * Adds the target socket's <code>targetAuth</code> authorizations to the atom's
     * acl, i.e., the authorizations the target socket of an atom's connection
     * specifies for the atom's ACL.
     * <p>
     * This operation should be executed when establishing a connection.
     * </p>
     */
    public void addTargetSocketAcls(Connection con) {
        URI atomUri = con.getAtomURI();
        URI socketAclGraphUri = WonRelativeUriHelper.createSocketAclGraphURIForAtomURI(atomUri);
        Atom atom = atomService.lockAtomRequired(atomUri);
        Dataset atomDataset = atom.getDatatsetHolder().getDataset();
        Graph socketAcls = atomDataset.containsNamedModel(socketAclGraphUri.toString())
                        ? atomDataset.getNamedModel(socketAclGraphUri.toString()).getGraph()
                        : GraphFactory.createDefaultGraph();
        Optional<URI> targetSocketType = socketService.getSocketType(con.getTargetSocketURI());
        Optional<SocketAuthorizations> sa = socketAuthorizationSource.getSocketAuthorizations(targetSocketType.get());
        if (sa.isPresent()) {
            socketAcls = socketAuthorizationAclModifierAlgorithms
                            .addAuthorizationsForSocket(socketAcls, sa.get().getTargetAuths(), con.getSocketURI(),
                                            con.getTargetAtomURI());
        }
        atomDataset.addNamedModel(socketAclGraphUri.toString(), ModelFactory.createModelForGraph(socketAcls));
    }

    /**
     * Removes the socket's <code>localAuth</code> authorizations from the atom's
     * acl, i.e., the authorizations that the socket of an atom specifies for the
     * atom's ACL.
     * <p>
     * This operation should be executed when the socket is removed from the atom,
     * ie. upon modification.
     * </p>
     */
    public void removeLocalSocketAcls(URI atomURI, Set<Socket> socketEntities, Dataset atomDataset) {
        URI socketAclGraphUri = WonRelativeUriHelper.createSocketAclGraphURIForAtomURI(atomURI);
        if (!atomDataset.containsNamedModel(socketAclGraphUri.toString())) {
            // nothing to do here
            return;
        }
        Graph socketAcls = atomDataset.getNamedModel(socketAclGraphUri.toString()).getGraph();
        for (Socket socketEntity : socketEntities) {
            socketAcls = socketAuthorizationAclModifierAlgorithms
                            .removeAuthorizationsForSocket(socketAcls, socketEntity.getSocketURI(),
                                            atomURI, true);
        }
        atomDataset.addNamedModel(socketAclGraphUri.toString(), ModelFactory.createModelForGraph(socketAcls));
    }

    /**
     * Removes the target socket's <code>targetAuth</code> authorizations from the
     * atom's acls i.e., the authorizations the target socket of an atom's
     * connection specifies for the atom's ACL.
     * <p>
     * This operation should be executed when closing an established connection.
     * </p>
     */
    public void removeTargetSocketAcls(Connection con) {
        URI atomUri = con.getAtomURI();
        URI socketAclGraphUri = WonRelativeUriHelper.createSocketAclGraphURIForAtomURI(atomUri);
        Atom atom = atomService.lockAtomRequired(atomUri);
        Dataset atomDataset = atom.getDatatsetHolder().getDataset();
        Graph socketAcls = atomDataset.containsNamedModel(socketAclGraphUri.toString())
                        ? atomDataset.getNamedModel(socketAclGraphUri.toString()).getGraph()
                        : GraphFactory.createDefaultGraph();
        boolean removeAsRequestingSocket = connectionService.hasEstablishedConnections(con.getSocketURI());
        socketAcls = socketAuthorizationAclModifierAlgorithms
                        .removeAuthorizationsForSocket(socketAcls, con.getSocketURI(),
                                        con.getTargetAtomURI(), removeAsRequestingSocket);
        atomDataset.addNamedModel(socketAclGraphUri.toString(), ModelFactory.createModelForGraph(socketAcls));
    }
}
