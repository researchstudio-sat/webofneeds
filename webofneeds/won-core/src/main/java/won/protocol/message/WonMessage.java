package won.protocol.message;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONMSG;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * User: ypanchenko
 * Date: 04.08.2014
 */
public class WonMessage implements Serializable
{

  final Logger logger = LoggerFactory.getLogger(getClass());

  private Dataset messageContent;
  //private Model messageMetadata;
  //private URI messageEventURI;
  private MessageEvent messageEvent;


  //private Resource msgBnode;
  // private Signature signature;


  public WonMessage() {
    initMessageMetadata();
  }

  public WonMessage(MessageEvent messageEvent, Dataset messageContent) {
    //this.messageEventURI = messageEventURI;
    this.messageEvent = messageEvent;
    this.messageContent = messageContent;
    //initMessageMetadata();
  }

  private void initMessageMetadata() {
    Model defaultModel = ModelFactory.createDefaultModel();
    defaultModel.setNsPrefix(WONMSG.DEFAULT_PREFIX, WONMSG.BASE_URI);
    Resource messageEventResource = defaultModel.createResource(messageEvent.getMessageURI().toString());
    defaultModel.createStatement(messageEventResource, RDF.type, WONMSG.ENVELOPE_GRAPH);
    //TODO own message event signature
  }


  public Dataset getMessageContent() {
    return messageContent;
  }

  public Model getMessageContent(String contentResourceUri) {
    String ngName = getNamedGraphNameForUri(contentResourceUri);
    return messageContent.getNamedModel(ngName);
  }

  public List<Model> getPayloadGraphs(){
    List<Model> ret = new ArrayList<Model>();
    List<Model> envelopeGraphs = getEnvelopeGraphs();
    for (Model envelopeGraph : envelopeGraphs){
      for (Resource payloadGraphUri :getPayloadReferences(envelopeGraph)){
        Model payload = this.messageContent.getNamedModel(payloadGraphUri.toString());
        if (payload == null){
          throw new IllegalStateException("payload graph " + payloadGraphUri + " reference in envelope graph but not " +
            "found in dataset");
        }
        ret.add(payload);
      }
    }
    return ret;
  }

  public List<Model> getEnvelopeGraphs(){
    Model model = this.messageContent.getDefaultModel();
    if (model == null) throw new IllegalStateException("default model must not be null");
    boolean mustContainEnvelopeGraphRef = true;
    List<Model> ret = new ArrayList<Model>();
    do {
      Resource envelopeGraphResource = getEnvelopeGraphReference(model, mustContainEnvelopeGraphRef);
      model = null;
      if (envelopeGraphResource != null) {
        mustContainEnvelopeGraphRef = false; //only needed for default model
        model = this.messageContent.getNamedModel(envelopeGraphResource.toString());
        if (model == null) throw new IllegalStateException("envelope graph referenced in model" +
          envelopeGraphResource + " but not found as named graph in dataset!");
        ret.add(model);
      }
    } while (model != null);
    return ret;
  }

  private Resource getEnvelopeGraphReference(Model model, boolean mustContainEnvelopeGraphRef){
    StmtIterator it = model.listStatements(null, RDF.type, WONMSG.ENVELOPE_GRAPH);
    if (mustContainEnvelopeGraphRef && !it.hasNext()) throw new IllegalStateException("no envelope graph found");
    Resource envelopeGraphResource = it.nextStatement().getSubject();
    if (it.hasNext()) throw new IllegalStateException("more than one envelope graphs " +
      "referenced in model!");
    return envelopeGraphResource;
  }

  private List<Resource> getPayloadReferences(Model envelopeGraph){
    StmtIterator it = envelopeGraph.listStatements(envelopeGraph.getResource(messageEvent.getMessageURI().toString()),
      WONMSG.HAS_CONTENT_PROPERTY, (RDFNode) null);
    List<Resource> ret = new ArrayList<Resource>();
    while (it.hasNext()){
      ret.add(it.nextStatement().getObject().asResource());
    }
    return ret;
  }

  public Dataset getMessageWithSignature(String contentResourceUri) {
    Dataset dataset = DatasetFactory.createMem();
    String ngName = getNamedGraphNameForUri(contentResourceUri);
    dataset.addNamedModel(ngName, messageContent.getNamedModel(ngName));
    RdfUtils.addPrefixMapping(dataset.getDefaultModel(), messageContent.getNamedModel(ngName));
    RdfUtils.addPrefixMapping(dataset.getDefaultModel(), messageContent.getDefaultModel());
    //TODO signature into default graph
    return dataset;
  }

  private String getNamedGraphNameForUri(final String resourceUri) {
    String ngName = resourceUri;
    // we commonly use resource url + #data for the name of named graph
    // with this resource content
    if (messageContent.getNamedModel(resourceUri) == null) {
      ngName = resourceUri + WonRdfUtils.NAMED_GRAPH_SUFFIX;
    }
    return ngName;
  }

  /**
   * Returns a list of all the URIs of the message contents
   * (not the corresponding graphs)
   *
   * @return List of strings each representing one of the requested URLs
   */
  public List<String> getMessageContentURIs()
  {
    List<String> result = new ArrayList<String>();

    Iterator<String> graphNames = messageContent.listNames();
    while (graphNames.hasNext()) {
        result.add(graphNames.next().replaceAll("#.*", ""));
    }
    return result;
  }

  public void setMessageContent(Dataset messageContent) {
    this.messageContent = messageContent;
  }

  public MessageEvent getMessageEvent() {
    return messageEvent;
  }

  public void setMessageEvent(final MessageEvent messageEvent) {
    this.messageEvent = messageEvent;
  }
}
