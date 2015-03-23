package won.node.messaging.processors;

import com.hp.hpl.jena.graph.TripleBoundary;
import com.hp.hpl.jena.rdf.model.*;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.node.facet.impl.FacetRegistry;
import won.node.service.DataAccessService;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageEncoder;
import won.protocol.message.processor.camel.WonCamelConstants;
import won.protocol.model.Connection;
import won.protocol.model.MessageEventPlaceholder;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.MessageEventRepository;
import won.protocol.repository.rdfstorage.RDFStorageService;
import won.protocol.util.DataAccessUtils;
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
public class SendMessageFromOwnerProcessor extends AbstractInOnlyMessageProcessor
{
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private FacetRegistry reg;
  private DataAccessService dataService;
  @Autowired
  private ConnectionRepository connectionRepository;
  @Autowired
  private won.node.service.impl.URIService URIService;
  @Autowired
  private RDFStorageService rdfStorageService;
  @Autowired
  private MessageEventRepository messageEventRepository;



  public void process(final Exchange exchange) throws Exception {
    Message message = exchange.getIn();
    WonMessage wonMessage = (WonMessage) message.getHeader(WonCamelConstants.WON_MESSAGE_EXCHANGE_HEADER);
    WonMessage newWonMessage = new WonMessageBuilder()
      .wrap(wonMessage)
      .setTimestamp(System.currentTimeMillis())
      .setWonMessageDirection(WonMessageDirection.FROM_EXTERNAL)
      .build();
    logger.debug("STORING message with id {}", newWonMessage.getMessageURI());
    rdfStorageService.storeDataset(newWonMessage.getMessageURI(),
                                   WonMessageEncoder.encodeAsDataset(newWonMessage));

    URI connectionURIFromWonMessage = newWonMessage.getSenderURI();

    final Connection con = DataAccessUtils.loadConnection(connectionRepository, connectionURIFromWonMessage);


    messageEventRepository.save(new MessageEventPlaceholder(connectionURIFromWonMessage,
                                                            newWonMessage));

    exchange.getIn().setHeader(WonCamelConstants.WON_MESSAGE_EXCHANGE_HEADER,newWonMessage);
    final Connection connection = con;
    boolean feedbackWasPresent = RdfUtils.applyMethod(newWonMessage.getMessageContent(),
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
      //TODO: before sending, we should actually create a new URI located on the target WoN node
      //      and copy all the message content to a new message with that URI,
      //      additionally point to the message we just received from the owner ,
      //      sign it and send it. This way, the remote WoN node can just store it the way it is
      //      note: this step may be left out if the message is to be delivered locally.
      //reg.get(con).sendMessageFromOwner(con, message, newWonMessage);
    }
    //todo: the method shall return an object that debugrms the owner that processing the message on the node side was done successfully.
    //return con.getConnectionURI();
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
