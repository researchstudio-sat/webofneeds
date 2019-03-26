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
        .addProperty(WON.HAS_CONNECTION_STATE, WON.toResource(connection.getState()))
        .addProperty(WON.BELONGS_TO_NEED, model.createResource(connection.getNeedURI().toString()));
    connectionMember.addProperty(RDF.type, WON.CONNECTION);
    if (connection.getRemoteConnectionURI() != null) {
      Resource remoteConnection = model.createResource(connection.getRemoteConnectionURI().toString());
      connectionMember.addProperty(WON.HAS_REMOTE_CONNECTION, remoteConnection);
    }
    if (connection.getRemoteNeedURI() != null) {
      Resource remoteNeed = model.createResource(connection.getRemoteNeedURI().toString());
      connectionMember.addProperty(WON.HAS_REMOTE_NEED, remoteNeed);
    }

    Literal lastUpdate = DateTimeUtils.toLiteral(connection.getLastUpdate(), model);
    if (lastUpdate != null) {
      connectionMember.addProperty(DCTerms.modified, lastUpdate);
    }

    // we need the following check for old connections so we can still generate RDF
    // for them.
    if (connection.getFacetURI() != null) {
      Resource facet = model.createResource(connection.getFacetURI().toString());
      connectionMember.addProperty(WON.HAS_FACET, facet);
      facet.addProperty(RDF.type, model.getResource(connection.getTypeURI().toString()));
      if (connection.getRemoteFacetURI() != null) {
        connectionMember.addProperty(WON.HAS_REMOTE_FACET,
            model.getResource(connection.getRemoteFacetURI().toString()));
      }
    }
    return model;
  }

  @Override
  public Connection fromModel(Model model) {
    URI connectionURI = RdfUtils.findFirstSubjectUri(model, RDF.type, WON.CONNECTION, false, true);
    if (connectionURI == null)
      return null;
    Resource connectionRes = model.getResource(connectionURI.toString());
    Connection connection = new Connection();

    connection.setConnectionURI(URI.create(connectionRes.getURI()));

    URI connectionStateURI = URI.create(connectionRes.getProperty(WON.HAS_CONNECTION_STATE).getResource().getURI());
    connection.setState(ConnectionState.parseString(connectionStateURI.getFragment()));
    Statement remoteConnectionStmt = connectionRes.getProperty(WON.HAS_REMOTE_CONNECTION);
    if (remoteConnectionStmt != null) {
      connection.setRemoteConnectionURI(
          URI.create(connectionRes.getProperty(WON.HAS_REMOTE_CONNECTION).getResource().getURI()));
    }
    connection.setNeedURI(URI.create(connectionRes.getProperty(WON.BELONGS_TO_NEED).getResource().getURI()));
    connection.setRemoteNeedURI(URI.create(connectionRes.getProperty(WON.HAS_REMOTE_NEED).getResource().getURI()));
    connection.setFacetURI(URI.create(connectionRes.getProperty(WON.HAS_FACET).getResource().getURI()));
    if (connectionRes.hasProperty(WON.HAS_REMOTE_FACET)) {
      connection.setRemoteFacetURI(URI.create(connectionRes.getProperty(WON.HAS_REMOTE_FACET).getResource().getURI()));
    }
    connection
        .setTypeURI(URI.create(connectionRes.getProperty(WON.HAS_FACET).getProperty(RDF.type).getResource().getURI()));
    Date lastUpdate = DateTimeUtils.parse(connectionRes.getProperty(DCTerms.modified).getString(), model);
    connection.setLastUpdate(lastUpdate);

    return connection;
  }
}
