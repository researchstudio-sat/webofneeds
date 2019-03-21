package won.node.camel.processor.fixed;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageReactionProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Connection;
import won.protocol.model.Facet;
import won.protocol.util.linkeddata.WonLinkedDataUtils;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;
import java.util.Optional;

@Component
@FixedMessageReactionProcessor(direction = WONMSG.TYPE_FROM_EXTERNAL_STRING, messageType = WONMSG.TYPE_CONNECT_STRING)
public class ConnectMessageFromNodeReactionProcessor extends AbstractCamelProcessor {

  Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public void process(Exchange exchange) throws Exception {
    // if the connection's facet isAutoOpen, send an open automatically.
    Message message = exchange.getIn();
    WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
    Optional<URI> needURI = Optional.of(wonMessage.getReceiverNeedURI());
    Optional<URI> connectionURI = Optional.of(wonMessage.getReceiverURI());
    if (connectionURI.isPresent() && needURI.isPresent()) {
      Optional<Connection> con = connectionRepository.findOneByConnectionURIForUpdate(connectionURI.get());
      if (con.isPresent()) {
        Facet facet = facetRepository.findOneByFacetURI(con.get().getFacetURI());
        Optional<URI> remoteFacet = WonLinkedDataUtils.getTypeOfFacet(con.get().getRemoteFacetURI(), linkedDataSource);
        if (remoteFacet.isPresent() && facetService.isAutoOpen(facet.getTypeURI(), remoteFacet.get())) {
          sendAutoOpenForConnect(wonMessage);
        }
      }
    }

  }

  private void sendAutoOpenForConnect(WonMessage connectMessageToReactTo) {

    URI fromWonNodeURI = connectMessageToReactTo.getReceiverNodeURI();
    URI messageURI = wonNodeInformationService.generateEventURI(fromWonNodeURI);
    WonMessage msg = WonMessageBuilder
        .setMessagePropertiesForOpen(messageURI, connectMessageToReactTo,
            "Connection request accepted automatically by WoN node")
        .setWonMessageDirection(WonMessageDirection.FROM_SYSTEM).build();
    logger.info("sending auto-open for connection {}, reacting to connect", msg.getSenderURI());
    super.sendSystemMessage(msg);
  }

}
