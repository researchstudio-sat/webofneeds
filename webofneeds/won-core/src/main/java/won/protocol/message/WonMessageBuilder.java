package won.protocol.message;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
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
  public static final String CONTENT_URI_SUFFIX = "#content-";
  public static final String SIGNATURE_URI_SUFFIX = "#signature-";
  public static final String ENVELOPE_URI_SUFFIX = "#envelope-";
  private static final CheapInsecureRandomString randomString = new CheapInsecureRandomString();
  private static final int RANDOM_SUFFIX_LENGTH = 5;

  private final URI messageURI;
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
  //messages can be forwarded. if they are, they are added to the message and referenced:
  private URI forwardedMessageURI;

  private Map<URI, Model> contentMap = new HashMap<>();
  private Map<URI, Model> signatureMap = new HashMap<>();
  private WonMessage wrappedMessage;
  private WonMessage forwardedMessage;
  private Long sentTimestamp;
  private Long receivedTimestamp;

  public WonMessageBuilder(URI messageURI){
    this.messageURI = messageURI;
  }

  private WonMessageBuilder(){
    throw new UnsupportedOperationException("A messageURI must be provided when creating the WonMessageBuilder");
  }


  public WonMessage build() throws WonMessageBuilderException {
    return build(DatasetFactory.createGeneral());
  }


  /**
   * Builds a WonMessage by adding data to the specified dataset.
   * The dataset may be null or empty.
   * @param dataset
   * @return
   * @throws WonMessageBuilderException
   */
  public WonMessage build(final Dataset dataset)
    throws WonMessageBuilderException {

    if (dataset == null) {
      throw new IllegalArgumentException("specified dataset must not be null. If a new dataset is to be created for the message, build() should be called.");
    }
    if (messageURI == null){
      throw new WonMessageBuilderException("No messageURI specified");
    }





    Model envelopeGraph = ModelFactory.createDefaultModel();
    DefaultPrefixUtils.setDefaultPrefixes(envelopeGraph);
    //create a new envelope graph uri and add the envelope graph to the dataset
    //... and make sure that the graph URI will be new by also checking inside the wrapped message
    String envelopeGraphURI = RdfUtils.createNewGraphURI(messageURI.toString(), ENVELOPE_URI_SUFFIX,4, graphUri -> {
        if (dataset.containsNamedModel(graphUri)) return false;
        if (wrappedMessage == null) return true;
        if (wrappedMessage.getEnvelopeGraphURIs().contains(graphUri)) return false;
        if (wrappedMessage.getContentGraphURIs().contains(graphUri)) return false;
        return true;
      }).toString();
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

    messageEventResource.addLiteral(WONMSG.PROTOCOL_VERSION, envelopeGraph.createTypedLiteral("1.0"));

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

    if (forwardedMessageURI != null) {
      messageEventResource.addProperty(
              WONMSG.HAS_FORWARDED_MESSAGE,
              envelopeGraph.createResource(forwardedMessageURI.toString()));
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
  public static WonMessageBuilder wrap(WonMessage toWrap){
    WonMessageBuilder builder = new WonMessageBuilder(toWrap.getMessageURI())
    .setWonMessageDirection(toWrap.getEnvelopeType());

    //make a copy to avoid modification in current message in case wrapped message
    //is modified externally
    builder.wrappedMessage = WonRdfUtils.MessageUtils.copyByDatasetSerialization(toWrap);
    return builder;
  }



  // complete MessageType specific setters
  public static WonMessageBuilder setMessagePropertiesForOpen(
    URI messageURI,
    URI localConnection,
    URI localNeed,
    URI localWonNode,
    URI remoteConnection,
    URI remoteNeed,
    URI remoteWonNode, String welcomeMessage) {

    return new WonMessageBuilder(messageURI)
      .setWonMessageDirection(WonMessageDirection.FROM_OWNER)
      .setWonMessageType(WonMessageType.OPEN)
      .setSenderURI(localConnection)
      .setSenderNeedURI(localNeed)
      .setSenderNodeURI(localWonNode)
      .setReceiverURI(remoteConnection)
      .setReceiverNeedURI(remoteNeed)
      .setReceiverNodeURI(remoteWonNode)
      .setTextMessage(welcomeMessage)
      .setSentTimestampToNow();
  }

  public static WonMessageBuilder setMessagePropertiesForClose(
    URI messageURI,
    URI localConnection,
    URI localNeed,
    URI localWonNode,
    URI remoteConnection,
    URI remoteNeed,
    URI remoteWonNode, String farewellMessage) {
   return setMessagePropertiesForClose(messageURI, WonMessageDirection.FROM_OWNER, localConnection, localNeed,
                                       localWonNode, remoteConnection, remoteNeed, remoteWonNode, farewellMessage);
  }

  public static  WonMessageBuilder setMessagePropertiesForClose(
    URI messageURI,
    WonMessageDirection direction,
    URI localConnection,
    URI localNeed,
    URI localWonNode,
    URI remoteConnection,
    URI remoteNeed,
    URI remoteWonNode,
    String farewellMessage) {

    return new WonMessageBuilder(messageURI)
      .setWonMessageDirection(direction)
      .setWonMessageType(WonMessageType.CLOSE)
      .setSenderURI(localConnection)
      .setSenderNeedURI(localNeed)
      .setSenderNodeURI(localWonNode)
      .setReceiverURI(remoteConnection)
      .setReceiverNeedURI(remoteNeed)
      .setReceiverNodeURI(remoteWonNode)
      .setTextMessage(farewellMessage)
      .setSentTimestampToNow();
  }

  public static WonMessageBuilder setMessagePropertiesForClose(
          URI messageURI,
          URI localConnection,
          URI localNeed,
          URI localWonNode, String farewellMessage) {

          return setMessagePropertiesForClose(messageURI, WonMessageDirection.FROM_OWNER, localConnection, localNeed, localWonNode, farewellMessage);
  }

  public static WonMessageBuilder setMessagePropertiesForClose(
          URI messageURI,
          WonMessageDirection direction,
          URI localConnection,
          URI localNeed,
          URI localWonNode, String farewellMessage) {

    return setMessagePropertiesForClose(messageURI, WonMessageDirection.FROM_OWNER, localConnection, localNeed,
            localWonNode, localConnection, localNeed, localWonNode, farewellMessage);
  }


  /**
   * Sets the MessageProperties for Closing open connections (happens when the need is closed and the system is closing
   * all the corresponding connections when no connection is present from the remoteNeed
   * @param messageURI
   * @param localConnection
   * @param localNeed
   * @param localWonNode
     * @return
     */
  public static WonMessageBuilder setMessagePropertiesForLocalOnlyClose(
    URI messageURI,
    URI localConnection,
    URI localNeed,
    URI localWonNode
    ) {

    return new WonMessageBuilder(messageURI)
      .setWonMessageDirection(WonMessageDirection.FROM_SYSTEM)
      .setWonMessageType(WonMessageType.CLOSE)
      .setSenderURI(localConnection)
      .setSenderNeedURI(localNeed)
      .setSenderNodeURI(localWonNode)
      .setSentTimestampToNow();
  }

  public static WonMessageBuilder setMessagePropertiesForDeactivateFromOwner(
    URI messageURI,
    URI localNeed,
    URI localWonNode) {
    return new WonMessageBuilder(messageURI)
      .setWonMessageDirection(WonMessageDirection.FROM_OWNER)
      .setWonMessageType(WonMessageType.DEACTIVATE)
      .setSenderNeedURI(localNeed)
      .setSenderNodeURI(localWonNode)
      .setReceiverNeedURI(localNeed)
      .setReceiverNodeURI(localWonNode)
      .setSentTimestampToNow();
  }

  public static WonMessageBuilder setMessagePropertiesForDeactivateFromSystem(
          URI messageURI,
          URI localNeed,
          URI localWonNode) {
    return new WonMessageBuilder(messageURI)
            .setWonMessageDirection(WonMessageDirection.FROM_SYSTEM)
            .setWonMessageType(WonMessageType.DEACTIVATE)
            .setSenderNeedURI(localNeed)
            .setSenderNodeURI(localWonNode)
            .setReceiverNeedURI(localNeed)
            .setReceiverNodeURI(localWonNode)
            .setSentTimestampToNow();
  }

  /**
   * Sets message properties for sending a 'need message' from System to Owner,
   * i.e. a notification from the node to the owner. This message will have no
   * effect on need or connection states and it is expected that a payload (e.g.
   * via setTextMessage()) is added to the message builder prior to calling
   * the build() method.
   * @param messageURI
   * @param localNeed
   * @param localWonNode
     * @return
     */
  public static WonMessageBuilder setMessagePropertiesForNeedMessageFromSystem(
          URI messageURI,
          URI localNeed,
          URI localWonNode) {
    return new WonMessageBuilder(messageURI)
            .setWonMessageDirection(WonMessageDirection.FROM_SYSTEM)
            .setWonMessageType(WonMessageType.NEED_MESSAGE)
            .setSenderNeedURI(localNeed)
            .setSenderNodeURI(localWonNode)
            .setReceiverNeedURI(localNeed)
            .setReceiverNodeURI(localWonNode)
            .setSentTimestampToNow();
  }

  public static WonMessageBuilder setMessagePropertiesForConnect(
    URI messageURI,
    URI localFacet,
    URI localNeed,
    URI localWonNode,
    URI remoteFacet,
    URI remoteNeed,
    URI remoteWonNode,
    String welcomeMessage) {

    // create facet model
    Model facetModel = WonRdfUtils.FacetUtils.createFacetModelForHintOrConnect(localFacet, remoteFacet);
    RdfUtils.replaceBaseResource(facetModel, facetModel.createResource(messageURI.toString()));
    if (welcomeMessage != null){
      WonRdfUtils.MessageUtils.addMessage(facetModel, welcomeMessage);
    }
    return new WonMessageBuilder(messageURI)
      .setWonMessageDirection(WonMessageDirection.FROM_OWNER)
      .setWonMessageType(WonMessageType.CONNECT)
      .setSenderNeedURI(localNeed)
      .setSenderNodeURI(localWonNode)
      .setReceiverNeedURI(remoteNeed)
      .setReceiverNodeURI(remoteWonNode)
      .addContent(facetModel, null)
      .setSentTimestampToNow();
  }

  public static WonMessageBuilder setMessagePropertiesForCreate(
    URI messageURI,
    URI needURI,
    URI wonNodeURI) {
    return new WonMessageBuilder(messageURI)
      .setWonMessageDirection(WonMessageDirection.FROM_OWNER)
      .setWonMessageType(WonMessageType.CREATE_NEED)
      .setSenderNeedURI(needURI)
      .setSenderNodeURI(wonNodeURI)
      .setReceiverNodeURI(wonNodeURI)
      .setSentTimestampToNow();
  }

  public static WonMessageBuilder setMessagePropertiesForHint(
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

    return new WonMessageBuilder(messageURI)
      .setWonMessageDirection(WonMessageDirection.FROM_EXTERNAL)
      .setWonMessageType(WonMessageType.HINT_MESSAGE)
      .setSenderNodeURI(matcherURI)
      .setReceiverNeedURI(needURI)
      .setReceiverNodeURI(wonNodeURI)
      .setSentTimestampToNow()
      .addContent(contentModel, null);
  }

  public static WonMessageBuilder setMessagePropertiesForHintFeedback(
    URI messageURI,
    URI connectionURI,
    URI needURI,
    URI wonNodeURI,
    boolean booleanFeedbackValue) {

    Model contentModel = WonRdfUtils.MessageUtils.binaryFeedbackMessage(connectionURI, booleanFeedbackValue);
    Resource msgResource = contentModel.createResource(messageURI.toString());
    RdfUtils.replaceBaseResource(contentModel, msgResource);

    return new WonMessageBuilder(messageURI)
      .setWonMessageDirection(WonMessageDirection.FROM_OWNER)
      .setWonMessageType(WonMessageType.HINT_FEEDBACK_MESSAGE)
      .setReceiverNodeURI(wonNodeURI)
      .setReceiverURI(connectionURI)
      .setReceiverNeedURI(needURI)
      .setSenderNodeURI(wonNodeURI)
      .setSenderNeedURI(needURI)
      .setSenderURI(connectionURI)
      .setSentTimestampToNow()
      .addContent(contentModel, null);
  }

  public static WonMessageBuilder setMessagePropertiesForHintNotification(
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

    return new WonMessageBuilder(messageURI)
      .setWonMessageDirection(WonMessageDirection.FROM_EXTERNAL)
      .setWonMessageType(WonMessageType.HINT_MESSAGE)
      .setSenderNodeURI(matcherURI)
      .setReceiverURI(needConnectionURI)
      .setReceiverNeedURI(needURI)
      .setReceiverNodeURI(wonNodeURI)
      .addContent(contentModel, null)
      .setSentTimestampToNow();
  }

  public static WonMessageBuilder setMessagePropertiesForConnectionMessage(
    URI messageURI,
    URI localConnection,
    URI localNeed,
    URI localWonNode,
    URI remoteConnection,
    URI remoteNeed,
    URI remoteWonNode,
    Model content) {

    return new WonMessageBuilder(messageURI)
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
  }

  public static WonMessageBuilder setMessagePropertiesForConnectionMessage(
    URI messageURI,
    URI localConnection,
    URI localNeed,
    URI localWonNode,
    URI remoteConnection,
    URI remoteNeed,
    URI remoteWonNode,
    String textMessage) {
    return new WonMessageBuilder(messageURI)
      .setWonMessageDirection(WonMessageDirection.FROM_OWNER)
      .setWonMessageType(WonMessageType.CONNECTION_MESSAGE)
      .setSenderURI(localConnection)
      .setSenderNeedURI(localNeed)
      .setSenderNodeURI(localWonNode)
      .setReceiverURI(remoteConnection)
      .setReceiverNeedURI(remoteNeed)
      .setReceiverNodeURI(remoteWonNode)
      .setTextMessage(textMessage)
      .setSentTimestampToNow();
  }


  public static WonMessageBuilder setMessagePropertiesForNeedCreatedNotification(
    URI messageURI,
    URI localNeed,
    URI localWonNode) {

    return new WonMessageBuilder(messageURI)
        .setWonMessageType(WonMessageType.NEED_CREATED_NOTIFICATION)
        .setWonMessageDirection(WonMessageDirection.FROM_EXTERNAL)
        .setSenderNeedURI(localNeed)
        .setSenderNodeURI(localWonNode)
        .setSentTimestampToNow();
  }

  public static WonMessageBuilder setPropertiesForPassingMessageToRemoteNode(final WonMessage ownerOrSystemMsg, URI
    newMessageUri){
     return setPropertiesForPassingMessageToRemoteNodeAndCopyLocalMessage(ownerOrSystemMsg, newMessageUri);
  }


  /**
   * Adds the complete message content to the message that will be built,
   * referencing toForward's envelope in the envelope of the new message.
   *
   * @param
   * @return
   */
  private WonMessageBuilder forward(WonMessage toForward){
    //make a copy to avoid modification in current message in case wrapped message
    //is modified externally
    this.forwardedMessage = WonRdfUtils.MessageUtils.copyByDatasetSerialization(toForward);
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
  private static WonMessageBuilder setPropertiesForPassingMessageToRemoteNodeAndCopyLocalMessage(final WonMessage
                                                                                         ownerOrSystemMsg,
                                                                          URI newMessageUri){
    return new WonMessageBuilder(newMessageUri)
      .setSentTimestamp(new Date().getTime())
      .forward(ownerOrSystemMsg) // copy
      .setCorrespondingRemoteMessageURI(ownerOrSystemMsg.getMessageURI())
      .setWonMessageDirection(WonMessageDirection.FROM_EXTERNAL);
  }

  @Deprecated
  public static WonMessageBuilder setPropertiesForPassingMessageToOwner(final WonMessage externalMsg){
    return WonMessageBuilder.wrap(externalMsg)
      .setReceivedTimestamp(new Date().getTime());
  }

  public static WonMessageBuilder setPropertiesForNodeResponse(WonMessage originalMessage, boolean isSuccess, URI
  messageURI){
    WonMessageBuilder messageBuilder = new WonMessageBuilder(messageURI)
      .setWonMessageType(isSuccess? WonMessageType.SUCCESS_RESPONSE : WonMessageType
      .FAILURE_RESPONSE);

    WonMessageDirection origDirection = originalMessage.getEnvelopeType();
    if (WonMessageDirection.FROM_EXTERNAL == origDirection){
      //if the message is an external message, the original receiver becomes
      //the sender of the response.
      messageBuilder
        .setSenderNodeURI(originalMessage.getReceiverNodeURI())
        .setSenderNeedURI(originalMessage.getReceiverNeedURI())
        .setSenderURI(originalMessage.getReceiverURI())
        .setIsRemoteResponseToMessageURI(originalMessage.getCorrespondingRemoteMessageURI());
    } else if (WonMessageDirection.FROM_OWNER == origDirection|| WonMessageDirection.FROM_SYSTEM == origDirection ){
      //if the message comes from the owner, the original sender is also
      //the sender of the response
      messageBuilder
        .setSenderNodeURI(originalMessage.getSenderNodeURI())
        .setSenderNeedURI(originalMessage.getSenderNeedURI())
        .setSenderURI(originalMessage.getSenderURI());
    }
    messageBuilder
      .setReceiverNeedURI(originalMessage.getSenderNeedURI())
      .setReceiverNodeURI(originalMessage.getSenderNodeURI())
      .setReceiverURI(originalMessage.getSenderURI())
      .setIsResponseToMessageURI(originalMessage.getMessageURI())
      .setIsResponseToMessageType(originalMessage.getMessageType())
      .setWonMessageDirection(WonMessageDirection.FROM_SYSTEM);
    return messageBuilder;
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

  /**
   * Retrieves one of the possibly multiple Models that does not have a signature yet. If there is none
   * (all are signed or none is found at all), a new model is created, added to the internal contentMap
   * and returned here.
   *
   */
  public Model getUnsignedContentGraph(){
    if (contentMap.isEmpty()){
      //no content graphs yet. Make one and return it.
      Model contentGraph = ModelFactory.createDefaultModel();
      RdfUtils.replaceBaseURI(contentGraph, this.messageURI.toString());
      addContent(contentGraph, null);
      return contentGraph;
    }
    //content map is not empty. find one without a signature:
    for (Map.Entry<URI, Model> entry: contentMap.entrySet()){
      if (!signatureMap.containsKey(entry.getKey())) return entry.getValue();
    }
    //all content graphs are signed. add a new one.
    Model contentGraph = ModelFactory.createDefaultModel();
    RdfUtils.replaceBaseURI(contentGraph, this.messageURI.toString());
    addContent(contentGraph, null);
    return contentGraph;
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

  public WonMessageBuilder setForwardedMessageURI(URI forwardedMessageURI) {
    this.forwardedMessageURI = forwardedMessageURI;
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
   * Adds a won:hasTextMessage triple to one of the unsigned content graphs in this builder. Creates a new
   * unsigned content graph if none is found.
   * @param textMessage may be null in which case the builder is not modified
   * @return
   */
  public WonMessageBuilder setTextMessage(String textMessage){
    if (textMessage != null) {
      WonRdfUtils.MessageUtils.addMessage(getUnsignedContentGraph(), textMessage);
    }
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
  public static WonMessageBuilder copyEnvelopeFromWonMessage(final WonMessage wonMessage) {
    WonMessageBuilder builder = new WonMessageBuilder(wonMessage.getMessageURI())
      .setWonMessageType(wonMessage.getMessageType())
      .setReceiverURI(wonMessage.getReceiverURI())
      .setReceiverNeedURI(wonMessage.getReceiverNeedURI())
      .setReceiverNodeURI(wonMessage.getReceiverNodeURI())
      .setSenderURI(wonMessage.getSenderURI())
      .setSenderNeedURI(wonMessage.getSenderNeedURI())
      .setSenderNodeURI(wonMessage.getSenderNodeURI());
    if (wonMessage.getIsResponseToMessageType() != null){
      builder.setIsResponseToMessageType(wonMessage.getIsResponseToMessageType());
    }
    if (wonMessage.getIsResponseToMessageURI() != null){
      builder.setIsResponseToMessageURI(wonMessage.getIsResponseToMessageURI());
    }
    if (wonMessage.getIsRemoteResponseToMessageURI() != null){
      builder.setIsRemoteResponseToMessageURI(wonMessage.getIsRemoteResponseToMessageURI());
    }
    return builder;
  }



  public static WonMessage forwardReceivedNodeToNodeMessageAsNodeToNodeMessage(final URI newMessageUri, final WonMessage wonMessage,
                                                                               final URI connectionURI, final URI needURI, final URI wonNodeUri,
                                                                               final URI remoteConnectionURI, final URI remoteNeedURI, final URI remoteWonNodeUri) {
    WonMessageBuilder builder = new WonMessageBuilder(newMessageUri)
      .setWonMessageType(wonMessage.getMessageType())
      .forward(wonMessage)
      .setForwardedMessageURI(wonMessage.getMessageURI())
      .setSenderNeedURI(needURI)
      .setSenderURI(connectionURI)
      .setSenderNodeURI(wonNodeUri)
      .setSentTimestamp(System.currentTimeMillis())
      .setReceiverURI(remoteConnectionURI)
      .setReceiverNeedURI(remoteNeedURI)
      .setReceiverNodeURI(remoteWonNodeUri)
      .setIsRemoteResponseToMessageURI(wonMessage.getIsRemoteResponseToMessageURI())
      .setIsResponseToMessageURI(wonMessage.getIsResponseToMessageURI())
      .setIsResponseToMessageType(wonMessage.getIsResponseToMessageType())
      .setWonMessageDirection(WonMessageDirection.FROM_SYSTEM);

    return builder.build();
  }




}
