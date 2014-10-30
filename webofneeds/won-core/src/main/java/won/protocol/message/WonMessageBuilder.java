package won.protocol.message;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.model.NeedState;
import won.protocol.util.CheapInsecureRandomString;
import won.protocol.util.DefaultPrefixUtils;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
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
  private Resource responseMessageState;

  private Set<URI> refersToURIs = new HashSet<>();

  private Map<URI, Model> contentMap = new HashMap<>();
  private Map<URI, Model> signatureMap = new HashMap<>();
  private WonMessage wrappedMessage;
  private WonMessage forwardedMessage;
  private Long timestamp;

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

    checkProperties();

    Model defaultModel = null;

    if (dataset == null) {
      // create the default model, containing a triple that denotes the message graph as
      // envelope graph
      dataset = DatasetFactory.createMem();
      defaultModel = dataset.getDefaultModel();
    } else {
      defaultModel = dataset.getDefaultModel();
      if (defaultModel == null) {
        defaultModel = ModelFactory.createDefaultModel();
        dataset.setDefaultModel(defaultModel);
      }
    }
    if (messageURI == null){
      throw new WonMessageBuilderException("No messageURI specified");
    }





    Model envelopeGraph = ModelFactory.createDefaultModel();
    DefaultPrefixUtils.setDefaultPrefixes(envelopeGraph);

    //add wrapped message first, including all its named graphs.
    //This way, we can later avoid clashed when generating new graph URIs
    if (this.wrappedMessage != null){
        if (this.forwardedMessage != null) throw new IllegalStateException("cannot wrap and forward with the same " +
          "builder");
        //the [wrappedMessage.envelopeGraphURI] rdf:type msg:EnvelopeGraph triple in the default graph is required to
        // find the wrapped envelope graph.
        envelopeGraph.createResource(this.wrappedMessage.getOuterEnvelopeGraphURI().toString(),
          WONMSG.ENVELOPE_GRAPH);
      //copy all named graphs to the new message dataset
      for (Iterator<String> names = this.wrappedMessage.getCompleteDataset().listNames(); names.hasNext(); ){
        String graphUri = names.next();
        dataset.addNamedModel(graphUri, this.wrappedMessage.getCompleteDataset().getNamedModel(graphUri));
      }
    }
    
    //add forwarded message next, including all its named graphs.
    //This way, we can later avoid clashed when generating new graph URIs
    if (this.forwardedMessage != null){
      if (this.wrappedMessage != null) throw new IllegalStateException("cannot wrap and forward with the same " +
        "builder");
      //the [forwardedMessage.envelopeGraphURI] rdf:type msg:EnvelopeGraph triple in the default graph is required to
      // find the forwarded envelope graph.
      envelopeGraph.createResource(this.forwardedMessage.getOuterEnvelopeGraphURI().toString(),
        WONMSG.FORWARDED_ENVELOPE_GRAPH);
      //copy all named graphs to the new message dataset
      for (Iterator<String> names = this.forwardedMessage.getCompleteDataset().listNames(); names.hasNext(); ){
        String graphUri = names.next();
        dataset.addNamedModel(graphUri, this.forwardedMessage.getCompleteDataset().getNamedModel(graphUri));
      }
    }
    //create a new envelope graph uri and add the envelope graph to the dataset
    String envelopeGraphURI = RdfUtils.createNewGraphURI(messageURI.toString(), ENVELOPE_URI_SUFFIX,4,dataset).toString();

    //the [envelopeGraphURI] rdf:type msg:EnvelopeGraph triple in the default graph is required to find the
    //envelope graph.
    defaultModel.createResource(envelopeGraphURI, WONMSG.ENVELOPE_GRAPH);
    dataset.addNamedModel(envelopeGraphURI, envelopeGraph);

    // message URI
    Resource messageEventResource = envelopeGraph.createResource(messageURI.toString());
    if (wonMessageType != null){
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

    // add responseMessageState
    if (responseMessageState != null) {
      messageEventResource.addProperty(
        WONMSG.HAS_RESPONSE_STATE_PROPERTY,
        envelopeGraph.createResource(responseMessageState.toString()));
    } else {
      if (wonMessageType != null && WONMSG.isResponseMessageType(wonMessageType.getResource())) {
        throw new WonMessageBuilderException(
          "Message type is " + wonMessageType.getResource().toString() +
            " but no response message state has been provided.");
      }
    }

    if (timestamp != null) {
      messageEventResource.addProperty(
        WONMSG.HAS_TIMESTAMP,
        envelopeGraph.createTypedLiteral(this.timestamp));
    }

    for (URI contentURI : contentMap.keySet()) {
      String uniqueContentUri = RdfUtils.createNewGraphURI(contentURI.toString(), CONTENT_URI_SUFFIX, 5,
        dataset).toString();
      dataset.addNamedModel(uniqueContentUri, contentMap.get(contentURI));
      messageEventResource.addProperty(
        WONMSG.HAS_CONTENT_PROPERTY, messageEventResource
          .getModel().createResource(uniqueContentUri));
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

    // ToDo (FS): add signature of the whole message

    return new WonMessage(dataset);

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
    this.wrappedMessage = toWrap;
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
    this.forwardedMessage = toForward;
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
      .setWonMessageType(WonMessageType.OPEN)
      .setSenderURI(localConnection)
      .setSenderNeedURI(localNeed)
      .setSenderNodeURI(localWonNode)
      .setReceiverURI(remoteConnection)
      .setReceiverNeedURI(remoteNeed)
      .setReceiverNodeURI(remoteWonNode)
      .setTimestampToNow();

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
      .setWonMessageType(WonMessageType.CLOSE)
      .setSenderURI(localConnection)
      .setSenderNeedURI(localNeed)
      .setSenderNodeURI(localWonNode)
      .setReceiverURI(remoteConnection)
      .setReceiverNeedURI(remoteNeed)
      .setReceiverNodeURI(remoteWonNode)
      .setTimestampToNow();

    return this;
  }

  public WonMessageBuilder setMessagePropertiesForNeedState(
    URI messageURI,
    NeedState needState,
    URI localNeed,
    URI localWonNode) {

    // create need state RDF (message event content)
    Model contentModel = ModelFactory.createDefaultModel();
    contentModel.add(contentModel.createResource(localNeed.toString()), WON.IS_IN_STATE, needState.getURI().toString());

    this
      .setMessageURI(messageURI)
      .setWonMessageType(WonMessageType.DEACTIVATE)
      .setSenderNeedURI(localNeed)
      .setReceiverNeedURI(localNeed)
      .setReceiverNodeURI(localWonNode)
        // ToDo (FS): remove the hardcoded part of the URI
      .addContent(URI.create(messageURI.toString() + "/content"), contentModel, null)
      .setTimestampToNow();

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
      .setWonMessageType(WonMessageType.CONNECT)
      .setSenderNeedURI(localNeed)
      .setSenderNodeURI(localWonNode)
      .setReceiverURI(remoteFacet)
      .setReceiverNeedURI(remoteNeed)
      .setReceiverNodeURI(receiverNodeURI)
        // ToDo (FS): remove the hardcoded part of the URI
      .addContent(URI.create(messageURI.toString() + "/content"), facetModel, null)
      .setTimestampToNow();

    return this;
  }

  public WonMessageBuilder setMessagePropertiesForCreate(
    URI messageURI,
    URI needURI,
    URI wonNodeURI) {

    this
      .setMessageURI(messageURI)
      .setWonMessageType(WonMessageType.CREATE_NEED)
      .setSenderNeedURI(needURI)
      .setReceiverNodeURI(wonNodeURI)
      .setTimestampToNow();

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
      .setWonMessageType(WonMessageType.HINT_MESSAGE)
      .setSenderNodeURI(matcherURI)
      .setReceiverURI(needFacetURI)
      .setReceiverNeedURI(needURI)
      .setReceiverNodeURI(wonNodeURI)
      .setTimestampToNow()
        // ToDo (FS): remove the hardcoded part of the URI
      .addContent(URI.create(messageURI.toString() + "/content"), contentModel, null);

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
      .setWonMessageType(WonMessageType.HINT_NOTIFICATION)
      .setSenderNodeURI(matcherURI)
      .setReceiverURI(needConnectionURI)
      .setReceiverNeedURI(needURI)
      .setReceiverNodeURI(wonNodeURI)
        // ToDo (FS): remove the hardcoded part of the URI
      .addContent(URI.create(messageURI.toString() + "/content"), contentModel, null)
      .setTimestampToNow();

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
      .setWonMessageType(WonMessageType.CONNECTION_MESSAGE)
      .setSenderURI(localConnection)
      .setSenderNeedURI(localNeed)
      .setSenderNodeURI(localWonNode)
      .setReceiverURI(remoteConnection)
      .setReceiverNeedURI(remoteNeed)
      .setReceiverNodeURI(remoteWonNode)
      .addContent(URI.create(messageURI.toString() + "/content"), content, null)
      .setTimestampToNow();

    return this;
  }


  public WonMessageBuilder setMessagePropertiesForNeedCreatedNotification(
    URI messageURI,
    URI localNeed,
    URI localWonNode) {

    this.setWonMessageType(WonMessageType.NEED_CREATED_NOTIFICATION)
        .setMessageURI(messageURI)
        .setSenderNeedURI(localNeed)
        .setSenderNodeURI(localWonNode)
        .setTimestampToNow();
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

  /**
   * Adds the specified content graph, and the specified signature graph, using the specified
   * contentURI as the graph name. The contentURI will be made unique inside the message dataset
   * by appending characters at the end.
   * @param contentURI
   * @param content
   * @param signature, may be null
   * @return
   */
  public WonMessageBuilder addContent(URI contentURI, Model content, Model signature) {
    Random rnd = new Random(System.currentTimeMillis());
    URI originalContentUri = contentURI;
    //add a random suffix to the uri
    while (contentMap.containsKey(contentURI)){
      contentURI = URI.create(originalContentUri.toString() + randomString.nextString(RANDOM_SUFFIX_LENGTH));
    }
    contentMap.put(contentURI, content);
    if (signature != null)
      signatureMap.put(contentURI, signature);
    return this;
  }

  public WonMessageBuilder addRefersToURI(URI refersTo) {
    refersToURIs.add(refersTo);
    return this;
  }

  public WonMessageBuilder setResponseMessageState(Resource responseMessageState) {
    this.responseMessageState = responseMessageState;
    return this;
  }

  public WonMessageBuilder setTimestamp(final long timestamp) {
    this.timestamp = timestamp;
    return this;
  }

  public WonMessageBuilder setTimestampToNow() {
    this.timestamp = System.currentTimeMillis();
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
  public WonMessageBuilder copyEnvelopeFromWonMessage(final WonMessage wonMessage) {
    return this
      .setWonMessageType(wonMessage.getMessageType())
      .setReceiverURI(wonMessage.getReceiverURI())
      .setReceiverNeedURI(wonMessage.getReceiverNeedURI())
      .setReceiverNodeURI(wonMessage.getReceiverNodeURI())
      .setSenderURI(wonMessage.getSenderURI())
      .setSenderNeedURI(wonMessage.getSenderNeedURI())
      .setSenderNodeURI(wonMessage.getSenderNodeURI());
  }

  /**
   * Copies all content graphs from the specified message, replacing all occurrences
   * of the specified message's URI with the messageURI of this builder.
   * @param wonMessage
   * @return
   */
  public WonMessageBuilder copyContentFromMessageReplacingMessageURI(final WonMessage wonMessage) {
    return copyContentFromMessage(wonMessage, true);
  }

  /**
   * Copies all content graphs from the specified message to this builder.
   *
   * @param wonMessage
   * @return
   */
  public WonMessageBuilder copyContentFromMessage(final WonMessage wonMessage) {
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
  public WonMessageBuilder copyContentFromMessage(final WonMessage wonMessage, boolean replaceMessageUri) {
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

      addContent(URI.create(newModelUri), model,null);
    }
    return this;
  }

  /**
   * Returns a WonMessageBuilder that is fully prepared to build a copy of the specified
   * inbound message.
   * That message will have a the specified newMessageUri and a copy of the
   * content that is modified such that the URI of the inbound message is replaced
   * by the new message URI. A timestamp will be set, and it will link to the original
   * message.
   *
   * @param newMessageUri
   * @param localConnectionUri
   * @param inboundWonMessage
   * @return
   */
  public static WonMessage copyInboundWonMessageForLocalStorage(final URI newMessageUri,
    final URI localConnectionUri, final WonMessage inboundWonMessage) {
    URI inboundReceiverURI = inboundWonMessage.getReceiverURI();
    if (inboundReceiverURI != null){
      assert inboundReceiverURI.equals(localConnectionUri) : "inbound wonMessage's receiverURI is not the expected " +
        "localConnectionURI";
    }
    return new WonMessageBuilder()
      .setMessageURI(newMessageUri)
      .copyEnvelopeFromWonMessage(inboundWonMessage)
      .copyContentFromMessageReplacingMessageURI(inboundWonMessage)
      .setReceiverURI(localConnectionUri)
      .setTimestamp(new Date().getTime())
      .addRefersToURI(inboundWonMessage.getMessageURI())
      .build();
  }

  public static WonMessage wrapOutboundWonMessageForLocalStorage(final URI localConnectionUri, final WonMessage
    outboundWonMessage){
    WonMessageBuilder builder = new WonMessageBuilder()
      .wrap(outboundWonMessage)
      .setTimestamp(new Date().getTime());
    if (localConnectionUri != null) {
      builder.setSenderURI(localConnectionUri);
    }
    return builder.build();
  }


  public static WonMessage wrapOwnerToNeedWonMessageForLocalStorage(final WonMessage
    ownerToNeedWonMessage){
    WonMessageBuilder builder = new WonMessageBuilder()
      .wrap(ownerToNeedWonMessage)
      .setTimestamp(new Date().getTime());
    return builder.build();
  }

  public static WonMessage forwardReceivedWonMessage(final URI newMessageUri, final WonMessage wonMessage,
    final URI connectionURI, final URI needURI, final URI wonNodeUri,
    final URI remoteConnectionURI, final URI remoteNeedURI, final URI remoteWonNodeUri) {
    WonMessageBuilder builder = new WonMessageBuilder()
      .setMessageURI(newMessageUri)
      .forward(wonMessage)
      .copyContentFromMessageReplacingMessageURI(wonMessage)
      .setTimestamp(System.currentTimeMillis())
      .setReceiverURI(remoteConnectionURI)
      .setReceiverNeedURI(remoteNeedURI)
      .setReceiverNodeURI(remoteWonNodeUri);
    return builder.build();
  }


  private void checkProperties() {

    // ToDo (FS): implement

  }

}
