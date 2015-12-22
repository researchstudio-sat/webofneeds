package won.protocol.message;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;
import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.util.RdfUtils;
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
  private URI isResponseToMessageURI;
  private URI isRemoteResponseToMessageURI;
    private List<String> contentGraphNames;
  private WonMessageType isResponseToMessageType;
  private URI correspondingRemoteMessageURI;
  private List<AttachmentHolder> attachmentHolders;

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
      //TODO: this deletion is no longer necessary, right?
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

  /**
   * Returns all content graphs that are attachments, including their signature graphs.
   * @return
   */
  public List<AttachmentHolder> getAttachments(){
    if (this.attachmentHolders != null) {
      return this.attachmentHolders;
    }
    final List<String> envelopeGraphUris = getEnvelopeGraphURIs();
    List<AttachmentHolder> newAttachmentHolders = new ArrayList<>();
    String queryString = "prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#>\n" +
      "prefix xsd:   <http://www.w3.org/2001/XMLSchema#>\n" +
      "prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
      "prefix won:   <http://purl.org/webofneeds/model#>\n" +
      "prefix msg:   <http://purl.org/webofneeds/message#>\n" +
      "prefix sig:   <http://icp.it-risk.iwvi.uni-koblenz.de/ontologies/signature.owl#>\n" +
      "\n"+
      "select ?attachmentSigGraphUri ?attachmentGraphUri ?envelopeGraphUri ?attachmentDestinationUri where { \n" +
      "graph ?attachmentSigGraphUri {?attachmentSigGraphUri " +
      "              a sig:Signature; \n" +
      "              msg:hasSignedGraph ?attachmentGraphUri.\n" +
      "}\n" +
      "graph ?envelopeGraphUri {?envelopeGraphUri rdf:type msg:EnvelopeGraph.  \n" +
      "    ?messageUri msg:hasAttachment ?attachmentData. \n" +
      "?attachmentData msg:hasDestinationUri ?attachmentDestinationUri; \n" +
      "                msg:hasAttachmentGraphUri ?attachmentGraphUri.\n" +
      "}\n" +
      "}";
    Query query = QueryFactory.create(queryString);
    QuerySolutionMap initialBinding = new QuerySolutionMap();
    initialBinding.add("messageUri", new ResourceImpl(getMessageURI().toString()));
    try (QueryExecution queryExecution = QueryExecutionFactory.create(query, completeDataset))  {
      queryExecution.getContext().set(TDB.symUnionDefaultGraph, true);

      ResultSet result = queryExecution.execSelect();
      while (result.hasNext()){
        QuerySolution solution = result.nextSolution();
        String envelopeGraphUri = solution.getResource("envelopeGraphUri").getURI();
        if (!envelopeGraphUris.contains(envelopeGraphUri)) {
          logger.warn("found resource {} of type msg:EnvelopeGraph that is not the URI of an envelope graph in message {}", envelopeGraphUri,  this.messageURI);
          continue;
        }
        String sigGraphUri = solution.getResource("attachmentSigGraphUri").getURI().toString();
        String attachmentGraphUri = solution.getResource("attachmentGraphUri").getURI();
        String attachmentSigGraphUri = solution.getResource("attachmentSigGraphUri").getURI();
        String attachmentDestinationUri = solution.getResource("attachmentDestinationUri").getURI();
        Dataset attachmentDataset = DatasetFactory.createMem();
        attachmentDataset.addNamedModel(attachmentGraphUri, this.completeDataset.getNamedModel(attachmentGraphUri));
        attachmentDataset.addNamedModel(attachmentSigGraphUri, this.completeDataset.getNamedModel(attachmentSigGraphUri));
        AttachmentHolder attachmentHolder = new AttachmentHolder(URI.create(attachmentDestinationUri),attachmentDataset);
        newAttachmentHolders.add(attachmentHolder);
      }
    } catch (Exception e){
      throw e;
    }
    this.attachmentHolders = newAttachmentHolders;
    return newAttachmentHolders;
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
    getEnvelopeGraphs(); //also sets the outerEnvelopeUri
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
    URI currentMessageURI = null;
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
        currentMessageURI = findMessageUri(envelopeGraph, envelopeGraphUri);
        //check if the envelope contains references to 'contained' envelopes and remember their names
        List<String> containedEnvelopes = findContainedEnvelopeUris(envelopeGraph, envelopeGraphUri);
        envelopesContainedInOthers.addAll(containedEnvelopes);
        if (currentMessageURI != null) {
            for (NodeIterator it = getContentGraphReferences(envelopeGraph,
                                                             envelopeGraph.getResource(currentMessageURI.toString())); it
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




  private URI findMessageUri(final Model model, final String modelUri) {
    RDFNode messageUriNode = RdfUtils.findOnePropertyFromResource(model, model.getResource(modelUri), RDFG.SUBGRAPH_OF);
    return URI.create(messageUriNode.asResource().getURI());
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
      this.messageURI = findMessageUri(getOuterEnvelopeGraph(), getOuterEnvelopeGraphURI().toString());
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
      if (type != null){
        this.envelopeType = WonMessageDirection.getWonMessageDirection(type);
      }
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

  public URI getIsResponseToMessageURI() {
    if (this.isResponseToMessageURI == null) {
      this.isResponseToMessageURI = getEnvelopePropertyURIValue(WONMSG.IS_RESPONSE_TO);
    }
    return this.isResponseToMessageURI;
  }

  public URI getIsRemoteResponseToMessageURI() {
    if (this.isRemoteResponseToMessageURI == null) {
      this.isRemoteResponseToMessageURI = getEnvelopePropertyURIValue(WONMSG.IS_REMOTE_RESPONSE_TO);
    }
    return this.isRemoteResponseToMessageURI;
  }

  public URI getCorrespondingRemoteMessageURI() {
    if (this.correspondingRemoteMessageURI == null) {
      this.correspondingRemoteMessageURI = getEnvelopePropertyURIValue(WONMSG.HAS_CORRESPONDING_REMOTE_MESSAGE);
    }
    return this.correspondingRemoteMessageURI;
  }

  public WonMessageType getIsResponseToMessageType() {
    if (this.isResponseToMessageType == null){
       URI typeURI = getEnvelopePropertyURIValue(WONMSG.IS_RESPONSE_TO_MESSAGE_TYPE);
      if (typeURI != null) {
        this.isResponseToMessageType = WonMessageType.getWonMessageType(typeURI);
      }
    }
    return isResponseToMessageType;
  }

  public URI getEnvelopePropertyURIValue(URI propertyURI){
    Property property = getCompleteDataset().getDefaultModel().createProperty(propertyURI.toString());
    return getEnvelopePropertyURIValue(property);
  }

  public URI getEnvelopePropertyURIValue(Property property){
    Model currentEnvelope = getOuterEnvelopeGraph();
    URI currentEnvelopeUri = getOuterEnvelopeGraphURI();
    //TODO would make sense to order envelope graphs in order from container to containee in the first place,
    //if proper done, we should avoid ending up in infinite loop if someone sends us malformed envelopes that
    // contain-in-other circular...
    while (currentEnvelope != null) {
      URI currentMessageURI = findMessageUri(currentEnvelope, currentEnvelopeUri.toString());
      StmtIterator it = currentEnvelope.listStatements(currentEnvelope.getResource(currentMessageURI.toString()),
                                                       property,
                                                       (RDFNode) null);
      if (it.hasNext()){
        return URI.create(it.nextStatement().getObject().asResource().toString());
      }
      // move to the next envelope
      currentEnvelopeUri = RdfUtils.findFirstObjectUri(currentEnvelope, WONMSG.CONTAINS_ENVELOPE, null, true, true);
      currentEnvelope = null;
      if (currentEnvelopeUri != null) {
        currentEnvelope = this.completeDataset.getNamedModel(currentEnvelopeUri.toString());
      }
    }
    return null;
  }

  private URI getEnvelopeSubjectURIValue(Property property, RDFNode object){
    for (Model envelopeGraph: getEnvelopeGraphs()){
      URI val = RdfUtils.findFirstSubjectUri(envelopeGraph, property, object, true, true);
      if (val != null) {
        return val;
      }
    }
    return null;
  }

  private List<URI> getEnvelopePropertyURIValues(Property property){
    List<URI> values = new ArrayList<URI>();
    Model currentEnvelope = getOuterEnvelopeGraph();
    URI currentEnvelopeUri = getOuterEnvelopeGraphURI();
    //TODO would make sense to order envelope graphs in order from container to containee in the first place
    while (currentEnvelope != null) {
      URI currentMessageURI = findMessageUri(currentEnvelope, currentEnvelopeUri.toString());
      StmtIterator it = currentEnvelope.listStatements(currentEnvelope.getResource(currentMessageURI.toString()),
                                                       property,
                                                       (RDFNode) null);
      while (it.hasNext()){
        values.add(URI.create(it.nextStatement().getObject().asResource().toString()));
      }
      currentEnvelopeUri = RdfUtils.findFirstObjectUri(currentEnvelope, WONMSG.CONTAINS_ENVELOPE, null, true, true);
      currentEnvelope = null;
      if (currentEnvelopeUri != null) {
        currentEnvelope = this.completeDataset.getNamedModel(currentEnvelopeUri.toString());
      }
    }
    return values;
  }

  //Used to remember attachment graph uri and destination uri during the process of extracting attachments.
  public class AttachmentMetaData {
    URI attachmentGraphUri;
    URI destinationUri;

    AttachmentMetaData(URI attachmentGraphUri, URI destinationUri) {
      this.attachmentGraphUri = attachmentGraphUri;
      this.destinationUri = destinationUri;
    }

    public URI getAttachmentGraphUri() {
      return attachmentGraphUri;
    }

    public URI getDestinationUri() {
      return destinationUri;
    }
  }

  public static class AttachmentHolder{
    private URI destinationUri;
    //holds the attachment graph and the signature graph
    private Dataset attachmentDataset;

    public AttachmentHolder(URI destinationUri, Dataset attachmentDataset) {
      this.destinationUri = destinationUri;
      this.attachmentDataset = attachmentDataset;
    }

    public URI getDestinationUri() {
      return destinationUri;
    }

    public Dataset getAttachmentDataset() {
      return attachmentDataset;
    }
  }
}
