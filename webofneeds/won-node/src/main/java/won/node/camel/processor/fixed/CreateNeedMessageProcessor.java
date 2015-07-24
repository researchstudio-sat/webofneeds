package won.node.camel.processor.fixed;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.springframework.stereotype.Service;
import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Facet;
import won.protocol.model.Need;
import won.protocol.model.NeedState;
import won.protocol.model.OwnerApplication;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * User: syim
 * Date: 02.03.2015
 */
@Service
@FixedMessageProcessor(direction= WONMSG.TYPE_FROM_OWNER_STRING,messageType = WONMSG.TYPE_CREATE_STRING)
public class CreateNeedMessageProcessor extends AbstractCamelProcessor
{


  @Override
  public void process(final Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
    Need need = storeNeed(wonMessage);
    authorizeOwnerApplicationForNeed(message, need);
    try {
      WonMessage newNeedNotificationMessage = makeNeedCreatedMessageForMatcher(need);
      //TODO: remove mPMC here, use method from base class
      Dataset needContent = wonMessage.getMessageContent();
      URI needURI = getNeedURIFromWonMessage(needContent);
      matcherProtocolMatcherClient.needCreated(needURI, ModelFactory.createDefaultModel(),
      newNeedNotificationMessage);
    } catch (Exception e) {
      logger.warn("could not create NeedCreatedNotification", e);
    }
  }

  private Need storeNeed(final WonMessage wonMessage) {
    Dataset needContent = wonMessage.getMessageContent();
    URI needURI = getNeedURIFromWonMessage(needContent);
    if (!needURI.equals(wonMessage.getSenderNeedURI()))
      throw new IllegalArgumentException("receiverNeedURI and NeedURI of the content are not equal");

    Need need = new Need();

    need.setState(NeedState.ACTIVE);
    need.setNeedURI(needURI);

    // ToDo (FS) check if the WON node URI corresponds with the WON node (maybe earlier in the message layer)
    need.setWonNodeURI(wonMessage.getReceiverNodeURI());

    need = needRepository.save(need);

    List<Facet> facets = WonRdfUtils.NeedUtils.getFacets(needURI, needContent);
    if (facets.size() == 0)
      throw new IllegalArgumentException("at least one property won:hasFacet required ");
    for (Facet f : facets) {
      // TODO: check if there is a implementation for the facet on the node
      facetRepository.save(f);
    }

    rdfStorage.storeDataset(needURI, needContent);
    return need;
  }

  private void authorizeOwnerApplicationForNeed(final Message message, final Need need) {
    String ownerApplicationID = message.getHeader(WonCamelConstants.OWNER_APPLICATION_ID).toString();
    authorizeOwnerApplicationForNeed(ownerApplicationID, need);
  }

  private WonMessage makeNeedCreatedMessageForMatcher(final Need need) throws NoSuchNeedException {
    Dataset needDataset = linkedDataService.getNeedDataset(need.getNeedURI());
    return new WonMessageBuilder()
      .setMessagePropertiesForNeedCreatedNotification(wonNodeInformationService.generateEventURI(),
                                                      need.getNeedURI(), need.getWonNodeURI())
      .setWonMessageDirection(WonMessageDirection.FROM_EXTERNAL)
      .build(needDataset);
  }

  private WonMessage makeCreateResponseMessage(final WonMessage wonMessage) {
    return new WonMessageBuilder().setPropertiesForNodeResponse(
            wonMessage,
            true,
            this.wonNodeInformationService.generateEventURI()).build();
  }


  private URI getNeedURIFromWonMessage(final Dataset wonMessage) {
    URI needURI;
    needURI = WonRdfUtils.NeedUtils.getNeedURI(wonMessage);
    if (needURI == null) {
      throw new IllegalArgumentException("at least one RDF node must be of type won:Need");
    }
    return needURI;
  }

  private void authorizeOwnerApplicationForNeedURI(final String ownerApplicationID, URI needURI) {
    logger.debug("AUTHORIZING owner application. needURI:{}, OwnerApplicationId:{}", needURI, ownerApplicationID);
    Need need = needRepository.findByNeedURI(needURI).get(0);

    authorizeOwnerApplicationForNeed(ownerApplicationID, need);
  }

  private void authorizeOwnerApplicationForNeed(final String ownerApplicationID, Need need) {
    String stopwatchName = getClass().getName() + ".authorizeOwnerApplicationForNeed";
    Stopwatch stopwatch = SimonManager.getStopwatch(stopwatchName + "_phase1");
    Split split = stopwatch.start();
    List<OwnerApplication> ownerApplications = ownerApplicationRepository.findByOwnerApplicationId(ownerApplicationID);
    split.stop();
    stopwatch = SimonManager.getStopwatch(stopwatchName + "_phase2");
    split = stopwatch.start();
    if (ownerApplications.size() > 0) {
      logger.debug("owner application is already known");
      OwnerApplication ownerApplication = ownerApplications.get(0);
      List<OwnerApplication> authorizedApplications = need.getAuthorizedApplications();
      if (authorizedApplications == null) {
        authorizedApplications = new ArrayList<OwnerApplication>(1);
      }
      authorizedApplications.add(ownerApplication);
      need.setAuthorizedApplications(authorizedApplications);
    } else {
      logger.debug("owner application is new - creating");
      List<OwnerApplication> ownerApplicationList = new ArrayList<>(1);
      OwnerApplication ownerApplication = new OwnerApplication();
      ownerApplication.setOwnerApplicationId(ownerApplicationID);
      ownerApplicationList.add(ownerApplication);
      need.setAuthorizedApplications(ownerApplicationList);
      logger.debug("setting OwnerApp ID: " + ownerApplicationList.get(0));
    }
    split.stop();
    stopwatch = SimonManager.getStopwatch(stopwatchName + "_phase3");
    split = stopwatch.start();
    need = needRepository.save(need);
    split.stop();
  }


}
