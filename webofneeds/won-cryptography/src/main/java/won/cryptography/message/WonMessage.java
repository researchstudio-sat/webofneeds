package won.cryptography.message;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.*;

import java.util.Iterator;

/**
 * User: ypanchenko
 * Date: 04.08.2014
 */
public class WonMessage
{

  private Dataset messageContent;
  private Model messageMetadata;
  private MessageMethod method;
  //private MessageProtocol protocol;
  private String protocolUri;
  // TODO
  // private Signature signature;


  public WonMessage() {
    initMessageMetadata();
  }

  //    public WonMessage(MessageProtocol protocol, MessageMethod method, Dataset messageContent) {
//        this.protocol = protocol;
//        this.method = method;
//        this.messageContent = messageContent;
//        initMessageMetadata(protocol, method);
//    }
  public WonMessage(String protocolUri, MessageMethod method, Dataset messageContent) {
    this.protocolUri = protocolUri;
    this.method = method;
    this.messageContent = messageContent;
    initMessageMetadata();
    addMessageMetadata(protocolUri, method);
  }

  private void initMessageMetadata() {
    messageMetadata = ModelFactory.createDefaultModel();
    messageMetadata.setNsPrefix(MessageOntology.DEFAULT_PREFIX, MessageOntology.MESSAGE_ONTOLOGY_URI);
    //messageMetadata.getNsPrefixMap().put(MessageOntology.DEFAULT_PREFIX, MessageOntology.MESSAGE_ONTOLOGY_URI);
  }


  private void addMessageMetadata(String protocolUri, MessageMethod method) {

    Resource msgBnode = messageMetadata.createResource();

    // create protocol triple
    Property protocolProp = messageMetadata
      .createProperty(MessageOntology.MESSAGE_ONTOLOGY_URI, MessageOntology.PROTOCOL_PROPERTY);
    Resource protocolResource = messageMetadata.createResource(protocolUri);
    msgBnode.addProperty(protocolProp, protocolResource);

    // create method triples
    Property methodProp = messageMetadata
      .createProperty(MessageOntology.MESSAGE_ONTOLOGY_URI, MessageOntology.METHOD_PROPERTY);
    Resource methodResource = messageMetadata.createResource(method.getMethodUri());
    msgBnode.addProperty(methodProp, methodResource);
    // including method parameters triples
    for (String paramUri : method.getParameterMap().keySet()) {
      Property paramNameProp = messageMetadata
        .createProperty(MessageOntology.MESSAGE_ONTOLOGY_URI, MessageOntology.PARAM_NAME_PROPERTY);
      Resource paramNameResource = messageMetadata.createResource(paramUri);
      methodResource.addProperty(paramNameProp, paramNameResource);
      Property paramValueProp = messageMetadata
        .createProperty(MessageOntology.MESSAGE_ONTOLOGY_URI, MessageOntology.PARAM_VALUE_PROPERTY);
      Literal paramValueResource = messageMetadata.createLiteral(method.getParameterMap().get(paramUri));
      methodResource.addProperty(paramValueProp, paramValueResource);
    }

  }

  public Dataset getMessageContent() {
    return messageContent;
  }

  public void setMessageContent(Dataset messageContent) {
    this.messageContent = messageContent;
  }

  public MessageMethod getMethod() {
    return method;
  }

  public void setMethod(MessageMethod method) {
    this.method = method;
    //TODO add triples to
  }

//    public MessageProtocol getProtocol() {
//        return protocol;
//    }

  public String getProtocol() {
    return protocolUri;
  }

  public void setProtocol(String protocolUri) {
    this.protocolUri = protocolUri;
  }

  public boolean hasMethod(String methodUri) {
    return (this.method != null && this.method.getMethodUri().equals(methodUri));
  }

  public Dataset asDataset() {
    // TODO create clone instead?
    Dataset dataset = DatasetFactory.createMem();
    dataset.getDefaultModel().add(messageMetadata);
    // TODO the approach from example 1 fits better since the signatures are already in the default graph
    dataset.getDefaultModel().add(messageContent.getDefaultModel());
    Iterator<String> names = messageContent.listNames();
    while (names.hasNext()) {
      String name = names.next();
      dataset.addNamedModel(name, messageContent.getNamedModel(name));
    }
    for (String prefix : messageContent.getDefaultModel().getNsPrefixMap().keySet()) {
      dataset.getDefaultModel().setNsPrefix(prefix, messageContent.getDefaultModel().getNsPrefixMap().get(prefix));
    }
    // TODO need to check if the prefix is already in use
    for (String prefix : messageMetadata.getNsPrefixMap().keySet()) {
      dataset.getDefaultModel().setNsPrefix(prefix, messageMetadata.getNsPrefixMap().get(prefix));
    }
    return dataset;
  }
}
