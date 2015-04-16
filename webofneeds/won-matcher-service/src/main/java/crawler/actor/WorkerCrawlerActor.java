package crawler.actor;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.path.Path;
import crawler.db.SparqlEndpointAccess;
import crawler.exception.CrawlingWrapperException;
import crawler.message.UriStatusMessage;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import won.protocol.rest.RdfDatasetConverter;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

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
  private SparqlEndpointAccess endpoint;
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
    endpoint = new SparqlEndpointAccess(sparqlEndpoint);
    basePropertyPaths = new LinkedList<Path>();
    basePropertyPaths.addAll(baseProperties);
    nonBasePropertyPaths = new LinkedList<Path>();
    nonBasePropertyPaths.addAll(nonBaseProperties);
  }

  /**
   * Receives messages with an URI and processes them by requesting the resource,
   * saving it to a triple store, extracting URIs from content and answering the sender.
   *
   * @param msg if type is {@link crawler.message.UriStatusMessage} then process it
   * @throws IOException
   */
  @Override
  public void onReceive(Object msg) throws IOException {

    if (msg instanceof UriStatusMessage) {

      // request and save data
      UriStatusMessage uriMsg = (UriStatusMessage) msg;
      Dataset ds = requestDataset(uriMsg);
      save(ds);

      // send extracted non-base URIs back to sender
      Set<String> extractedURIs = endpoint.extractURIs(uriMsg.getUri(), uriMsg.getBaseUri(), nonBasePropertyPaths);
      for (String extractedURI : extractedURIs) {
        UriStatusMessage newUriMsg = new UriStatusMessage(
          extractedURI, uriMsg.getBaseUri(), UriStatusMessage.STATUS.PROCESS);
        getSender().tell(newUriMsg, getSelf());
      }

      // send extracted base URIs back to sender
      extractedURIs = endpoint.extractURIs(uriMsg.getUri(), uriMsg.getBaseUri(), basePropertyPaths);
      for (String extractedURI : extractedURIs) {
        UriStatusMessage newUriMsg = new UriStatusMessage(
          extractedURI, extractedURI, UriStatusMessage.STATUS.PROCESS);
        getSender().tell(newUriMsg, getSelf());
      }

      // signal sender that this URI is processed
      UriStatusMessage uriDoneMsg = new UriStatusMessage(
        uriMsg.getUri(), uriMsg.getBaseUri(), UriStatusMessage.STATUS.DONE);
      getSender().tell(uriDoneMsg, getSelf());

    } else {
      unhandled(msg);
    }
  }

  /**
   * Request the URI using HTTP
   *
   * @param msg message which holds the requested URI
   * @return dataset that represents the linked data URI
   */
  private Dataset requestDataset(UriStatusMessage msg)  {

    ResponseEntity<Dataset> response = null;
    try {
      log.debug("Request from URL: {}", msg.getUri());
      response = restTemplate.exchange(msg.getUri(), HttpMethod.GET, entity, Dataset.class);

      if (response.getStatusCode() != HttpStatus.OK) {
        log.warning("HTTP GET request returned status code: {}", response.getStatusCode());
        throw new HttpClientErrorException(response.getStatusCode());
      }
      return response.getBody();
    } catch (RestClientException e) {
      throw new CrawlingWrapperException(e, msg);
    }
  }

  /**
   * Save dataset to triple store using SPARQL
   * @param ds dataset that is saved
   */
  private void save(Dataset ds) {

    Iterator<String> graphNames = ds.listNames();
    while (graphNames.hasNext()) {

      String graphName = graphNames.next();
      Model model = ds.getNamedModel(graphName);
      endpoint.updateGraph(graphName, model);
    }
  }

}
