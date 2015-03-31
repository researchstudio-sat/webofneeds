package won.node.camel.processor.fixed;

import com.hp.hpl.jena.graph.TripleBoundary;
import com.hp.hpl.jena.rdf.model.*;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.springframework.stereotype.Component;
import won.node.camel.processor.AbstractFromOwnerCamelProcessor;
import won.node.camel.processor.annotation.FixedMessageProcessor;
import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.message.processor.exception.MissingMessagePropertyException;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;

/**
 * User: syim
 * Date: 02.03.2015
 */
@Component
@FixedMessageProcessor(direction= WONMSG.TYPE_FROM_OWNER_STRING,messageType = WONMSG.TYPE_CONNECTION_MESSAGE_STRING)
public class SendMessageFromOwnerProcessor extends AbstractFromOwnerCamelProcessor
{

  public void process(final Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.MESSAGE_HEADER);
    URI connectionUri = wonMessage.getSenderURI();
    if (connectionUri == null){
      throw new MissingMessagePropertyException(URI.create(WONMSG.SENDER_PROPERTY.toString()));
    }
    Connection con = connectionRepository.findOneByConnectionURI(connectionUri);
    if (con.getState() != ConnectionState.CONNECTED) {
      throw new IllegalMessageForConnectionStateException(connectionUri, "CONNECTION_MESSAGE", con.getState());
    }
    WonMessage newWonMessage = createMessageToSendToRemoteNode(wonMessage);
    //add the information about the remote message to the locally stored one
    wonMessage = new WonMessageBuilder()
            .wrap(wonMessage)
            .setCorrespondingRemoteMessageURI(newWonMessage.getMessageURI())
            .build();
    //put it into the header so the persister will pick it up later
    message.setHeader(WonCamelConstants.MESSAGE_HEADER,wonMessage);

    exchange.getIn().setHeader(WonCamelConstants.OUTBOUND_MESSAGE_HEADER,newWonMessage);
  }

  private WonMessage createMessageToSendToRemoteNode(WonMessage wonMessage) {
    //create the message to send to the remote node
    return new WonMessageBuilder()
      .setPropertiesForPassingMessageToRemoteNode(
        wonMessage,
        wonNodeInformationService
          .generateEventURI(wonMessage.getReceiverNodeURI()))
      .build();
  }

  /////// TODO: move code below to the implementation of a FEEDBACK message


  /**
   * Finds feedback in the message, processes it and removes it from the message.
   *
   * @param con
   * @param message
   * @return true if feedback was present, false otherwise
   */
  private boolean processFeedbackMessage(final Connection con, final Model message) {
    assert con != null : "connection must not be null";
    assert message != null : "message must not be null";
    boolean feedbackWasPresent = false;
    Resource baseResource = RdfUtils.getBaseResource(message);
    List<Resource> resourcesToRemove = new LinkedList<Resource>();
    StmtIterator stmtIterator = baseResource.listProperties(WON.HAS_FEEDBACK);
    //iterate over feedback nodes, find which resources there is feedback about,
    //and add the feedback to the resource's description
    while (stmtIterator.hasNext()) {
      feedbackWasPresent = true;
      final Statement stmt = stmtIterator.nextStatement();
      processFeedback(baseResource, resourcesToRemove, stmt.getObject());
    }
    if (feedbackWasPresent) {
      removeResourcesWithSubgraphs(message, resourcesToRemove);
    }
    return feedbackWasPresent;
  }
  private void processFeedback(final Resource baseResource, final List<Resource> resourcesToRemove,
                               final RDFNode feedbackNode) {
    if (!feedbackNode.isResource()) {
      logger.warn("feedback node is not a resource, cannot process feedback in message {}", baseResource);
      return;
    }
    final Resource feedbackRes = (Resource) feedbackNode;
    final Statement forResourceStmt = feedbackRes.getProperty(WON.FOR_RESOURCE);
    final RDFNode forResourceNode = forResourceStmt.getObject();
    if (!forResourceNode.isResource()) {
      logger.warn("for_resource node is not a resource, cannot process feedback in message {}", baseResource);
      return;
    }
    final Resource forResource = forResourceNode.asResource();
    if (!dataService.addFeedback(URI.create(forResource.getURI().toString()), feedbackRes)) {
      logger.warn("failed to add feedback to resource {}", baseResource);
    }
    resourcesToRemove.add(feedbackRes);
  }
  private void removeResourcesWithSubgraphs(final Model model, final List<Resource> resourcesToRemove) {
    logger.debug("removing feedback from message");
    ModelExtract extract = new ModelExtract(new StatementTripleBoundary(TripleBoundary.stopNowhere));
    for (Resource resourceToRemove : resourcesToRemove) {
      logger.debug("removing resource {}", resourcesToRemove);
      Model modelToRemove = extract.extract(resourceToRemove, model);
      model.remove(modelToRemove);
      logger.debug("removed subgraph");
      //additionally, remove the triples linking to the resourceToRemove
      logger.debug("removing statements linking to subgraph");
      model.remove(model.listStatements((Resource) null, (Property) null, (RDFNode) resourceToRemove));
    }
  }
}
