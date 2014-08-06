package won.cryptography.message;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * User: ypanchenko
 * Date: 04.08.2014
 */
public class WonMessage
{

  private Dataset messageContent;
  private Model messageMetadata;
  private WonMessageMethod method;
  private String protocolUri;
  private Resource msgBnode;
  // private Signature signature;


  public WonMessage() {
    initMessageMetadata();
  }

  public WonMessage(String protocolUri, WonMessageMethod method, Dataset messageContent) {
    this.protocolUri = protocolUri;
    this.method = method;
    this.messageContent = messageContent;
    initMessageMetadata();
    addMessageMetadata(protocolUri, method);
  }

  private void initMessageMetadata() {
    messageMetadata = ModelFactory.createDefaultModel();
    messageMetadata.setNsPrefix(WonMessageOntology.DEFAULT_PREFIX, WonMessageOntology.MESSAGE_ONTOLOGY_URI);
    this.msgBnode = messageMetadata.createResource();
    Resource protocolResource = this.messageMetadata.createResource(WonMessageOntology.MESSAGE_TYPE_RESOURCE);
    msgBnode.addProperty(RDF.type, protocolResource);
  }


  private void addMessageMetadata(String protocolUri, WonMessageMethod method) {
    // create protocol triple
    addProtocolTriples(protocolUri);
    // create method triples
    addMethodTriples(method);
  }

  private void addProtocolTriples(final String protocolUri) {

    Property protocolProp = this.messageMetadata
      .createProperty(WonMessageOntology.MESSAGE_ONTOLOGY_URI, WonMessageOntology.PROTOCOL_PROPERTY);
    Resource protocolResource = this.messageMetadata.createResource(this.protocolUri);
    msgBnode.addProperty(protocolProp, protocolResource);
  }

  private void addMethodTriples(final WonMessageMethod method) {
    Property methodProp = messageMetadata
      .createProperty(WonMessageOntology.MESSAGE_ONTOLOGY_URI, WonMessageOntology.METHOD_PROPERTY);
    Resource methodResource = messageMetadata.createResource(method.getMethodUri());
    msgBnode.addProperty(methodProp, methodResource);
    // including method parameters triples
    for (String paramUri : method.getParameterMap().keySet()) {
      Property paramNameProp = messageMetadata
        .createProperty(WonMessageOntology.MESSAGE_ONTOLOGY_URI, WonMessageOntology.PARAM_NAME_PROPERTY);
      Resource paramNameResource = messageMetadata.createResource(paramUri);
      methodResource.addProperty(paramNameProp, paramNameResource);
      Property paramValueProp = messageMetadata
        .createProperty(WonMessageOntology.MESSAGE_ONTOLOGY_URI, WonMessageOntology.PARAM_VALUE_PROPERTY);
      Literal paramValueResource = messageMetadata.createLiteral(method.getParameterMap().get(paramUri));
      methodResource.addProperty(paramValueProp, paramValueResource);
    }
  }

  public Dataset getMessageContent() {
    return messageContent;
  }

  public Model getMessageMetadata() {
    return messageMetadata;
  }

  public void setMessageContent(Dataset messageContent) {
    this.messageContent = messageContent;
  }

  public WonMessageMethod getMethod() {
    return method;
  }

  public void setMethod(WonMessageMethod method) {
    this.method = method;
    addMethodTriples(method);
  }

  public String getProtocol() {
    return protocolUri;
  }

  public void setProtocol(String protocolUri) {
    this.protocolUri = protocolUri;
    addProtocolTriples(protocolUri);
  }

  public boolean hasMethod(String methodUri) {
    return (this.method != null && this.method.getMethodUri().equals(methodUri));
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (!(o instanceof WonMessage)) return false;

    final WonMessage message = (WonMessage) o;

    if (messageContent != null ? !messageContent.equals(message.messageContent) : message.messageContent != null)
      return false;
    if (method != null ? !method.equals(message.method) : message.method != null) return false;
    if (protocolUri != null ? !protocolUri.equals(message.protocolUri) : message.protocolUri != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = messageContent != null ? messageContent.hashCode() : 0;
    result = 31 * result + (method != null ? method.hashCode() : 0);
    result = 31 * result + (protocolUri != null ? protocolUri.hashCode() : 0);
    return result;
  }
}
