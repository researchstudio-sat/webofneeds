package actors;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.modify.UpdateProcessRemote;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;
import messages.URIActionMessage;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import won.protocol.rest.RdfDatasetConverter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;

/**
 * Actor requests linked data URI using HTTP and saves it to a triple store using SPARQL UPDATE query.
 * User: hfriedrich
 * Date: 07.04.2015
 */
public class ProcessLinkedDataUriActor extends UntypedActor
{
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
  private RestTemplate restTemplate;
  private HttpEntity entity;
  private String sparqlEndpoint;

  /**
   * @param sparqlEndpoint SPARQL endpoint to save the linked data to
   */
  public ProcessLinkedDataUriActor(String sparqlEndpoint) {
    HttpMessageConverter datasetConverter = new RdfDatasetConverter();
    restTemplate = new RestTemplate();
    restTemplate.getMessageConverters().add(datasetConverter);
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(datasetConverter.getSupportedMediaTypes());
    entity = new HttpEntity(headers);
    this.sparqlEndpoint = sparqlEndpoint;
  }

  /**
   * Receives {@link messages.URIActionMessage} and processes them. After successful processing the actor responds
   * with an {@link messages.URIActionMessage} with the same URI but another action to signal end of processing.
   * @param msg if type is {@link messages.URIActionMessage} then process it
   * @throws IOException
   */
  @Override
  public void onReceive(Object msg) throws IOException {
    if (msg instanceof URIActionMessage) {
      URIActionMessage uriMsg = (URIActionMessage) msg;
      Dataset ds = requestDataset(uriMsg.getUri());
      save(ds);
      getSender().tell(new URIActionMessage(uriMsg.getUri(), URIActionMessage.ACTION.REMOVE), getSelf());
    } else {
      unhandled(msg);
    }
  }

  /**
   * Request the URI using HTTP
   * @param uri requested URI
   * @return dataset that represents the linked data URI
   */
  private Dataset requestDataset(String uri)  {
    log.debug("Request from URL: {}", uri);
    ResponseEntity<Dataset> response = restTemplate.exchange(uri, HttpMethod.GET, entity, Dataset.class);
    if(response.getStatusCode()!= HttpStatus.OK){
      log.error("HTTP GET request returned status code: {}", response.getStatusCode());
      throw new HttpClientErrorException(response.getStatusCode());
    }
    return response.getBody();
  }

  /**
   * Save dataset to triple store using SPARQL
   * @param ds dataset that is saved
   */
  private void save(Dataset ds) {

    Iterator<String> graphNames = ds.listNames();
    while (graphNames.hasNext()) {

      StringBuilder quadPattern = new StringBuilder();
      String graphName = graphNames.next();
      Model model = ds.getNamedModel(graphName);
      StringWriter sw = new StringWriter();
      RDFDataMgr.write(sw, model, Lang.NTRIPLES);

      quadPattern.append("\nCLEAR GRAPH <")
                 .append(graphName)
                 .append(">;\n");
      quadPattern.append("\nINSERT DATA { GRAPH <")
                 .append(graphName)
                 .append("> { ")
                 .append(sw)
                 .append("}};\n");

      log.debug("Save to SPARQL Endpoint: {}", sparqlEndpoint);
      log.debug("Query: {}", quadPattern.toString());
      UpdateRequest update = UpdateFactory.create(quadPattern.toString());
      UpdateProcessRemote riStore = (UpdateProcessRemote)
        UpdateExecutionFactory.createRemote(update, sparqlEndpoint);
      riStore.execute();
    }
  }
}
