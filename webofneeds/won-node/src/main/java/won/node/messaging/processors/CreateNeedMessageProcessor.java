package won.node.messaging.processors;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.node.annotation.FixedMessageProcessor;
import won.node.protocol.MatcherProtocolMatcherServiceClientSide;
import won.protocol.message.WonEnvelopeType;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageEncoder;
import won.protocol.model.Facet;
import won.protocol.model.MessageEventPlaceholder;
import won.protocol.model.Need;
import won.protocol.model.NeedState;
import won.protocol.repository.FacetRepository;
import won.protocol.repository.MessageEventRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.repository.rdfstorage.RDFStorageService;
import won.protocol.service.LinkedDataService;
import won.protocol.service.NeedManagementService;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;
import java.util.List;

/**
 * User: syim
 * Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction= WONMSG.TYPE_FROM_OWNER_STRING,messageType = WONMSG.TYPE_CREATE_STRING)
public class CreateNeedMessageProcessor extends AbstractInOutMessageProcessor
{

  @Autowired
  RDFStorageService rdfStorage;

  @Autowired
  protected NeedManagementService needManagementService;
  @Autowired
  protected NeedRepository needRepository;
  @Autowired
  protected FacetRepository facetRepository;
  @Autowired
  protected MessageEventRepository messageEventRepository;
  @Autowired
  protected LinkedDataService linkedDataService;
  @Autowired
  protected WonNodeInformationService wonNodeInformationService;
  @Autowired
  protected MatcherProtocolMatcherServiceClientSide matcherProtocolMatcherClient;

  @Override
  public Object process(final Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    String ownerApplicationID = message.getHeader("ownerApplicationId").toString();
    WonMessage wonMessage = message.getBody(WonMessage.class);
    WonMessage newWonMessage = WonMessageBuilder.wrapOutboundOwnerToNodeOrSystemMessageAsNodeToNodeMessage
      (wonMessage);
    // store the newWonMessage as it is
    logger.debug("STORING message with id {}", newWonMessage.getMessageURI());
    rdfStorage.storeDataset(newWonMessage.getMessageURI(),
                            WonMessageEncoder.encodeAsDataset(newWonMessage));


    // the model where all the information created by the WON node is stored
    // Update: the meta data model is not needed at the moment since the meta data
    // are generated on each request from the LinkedDataService
    // Model needMeta = ModelFactory.createDefaultModel();

    // the dataset which contains the need model graphs from the owner application
    Dataset needContent = newWonMessage.getMessageContent();

    URI needURI = getNeedURIFromWonMessage(needContent);

    if (!needURI.equals(newWonMessage.getSenderNeedURI()))
      throw new IllegalArgumentException("receiverNeedURI and NeedURI of the content are not equal");

    Need need = new Need();

    need.setState(NeedState.ACTIVE);
    need.setNeedURI(needURI);

    // ToDo (FS) check if the WON node URI corresponds with the WON node (maybe earlier in the message layer)
    need.setWonNodeURI(newWonMessage.getReceiverNodeURI());

    need = needRepository.save(need);

    // store the message event placeholder to keep the connection between need and message event
    messageEventRepository.save(new MessageEventPlaceholder(needURI, newWonMessage));

    List<Facet> facets = WonRdfUtils.NeedUtils.getFacets(needURI, needContent);
    if (facets.size() == 0)
      throw new IllegalArgumentException("at least one RDF node must be of type won:HAS_FACET");
    for (Facet f : facets) {
      // TODO: check if there is a implementation for the facet on the node
      facetRepository.save(f);
    }

    // remove connection container if the create message contains already one (or some)
    WonRdfUtils.NeedUtils.removeConnectionContainer(needContent, needURI);

    rdfStorage.storeDataset(needURI, needContent);
    needManagementService.authorizeOwnerApplicationForNeed(ownerApplicationID, need);

    // ToDo (FS): send the same newWonMessage or create a new one (with new type)?


    try {
      Dataset needDataset = linkedDataService.getNeedDataset(need.getNeedURI());
      WonMessage newNeedNotificationMessage =
        new WonMessageBuilder()
          .setMessagePropertiesForNeedCreatedNotification(wonNodeInformationService.generateEventURI(),
                                                          need.getNeedURI(), need.getWonNodeURI())
          .setWonEnvelopeType(WonEnvelopeType.FROM_NODE)
          .build(needDataset);
      matcherProtocolMatcherClient.needCreated(needURI, ModelFactory.createDefaultModel(),
      newNeedNotificationMessage);
    } catch (Exception e) {
      logger.warn("could not create NeedCreatedNotification", e);
    }
    //TODO: send needCreated Message with needURI?
    exchange.getOut().setBody(needURI);
    return needURI;
  }


  private URI getNeedURIFromWonMessage(final Dataset wonMessage) {
    URI needURI;
    needURI = WonRdfUtils.NeedUtils.getNeedURI(wonMessage);
    if (needURI == null) {
      throw new IllegalArgumentException("at least one RDF node must be of type won:Need");
    }
    return needURI;
  }

  public void setRdfStorage(final RDFStorageService rdfStorage) {
    this.rdfStorage = rdfStorage;
  }

  public void setNeedManagementService(final NeedManagementService needManagementService) {
    this.needManagementService = needManagementService;
  }

  public void setNeedRepository(final NeedRepository needRepository) {
    this.needRepository = needRepository;
  }

  public void setFacetRepository(final FacetRepository facetRepository) {
    this.facetRepository = facetRepository;
  }

  public void setMessageEventRepository(final MessageEventRepository messageEventRepository) {
    this.messageEventRepository = messageEventRepository;
  }

  public void setLinkedDataService(final LinkedDataService linkedDataService) {
    this.linkedDataService = linkedDataService;
  }

  public void setWonNodeInformationService(final WonNodeInformationService wonNodeInformationService) {
    this.wonNodeInformationService = wonNodeInformationService;
  }

  public void setMatcherProtocolMatcherClient(final MatcherProtocolMatcherServiceClientSide matcherProtocolMatcherClient) {
    this.matcherProtocolMatcherClient = matcherProtocolMatcherClient;
  }
}
