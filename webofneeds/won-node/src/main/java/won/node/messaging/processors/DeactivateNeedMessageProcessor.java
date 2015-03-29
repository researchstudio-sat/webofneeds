package won.node.messaging.processors;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.WonMessageProcessingException;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.model.Need;
import won.protocol.model.NeedState;
import won.protocol.util.DataAccessUtils;
import won.protocol.util.linkeddata.WonLinkedDataUtils;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;
import java.util.Collection;

/**
 * User: syim
 * Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction= WONMSG.TYPE_FROM_OWNER_STRING,messageType = WONMSG.TYPE_DEACTIVATE_STRING)
public class DeactivateNeedMessageProcessor extends AbstractCamelProcessor
{
  Logger logger = LoggerFactory.getLogger(this.getClass());



  public void process(final Exchange exchange) throws Exception {
    WonMessage wonMessage = (WonMessage) exchange.getIn().getHeader(WonCamelConstants.MESSAGE_HEADER);
    URI receiverNeedURI = wonMessage.getReceiverNeedURI();
    logger.debug("DEACTIVATING need. needURI:{}", receiverNeedURI);
    if (receiverNeedURI == null) throw new WonMessageProcessingException("receiverNeedURI is not set");
    Need need = DataAccessUtils.loadNeed(needRepository, receiverNeedURI);
    need.setState(NeedState.INACTIVE);
    need = needRepository.save(need);


    //close all connections
    Collection<Connection> conns = connectionRepository.getConnectionsByNeedURIAndNotInState(need.getNeedURI
      (), ConnectionState.CLOSED);
    for (Connection con: conns) {
      closeConnection(need, con);
    }
    matcherProtocolMatcherClient.needDeactivated(need.getNeedURI(), wonMessage);
  }

  public void closeConnection(final Need need, final Connection con) {
    URI messageURI = wonNodeInformationService.generateEventURI();
    URI remoteWonNode = WonLinkedDataUtils.getWonNodeURIForNeedOrConnectionURI(con.getRemoteNeedURI(),
      linkedDataSource);
    WonMessage message = new WonMessageBuilder().setMessagePropertiesForClose(messageURI,
      con.getConnectionURI(), con.getNeedURI(), need.getWonNodeURI(), con.getRemoteConnectionURI(),
      con.getRemoteNeedURI(), remoteWonNode).build();
    sendSystemMessageToRemoteNode(message);
  }

}
