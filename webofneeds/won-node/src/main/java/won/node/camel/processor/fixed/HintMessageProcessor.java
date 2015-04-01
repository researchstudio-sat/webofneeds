package won.node.camel.processor.fixed;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.ConnectionState;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;
import java.util.Collection;

/**
 * User: syim
 * Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction = WONMSG.TYPE_FROM_EXTERNAL_STRING, messageType = WONMSG.TYPE_HINT_STRING)
public class HintMessageProcessor extends AbstractCamelProcessor
{


  public void process(Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);

    logger.debug("STORING message with id {}", wonMessage.getMessageURI());

    URI needURIFromWonMessage = wonMessage.getReceiverNeedURI();
    URI otherNeedURIFromWonMessage = URI.create(RdfUtils.findOnePropertyFromResource(
      wonMessage.getMessageContent(), wonMessage.getMessageURI(),
      WON.HAS_MATCH_COUNTERPART).asResource().getURI());
    double wmScore = RdfUtils.findOnePropertyFromResource(
      wonMessage.getMessageContent(), wonMessage.getMessageURI(),
      WON.HAS_MATCH_SCORE).asLiteral().getDouble();
    URI wmOriginator = wonMessage.getSenderNodeURI();
    if (wmScore < 0 || wmScore > 1) throw new IllegalArgumentException("score is not in [0,1]");
    if (wmOriginator == null)
      throw new IllegalArgumentException("originator is not set");

    //create Connection in Database
    Connection con = null;
    Model facetModel = ModelFactory.createDefaultModel();

    URI facet = WonRdfUtils.FacetUtils.getFacet(wonMessage);
    if (facet == null) {
      //get the first one of the need's supported facets. TODO: implement some sort of strategy for choosing a facet here (and in the matcher)
      Collection<URI> facets = dataService.getSupportedFacets(needURIFromWonMessage);
      if (facets.isEmpty()) throw new IllegalArgumentException(
        "hint does not specify facets, falling back to using one of the need's supported facets failed as the need does not support any facets");
      //add the facet to the model.
      facet = facets.iterator().next();
    }
    con = dataService.createConnection(
      needURIFromWonMessage, otherNeedURIFromWonMessage,
      null, facet, ConnectionState.SUGGESTED, ConnectionEventType.MATCHER_HINT);


    URI wrappedMessageURI = this.wonNodeInformationService.generateEventURI();
    //build message to send to owner, put in header
    final WonMessage newWonMessage = createMessageToSendToOwner(wonMessage, con);
    exchange.getIn().setHeader(WonCamelConstants.MESSAGE_HEADER, newWonMessage);
  }

  private WonMessage createMessageToSendToOwner(WonMessage wonMessage, Connection con) {
    //create the message to send to the owner
    return new WonMessageBuilder()
      .setPropertiesForPassingMessageToOwner(wonMessage)
        //set the uri of the newly created connection as receiver
      .setReceiverURI(con.getConnectionURI())
      .build();
  }


}
