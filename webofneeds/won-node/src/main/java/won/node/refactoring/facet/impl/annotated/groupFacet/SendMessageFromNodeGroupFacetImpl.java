package won.node.refactoring.facet.impl.annotated.groupFacet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.node.annotation.FacetMessageProcessor;
import won.node.refactoring.facet.impl.annotated.AbstractFacetAnnotated;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.model.FacetType;
import won.protocol.repository.ConnectionRepository;
import won.protocol.util.linkeddata.WonLinkedDataUtils;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 16.09.13
 * Time: 18:42
 * To change this template use File | Settings | File Templates.
 */
@Component
@FacetMessageProcessor(
  facetType = WON.GROUP_FACET_STRING,
  direction = WONMSG.TYPE_FROM_EXTERNAL_STRING,
  messageType = WONMSG.TYPE_CONNECTION_MESSAGE_STRING)
public class SendMessageFromNodeGroupFacetImpl extends AbstractFacetAnnotated
{
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private ConnectionRepository connectionRepository;


  @Override
  public void process(final WonMessage wonMessage) {
    final Connection con = connectionRepository.findByConnectionURI(wonMessage.getReceiverURI()).get(0);
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
             // needFacingConnectionClient.sendMessage(newWonMessage);
            }
          } catch (Exception e) {
            logger.warn("caught Exception:", e);
          }
        }
      }
    });
  }

  public FacetType getFacetType() {
    return FacetType.GroupFacet;
  }
}
