package won.node.camel.processor.fixed;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageReactionProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.model.Facet;
import won.protocol.util.linkeddata.WonLinkedDataUtils;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;
import java.util.Optional;

@Component
@FixedMessageReactionProcessor(direction = WONMSG.TYPE_FROM_EXTERNAL_STRING, messageType = WONMSG.TYPE_OPEN_STRING)
public class OpenMessageFromNodeReactionProcessor extends AbstractCamelProcessor {

  @Override
  public void process(Exchange exchange) throws Exception {
    // if the connection's facet isAutoOpen and the connection state is
    // REQUEST_RECEIVED
    Message message = exchange.getIn();
    WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
    Optional<URI> needURI = Optional.of(wonMessage.getReceiverNeedURI());
    Optional<URI> connectionURI = Optional.of(wonMessage.getReceiverURI());
    if (connectionURI.isPresent() && needURI.isPresent()) {
      Optional<Connection> con = connectionRepository.findOneByConnectionURIForUpdate(connectionURI.get());
      if (con.isPresent() && con.get().getState() == ConnectionState.REQUEST_RECEIVED) {
        Facet facet = facetRepository.findOneByFacetURI(con.get().getFacetURI());
        Optional<URI> remoteFacet = WonLinkedDataUtils.getTypeOfFacet(con.get().getRemoteFacetURI(), linkedDataSource);
        if (remoteFacet.isPresent() && facetService.isAutoOpen(facet.getTypeURI(), remoteFacet.get())) {
          sendAutoOpenForOpen(wonMessage);
        }
      }
    }

  }

  private void sendAutoOpenForOpen(WonMessage connectMessageToReactTo) {
    URI fromWonNodeURI = connectMessageToReactTo.getReceiverNodeURI();
    URI messageURI = wonNodeInformationService.generateEventURI(fromWonNodeURI);
    WonMessage msg = WonMessageBuilder
        .setMessagePropertiesForOpen(messageURI, connectMessageToReactTo,
            "This is an automatic OPEN message sent by the WoN node")
        .setWonMessageDirection(WonMessageDirection.FROM_SYSTEM).build();
    logger.info("sending auto-open for connection {}, reacting to open", msg.getSenderURI());
    super.sendSystemMessage(msg);
  }

}
