package won.node.messaging.processors;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.node.annotation.FixedMessageProcessor;
import won.node.service.DataAccessService;
import won.protocol.exception.ConnectionAlreadyExistsException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.ConnectionState;
import won.protocol.model.MessageEventPlaceholder;
import won.protocol.repository.MessageEventRepository;
import won.protocol.repository.rdfstorage.RDFStorageService;
import won.protocol.service.WonNodeInformationService;
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
@FixedMessageProcessor(direction= WONMSG.TYPE_FROM_EXTERNAL_STRING,messageType = WONMSG.TYPE_HINT_STRING)
public class HintMessageProcessor extends AbstractInOnlyMessageProcessor
{


  @Autowired
  WonNodeInformationService wonNodeInformationService;

  @Autowired
  RDFStorageService rdfStorage;

  @Autowired
  DataAccessService dataService;

  @Autowired
  MessageEventRepository messageEventRepository;

  public void process(Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = message.getBody(WonMessage.class);

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

    try {
      URI facet = WonRdfUtils.FacetUtils.getFacet(wonMessage);
      // ToDo (FS): adapt this part to the new message format (dont use content)
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
    } catch (ConnectionAlreadyExistsException e) {
      logger.warn("could not create connection", e);
    }

    URI wrappedMessageURI = this.wonNodeInformationService.generateEventURI();
    //TODO; hint messages are strictly said, not an inbound message since it doesn't have a remote counterpart. should be refactored
    WonMessage wrappedMessage  =  WonMessageBuilder
      .copyInboundNodeToNodeMessageAsNodeToOwnerMessage(wrappedMessageURI, con.getConnectionURI(), wonMessage);

    rdfStorage.storeDataset(wrappedMessageURI, wrappedMessage.getCompleteDataset());

    messageEventRepository.save(new MessageEventPlaceholder(
      con.getConnectionURI(), wrappedMessage));

    //reg.get(con).hint(con, wmScore, wmOriginator, facetModel, wrappedMessage);
             /*
      WonMessage newWonMessage = WonMessageBuilder.wrapOutboundOwnerToNodeOrSystemMessageAsNodeToNodeMessage(con.getConnectionURI(),
                                                                                       wonMessage);

      messageEventRepository.save(new MessageEventPlaceholder(con.getConnectionURI(), newWonMessage));
      logger.debug("STORING message with id {}", newWonMessage.getMessageURI());
      rdfStorageService.storeDataset(newWonMessage.getMessageURI(),
                                     newWonMessage.getCompleteDataset());

      reg.get(con).hint(con, wmScore, wmOriginator, facetModel, wonMessage);
                                */
    // messageEventRepository.save(new MessageEventPlaceholder(con.getConnectionURI(), wonMessage));

    //invoke facet implementation
    //reg.get(con).hint(con, wmScore, wmOriginator, facetModel, wonMessage);
  }

  public void setWonNodeInformationService(final WonNodeInformationService wonNodeInformationService) {
    this.wonNodeInformationService = wonNodeInformationService;
  }

  public void setRdfStorage(final RDFStorageService rdfStorage) {
    this.rdfStorage = rdfStorage;
  }

  public void setDataService(final DataAccessService dataService) {
    this.dataService = dataService;
  }

  public void setMessageEventRepository(final MessageEventRepository messageEventRepository) {
    this.messageEventRepository = messageEventRepository;
  }
}
