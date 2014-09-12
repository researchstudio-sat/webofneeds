package won.protocol.message;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.util.DefaultPrefixUtils;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class to build a WonMessage based on the specific properties.
 *
 * @author Fabian Salcher
 */
public class WonMessageBuilder
{

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

    Model defaultModel = null;

    if (dataset == null) {
      // create the default model, containing a triple that denotes the message graph as
      // envelope graph
      defaultModel = ModelFactory.createDefaultModel();
      dataset = DatasetFactory.create(defaultModel);
    } else {
      defaultModel = dataset.getDefaultModel();
      if (defaultModel == null) {
        defaultModel = ModelFactory.createDefaultModel();
        dataset.setDefaultModel(defaultModel);
      }
    }

    defaultModel.createResource(WONMSG.getGraphURI(messageURI.toString()), WONMSG.ENVELOPE_GRAPH);

    Model messageEvent = ModelFactory.createDefaultModel();
    DefaultPrefixUtils.setDefaultPrefixes(messageEvent);


    dataset.addNamedModel(WONMSG.getGraphURI(messageURI.toString()), messageEvent);

    // message URI
    Resource messageEventResource = messageEvent.createResource(messageURI.toString());

    messageEventResource.addProperty(WONMSG.HAS_MESSAGE_TYPE_PROPERTY, wonMessageType.getResource());

    // ToDo (FS): also add the signatures
    for (URI contentURI : contentMap.keySet()) {
      messageEventResource.addProperty(
        WONMSG.HAS_CONTENT_PROPERTY,
        messageEvent.createResource(contentURI.toString()));
    }

    // add sender
    if (senderURI != null)
      messageEventResource.addProperty(
        WONMSG.SENDER_PROPERTY,
        messageEvent.createResource(senderURI.toString()));
    if (senderNeedURI != null)
      messageEventResource.addProperty(
        WONMSG.SENDER_NEED_PROPERTY,
        messageEvent.createResource(senderNeedURI.toString()));
    if (senderNodeURI != null)
      messageEventResource.addProperty(
        WONMSG.SENDER_NODE_PROPERTY,
        messageEvent.createResource(senderNodeURI.toString()));

    // add receiver
    if (receiverURI != null)
      messageEventResource.addProperty(
        WONMSG.RECEIVER_PROPERTY,
        messageEvent.createResource(receiverURI.toString()));
    if (receiverNeedURI != null)
      messageEventResource.addProperty(
        WONMSG.RECEIVER_NEED_PROPERTY,
        messageEvent.createResource(receiverNeedURI.toString()));
    if (receiverNodeURI != null)
      messageEventResource.addProperty(
        WONMSG.RECEIVER_NODE_PROPERTY,
        messageEvent.createResource(receiverNodeURI.toString()));

    // add refersTo
    for (URI refersToURI : refersToURIs) {
      messageEventResource.addProperty(
        WONMSG.REFERS_TO_PROPERTY,
        messageEvent.createResource(refersToURI.toString()));
    }

    // add responseMessageState
    if (responseMessageState != null) {
      messageEventResource.addProperty(
        WONMSG.HAS_RESPONSE_STATE_PROPERTY,
        messageEvent.createResource(responseMessageState.toString()));
    } else {
      if (WONMSG.isResponseMessageType(wonMessageType.getResource())) {
        throw new WonMessageBuilderException(
          "Message type is " + wonMessageType.getResource().toString() +
            " but no response message state has been provided.");
      }
    }


    for (URI contentURI : contentMap.keySet()) {
      dataset.addNamedModel(
        contentURI.toString(),
        contentMap.get(contentURI));
    }
    dataset.setDefaultModel(defaultModel);

    // ToDo (FS): add signature of the whole message

    // ToDo (FS): since all the properties are already available this can be done more efficiently
    WonMessage wonMessage = WonMessageDecoder.decodeFromDataset(dataset);
    return wonMessage;

  }

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

}
