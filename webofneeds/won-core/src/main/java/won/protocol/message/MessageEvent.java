package won.protocol.message;

import com.hp.hpl.jena.rdf.model.Model;
import won.protocol.model.NeedState;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: ypanchenko
 * Date: 12.08.2014
 */
public class MessageEvent
{

  // ToDo (FS): now we also store the model so either each setter method also modifies the model or we only allow the setting of the properties by a constructor

  private URI messageURI;
  private WonMessageType messageType; // ConnectMessage, CreateMessage, NeedStateMessage
  // TODO can Connection event be a message event???
  private List<URI> hasContent = new ArrayList<>();
  private Map<URI,Model> signatures = new HashMap<>();
  private URI senderURI;
  private URI senderNeedURI;
  private URI senderNodeURI;
  private URI receiverURI;
  private URI receiverNeedURI;
  private URI receiverNodeURI;
  private List<URI> refersTo = new ArrayList<>();
  private URI responseState;

  // ToDo (FS): move such properties into specialized sub classes?
  // are there other param that should be part of the messageEvent object?
  private NeedState newNeedState;

  // the RDF model representing the MessageEvent
  private Model model;

  //TODO should signature be stored inside the object or otside?
  //if inside - as Model or as Signature object (to be implemented or used the one from signingframework)
  //private Model ownSignature;


  public URI getMessageURI() {
    return messageURI;
  }

  public void setMessageURI(final URI messageURI) {
    this.messageURI = messageURI;
  }

  public WonMessageType getMessageType() {
    return messageType;
  }

  public void setMessageType(final WonMessageType messageType) {
    this.messageType = messageType;
  }

  public List<URI> getHasContent() {
    return hasContent;
  }

  public void setHasContent(final List<URI> hasContent) {
    this.hasContent = hasContent;
  }

  public void addHasContent(URI contentURI) {
    this.hasContent.add(contentURI);
  }

  public Map<URI, Model> getSignatures() {
    return signatures;
  }

  //TODO: here also: signatures should be stored as model or signature objects themselves?...
  public void setSignatures(final Map<URI, Model> signatures) {
    this.signatures = signatures;
  }

  public void addSignature(URI resourceURI, Model resourceSignature) {
    this.signatures.put(resourceURI, resourceSignature);
  }

  public URI getSenderURI() {
    return senderURI;
  }

  public void setSenderURI(final URI senderURI) {
    this.senderURI = senderURI;
  }

  public URI getSenderNeedURI() {
    return senderNeedURI;
  }

  public void setSenderNeedURI(final URI senderNeedURI) {
    this.senderNeedURI = senderNeedURI;
  }

  public URI getSenderNodeURI() {
    return senderNodeURI;
  }

  public void setSenderNodeURI(final URI senderNodeURI) {
    this.senderNodeURI = senderNodeURI;
  }

  public URI getReceiverURI() {
    return receiverURI;
  }

  public void setReceiverURI(final URI receiverURI) {
    this.receiverURI = receiverURI;
  }

  public URI getReceiverNeedURI() {
    return receiverNeedURI;
  }

  public void setReceiverNeedURI(final URI receiverNeedURI) {
    this.receiverNeedURI = receiverNeedURI;
  }

  public URI getReceiverNodeURI() {
    return receiverNodeURI;
  }

  public void setReceiverNodeURI(final URI receiverNodeURI) {
    this.receiverNodeURI = receiverNodeURI;
  }

  public List<URI> getRefersTo() {
    return refersTo;
  }

  public void setRefersTo(final List<URI> refersTo) {
    this.refersTo = refersTo;
  }

  public void addRefersTo(URI refersToURI) {
    this.refersTo.add(refersToURI);
  }

  public URI getResponseState() {
    return responseState;
  }

  public void setResponseState(final URI responseState) {
    this.responseState = responseState;
  }

  public NeedState getNewNeedState() {
    return newNeedState;
  }

  public void setNewNeedState(final NeedState newNeedState) {
    this.newNeedState = newNeedState;
  }

  public void setModel(final Model model)
  {
    this.model = model;
  }

  public Model getModel()
  {
    return model;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (!(o instanceof MessageEvent)) return false;

    final MessageEvent that = (MessageEvent) o;

    if (hasContent != null ? !hasContent.equals(that.hasContent) : that.hasContent != null) return false;
    if (messageType != null ? !messageType.equals(that.messageType) : that.messageType != null) return false;
    if (messageURI != null ? !messageURI.equals(that.messageURI) : that.messageURI != null) return false;
    if (newNeedState != null ? !newNeedState.equals(that.newNeedState) : that.newNeedState != null) return false;
    if (receiverURI != null ? !receiverURI.equals(that.receiverURI) : that.receiverURI != null) return false;
    if (refersTo != null ? !refersTo.equals(that.refersTo) : that.refersTo != null) return false;
    if (senderURI != null ? !senderURI.equals(that.senderURI) : that.senderURI != null) return false;
    if (signatures != null ? !signatures.equals(that.signatures) : that.signatures != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = messageURI != null ? messageURI.hashCode() : 0;
    result = 31 * result + (messageType != null ? messageType.hashCode() : 0);
    result = 31 * result + (hasContent != null ? hasContent.hashCode() : 0);
    result = 31 * result + (signatures != null ? signatures.hashCode() : 0);
    result = 31 * result + (senderURI != null ? senderURI.hashCode() : 0);
    result = 31 * result + (receiverURI != null ? receiverURI.hashCode() : 0);
    result = 31 * result + (refersTo != null ? refersTo.hashCode() : 0);
    result = 31 * result + (newNeedState != null ? newNeedState.hashCode() : 0);
    return result;
  }
}
