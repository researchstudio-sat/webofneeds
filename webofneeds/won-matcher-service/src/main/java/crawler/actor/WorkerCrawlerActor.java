package crawler.actor;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.sparql.path.Path;
import crawler.db.SparqlEndpointService;
import crawler.exception.CrawlingWrapperException;
import crawler.message.UriStatusMessage;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import won.protocol.rest.RdfDatasetConverter;

import java.io.IOException;
import java.util.Collection;
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
  private SparqlEndpointService endpoint;
  private Collection<Path> nonBasePropertyPaths;
  private Collection<Path> basePropertyPaths;
  private static final int HTTP_CONNECTION_TIMEOUT = 2000;
  private static final int HTTP_READ_TIMEOUT = 2000;

  /**
   * This class uses property paths to extract URIs from linked data resources. These property paths are executed
   * relative to base URIs. Therefore there are two types of property paths. Base property path extract URIs that are
   * taken as new base URIs. Non-base property paths extract URIs that keep the current base URI.
   *
   * @param sparqlEndpoint SPARQL endpoint to save and query the linked data
   * @param baseProperties base properties extract new base URIs from linked data resources
   * @param nonBaseProperties non-base properties extract non-base URIs from linked data resources
   */
  public WorkerCrawlerActor(String sparqlEndpoint, Collection<Path> baseProperties,
                            Collection<Path> nonBaseProperties) {

    HttpMessageConverter datasetConverter = new RdfDatasetConverter();
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    factory.setReadTimeout(HTTP_READ_TIMEOUT);
    factory.setConnectTimeout(HTTP_CONNECTION_TIMEOUT);
    restTemplate = new RestTemplate(factory);
    restTemplate.getMessageConverters().add(datasetConverter);
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(datasetConverter.getSupportedMediaTypes());
    entity = new HttpEntity(headers);
    endpoint = new SparqlEndpointService(sparqlEndpoint);
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

    if (!(msg instanceof UriStatusMessage)) {
      unhandled(msg);
      return;
    }

    UriStatusMessage uriMsg = (UriStatusMessage) msg;
    if (!uriMsg.getStatus().equals(UriStatusMessage.STATUS.PROCESS)) {
      unhandled(msg);
      return;
    }

    // URI message to process received
    // start the crawling request
    Dataset ds = requestDataset(uriMsg);

    // Save dataset to triple store
    endpoint.updateDataset(ds);

    // send extracted non-base URIs back to sender and save meta data about crawling the URI
    log.debug("Extract non-base URIs from message {}", uriMsg);
    Set<String> extractedURIs = endpoint.extractURIs(uriMsg.getUri(), uriMsg.getBaseUri(), nonBasePropertyPaths);
    for (String extractedURI : extractedURIs) {
      UriStatusMessage newUriMsg = new UriStatusMessage(
        extractedURI, uriMsg.getBaseUri(), UriStatusMessage.STATUS.PROCESS);
      getSender().tell(newUriMsg, getSelf());
    }

    // send extracted base URIs back to sender and save meta data about crawling the URI
    log.debug("Extract base URIs from message {}", uriMsg);
    extractedURIs = endpoint.extractURIs(uriMsg.getUri(), uriMsg.getBaseUri(), basePropertyPaths);
    for (String extractedURI : extractedURIs) {
      UriStatusMessage newUriMsg = new UriStatusMessage(
        extractedURI, extractedURI, UriStatusMessage.STATUS.PROCESS);
      getSender().tell(newUriMsg, getSelf());
    }

    // signal sender that this URI is processed and save meta data about crawling the URI.
    // This needs to be done after all extracted URI messages have been sent to guarantee consistency
    // in case of failure
    UriStatusMessage uriDoneMsg = new UriStatusMessage(
      uriMsg.getUri(), uriMsg.getBaseUri(), UriStatusMessage.STATUS.DONE);
    log.debug("Crawling done for URI {}", uriDoneMsg.getUri());
    getSender().tell(uriDoneMsg, getSelf());
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
      log.debug("Request URI: {}", msg.getUri());
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

}
