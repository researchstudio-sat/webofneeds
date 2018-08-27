package won.node.camel.processor.fixed;

import java.net.URI;
import java.util.Collection;

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
import won.protocol.repository.ConnectionRepository;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

/**
 * User: syim
 * Date: 02.03.2015
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
          return;
        }
        
        URI wonNodeFromWonMessage = wonMessage.getReceiverNodeURI();
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


        URI facet = WonRdfUtils.FacetUtils.getFacet(wonMessage);
        if (facet == null) {
            //get the first one of the need's supported facets. TODO: implement some sort of strategy for choosing a facet here (and in the matcher)
            Collection<URI> facets = dataService.getSupportedFacets(needURIFromWonMessage);
            if (facets.isEmpty()) throw new IllegalArgumentException(
                    "hint does not specify facets, falling back to using one of the need's supported facets failed as the need does not support any facets");
            //add the facet to the model.
            facet = facets.iterator().next();
        }
        //create Connection in Database
        Connection con = connectionRepository.findOneByNeedURIAndRemoteNeedURIAndTypeURIForUpdate(needURIFromWonMessage, otherNeedURIFromWonMessage, facet);
        if (con == null) {
            URI connectionUri = wonNodeInformationService.generateConnectionURI(
                    wonNodeFromWonMessage);
            con = dataService.createConnection(
                    connectionUri, needURIFromWonMessage, otherNeedURIFromWonMessage,
                    null, facet, ConnectionState.SUGGESTED, ConnectionEventType.MATCHER_HINT);
        }
        //build message to send to owner, put in header
        //set the receiver to the newly generated connection uri
        wonMessage.addMessageProperty(WONMSG.RECEIVER_PROPERTY, con.getConnectionURI());
    }

    private boolean isTooManyHints(URI needURIFromWonMessage) {
      long hintCount = connectionRepository.countByNeedURIAndState(needURIFromWonMessage, ConnectionState.SUGGESTED);
      return (hintCount > maxSuggestedConnectionCount );
    }


}
