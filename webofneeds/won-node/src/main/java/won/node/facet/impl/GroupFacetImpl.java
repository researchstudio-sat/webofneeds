package won.node.facet.impl;

import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.model.FacetType;
import won.protocol.repository.ConnectionRepository;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

import java.net.URI;
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
  public void sendMessageFromNeed(final Connection con, final Model message, final WonMessage wonMessage)
          throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
    final List<Connection> cons = connectionRepository.findByNeedURIAndStateAndTypeURI(con.getNeedURI(),
      ConnectionState.CONNECTED, FacetType.GroupFacet.getURI());
      //inform the other side
      executorService.execute(new Runnable() {
        @Override
        public void run() {
          for (final Connection c : cons) {
            try {
              if(! c.equals(con)) {
                URI forwardedMessageURI = wonNodeInformationService.generateEventURI(wonMessage.getReceiverNodeURI());
                URI remoteWonNodeUri = WonLinkedDataUtils.getWonNodeURIForNeedOrConnectionURI(con.getRemoteConnectionURI(),
                  linkedDataSource);
                WonMessage newWonMessage = WonMessageBuilder.forwardReceivedNodeToNodeMessageAsNodeToNodeMessage(
                  forwardedMessageURI, wonMessage,
                  con.getConnectionURI(), con.getNeedURI(), wonMessage.getReceiverNodeURI(),
                  con.getRemoteConnectionURI(), con.getRemoteNeedURI(), remoteWonNodeUri);
                needFacingConnectionClient.sendMessage(c, message, newWonMessage);
              }
          } catch (Exception e) {
            logger.warn("caught Exception:", e);
          }
        }
      }
    });
   }

}
