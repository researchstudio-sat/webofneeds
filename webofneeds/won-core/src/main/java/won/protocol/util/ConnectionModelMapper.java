package won.protocol.util;

import com.hp.hpl.jena.rdf.model.*;
import won.protocol.model.Connection;
import won.protocol.vocabulary.WON;

import java.net.URI;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 09.04.13
 * Time: 15:38
 * To change this template use File | Settings | File Templates.
 */
public class ConnectionModelMapper implements ModelMapper<Connection>
{

  //TODO: needs rewriting
  @Override
  public Model toModel(Connection connection)
  {
    Model model = ModelFactory.createDefaultModel();
    Resource connectionMember = model.createResource(connection.getConnectionURI().toString());

    Resource remoteConnection = model.createResource(connection.getRemoteConnectionURI().toString());
    model.add(model.createStatement(connectionMember, WON.HAS_REMOTE_CONNECTION, remoteConnection));

    Resource eventContainer = model.createResource(WON.EVENT_CONTAINER);

//    connectionMember.addProperty(WON.IS_IN_STATE, connection.getState().name());

    return model;
  }

  @Override
  public Connection fromModel(Model model)
  {
    Connection c = new Connection();

    Resource connectionRes = model.getResource(WON.CONNECTION.toString());
    c.setConnectionURI(URI.create(connectionRes.getURI()));

    Statement remoteConnectionStat = connectionRes.getProperty(WON.HAS_REMOTE_CONNECTION);
    c.setRemoteConnectionURI(URI.create(remoteConnectionStat.getResource().getURI()));

    //c.setNeedURI(URI.create(rCon.listProperties(WON.BELONGS_TO_NEED).next().getString()));
    //c.setRemoteNeedURI(URI.create(rCon.listProperties(WON.REMOTE_NEED).next().getString()));

    return c;
  }
}
