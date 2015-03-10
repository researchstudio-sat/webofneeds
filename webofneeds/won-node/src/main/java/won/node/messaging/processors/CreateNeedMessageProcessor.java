package won.node.messaging.processors;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import won.protocol.message.WonEnvelopeType;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageEncoder;
import won.protocol.model.Facet;
import won.protocol.model.MessageEventPlaceholder;
import won.protocol.model.Need;
import won.protocol.model.NeedState;
import won.protocol.util.WonRdfUtils;

import java.net.URI;
import java.util.List;

/**
 * User: syim
 * Date: 02.03.2015
 */
public class CreateNeedMessageProcessor extends AbstractInOutMessageProcessor
{



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
}
