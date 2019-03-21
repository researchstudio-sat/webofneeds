package won.node.facet.impl;

import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.model.Connection;
import won.protocol.model.FacetType;
import won.protocol.repository.ConnectionRepository;
import won.protocol.util.DataAccessUtils;

import java.net.URI;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA. User: gabriel Date: 16.09.13 Time: 18:43 To
 * change this template use File | Settings | File Templates.
 */
public class FacetRegistry {
  @Autowired
  private ConnectionRepository connectionRepository;

  private HashMap<FacetType, FacetLogic> map;

  public FacetLogic get(Connection con) {
    return get(FacetType.getFacetType(con.getTypeURI()));
  }

  public FacetLogic get(URI connectionURI) throws NoSuchConnectionException {
    return get(
        FacetType.getFacetType(DataAccessUtils.loadConnection(connectionRepository, connectionURI).getTypeURI()));
  }

  public FacetLogic get(FacetType ft) {
    return map.get(ft);
  }

  public void register(FacetType ft, FacetLogic fi) {
    map.put(ft, fi);
  }

  public void setMap(HashMap<FacetType, FacetLogic> map) {
    this.map = map;
  }
}
