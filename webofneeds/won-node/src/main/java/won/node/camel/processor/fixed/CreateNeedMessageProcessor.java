package won.node.camel.processor.fixed;

import org.apache.jena.query.Dataset;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import won.node.camel.processor.AbstractCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.*;
import won.protocol.util.NeedModelWrapper;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
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
  @Transactional(propagation = Propagation.REQUIRED)
  public void process(final Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
    Need need = storeNeed(wonMessage);
    authorizeOwnerApplicationForNeed(message, need);
  }

  private Need storeNeed(final WonMessage wonMessage) {
    Dataset needContent = wonMessage.getMessageContent();
    List<WonMessage.AttachmentHolder> attachmentHolders = wonMessage.getAttachments();
    //remove attachment and its signature from the needContent
    removeAttachmentsFromNeedContent(needContent, attachmentHolders);
    URI needURI = getNeedURIFromWonMessage(needContent);
    if (!needURI.equals(wonMessage.getSenderNeedURI()))
      throw new IllegalArgumentException("receiverNeedURI and NeedURI of the content are not equal");

    Need need = new Need();

    need.setState(NeedState.ACTIVE);
    need.setNeedURI(needURI);

    // ToDo (FS) check if the WON node URI corresponds with the WON node (maybe earlier in the message layer)
    NeedEventContainer needEventContainer = needEventContainerRepository.findOneByParentUri(needURI);
    if (needEventContainer == null) {
      needEventContainer = new NeedEventContainer(need, need.getNeedURI());
    }
    needEventContainer.getEvents().add(messageEventRepository.findOneByMessageURI(wonMessage.getMessageURI()));
    need.setWonNodeURI(wonMessage.getReceiverNodeURI());
    ConnectionContainer connectionContainer = new ConnectionContainer(need);


    need.setConnectionContainer(connectionContainer);
    need.setEventContainer(needEventContainer);

    //store the need content
    DatasetHolder datasetHolder = new DatasetHolder(needURI, needContent);
    //store attachments
    List<DatasetHolder> attachments = new ArrayList<>(attachmentHolders.size());
    for(WonMessage.AttachmentHolder attachmentHolder: attachmentHolders){
      datasetHolder = new DatasetHolder(attachmentHolder.getDestinationUri(), attachmentHolder.getAttachmentDataset());
      attachments.add(datasetHolder);
    }
    //add everything to the need model class and save it
    need.setDatatsetHolder(datasetHolder);
    need.setAttachmentDatasetHolders(attachments);
    need = needRepository.save(need);
    connectionContainerRepository.save(connectionContainer);

    NeedModelWrapper needModelWrapper = new NeedModelWrapper(needContent);
    Collection<String> facets = needModelWrapper.getFacetUris();
    if (facets.size() == 0)
      throw new IllegalArgumentException("at least one property won:hasFacet required ");
    for (String facetUri : facets) {
      // TODO: check if there is a implementation for the facet on the node
      Facet f = new Facet();
      f.setNeedURI(needURI);
      f.setTypeURI(URI.create(facetUri));
      facetRepository.save(f);
    }


    return need;
  }

  private void removeAttachmentsFromNeedContent(Dataset needContent, List<WonMessage.AttachmentHolder> attachmentHolders) {
    for (WonMessage.AttachmentHolder attachmentHolder: attachmentHolders){
      for (Iterator<String> it = attachmentHolder.getAttachmentDataset().listNames(); it.hasNext(); ){
        String modelName =it.next();
        needContent.removeNamedModel(modelName);
      }
    }
  }

  private void authorizeOwnerApplicationForNeed(final Message message, final Need need) {
    String ownerApplicationID = message.getHeader(WonCamelConstants.OWNER_APPLICATION_ID).toString();
    authorizeOwnerApplicationForNeed(ownerApplicationID, need);
  }



  private URI getNeedURIFromWonMessage(final Dataset wonMessage) {
    URI needURI;
    needURI = WonRdfUtils.NeedUtils.getNeedURI(wonMessage);
    if (needURI == null) {
      throw new IllegalArgumentException("at least one RDF node must be of type won:Need");
    }
    return needURI;
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
