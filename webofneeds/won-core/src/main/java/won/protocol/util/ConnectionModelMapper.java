package won.protocol.util;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.vocabulary.WON;

import java.net.URI;

/**
 * User: gabriel
 * Date: 09.04.13
 * Time: 15:38
 */
public class ConnectionModelMapper implements ModelMapper<Connection>
{

  @Override
  public Model toModel(Connection connection)
  {
    Model model = ModelFactory.createDefaultModel();
    Resource connectionMember = model.createResource(connection.getConnectionURI().toString())
        .addProperty(WON.HAS_CONNECTION_STATE, WON.toResource(connection.getState()))
        .addProperty(WON.BELONGS_TO_NEED, model.createResource(connection.getNeedURI().toString()));

    if (connection.getRemoteConnectionURI() != null) {
      Resource remoteConnection = model.createResource(connection.getRemoteConnectionURI().toString());
      model.add(model.createStatement(connectionMember, WON.HAS_REMOTE_CONNECTION, remoteConnection));
    }

    return model;
  }

  @Override
  public Connection fromModel(Model model)
  {
    Connection connection = new Connection();

    Resource connectionRes = model.getResource(WON.CONNECTION.toString());
    connection.setConnectionURI(URI.create(connectionRes.getURI()));

    URI connectionStateURI = URI.create(connectionRes.getProperty(WON.HAS_CONNECTION_STATE).getResource().getURI());
    connection.setState(ConnectionState.parseString(connectionStateURI.getFragment()));

    connection.setRemoteConnectionURI(URI.create(connectionRes.getProperty(WON.HAS_REMOTE_CONNECTION).getResource().getURI()));

    connection.setNeedURI(URI.create(connectionRes.getProperty(WON.BELONGS_TO_NEED).getResource().getURI()));

    return connection;
  }
}
