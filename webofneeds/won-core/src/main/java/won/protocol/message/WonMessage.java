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
  private MessageEvent messageEvent;
  private List<Model> envelopeGraphs;
  private List<String> envelopeGraphNames;



  //private Resource msgBnode;
  // private Signature signature;



  public WonMessage(Dataset completeDataset) {
    //this.messageEventURI = messageEventURI;
    this.completeDataset = completeDataset;
    MessageEventMapper mapper = new MessageEventMapper();
    this.messageEvent = mapper.fromModel(
        completeDataset.getNamedModel(
            getEnvelopeGraphReference(completeDataset.getDefaultModel(), true).toString()));
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
      List<String> envelopeGraphNames = getEnvelopeGraphNames();
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

  public List<Model> getEnvelopeGraphs(){
    if (envelopeGraphs != null) return envelopeGraphs;
    this.envelopeGraphNames = new ArrayList<String>();
    Model model = this.completeDataset.getDefaultModel();
    if (model == null) throw new IllegalStateException("default model must not be null");
    boolean mustContainEnvelopeGraphRef = true;
    List<Model> ret = new ArrayList<Model>();
    do {
      Resource envelopeGraphResource = getEnvelopeGraphReference(model, mustContainEnvelopeGraphRef);
      model = null;
      if (envelopeGraphResource != null) {
        this.envelopeGraphNames.add(envelopeGraphResource.toString());
        mustContainEnvelopeGraphRef = false; //only needed for default model
        model = this.completeDataset.getNamedModel(envelopeGraphResource.toString());
        if (model == null) throw new IllegalStateException("envelope graph referenced in model" +
          envelopeGraphResource + " but not found as named graph in dataset!");
        ret.add(model);
      }
    } while (model != null);
    this.envelopeGraphs = ret;
    return Collections.unmodifiableList(ret);
  }

  public List<String> getEnvelopeGraphNames(){
    if (this.envelopeGraphNames != null) {
      return Collections.unmodifiableList(this.envelopeGraphNames);
    }
    getEnvelopeGraphs(); //also sets envelopeGraphNames
    return Collections.unmodifiableList(this.envelopeGraphNames);
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
    dataset.addNamedModel(ngName, completeDataset.getNamedModel(ngName));
    RdfUtils.addPrefixMapping(dataset.getDefaultModel(), completeDataset.getNamedModel(ngName));
    RdfUtils.addPrefixMapping(dataset.getDefaultModel(), completeDataset.getDefaultModel());
    //TODO signature into default graph
    return dataset;
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

  public MessageEvent getMessageEvent() {
    return messageEvent;
  }

  public void setMessageEvent(final MessageEvent messageEvent) {
    this.messageEvent = messageEvent;
  }
}
