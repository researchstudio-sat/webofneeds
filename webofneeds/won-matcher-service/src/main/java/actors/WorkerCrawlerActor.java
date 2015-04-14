package actors;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.modify.UpdateProcessRemote;
import com.hp.hpl.jena.sparql.path.Path;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;
import messages.UriActionMessage;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import won.protocol.rest.RdfDatasetConverter;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.*;

/**
 * Actor requests linked data URI using HTTP and saves it to a triple store using SPARQL UPDATE query.
 * Responds to the sender with extracted URIs from the linked data URI.
 *
 * User: hfriedrich
 * Date: 07.04.2015
 */
public class WorkerCrawlerActor extends UntypedActor
{
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
  private RestTemplate restTemplate;
  private HttpEntity entity;
  private String sparqlEndpoint;
  private Collection<Path> nonBasePropertyPaths;
  private Collection<Path> basePropertyPaths;

  /**
   * This class uses property paths to extract URIs from linked data resources. These property paths are executed
   * relative to base URIs. Therefore there are two types of property paths. Base property path extract URIs that are
   * taken as new base URIs. Non-base property paths extract URIs that keep the current base URI.
   *
   * @param sparqlEndpoint SPARQL endpoint to save and query the linked data
   * @param baseProperties base properties extract new base URIs from linked data resources
   * @param nonBaseProperties non-base properties extract non-base URIs from linked data resources
   */
  public WorkerCrawlerActor(String sparqlEndpoint, Collection<Path> baseProperties, Collection<Path> nonBaseProperties) {

    HttpMessageConverter datasetConverter = new RdfDatasetConverter();
    restTemplate = new RestTemplate();
    restTemplate.getMessageConverters().add(datasetConverter);
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(datasetConverter.getSupportedMediaTypes());
    entity = new HttpEntity(headers);
    this.sparqlEndpoint = sparqlEndpoint;
    basePropertyPaths = new LinkedList<Path>();
    basePropertyPaths.addAll(baseProperties);
    nonBasePropertyPaths = new LinkedList<Path>();
    nonBasePropertyPaths.addAll(nonBaseProperties);
  }

  /**
   * Receives messages with an URI and processes them by requesting the resource,
   * saving it to a triple store, extracting URIs from content and answering the sender.
   *
   * @param msg if type is {@link messages.UriActionMessage} then process it
   * @throws IOException
   */
  @Override
  public void onReceive(Object msg) throws IOException {

    if (msg instanceof UriActionMessage) {

      // request and save data
      UriActionMessage uriMsg = (UriActionMessage) msg;
      Dataset ds = requestDataset(uriMsg.getUri());
      save(ds);

      // send extracted non-base URIs back to sender
      Set<String> extractedURIs = extractURIs(uriMsg.getBaseUri(), nonBasePropertyPaths);
      for (String extractedURI : extractedURIs) {
        UriActionMessage newUriMsg = new UriActionMessage(
          extractedURI, uriMsg.getBaseUri(), UriActionMessage.ACTION.PROCESS);
        getSender().tell(newUriMsg, getSelf());
      }

      // send extracted base URIs back to sender
      extractedURIs = extractURIs(uriMsg.getBaseUri(), basePropertyPaths);
      for (String extractedURI : extractedURIs) {
        UriActionMessage newUriMsg = new UriActionMessage(
          extractedURI, extractedURI, UriActionMessage.ACTION.PROCESS);
        getSender().tell(newUriMsg, getSelf());
      }

      // signal sender that this URI is processed
      UriActionMessage uriDoneMsg = new UriActionMessage(
        uriMsg.getUri(), uriMsg.getBaseUri(), UriActionMessage.ACTION.REMOVE);
      getSender().tell(uriDoneMsg, getSelf());

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

      quadPattern.append("\nCLEAR GRAPH <").append(graphName).append(">;\n");
      quadPattern.append("\nINSERT DATA { GRAPH <").append(graphName)
                 .append("> { ").append(sw).append("}};\n");

      log.debug("Save to SPARQL Endpoint: {}", sparqlEndpoint);
      log.debug("Execute query: {}", quadPattern.toString());
      UpdateRequest update = UpdateFactory.create(quadPattern.toString());
      UpdateProcessRemote riStore = (UpdateProcessRemote)
        UpdateExecutionFactory.createRemote(update, sparqlEndpoint);
      riStore.execute();
    }
  }

  /**
   * Extract linked URIs of the current processed resource URI that are not already in the database. Use specified
   * property paths to construct the query for the sparql endpoint.
   *
   * @param resourceURI current processed resource URI
   * @param properties property paths used to query the sparql endpoint
   */
  private Set<String> extractURIs(String resourceURI, Iterable<Path> properties) {

    Set<String> extractedURIs = new HashSet<String>();
    URI uri = URI.create(resourceURI);
    for (Path prop : properties) {

      // select URIs specified by property paths that do not already exists as subjects
      String queryString = "select ?obj where { <" + uri + "> " + prop.toString() +
                           " ?obj FILTER NOT EXISTS { ?obj ?pred ?obj2}}";
      log.debug("Query sparql endpoint: {}", sparqlEndpoint);
      log.debug("Execute query: {}", queryString);
      Query query = QueryFactory.create(queryString);
      QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, query);
      ResultSet results = qexec.execSelect();

      while (results.hasNext()) {
        QuerySolution qs = results.nextSolution();
        RDFNode node = qs.get("obj");
        log.debug("Extracted URI: {}", node.toString());
        extractedURIs.add(node.toString());
      }
      qexec.close();
    }

    return extractedURIs;
  }

}
