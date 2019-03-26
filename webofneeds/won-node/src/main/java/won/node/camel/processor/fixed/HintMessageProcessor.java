package won.node.camel.processor.fixed;

import java.net.URI;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.ConnectionState;
import won.protocol.model.Facet;
import won.protocol.repository.ConnectionRepository;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

/**
 * User: syim Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction = WONMSG.TYPE_FROM_EXTERNAL_STRING, messageType = WONMSG.TYPE_HINT_STRING)
public class HintMessageProcessor extends AbstractCamelProcessor {

  @Autowired
  private ConnectionRepository connectionRepository;

  @Value("${ignore.hints.suggested.connection.count.max}")
  private Long maxSuggestedConnectionCount = 100L;

  public void process(Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);

    logger.debug("STORING message with id {}", wonMessage.getMessageURI());

    URI needURIFromWonMessage = wonMessage.getReceiverNeedURI();
    if (isTooManyHints(needURIFromWonMessage)) {
      exchange.getIn().setHeader(WonCamelConstants.IGNORE_HINT, Boolean.TRUE);
      return;
    }

    URI wonNodeFromWonMessage = wonMessage.getReceiverNodeURI();
    URI otherNeedURIFromWonMessage = URI.create(RdfUtils.findOnePropertyFromResource(wonMessage.getMessageContent(),
        wonMessage.getMessageURI(), WON.HAS_MATCH_COUNTERPART).asResource().getURI());

    double wmScore = RdfUtils
        .findOnePropertyFromResource(wonMessage.getMessageContent(), wonMessage.getMessageURI(), WON.HAS_MATCH_SCORE)
        .asLiteral().getDouble();
    URI wmOriginator = wonMessage.getSenderNodeURI();
    if (wmScore < 0 || wmScore > 1)
      throw new IllegalArgumentException("score is not in [0,1]");
    if (wmOriginator == null)
      throw new IllegalArgumentException("originator is not set");

    // facet: either specified or default
    URI facetURI = WonRdfUtils.FacetUtils.getFacet(wonMessage);
    // remote facet: either specified or null
    Optional<URI> remoteFacetURI = Optional.ofNullable(WonRdfUtils.FacetUtils.getRemoteFacet(wonMessage));
    Facet facet = dataService.getFacet(needURIFromWonMessage,
        facetURI == null ? Optional.empty() : Optional.of(facetURI));

    // create Connection in Database
    Optional<Connection> con = Optional.empty();
    if (remoteFacetURI.isPresent()) {
      con = connectionRepository.findOneByNeedURIAndRemoteNeedURIAndFacetURIAndRemoteFacetURIForUpdate(
          needURIFromWonMessage, otherNeedURIFromWonMessage, facet.getFacetURI(), remoteFacetURI.get());
    } else {
      con = connectionRepository.findOneByNeedURIAndRemoteNeedURIAndFacetURIAndNullRemoteFacetForUpdate(
          needURIFromWonMessage, otherNeedURIFromWonMessage, facet.getFacetURI());
    }
    if (!con.isPresent()) {
      URI connectionUri = wonNodeInformationService.generateConnectionURI(wonNodeFromWonMessage);
      con = Optional.of(dataService.createConnection(connectionUri, needURIFromWonMessage, otherNeedURIFromWonMessage,
          null, facet.getFacetURI(), facet.getTypeURI(), remoteFacetURI.orElse(null), ConnectionState.SUGGESTED,
          ConnectionEventType.MATCHER_HINT));
    }
    // build message to send to owner, put in header
    // set the receiver to the newly generated connection uri
    wonMessage.addMessageProperty(WONMSG.RECEIVER_PROPERTY, con.get().getConnectionURI());
  }

  private boolean isTooManyHints(URI needURIFromWonMessage) {
    long hintCount = connectionRepository.countByNeedURIAndState(needURIFromWonMessage, ConnectionState.SUGGESTED);
    return (hintCount > maxSuggestedConnectionCount);
  }

}
