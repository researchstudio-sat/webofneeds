/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package won.node.service.impl;

import com.hp.hpl.jena.graph.TripleBoundary;
import com.hp.hpl.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.node.facet.impl.FacetRegistry;
import won.node.service.DataAccessService;
import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageEncoder;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEvent;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.MessageEventPlaceholder;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.MessageEventRepository;
import won.protocol.repository.rdfstorage.RDFStorageService;
import won.protocol.service.ConnectionCommunicationService;
import won.protocol.util.DataAccessUtils;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.WON;

import java.io.StringWriter;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;


/**
 *
 */
public class OwnerFacingConnectionCommunicationServiceImpl implements ConnectionCommunicationService
{
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private FacetRegistry reg;
  private DataAccessService dataService;
  @Autowired
  private ConnectionRepository connectionRepository;
  @Autowired
  private URIService URIService;
  @Autowired
  private RDFStorageService rdfStorageService;
  @Autowired
  private MessageEventRepository messageEventRepository;

  @Override
  public void open(final URI connectionURI, final Model content, WonMessage wonMessage)
    throws NoSuchConnectionException, IllegalMessageForConnectionStateException {

    if (wonMessage != null) {
      //TODO: currently, the WoN node does not alter (extend) the WonMessage. However,
      //the sender (owner) could only fill in senderURI (=connectionURI) and wonNodeURI, and
      //the WoN node could add the properties required for routing to the destination, as these
      //properties are stored on the WoN node with the connection data.

      Connection con = connectionRepository.findOneByConnectionURI(connectionURI);

      logger.debug("STORING message with id {}", wonMessage.getMessageURI());
      //TODO: which properties are really needed to route the messae correctly?
      WonMessage newWonMessage = new WonMessageBuilder()
        .wrap(wonMessage)
        .setSenderURI(con.getConnectionURI())
        .setReceiverURI(con.getRemoteConnectionURI())
        .setReceiverNeedURI(con.getRemoteNeedURI())
        .build();

      rdfStorageService.storeDataset(newWonMessage.getMessageURI(),
                                     WonMessageEncoder.encodeAsDataset(newWonMessage));

      URI connectionURIFromWonMessage = newWonMessage.getSenderURI();

      logger.debug("OPEN received from the owner side for connection {}", connectionURIFromWonMessage);

      con = dataService.nextConnectionState(connectionURIFromWonMessage, ConnectionEventType.OWNER_OPEN);

      messageEventRepository.save(new MessageEventPlaceholder(connectionURIFromWonMessage,
        newWonMessage));

      //invoke facet implementation
      reg.get(con).openFromOwner(con, content, newWonMessage);

    } else {

      logger.debug("OPEN received from the owner side for connection {} with content {}", connectionURI, content);

      Connection con = dataService.nextConnectionState(connectionURI, ConnectionEventType.OWNER_OPEN);

      ConnectionEvent event = dataService
        .createConnectionEvent(connectionURI, connectionURI, ConnectionEventType.OWNER_OPEN);

      dataService.saveAdditionalContentForEvent(content, con, event);

      //invoke facet implementation
      reg.get(con).openFromOwner(con, content, wonMessage);
    }
  }

  @Override
  public void close(final URI connectionURI, final Model content, WonMessage wonMessage)
    throws NoSuchConnectionException, IllegalMessageForConnectionStateException {

    // distinguish between the new message format (WonMessage) and the old parameters
    // ToDo (FS): remove this distinction if the old parameters not used anymore
    if (wonMessage != null) {
      logger.debug("STORING message with id {}", wonMessage.getMessageURI());
      rdfStorageService.storeDataset(wonMessage.getMessageURI(),
                                     WonMessageEncoder.encodeAsDataset(wonMessage));

      logger.debug("CLOSE received from the owner side for connection {}", connectionURI);

      Connection con = dataService.nextConnectionState(connectionURI, ConnectionEventType.OWNER_CLOSE);

      // store wonMessage and messageEventPlaceholder
      rdfStorageService.storeDataset(wonMessage.getMessageURI(),
                                     WonMessageEncoder.encodeAsDataset(wonMessage));
      messageEventRepository.save(new MessageEventPlaceholder(con.getConnectionURI(),
                                                              wonMessage));

      //invoke facet implementation
      reg.get(con).closeFromOwner(con, content, wonMessage);

    } else {

      logger.debug("CLOSE received from the owner side for connection {} with content {}", connectionURI, content);

      Connection con = dataService.nextConnectionState(connectionURI, ConnectionEventType.OWNER_CLOSE);

      ConnectionEvent event = dataService
        .createConnectionEvent(connectionURI, connectionURI, ConnectionEventType.OWNER_CLOSE);

      dataService.saveAdditionalContentForEvent(content, con, event);

      //invoke facet implementation
      reg.get(con).closeFromOwner(con, content, wonMessage);
    }
  }

  @Override
  public void sendMessage(final URI connectionURI, final Model message, WonMessage wonMessage)
    throws NoSuchConnectionException, IllegalMessageForConnectionStateException {

    // distinguish between the new message format (WonMessage) and the old parameters
    // ToDo (FS): remove this distinction if the old parameters not used anymore
    if (wonMessage != null) {
      logger.debug("STORING message with id {}", wonMessage.getMessageURI());
      rdfStorageService.storeDataset(wonMessage.getMessageURI(),
                                     WonMessageEncoder.encodeAsDataset(wonMessage));

      URI connectionURIFromWonMessage = wonMessage.getSenderURI();

      final Connection con = DataAccessUtils.loadConnection(connectionRepository, connectionURIFromWonMessage);


      messageEventRepository.save(new MessageEventPlaceholder(connectionURIFromWonMessage,
                                                              wonMessage));

      final Connection connection = con;
      boolean feedbackWasPresent = RdfUtils.applyMethod(wonMessage.getMessageContent(),
                           new RdfUtils.ModelVisitor<Boolean>()
                           {
                             @Override
                             public Boolean visit(final Model model) {
                               return processFeedbackMessage(connection, model);
                             }
                           },
                           new RdfUtils.ResultCombiner<Boolean>()
                           {
                             @Override
                             public Boolean combine(final Boolean first, final Boolean second) {
                               return first || second;
                             }
                           });

      if (!feedbackWasPresent) {
        //a feedback message is not forwarded to the remote connection, and facets cannot react to it.
        //invoke facet implementation
        //TODO: this may be much more responsive if done asynchronously. We dont return anything here anyway.
        reg.get(con).sendMessageFromOwner(con, message, wonMessage);
      }
      //todo: the method shall return an object that debugrms the owner that processing the message on the node side was done successfully.
      //return con.getConnectionURI();

    } else {

      Connection con = DataAccessUtils.loadConnection(connectionRepository, connectionURI);

      //create ConnectionEvent in Database

      ConnectionEvent event = dataService
        .createConnectionEvent(con.getConnectionURI(), connectionURI, ConnectionEventType.OWNER_MESSAGE);
      Resource eventNode = message.createResource(this.URIService.createEventURI(con, event).toString());
      RdfUtils.replaceBaseResource(message, eventNode);
      //create rdf content for the ConnectionEvent and save it to disk
      dataService.saveAdditionalContentForEvent(message, con, event);
      if (logger.isDebugEnabled()) {
        StringWriter writer = new StringWriter();
        RDFDataMgr.write(writer, message, Lang.TTL);
        logger.debug("message after saving:\n{}", writer.toString());
      }
      boolean feedbackWasPresent = processFeedbackMessage(con, message);

      if (!feedbackWasPresent) {
        //a feedback message is not forwarded to the remote connection, and facets cannot react to it.
        //invoke facet implementation
        //TODO: this may be much more responsive if done asynchronously. We dont return anything here anyway.
        reg.get(con).sendMessageFromOwner(con, message, wonMessage);
      }
      //todo: the method shall return an object that debugrms the owner that processing the message on the node side was done successfully.
      //return con.getConnectionURI();
    }
  }

  /*
@Override
public void textMessage(final URI connectionURI, final Model message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
{
  logger.debug("SEND_TEXT_MESSAGE received from the owner side for connection {} with message '{}'", connectionURI, message);
  Connection con = dataService.saveChatMessage(connectionURI,message);

  //invoke facet implementation
  reg.get(con).textMessageFromOwner(con, message);

}
   */
  public void setReg(FacetRegistry reg) {
    this.reg = reg;
  }

  public void setDataService(DataAccessService dataService) {
    this.dataService = dataService;
  }

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
