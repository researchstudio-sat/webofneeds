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
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
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
  private Dataset completeDataset;
  //private Model messageMetadata;
  //private URI messageEventURI;
  private List<Model> envelopeGraphs;
  private List<String> envelopeGraphNames;
  private URI outerEnvelopeGraphURI;
  private Model outerEnvelopeGraph;

  private URI messageURI;
  private WonMessageType messageType; // ConnectMessage, CreateMessage, NeedStateMessage
  private WonEnvelopeType envelopeType;
  private URI senderURI;
  private URI senderNeedURI;
  private URI senderNodeURI;
  private URI receiverURI;
  private URI receiverNeedURI;
  private URI receiverNodeURI;
  private List<URI> refersTo = new ArrayList<>();
  private URI responseState;
  private List<String> contentGraphNames;


  //private Resource msgBnode;
  // private Signature signature;



  public WonMessage(Dataset completeDataset) {
    this.completeDataset = completeDataset;
  }

  public Dataset getCompleteDataset(){
    return this.completeDataset;
  }

  /**
   * Creates a copy of the message dataset where all traces
   * of the envelope graph are deleted.
   * @return
   */
  public Dataset getMessageContent() {
    if (this.messageContent != null){
      return this.messageContent;
    } else {
      Dataset newMsgContent = DatasetFactory.createMem();
      Iterator<String> modelNames = this.completeDataset.listNames();
      List<String> envelopeGraphNames = getEnvelopeGraphURIs();
      //add all models that are not envelope graphs to the messageContent
      while(modelNames.hasNext()){
        String modelName = modelNames.next();
        if (envelopeGraphNames.contains(modelName)) {
          continue;
        }
        newMsgContent.addNamedModel(modelName, this.completeDataset.getNamedModel(modelName));
      }
      //copy the default model, but delete the triples referencing the envelope graphs
      Model newDefaultModel = ModelFactory.createDefaultModel();
      newDefaultModel.add(this.completeDataset.getDefaultModel());
      StmtIterator it = newDefaultModel.listStatements(null, RDF.type, WONMSG.ENVELOPE_GRAPH);
      while (it.hasNext()){
        Statement stmt = it.nextStatement();
        String subjString = stmt.getSubject().toString();
        if (envelopeGraphNames.contains(subjString)){
          it.remove();
        }
      }
      newMsgContent.setDefaultModel(newDefaultModel);
      this.messageContent = newMsgContent;
    }
    return this.messageContent;
  }


  public Model getOuterEnvelopeGraph(){
    if (this.outerEnvelopeGraph != null) {
      return this.outerEnvelopeGraph;
    }
    this.outerEnvelopeGraph = completeDataset.getNamedModel(getOuterEnvelopeGraphURI().toString());
    return this.outerEnvelopeGraph;
  }

  public URI getOuterEnvelopeGraphURI(){
    if (this.outerEnvelopeGraphURI != null) {
      return this.outerEnvelopeGraphURI;
    }
    Model model = this.completeDataset.getDefaultModel();
    if (model == null) throw new IllegalStateException("default model must not be null");
    Resource envelopeGraphResource = getEnvelopeGraphReference(model, true);
    this.outerEnvelopeGraphURI =  URI.create(envelopeGraphResource.getURI().toString());
    return this.outerEnvelopeGraphURI;
  }

  public List<Model> getEnvelopeGraphs(){
    if (envelopeGraphs != null) return envelopeGraphs;
    this.envelopeGraphNames = new ArrayList<String>();
    this.contentGraphNames = new ArrayList<String>();
    Model model = this.completeDataset.getDefaultModel();
    if (model == null) throw new IllegalStateException("default model must not be null");
    boolean mustContainEnvelopeGraphRef = true;
    List<Model> ret = new ArrayList<Model>();
    do {
      //find the reference to the envelope graph in the current model (the default model in the first iteration)
      Resource envelopeGraphResource = getEnvelopeGraphReference(model, mustContainEnvelopeGraphRef);
      model = null;
      if (envelopeGraphResource != null) {
        //look at the envelope graph (model):
        this.envelopeGraphNames.add(envelopeGraphResource.toString());
        mustContainEnvelopeGraphRef = false; //only needed for default model
        model = this.completeDataset.getNamedModel(envelopeGraphResource.toString());
        if (model == null) throw new IllegalStateException("envelope graph referenced in model" +
          envelopeGraphResource + " but not found as named graph in dataset!");
        //find all content graphs referenced in it
        if (this.messageURI == null) {
          //this can only happen when this method is called before getMessageURI.
          //we will search for the msg:hasMessageType triple.
          this.messageURI = RdfUtils.findFirstObjectUri(model, WONMSG.HAS_MESSAGE_TYPE_PROPERTY, null, false, true);
        }
        if (this.messageURI == null) {
          //this can only happen when this method is called before getMessageURI.
          //we will search for the msg:hasContent triple (because we need it for the code below
          this.messageURI = RdfUtils.findFirstObjectUri(model, WONMSG.HAS_CONTENT_PROPERTY, null, true, true);
        }
        if (this.messageURI != null) {
          for (NodeIterator it = getContentGraphReferences(model, model.getResource(getMessageURI().toString())); it
            .hasNext(); ) {
            RDFNode node = it.next();
            this.contentGraphNames.add(node.asResource().toString());
          }
        }
        //add it to the list of envelope graph models
        ret.add(model);
      }
    } while (model != null);
    this.envelopeGraphs = ret;
    return Collections.unmodifiableList(ret);
  }

  public List<String> getEnvelopeGraphURIs(){
    if (this.envelopeGraphs == null) {
      getEnvelopeGraphs(); //also sets envelopeGraphNames
    }
    return Collections.unmodifiableList(this.envelopeGraphNames);
  }

  public List<String> getContentGraphURIs(){
    //since there may not be any content graphs, we can't check
    //if this.contentGraphNames == null. We instead have to check
    //if we ran the detection of the envelope graphs at least once.
    if (this.envelopeGraphs == null) {
      getEnvelopeGraphs(); //also sets envelopeGraphNames
    }
    return Collections.unmodifiableList(this.contentGraphNames);
  }

  private Resource getEnvelopeGraphReference(Model model, boolean mustContainEnvelopeGraphRef){
    StmtIterator it = model.listStatements(null, RDF.type, WONMSG.ENVELOPE_GRAPH);
    if (!it.hasNext()){
      if (mustContainEnvelopeGraphRef) {
        throw new IllegalStateException("no envelope graph found");
      } else {
        return null;
      }
    }
    Resource envelopeGraphResource = it.nextStatement().getSubject();
    if (it.hasNext()) throw new IllegalStateException("more than one envelope graphs " +
      "referenced in model!");
    return envelopeGraphResource;
  }

  private NodeIterator getContentGraphReferences(Model model, Resource envelopeGraphResource){
    return model.listObjectsOfProperty(envelopeGraphResource, WONMSG.HAS_CONTENT_PROPERTY);
  }



  public URI getMessageURI() {
    if (this.messageURI == null) {
      this.messageURI = getEnvelopeSubjectURIValue(WONMSG.HAS_MESSAGE_TYPE_PROPERTY, null);
    }
    return this.messageURI;
  }

  public WonMessageType getMessageType() {
    if (this.messageType == null){
      URI type = getEnvelopePropertyURIValue(WONMSG.HAS_MESSAGE_TYPE_PROPERTY);
      this.messageType = WonMessageType.getWonMessageType(type);
    }
    return this.messageType;
  }

  public WonEnvelopeType getEnvelopeType() {
    if (this.envelopeType == null){
      URI type = getEnvelopePropertyURIValue(RDF.type);
      this.envelopeType = WonEnvelopeType.getWonEnvelopeType(type);
    }
    return this.envelopeType;
  }

  public URI getSenderURI() {
    if (this.senderURI == null) {
      this.senderURI = getEnvelopePropertyURIValue(WONMSG.SENDER_PROPERTY);
    }
    return this.senderURI;
   }


  public URI getSenderNeedURI() {
    if (this.senderNeedURI == null) {
      this.senderNeedURI = getEnvelopePropertyURIValue(WONMSG.SENDER_NEED_PROPERTY);
    }
    return this.senderNeedURI;
  }

  public URI getSenderNodeURI() {
    if (this.senderNodeURI == null) {
      this.senderNodeURI = getEnvelopePropertyURIValue(WONMSG.SENDER_NODE_PROPERTY);
    }
    return this.senderNodeURI;
  }


  public URI getReceiverURI() {
    if (this.receiverURI == null) {
      this.receiverURI = getEnvelopePropertyURIValue(WONMSG.RECEIVER_PROPERTY);
    }
    return this.receiverURI;
  }


  public URI getReceiverNeedURI() {
    if (this.receiverNeedURI == null) {
      this.receiverNeedURI = getEnvelopePropertyURIValue(WONMSG.RECEIVER_NEED_PROPERTY);
    }
    return this.receiverNeedURI;
  }


  public URI getReceiverNodeURI() {
    if (this.receiverNodeURI == null) {
      this.receiverNodeURI = getEnvelopePropertyURIValue(WONMSG.RECEIVER_NODE_PROPERTY);
    }
    return this.receiverNodeURI;
  }


  public List<URI> getRefersTo() {
    if (this.refersTo == null) {
      this.refersTo = getEnvelopePropertyURIValues(WONMSG.REFERS_TO_PROPERTY);
    }
    return this.refersTo;

  }


  public URI getResponseState() {
    if (this.responseState == null) {
      this.responseState = getEnvelopePropertyURIValue(WONMSG.HAS_RESPONSE_STATE_PROPERTY);
    }
    return this.responseState;
  }

  private URI getEnvelopePropertyURIValue(Property property){
    for (Model envelopeGraph: getEnvelopeGraphs()){
      StmtIterator it = envelopeGraph.listStatements(envelopeGraph.getResource(getMessageURI().toString()), property,
        (RDFNode) null);
      if (it.hasNext()){
        return URI.create(it.nextStatement().getObject().asResource().toString());
      }
    }
    return null;
  }

  private URI getEnvelopeSubjectURIValue(Property property, RDFNode object){
    for (Model envelopeGraph: getEnvelopeGraphs()){
      URI val = RdfUtils.findFirstObjectUri(envelopeGraph, property, object, true, true);
      if (val != null) {
        return val;
      }
    }
    return null;
  }

  private List<URI> getEnvelopePropertyURIValues(Property property){
    List<URI> values = new ArrayList<URI>();
    for (Model envelopeGraph: getEnvelopeGraphs()){
      StmtIterator it = envelopeGraph.listStatements(envelopeGraph.getResource(getMessageURI().toString()), property,
        (RDFNode) null);
      while (it.hasNext()){
        values.add(URI.create(it.nextStatement().getObject().asResource().toString()));
      }
    }
    return values;
  }

  private String getNamedGraphNameForUri(final String resourceUri) {
    String ngName = resourceUri;
    // we commonly use resource url + #data for the name of named graph
    // with this resource content
    if (completeDataset.getNamedModel(resourceUri) == null) {
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

    Iterator<String> graphNames = getMessageContent().listNames();
    while (graphNames.hasNext()) {
        result.add(graphNames.next().replaceAll("#.*", ""));
    }
    return result;
  }
}
