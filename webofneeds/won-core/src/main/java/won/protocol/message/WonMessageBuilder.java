package won.protocol.message;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.model.NeedState;
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
  private static final String CONTENT_URI_APPENDIX = "#content-";
  private static final String SIGNATURE_URI_APPENDIX = "#signature-";
  private static final String ENVELOPE_URI_APPENDIX = "#envelope-";

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
        //the [wrappedMessage.envelopeGraphURI] rdf:type msg:EnvelopeGraph triple in the default graph is required to
        // find the wrapped envelope graph.
        envelopeGraph.createResource(wrappedMessage.getOuterEnvelopeGraphURI().toString(),
          WONMSG.ENVELOPE_GRAPH);
      //copy all named graphs to the new message dataset
      for (Iterator<String> names = wrappedMessage.getCompleteDataset().listNames(); names.hasNext(); ){
        String graphUri = names.next();
        dataset.addNamedModel(graphUri, wrappedMessage.getCompleteDataset().getNamedModel(graphUri));
      }
    }
    //create a new envelope graph uri and add the envelope graph to the dataset
    String envelopeGraphURI = RdfUtils.createNewGraphURI(messageURI.toString(), ENVELOPE_URI_APPENDIX ,4,dataset).toString();

    //the [envelopeGraphURI] rdf:type msg:EnvelopeGraph triple in the default graph is required to find the
    //envelope graph.
    defaultModel.createResource(envelopeGraphURI, WONMSG.ENVELOPE_GRAPH);
    dataset.addNamedModel(envelopeGraphURI, envelopeGraph);

    // message URI
    Resource messageEventResource = envelopeGraph.createResource(messageURI.toString());
    if (wonMessageType != null){
      messageEventResource.addProperty(WONMSG.HAS_MESSAGE_TYPE_PROPERTY, wonMessageType.getResource());
    }

    // ToDo (FS): also add the signatures
    for (URI contentURI : contentMap.keySet()) {
      messageEventResource.addProperty(
        WONMSG.HAS_CONTENT_PROPERTY,
        envelopeGraph.createResource(contentURI.toString()));
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

    if (timestamp != null){
      messageEventResource.addProperty(
        WONMSG.HAS_TIMESTAMP,
        envelopeGraph.createTypedLiteral(this.timestamp));
    }

    for (URI contentURI : contentMap.keySet()) {
      String uniqueContentUri = RdfUtils.createNewGraphURI(contentURI.toString(), CONTENT_URI_APPENDIX, 5,
        dataset).toString();
      dataset.addNamedModel(uniqueContentUri, contentMap.get(contentURI));
      Model signatureGraph = signatureMap.get(contentURI);
      if (signatureGraph != null) {
        uniqueContentUri = RdfUtils.createNewGraphURI(contentURI.toString(), SIGNATURE_URI_APPENDIX, 5,
          dataset).toString();
        //the signature refers to the name of the other graph. We changed that name
        //so we have to replace the resource referencing it, too:
        signatureGraph = RdfUtils.replaceResource(signatureGraph.getResource(contentURI.toString()),
          signatureGraph.getResource(uniqueContentUri));
        dataset.addNamedModel(uniqueContentUri, signatureGraph);
      }
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
      .setReceiverNodeURI(remoteWonNode);

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
      .setReceiverNodeURI(remoteWonNode);

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
      .addContent(URI.create(messageURI.toString() + "/content"), contentModel, null);

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
      .addContent(URI.create(messageURI.toString() + "/content"), facetModel, null);

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
      .setReceiverNodeURI(wonNodeURI);

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
    double score,
    long timestamp) {

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
      .setTimestamp(timestamp)
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
      .addContent(URI.create(messageURI.toString() + "/content"), contentModel, null);

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
      .addContent(URI.create(messageURI.toString() + "/content"), content, null);

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
    Dataset messageContent = wonMessage.getMessageContent();
    for (Iterator<String> nameIt = messageContent.listNames(); nameIt.hasNext(); ){
      //replace the messageURI of the specified message with that of this builder, just in case
      //there are triples in the model that about the message
      String modelUri = nameIt.next();
      Model model = messageContent.getNamedModel(modelUri);
      model = RdfUtils.replaceResource(model.getResource(wonMessage.getMessageURI().toString()),
        model.getResource(this.messageURI.toString()));
      addContent(URI.create(modelUri), model,null);
    }
    return this;
  }


  private void checkProperties() {

    // ToDo (FS): implement

  }

}
