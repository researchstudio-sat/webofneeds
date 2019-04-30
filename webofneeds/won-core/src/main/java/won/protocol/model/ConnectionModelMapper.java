package won.protocol.model;

import java.net.URI;
import java.util.Date;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;

import won.protocol.util.DateTimeUtils;
import won.protocol.util.ModelMapper;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.WON;

/**
 * User: gabriel Date: 09.04.13 T ime: 15:38
 */
public class ConnectionModelMapper implements ModelMapper<Connection> {
    @Override
    public Model toModel(Connection connection) {
        Model model = ModelFactory.createDefaultModel();
        Resource connectionMember = model.createResource(connection.getConnectionURI().toString())
                        .addProperty(WON.connectionState, WON.toResource(connection.getState()))
                        .addProperty(WON.sourceAtom, model.createResource(connection.getAtomURI().toString()));
        connectionMember.addProperty(RDF.type, WON.Connection);
        if (connection.getTargetConnectionURI() != null) {
            Resource targetConnection = model.createResource(connection.getTargetConnectionURI().toString());
            connectionMember.addProperty(WON.targetConnection, targetConnection);
        }
        if (connection.getTargetAtomURI() != null) {
            Resource targetAtom = model.createResource(connection.getTargetAtomURI().toString());
            connectionMember.addProperty(WON.targetAtom, targetAtom);
        }
        Literal lastUpdate = DateTimeUtils.toLiteral(connection.getLastUpdate(), model);
        if (lastUpdate != null) {
            connectionMember.addProperty(DCTerms.modified, lastUpdate);
        }
        // we need the following check for old connections so we can still generate RDF
        // for them.
        if (connection.getSocketURI() != null) {
            Resource socket = model.createResource(connection.getSocketURI().toString());
            connectionMember.addProperty(WON.socket, socket);
            socket.addProperty(WON.socketDefinition, model.getResource(connection.getTypeURI().toString()));
            if (connection.getTargetSocketURI() != null) {
                connectionMember.addProperty(WON.targetSocket,
                                model.getResource(connection.getTargetSocketURI().toString()));
            }
        }
        return model;
    }

    @Override
    public Connection fromModel(Model model) {
        URI connectionURI = RdfUtils.findFirstSubjectUri(model, RDF.type, WON.Connection, false, true);
        if (connectionURI == null)
            return null;
        Resource connectionRes = model.getResource(connectionURI.toString());
        Connection connection = new Connection();
        connection.setConnectionURI(URI.create(connectionRes.getURI()));
        URI connectionStateURI = URI.create(connectionRes.getProperty(WON.connectionState).getResource().getURI());
        connection.setState(ConnectionState.parseString(connectionStateURI.getFragment()));
        Statement targetConnectionStmt = connectionRes.getProperty(WON.targetConnection);
        if (targetConnectionStmt != null) {
            connection.setTargetConnectionURI(
                            URI.create(connectionRes.getProperty(WON.targetConnection).getResource().getURI()));
        }
        connection.setAtomURI(URI.create(connectionRes.getProperty(WON.sourceAtom).getResource().getURI()));
        connection.setTargetAtomURI(URI.create(connectionRes.getProperty(WON.targetAtom).getResource().getURI()));
        connection.setSocketURI(URI.create(connectionRes.getProperty(WON.socket).getResource().getURI()));
        if (connectionRes.hasProperty(WON.targetSocket)) {
            connection.setTargetSocketURI(
                            URI.create(connectionRes.getProperty(WON.targetSocket).getResource().getURI()));
        }
        connection.setTypeURI(URI.create(connectionRes.getProperty(WON.socket).getProperty(WON.socketDefinition)
                        .getResource().getURI()));
        Date lastUpdate = DateTimeUtils.parse(connectionRes.getProperty(DCTerms.modified).getString(), model);
        connection.setLastUpdate(lastUpdate);
        return connection;
    }
}
