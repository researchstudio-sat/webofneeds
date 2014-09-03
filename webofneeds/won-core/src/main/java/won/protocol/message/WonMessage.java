package won.protocol.message;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
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
