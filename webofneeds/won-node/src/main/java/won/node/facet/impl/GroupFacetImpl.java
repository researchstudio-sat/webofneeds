package won.node.facet.impl;

import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.model.FacetType;
import won.protocol.repository.ConnectionRepository;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 16.09.13
 * Time: 18:42
 * To change this template use File | Settings | File Templates.
 */
public class GroupFacetImpl extends AbstractFacet
{
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private ConnectionRepository connectionRepository;

  @Override
  public FacetType getFacetType() {
    return FacetType.GroupFacet;
  }

  @Override
  public void sendMessageFromNeed(final Connection con, final Model message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
    final List<Connection> cons = connectionRepository.findByNeedURIAndStateAndTypeURI(con.getNeedURI(),
      ConnectionState.CONNECTED, FacetType.GroupFacet.getURI());
      //inform the other side
      executorService.execute(new Runnable() {
        @Override
        public void run() {
          for (final Connection c : cons) {
            try {
              if(! c.equals(con)) {
                needFacingConnectionClient.sendMessage(c, message);
              }
          } catch (Exception e) {
            logger.warn("caught Exception:", e);
          }
        }
      }
    });
   }

}
