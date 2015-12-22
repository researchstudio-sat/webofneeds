package won.protocol.message;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.util.CheapInsecureRandomString;
import won.protocol.util.DefaultPrefixUtils;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.RDFG;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;
import java.util.*;

/**
 * Class to build a WonMessage based on the specific properties.
 *
 * @author Fabian Salcher
 */
public class WonMessageBuilder
{
  private static final String CONTENT_URI_SUFFIX = "#content-";
  private static final String SIGNATURE_URI_SUFFIX = "#signature-";
  private static final String ENVELOPE_URI_SUFFIX = "#envelope-";
  private static final CheapInsecureRandomString randomString = new CheapInsecureRandomString();
  private static final int RANDOM_SUFFIX_LENGTH = 5;

  // ToDo (FS): move to some vocabulary class

  private URI messageURI;
  private URI senderURI;
  private URI senderNeedURI;
  private URI senderNodeURI;
  private URI receiverURI;
  private URI receiverNeedURI;
  private URI receiverNodeURI;

  private WonMessageType wonMessageType;
  private WonMessageDirection wonMessageDirection;

  //a message may refer to a number of other messages (e.g. previously sent messages)
  private Set<URI> refersToURIs = new HashSet<>();
  //if the message is a response message, it MUST have exactly one isResponseToMessageURI set.
  private URI isResponseToMessageURI;
  //if the message is a response message, and the original message has a correspondingRemoteMessage, set it here
  private URI isRemoteResponseToMessageURI;

  private WonMessageType isResponseToMessageType;
  //some message exist twice: once on the receiver's won node and once on the sender's.
  //this property allows to link to the corresponding remote message
  private URI correspondingRemoteMessageURI;

  private Map<URI, Model> contentMap = new HashMap<>();
  private Map<URI, Model> signatureMap = new HashMap<>();
  private WonMessage wrappedMessage;
  private WonMessage forwardedMessage;
  private Long sentTimestamp;
  private Long receivedTimestamp;




  public WonMessage build() throws WonMessageBuilderException {
    return build(null);
  }


  /**
   * Builds a WonMessage by adding data to the specified dataset.
   * The dataset may be null or empty.
   * @param dataset
   * @return
   * @throws WonMessageBuilderException
   */
  public WonMessage build(Dataset dataset)
    throws WonMessageBuilderException {

    if (dataset == null) {
      dataset = DatasetFactory.createMem();
    }
    if (messageURI == null){
      throw new WonMessageBuilderException("No messageURI specified");
    }





    Model envelopeGraph = ModelFactory.createDefaultModel();
    DefaultPrefixUtils.setDefaultPrefixes(envelopeGraph);
    //create a new envelope graph uri and add the envelope graph to the dataset
    String envelopeGraphURI = RdfUtils.createNewGraphURI(messageURI.toString(), ENVELOPE_URI_SUFFIX,4,dataset).toString();
    dataset.addNamedModel(envelopeGraphURI, envelopeGraph);
    // message URI
    Resource messageEventResource = envelopeGraph.createResource(messageURI.toString(),
      this.wonMessageDirection.getResource());
    //the [envelopeGraphURI] rdf:type msg:EnvelopeGraph makes it easy to select graphs by type
    Resource envelopeGraphResource = envelopeGraph.createResource(envelopeGraphURI, WONMSG.ENVELOPE_GRAPH);
    envelopeGraphResource.addProperty(RDFG.SUBGRAPH_OF, messageEventResource);

    addWrappedOrForwardedMessage(dataset, envelopeGraph, envelopeGraphResource, messageURI);

    //make sure the envelope type has been set
    if (this.wonMessageDirection == null) {
      throw new IllegalStateException("envelopeType must be set!");
    }

    if (wonMessageType != null) {
      messageEventResource.addProperty(WONMSG.HAS_MESSAGE_TYPE_PROPERTY, wonMessageType.getResource());
    }

    // add sender
    if (senderURI != null)
      messageEventResource.addProperty(
        WONMSG.SENDER_PROPERTY,
        envelopeGraph.createResource(senderURI.toString()));
    if (senderNeedURI != null)
      messageEventResource.addProperty(
        WONMSG.SENDER_NEED_PROPERTY,
        envelopeGraph.createResource(senderNeedURI.toString()));
    if (senderNodeURI != null)
      messageEventResource.addProperty(
        WONMSG.SENDER_NODE_PROPERTY,
        envelopeGraph.createResource(senderNodeURI.toString()));

    // add receiver
    if (receiverURI != null)
      messageEventResource.addProperty(
        WONMSG.RECEIVER_PROPERTY,
        envelopeGraph.createResource(receiverURI.toString()));
    if (receiverNeedURI != null)
      messageEventResource.addProperty(
        WONMSG.RECEIVER_NEED_PROPERTY,
        envelopeGraph.createResource(receiverNeedURI.toString()));
    if (receiverNodeURI != null)
      messageEventResource.addProperty(
        WONMSG.RECEIVER_NODE_PROPERTY,
        envelopeGraph.createResource(receiverNodeURI.toString()));

    // add refersTo
    for (URI refersToURI : refersToURIs) {
      messageEventResource.addProperty(
        WONMSG.REFERS_TO_PROPERTY,
        envelopeGraph.createResource(refersToURI.toString()));
    }

    if (isResponseToMessageURI != null) {
      if (wonMessageType != WonMessageType.SUCCESS_RESPONSE && wonMessageType != WonMessageType.FAILURE_RESPONSE ){
        throw new IllegalArgumentException("isResponseToMessageURI can only be used for SUCCESS_RESPONSE and " +
          "FAILURE_RESPONSE types");
      }
      if (isResponseToMessageType == null){
        throw new IllegalArgumentException("response messages must specify the type of message they are a response to" +
          ". Use setIsResponseToMessageType(type)");
      }
      messageEventResource.addProperty(
        WONMSG.IS_RESPONSE_TO,
        envelopeGraph.createResource(isResponseToMessageURI.toString()));
      messageEventResource.addProperty(
        WONMSG.IS_RESPONSE_TO_MESSAGE_TYPE, this.isResponseToMessageType.getResource());
      if (isRemoteResponseToMessageURI != null) {
        messageEventResource.addProperty(
          WONMSG.IS_REMOTE_RESPONSE_TO,
          envelopeGraph.createResource(isRemoteResponseToMessageURI.toString()));
      }
    }

    if (correspondingRemoteMessageURI != null) {
      messageEventResource.addProperty(
              WONMSG.HAS_CORRESPONDING_REMOTE_MESSAGE,
              envelopeGraph.createResource(correspondingRemoteMessageURI.toString()));
    }

    if (sentTimestamp != null) {
      messageEventResource.addProperty(
        WONMSG.HAS_SENT_TIMESTAMP,
        envelopeGraph.createTypedLiteral(this.sentTimestamp));
    }

    if (receivedTimestamp != null) {
      messageEventResource.addProperty(
              WONMSG.HAS_RECEIVED_TIMESTAMP,
              envelopeGraph.createTypedLiteral(this.receivedTimestamp));
    }

    for (URI contentURI : contentMap.keySet()) {
      String contentUriString = contentURI.toString();
      dataset.addNamedModel(contentUriString, contentMap.get(contentURI));
      messageEventResource.addProperty(
        WONMSG.HAS_CONTENT_PROPERTY, messageEventResource
          .getModel().createResource(contentUriString));
      //add the [content-graph] rdfg:subGraphOf [message-uri] triple to the envelope
      envelopeGraph.createStatement(envelopeGraph.getResource(contentURI.toString()), RDFG.SUBGRAPH_OF,
        messageEventResource);

      Model signatureGraph = signatureMap.get(contentURI);
      if (signatureGraph != null) {
        throw new UnsupportedOperationException("signatures are not supported yet");
        /* in principle, this should work, but its untested:

          uniqueContentUri = RdfUtils.createNewGraphURI(contentURI.toString(), SIGNATURE_URI_SUFFIX, 5,
          dataset).toString();
        //the signature refers to the name of the other graph. We changed that name
        //so we have to replace the resource referencing it, too:
        signatureGraph = RdfUtils.replaceResource(signatureGraph.getResource(contentURI.toString()),
          signatureGraph.getResource(uniqueContentUri));
        dataset.addNamedModel(uniqueContentUri, signatureGraph);
        */
      }

      //now replace the content URIs
    }

    return new WonMessage(dataset);

  }

  public void addWrappedOrForwardedMessage(final Dataset dataset, final Model envelopeGraph,
    final Resource envelopeGraphResource, URI messageURI) {
    //add wrapped message first, including all its named graphs.
    //This way, we can later avoid clashed when generating new graph URIs
    if (this.wrappedMessage != null){
      if (this.forwardedMessage != null) throw new IllegalStateException("cannot wrap and forward with the same " +
        "builder");
      addAsContainedEnvelope(dataset, envelopeGraph, envelopeGraphResource, wrappedMessage, messageURI);
    }

    //add forwarded message next, including all its named graphs.
    //This way, we can later avoid clashed when generating new graph URIs
    if (this.forwardedMessage != null){
      if (this.wrappedMessage != null) throw new IllegalStateException("cannot wrap and forward with the same " +
        "builder");
      addAsContainedEnvelope(dataset, envelopeGraph, envelopeGraphResource, forwardedMessage, messageURI);
    }
  }

  public void addAsContainedEnvelope(final Dataset dataset, final Model envelopeGraph,
    final Resource envelopeGraphResource, WonMessage messageToAdd, URI messageURI) {
    String messageUriString = messageURI.toString();
    //the [wrappedMessage.envelopeGraphURI] rdf:type msg:EnvelopeGraph triple in the default graph is required to
    // find the wrapped envelope graph.
    envelopeGraphResource.addProperty(WONMSG.CONTAINS_ENVELOPE, envelopeGraph.getResource(messageToAdd
      .getOuterEnvelopeGraphURI().toString()));
    //copy all named graphs to the new message dataset
    for (Iterator<String> names = messageToAdd.getCompleteDataset().listNames(); names.hasNext(); ){
      String graphUri = names.next();
      Model modelToAdd = messageToAdd.getCompleteDataset().getNamedModel(graphUri);
      dataset.addNamedModel(graphUri, modelToAdd);
      //define that the added graph is a subgraph of the message if that is not yet
      //expressed in the graph itself
      if (!modelToAdd
            .contains(
              modelToAdd.getResource(graphUri),
              RDFG.SUBGRAPH_OF,
              modelToAdd.getResource(messageUriString))){
        envelopeGraph.createStatement(envelopeGraph.getResource(graphUri), RDFG.SUBGRAPH_OF,
          envelopeGraph.getResource(messageUriString));
      }
    }
  }

  /**
   * Adds the complete message content to the message that will be built,
   * referencing toWrap's envelope in the envelope of the new message.
   * The message that will be built has the same messageURI as the wrapped message.
   *
   * @param
   * @return
   */
  public WonMessageBuilder wrap(WonMessage toWrap){
    this.setMessageURI(toWrap.getMessageURI());
    this.setWonMessageDirection(toWrap.getEnvelopeType());

    //make a copy to avoid modification in current message in case wrapped message
    //is modified externally
    this.wrappedMessage = WonRdfUtils.MessageUtils.copyByDatasetSerialization(toWrap);
    return this;
  }

  /**
   * Adds the complete message content to the message that will be built,
   * referencing toForward's envelope in the envelope of the new message.
   *
   * @param
   * @return
   */
  public WonMessageBuilder forward(WonMessage toForward){
    //make a copy to avoid modification in current message in case wrapped message
    //is modified externally
    this.forwardedMessage = WonRdfUtils.MessageUtils.copyByDatasetSerialization(toForward);
    return this;
  }

  // complete MessageType specific setters
  public WonMessageBuilder setMessagePropertiesForOpen(
    URI messageURI,
    URI localConnection,
    URI localNeed,
    URI localWonNode,
    URI remoteConnection,
    URI remoteNeed,
    URI remoteWonNode) {

    this
      .setMessageURI(messageURI)
      .setWonMessageDirection(WonMessageDirection.FROM_OWNER)
      .setWonMessageType(WonMessageType.OPEN)
      .setSenderURI(localConnection)
      .setSenderNeedURI(localNeed)
      .setSenderNodeURI(localWonNode)
      .setReceiverURI(remoteConnection)
      .setReceiverNeedURI(remoteNeed)
      .setReceiverNodeURI(remoteWonNode)
      .setSentTimestampToNow();

    return this;
  }

  public WonMessageBuilder setMessagePropertiesForClose(
    URI messageURI,
    URI localConnection,
    URI localNeed,
    URI localWonNode,
    URI remoteConnection,
    URI remoteNeed,
    URI remoteWonNode) {

    this
      .setMessageURI(messageURI)
      .setWonMessageDirection(WonMessageDirection.FROM_OWNER)
      .setWonMessageType(WonMessageType.CLOSE)
      .setSenderURI(localConnection)
      .setSenderNeedURI(localNeed)
      .setSenderNodeURI(localWonNode)
      .setReceiverURI(remoteConnection)
      .setReceiverNeedURI(remoteNeed)
      .setReceiverNodeURI(remoteWonNode)
      .setSentTimestampToNow();

    return this;
  }

  public WonMessageBuilder setMessagePropertiesForLocalOnlyClose(
    URI messageURI,
    URI localConnection,
    URI localNeed,
    URI localWonNode
    ) {

    this
      .setMessageURI(messageURI)
      .setWonMessageDirection(WonMessageDirection.FROM_SYSTEM)
      .setWonMessageType(WonMessageType.CLOSE)
      .setSenderURI(localConnection)
      .setSenderNeedURI(localNeed)
      .setSenderNodeURI(localWonNode)
      .setSentTimestampToNow();
    return this;
  }

  public WonMessageBuilder setMessagePropertiesForDeactivate(
    URI messageURI,
    URI localNeed,
    URI localWonNode) {
    this
      .setMessageURI(messageURI)
      .setWonMessageDirection(WonMessageDirection.FROM_OWNER)
      .setWonMessageType(WonMessageType.DEACTIVATE)
      .setSenderNeedURI(localNeed)
      .setSenderNodeURI(localWonNode)
      .setReceiverNeedURI(localNeed)
      .setReceiverNodeURI(localWonNode)
      .setSentTimestampToNow();

    return this;
  }

  public WonMessageBuilder setMessagePropertiesForConnect(
    URI messageURI,
    URI localFacet,
    URI localNeed,
    URI localWonNode,
    URI remoteFacet,
    URI remoteNeed,
    URI remoteWonNode) {

    // create facet model
    Model facetModel = WonRdfUtils.FacetUtils.createFacetModelForHintOrConnect(localFacet, remoteFacet);
    RdfUtils.replaceBaseResource(facetModel, facetModel.createResource(messageURI.toString()));
    this
      .setMessageURI(messageURI)
      .setWonMessageDirection(WonMessageDirection.FROM_OWNER)
      .setWonMessageType(WonMessageType.CONNECT)
      .setSenderNeedURI(localNeed)
      .setSenderNodeURI(localWonNode)
      .setReceiverNeedURI(remoteNeed)
      .setReceiverNodeURI(remoteWonNode);
      this.addContent(facetModel, null)
      .setSentTimestampToNow();

    return this;
  }

  public WonMessageBuilder setMessagePropertiesForCreate(
    URI messageURI,
    URI needURI,
    URI wonNodeURI) {

    this
      .setMessageURI(messageURI)
      .setWonMessageDirection(WonMessageDirection.FROM_OWNER)
      .setWonMessageType(WonMessageType.CREATE_NEED)
      .setSenderNeedURI(needURI)
      .setSenderNodeURI(wonNodeURI)
      .setReceiverNodeURI(wonNodeURI)
      .setSentTimestampToNow();
    return this;
  }

  public WonMessageBuilder setMessagePropertiesForHint(
    URI messageURI,
    URI needURI,
    URI needFacetURI,
    URI wonNodeURI,
    URI otherNeedURI,
    URI otherNeedFacet,
    URI matcherURI,
    double score) {

    Model contentModel = WonRdfUtils.FacetUtils.createFacetModelForHintOrConnect(needFacetURI, otherNeedFacet);
    Resource msgResource = contentModel.createResource(messageURI.toString());
    RdfUtils.replaceBaseResource(contentModel, msgResource);
    contentModel.add(msgResource, WON.HAS_MATCH_SCORE,
      contentModel.createTypedLiteral(score));
    contentModel.add(msgResource, WON.HAS_MATCH_COUNTERPART,
      contentModel.createResource(otherNeedURI.toString()));

    this
      .setMessageURI(messageURI)
      .setWonMessageDirection(WonMessageDirection.FROM_EXTERNAL)
      .setWonMessageType(WonMessageType.HINT_MESSAGE)
      .setSenderNodeURI(matcherURI)
      .setReceiverNeedURI(needURI)
      .setReceiverNodeURI(wonNodeURI)
      .setSentTimestampToNow()
      .addContent(contentModel, null);

    return this;
  }

  public WonMessageBuilder setMessagePropertiesForHintNotification(
    URI messageURI,
    URI needURI,
    URI needFacetURI,
    URI needConnectionURI,
    URI wonNodeURI,
    URI otherNeedURI,
    URI otherNeedFacet,
    URI matcherURI,
    double score) {

    Model contentModel = WonRdfUtils.FacetUtils.createFacetModelForHintOrConnect(needFacetURI, otherNeedFacet);
    Resource msgResource = contentModel.createResource(messageURI.toString());
    RdfUtils.replaceBaseResource(contentModel, msgResource);
    contentModel.add(msgResource, WON.HAS_MATCH_SCORE,
                     contentModel.createTypedLiteral(score));
    contentModel.add(msgResource, WON.HAS_MATCH_COUNTERPART,
                     contentModel.createResource(otherNeedURI.toString()));

    this
      .setMessageURI(messageURI)
      .setWonMessageDirection(WonMessageDirection.FROM_EXTERNAL)
      .setWonMessageType(WonMessageType.HINT_MESSAGE)
      .setSenderNodeURI(matcherURI)
      .setReceiverURI(needConnectionURI)
      .setReceiverNeedURI(needURI)
      .setReceiverNodeURI(wonNodeURI)
      .addContent(contentModel, null)
      .setSentTimestampToNow();

    return this;
  }

  public WonMessageBuilder setMessagePropertiesForConnectionMessage(
    URI messageURI,
    URI localConnection,
    URI localNeed,
    URI localWonNode,
    URI remoteConnection,
    URI remoteNeed,
    URI remoteWonNode,
    Model content) {

    this
      .setMessageURI(messageURI)
      .setWonMessageDirection(WonMessageDirection.FROM_OWNER)
      .setWonMessageType(WonMessageType.CONNECTION_MESSAGE)
      .setSenderURI(localConnection)
      .setSenderNeedURI(localNeed)
      .setSenderNodeURI(localWonNode)
      .setReceiverURI(remoteConnection)
      .setReceiverNeedURI(remoteNeed)
      .setReceiverNodeURI(remoteWonNode)
      .addContent(content, null)
      .setSentTimestampToNow();

    return this;
  }


  public WonMessageBuilder setMessagePropertiesForNeedCreatedNotification(
    URI messageURI,
    URI localNeed,
    URI localWonNode) {

    this.setWonMessageType(WonMessageType.NEED_CREATED_NOTIFICATION)
        .setWonMessageDirection(WonMessageDirection.FROM_EXTERNAL)
        .setMessageURI(messageURI)
        .setSenderNeedURI(localNeed)
        .setSenderNodeURI(localWonNode)
        .setSentTimestampToNow();
    return this;
  }

  public WonMessageBuilder setPropertiesForPassingMessageToRemoteNode(final WonMessage ownerOrSystemMsg, URI newMessageUri){
     return setPropertiesForPassingMessageToRemoteNodeAndCopyLocalMessage(ownerOrSystemMsg, newMessageUri);
  }

  @Deprecated
  /**
   * The message to remote node will contain envelope with properties extracted from corresponding
   * local message, as well as copy of the local message content with replaced uris (remote message uris).
   * The method should no longer be used since introducing signatures, which led to preserving
   * exact copy of local envelopes and local contents in remote message (no uris are replaced)
   *
   */
  private WonMessageBuilder setPropertiesForPassingMessageToRemoteNodeAndCopyLocalMessageReplacingUris(final WonMessage
                                                                                                      ownerOrSystemMsg,
                                                                       URI newMessageUri){
    this.setMessageURI(newMessageUri)
      .setSentTimestamp(new Date().getTime())
      .copyContentFromMessageReplacingMessageURI(ownerOrSystemMsg)
      .copyEnvelopeFromWonMessage(ownerOrSystemMsg)
      .setCorrespondingRemoteMessageURI(ownerOrSystemMsg.getMessageURI())
      .setWonMessageDirection(WonMessageDirection.FROM_EXTERNAL);
    return this;
  }

  /**
   * The message to remote node will contain envelope with sentTimestamp, remoteMessageUri, direction,
   * as well as exact copy of the local message envelopes and contents.
   *
   * @param ownerOrSystemMsg
   * @param newMessageUri
   * @return
   */
  private WonMessageBuilder setPropertiesForPassingMessageToRemoteNodeAndCopyLocalMessage(final WonMessage
                                                                                         ownerOrSystemMsg,
                                                                          URI newMessageUri){
    this.setMessageURI(newMessageUri)
      .setSentTimestamp(new Date().getTime())
      .forward(ownerOrSystemMsg) // copy
      .setCorrespondingRemoteMessageURI(ownerOrSystemMsg.getMessageURI())
      .setWonMessageDirection(WonMessageDirection.FROM_EXTERNAL);
    return this;
  }

  public WonMessageBuilder setPropertiesForPassingMessageToOwner(final WonMessage externalMsg){
    this.wrap(externalMsg)
      .setReceivedTimestamp(new Date().getTime());
    return this;
  }

  public WonMessageBuilder setPropertiesForNodeResponse(WonMessage originalMessage, boolean isSuccess, URI messageURI){
    this.setWonMessageType(isSuccess? WonMessageType.SUCCESS_RESPONSE : WonMessageType.FAILURE_RESPONSE)
        .setMessageURI(messageURI);

    WonMessageDirection origDirection = originalMessage.getEnvelopeType();
    if (WonMessageDirection.FROM_EXTERNAL == origDirection){
      //if the message is an external message, the original receiver becomes
      //the sender of the response.
      this
        .setSenderNodeURI(originalMessage.getReceiverNodeURI())
        .setSenderNeedURI(originalMessage.getReceiverNeedURI())
        .setSenderURI(originalMessage.getReceiverURI())
        .setIsRemoteResponseToMessageURI(originalMessage.getCorrespondingRemoteMessageURI());
    } else if (WonMessageDirection.FROM_OWNER == origDirection|| WonMessageDirection.FROM_SYSTEM == origDirection ){
      //if the message comes from the owner, the original sender is also
      //the sender of the response
      this
        .setSenderNodeURI(originalMessage.getSenderNodeURI())
        .setSenderNeedURI(originalMessage.getSenderNeedURI())
        .setSenderURI(originalMessage.getSenderURI());
    }
    this
      .setReceiverNeedURI(originalMessage.getSenderNeedURI())
      .setReceiverNodeURI(originalMessage.getSenderNodeURI())
      .setReceiverURI(originalMessage.getSenderURI())
      .setIsResponseToMessageURI(originalMessage.getMessageURI())
      .setIsResponseToMessageType(originalMessage.getMessageType())
      .setWonMessageDirection(WonMessageDirection.FROM_SYSTEM);
    return this;
  }


  // setters
  public WonMessageBuilder setMessageURI(URI messageURI) {
    this.messageURI = messageURI;
    return this;
  }

  public WonMessageBuilder setSenderURI(URI senderURI) {
    this.senderURI = senderURI;
    return this;
  }

  public WonMessageBuilder setSenderNeedURI(URI senderNeedURI) {
    this.senderNeedURI = senderNeedURI;
    return this;
  }

  public WonMessageBuilder setSenderNodeURI(URI senderNodeURI) {
    this.senderNodeURI = senderNodeURI;
    return this;
  }

  public WonMessageBuilder setReceiverURI(URI receiverURI) {
    this.receiverURI = receiverURI;
    return this;
  }

  public WonMessageBuilder setReceiverNeedURI(URI receiverNeedURI) {
    this.receiverNeedURI = receiverNeedURI;
    return this;
  }

  public WonMessageBuilder setReceiverNodeURI(URI receiverNodeURI) {
    this.receiverNodeURI = receiverNodeURI;
    return this;
  }

  public WonMessageBuilder setWonMessageType(WonMessageType wonMessageType) {
    this.wonMessageType = wonMessageType;
    return this;
  }
  public WonMessageBuilder setWonMessageDirection(WonMessageDirection wonMessageDirection){
    this.wonMessageDirection = wonMessageDirection;
    return this;
  }

  /**
   * Adds the specified content graph, and the specified signature graph, using the specified
   * contentURI as the graph name. The contentURI will be made unique inside the message dataset
   * by appending characters at the end.
   * @param content
   * @return
   */
  public WonMessageBuilder addContent(Model content, Model signature) {
    URI contentGraphUri = RdfUtils.createNewGraphURI(messageURI.toString(), CONTENT_URI_SUFFIX, 4,
      new RdfUtils.GraphNameCheck()
      {
        @Override
        public boolean isGraphUriOk(final String graphUri) {

          return !contentMap.keySet().contains(URI.create(graphUri));
        }
      });
    contentMap.put(contentGraphUri, content);
    if (signature != null)
      signatureMap.put(contentGraphUri, signature);
    return this;
  }
  
  public WonMessageBuilder addRefersToURI(URI refersTo) {
    refersToURIs.add(refersTo);
    return this;
  }

  public WonMessageBuilder setIsResponseToMessageURI(URI isResponseToMessageURI){
    this.isResponseToMessageURI = isResponseToMessageURI;
    return this;
  }

  public WonMessageBuilder setIsRemoteResponseToMessageURI(final URI isRemoteResponseToMessageURI) {
    this.isRemoteResponseToMessageURI = isRemoteResponseToMessageURI;
    return this;
  }

  public WonMessageBuilder setIsResponseToMessageType(final WonMessageType isResponseToMessageType) {
    this.isResponseToMessageType = isResponseToMessageType;
    return this;
  }


  public WonMessageBuilder setCorrespondingRemoteMessageURI(URI correspondingRemoteMessageURI){
    this.correspondingRemoteMessageURI = correspondingRemoteMessageURI;
    return this;
  }

  public WonMessageBuilder setSentTimestamp(final long sentTimestamp) {
    this.sentTimestamp = sentTimestamp;
    return this;
  }

  public WonMessageBuilder setReceivedTimestamp(Long receivedTimestamp) {
    this.receivedTimestamp = receivedTimestamp;
    return this;
  }

  public WonMessageBuilder setSentTimestampToNow() {
    this.sentTimestamp = System.currentTimeMillis();
    return this;
  }

  public WonMessageBuilder setReceivedTimestampToNow() {
    this.receivedTimestamp = System.currentTimeMillis();
    return this;
  }


  /**
   * Copies the envelope properties from the specified message to this message.
   *
   * Note that this does not copy the original envelope graph, only the 
   * standard envelope properties.
   *
   * @param wonMessage
   * @return
   */
  WonMessageBuilder copyEnvelopeFromWonMessage(final WonMessage wonMessage) {
    this
      .setWonMessageType(wonMessage.getMessageType())
      .setReceiverURI(wonMessage.getReceiverURI())
      .setReceiverNeedURI(wonMessage.getReceiverNeedURI())
      .setReceiverNodeURI(wonMessage.getReceiverNodeURI())
      .setSenderURI(wonMessage.getSenderURI())
      .setSenderNeedURI(wonMessage.getSenderNeedURI())
      .setSenderNodeURI(wonMessage.getSenderNodeURI());
    if (wonMessage.getIsResponseToMessageType() != null){
      this.setIsResponseToMessageType(wonMessage.getIsResponseToMessageType());
    }
    if (wonMessage.getIsResponseToMessageURI() != null){
      this.setIsResponseToMessageURI(wonMessage.getIsResponseToMessageURI());
    }
    if (wonMessage.getIsRemoteResponseToMessageURI() != null){
      this.setIsRemoteResponseToMessageURI(wonMessage.getIsRemoteResponseToMessageURI());
    }
    return this;
  }

  /**
   * Copies all content graphs from the specified message, replacing all occurrences
   * of the specified message's URI with the messageURI of this builder.
   * @param wonMessage
   * @return
   */
  WonMessageBuilder copyContentFromMessageReplacingMessageURI(final WonMessage wonMessage) {
    return copyContentFromMessage(wonMessage, true);
  }

  /**
   * Copies all content graphs from the specified message to this builder.
   *
   * @param wonMessage
   * @return
   */
  WonMessageBuilder copyContentFromMessage(final WonMessage wonMessage) {
    return copyContentFromMessage(wonMessage, false);
  }


  /**
   * Copies all content graphs from the specified message to this builder.
   *
   * If replaceMessageUri is true, replaces all occurrences
   * of the specified message's URI with the messageURI of this builder.
   *
   * @param wonMessage
   * @return
   */
   WonMessageBuilder copyContentFromMessage(final WonMessage wonMessage, boolean replaceMessageUri) {
    Dataset messageContent = wonMessage.getMessageContent();
    for (Iterator<String> nameIt = messageContent.listNames(); nameIt.hasNext(); ){
      String modelUri = nameIt.next();
      Model model = messageContent.getNamedModel(modelUri);
      String otherMessageUri = wonMessage.getMessageURI().toString();
      if (replaceMessageUri) {
        //replace the messageURI of the specified message with that of this builder, just in case
        //there are triples in the model referring to it
        model = RdfUtils.replaceResource(model.getResource(otherMessageUri),
          model.getResource(this.messageURI.toString()));
      }
      //change the model name: replace the message uri of the specified message with our uri
      //we have to do that in any case as the content graph's URI must be one within the
      //'URI space' of the message
      String newModelUri = this.messageURI.toString()+"/copied";

      addContent(model,null);
    }
    return this;
  }







  public static WonMessage wrapMessageReceivedByNode(final WonMessage wonMessage, WonMessageDirection direction){
    WonMessageBuilder builder = new WonMessageBuilder()
      .wrap(wonMessage)
      .setWonMessageDirection(direction)
      .setReceivedTimestamp(new Date().getTime());
    return builder.build();
  }

  public static WonMessage forwardReceivedNodeToNodeMessageAsNodeToNodeMessage(final URI newMessageUri, final WonMessage wonMessage,
                                                                               final URI connectionURI, final URI needURI, final URI wonNodeUri,
                                                                               final URI remoteConnectionURI, final URI remoteNeedURI, final URI remoteWonNodeUri) {
    WonMessageBuilder builder = new WonMessageBuilder()
      .setMessageURI(newMessageUri)
      .setWonMessageType(wonMessage.getMessageType())
      .forward(wonMessage)
      .copyContentFromMessageReplacingMessageURI(wonMessage)
      .setSentTimestamp(System.currentTimeMillis())
      .setReceiverURI(remoteConnectionURI)
      .setReceiverNeedURI(remoteNeedURI)
      .setReceiverNodeURI(remoteWonNodeUri)
      .setIsRemoteResponseToMessageURI(wonMessage.getIsRemoteResponseToMessageURI())
      .setIsResponseToMessageURI(wonMessage.getIsResponseToMessageURI())
      .setIsResponseToMessageType(wonMessage.getIsResponseToMessageType())
      .setWonMessageDirection(WonMessageDirection.FROM_EXTERNAL);

    return builder.build();
  }




}
