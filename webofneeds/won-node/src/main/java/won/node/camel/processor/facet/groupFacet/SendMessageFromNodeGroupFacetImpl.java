package won.node.camel.processor.facet.groupFacet;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractFromOwnerCamelProcessor;
import won.node.camel.processor.annotation.FacetMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.model.FacetType;
import won.protocol.repository.ConnectionRepository;
import won.protocol.util.WonRdfUtils;
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
public class SendMessageFromNodeGroupFacetImpl extends AbstractFromOwnerCamelProcessor
{
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private ConnectionRepository connectionRepository;


  @Override
  public void process(final Exchange exchange) throws Exception {
    final WonMessage wonMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
    final Connection conOfIncomingMessage = connectionRepository.findByConnectionURI(wonMessage.getReceiverURI()).get(0);

    final List<Connection> consInGroup = connectionRepository.findByNeedURIAndStateAndTypeURI(conOfIncomingMessage.getNeedURI(),
      ConnectionState.CONNECTED, FacetType.GroupFacet.getURI());
    exchange.getIn().setHeader(WonCamelConstants.SUPPRESS_MESSAGE_TO_OWNER, Boolean.TRUE);
    if (consInGroup == null || consInGroup.size() < 2) return;
    if (logger.isDebugEnabled()) {
      logger.debug("sending group message {} from need {} to {} other needs in group (text message: '{}'}", new Object[]{wonMessage.getMessageURI(), wonMessage.getSenderNeedURI(), consInGroup.size() - 1, WonRdfUtils.MessageUtils.getTextMessage(wonMessage)});
    }
    for (final Connection conToSendTo : consInGroup) {
      try {
        if (!conToSendTo.equals(conOfIncomingMessage)) {
          if (logger.isDebugEnabled()) {
            logger.debug("sending group message {} from need {} to need {}", new Object[]{wonMessage.getMessageURI(), wonMessage.getSenderNeedURI(), conToSendTo.getRemoteNeedURI()});
          }
          URI forwardedMessageURI = wonNodeInformationService.generateEventURI(wonMessage.getReceiverNodeURI());
          URI remoteWonNodeUri = WonLinkedDataUtils.getWonNodeURIForNeedOrConnectionURI(conOfIncomingMessage.getRemoteConnectionURI(),
            linkedDataSource);
          WonMessage newWonMessage = WonMessageBuilder.forwardReceivedNodeToNodeMessageAsNodeToNodeMessage(
            forwardedMessageURI, wonMessage,
                  conToSendTo.getConnectionURI(), conToSendTo.getNeedURI(), wonMessage.getReceiverNodeURI(),
                  conToSendTo.getRemoteConnectionURI(), conToSendTo.getRemoteNeedURI(), remoteWonNodeUri);
          sendSystemMessage(newWonMessage);
        }
      } catch (Exception e) {
        logger.warn("caught Exception:", e);
      }
    }
  }

  public FacetType getFacetType() {
    return FacetType.GroupFacet;
  }
}
