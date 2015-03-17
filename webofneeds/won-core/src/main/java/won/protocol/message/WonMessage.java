package won.protocol.message;

import com.google.common.collect.Sets;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.RDFG;
import won.protocol.vocabulary.WONMSG;

import java.io.Serializable;
import java.net.URI;
import java.util.*;


/**
 * Wraps an RDF dataset representing a WoN message.
 *
 * Note: this implementation is not thread-safe.
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
  private WonMessageDirection envelopeType;
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
      getEnvelopeGraphs(); //also sets the outerEnvelopeUri
    }
    return this.outerEnvelopeGraphURI;
  }

  /**
   * Returns all envelope graphs found in the message.
   *
   * Not that this method has side effects: all intermediate results are cached for re-use. This
   * concerns the envelopeGraphNames, contentGraphNames and messageURI members.
   * @return
   */
  public List<Model> getEnvelopeGraphs() {
    //return cached instance if we have it
    if (this.envelopeGraphs != null) return this.envelopeGraphs;
    //initialize
    List<Model> allEnvelopes = new ArrayList<Model>();
    this.envelopeGraphNames = new ArrayList<String>();
    this.contentGraphNames = new ArrayList<String>();
    this.messageURI = null;
    this.outerEnvelopeGraph = null;
    Set<String> envelopesContainedInOthers = new HashSet<String>();
    Set<String> allEnvelopeGraphNames = new HashSet<String>();
    //iterate over named graphs
    Iterator<String> modelUriIterator = this.completeDataset.listNames();
    while (modelUriIterator.hasNext()) {
      String envelopeGraphUri = modelUriIterator.next();
      Model envelopeGraph = this.completeDataset.getNamedModel(envelopeGraphUri);
      //check if the current named graph is an envelope graph (G rdf:type wonmsg:EnvelopeGraph)
      if (isEnvelopeGraph(envelopeGraphUri, envelopeGraph)) {
        this.envelopeGraphNames.add(envelopeGraphUri);
        allEnvelopeGraphNames.add(envelopeGraphUri);
        allEnvelopes.add(envelopeGraph);
        //find out the message's URI (if we haven't yet)
        if (this.messageURI == null) {
          findMessageUri(envelopeGraph, envelopeGraphUri);
        }
        //check if the envelope contains references to 'contained' envelopes and remember their names
        List<String> containedEnvelopes = findContainedEnvelopeUris(envelopeGraph, envelopeGraphUri);
        envelopesContainedInOthers.addAll(containedEnvelopes);
        if (this.messageURI != null) {
          for (NodeIterator it = getContentGraphReferences(envelopeGraph, envelopeGraph.getResource(getMessageURI().toString())); it
            .hasNext(); ) {
            RDFNode node = it.next();
            this.contentGraphNames.add(node.asResource().toString());
          }
        }
      }
    }
    Set<String> candidatesForOuterEnvelope = Sets.symmetricDifference(allEnvelopeGraphNames,
      envelopesContainedInOthers);
    //we've now visited all named graphs. We should now have exactly one candidate for the outer envelope
    if (candidatesForOuterEnvelope.size() != 1){
      throw new IllegalStateException(String.format("Message dataset must contain exactly one envelope graph that is " +
        "not included " +
        "in another one, but found %d", candidatesForOuterEnvelope.size()));
    }
    String outerEnvelopeUri = candidatesForOuterEnvelope.iterator().next();
    this.outerEnvelopeGraphURI = URI.create(outerEnvelopeUri);
    this.outerEnvelopeGraph = this.completeDataset.getNamedModel(outerEnvelopeUri);
    this.envelopeGraphs = allEnvelopes;
    return Collections.unmodifiableList(allEnvelopes);
  }


  private void findMessageUri(final Model model, final String modelUri) {
    RDFNode messageUriNode = RdfUtils.findOnePropertyFromResource(model, model.getResource(modelUri), RDFG.SUBGRAPH_OF);
    this.messageURI = URI.create(messageUriNode.asResource().getURI());
  }

  private List<String> findContainedEnvelopeUris(final Model envelopeGraph, final String envelopeGraphUri) {
    Resource envelopeGraphResource = envelopeGraph.getResource(envelopeGraphUri);
    StmtIterator it = envelopeGraphResource.listProperties(WONMSG.CONTAINS_ENVELOPE);
    if (it.hasNext()) {
      List ret = new ArrayList<String>();
      while (it.hasNext()) {
        ret.add(it.nextStatement().getObject().asResource().getURI());
      }
      return ret;
    }
    return Collections.emptyList();
  }

  public boolean isEnvelopeGraph(final String modelUri, final Model model) {
    return model.contains(model.getResource(modelUri), RDF.type, WONMSG.ENVELOPE_GRAPH);
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

  public WonMessageDirection getEnvelopeType() {
    if (this.envelopeType == null){
      URI type = getEnvelopePropertyURIValue(RDF.type);
      this.envelopeType = WonMessageDirection.getWonMessageDirection(type);
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
