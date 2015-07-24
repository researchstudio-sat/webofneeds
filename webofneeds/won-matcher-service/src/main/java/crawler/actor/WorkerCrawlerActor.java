package crawler.actor;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.hp.hpl.jena.query.Dataset;
import common.config.CommonSettings;
import common.config.CommonSettingsImpl;
import common.service.HttpRequestService;
import crawler.config.CrawlSettings;
import crawler.config.CrawlSettingsImpl;
import crawler.exception.CrawlWrapperException;
import crawler.msg.CrawlUriMessage;
import crawler.service.CrawlSparqlService;
import org.springframework.web.client.RestClientException;
import won.protocol.exception.IncorrectPropertyCountException;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.WON;

import java.net.URI;
import java.util.Set;

/**
 * Actor requests linked data URI using HTTP and saves it to a triple store using SPARQL UPDATE query.
 * Responds to the sender with extracted URIs from the linked data URI.
 *
 * This class uses property paths to extract URIs from linked data resources. These property paths are executed
 * relative to base URIs. Therefore there are two types of property paths. Base property path extract URIs that are
 * taken as new base URIs. Non-base property paths extract URIs that keep the current base URI.
 *
 * User: hfriedrich
 * Date: 07.04.2015
 */
public class WorkerCrawlerActor extends UntypedActor
{
  private final CrawlSettingsImpl crawlSettings = CrawlSettings.SettingsProvider.get(getContext().system());
  private final CommonSettingsImpl commonSettings = CommonSettings.SettingsProvider.get(getContext().system());
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
  private HttpRequestService httpRequestService;
  private CrawlSparqlService sparqlService;

  public WorkerCrawlerActor() {
    httpRequestService = new HttpRequestService(crawlSettings.HTTP_READ_TIMEOUT, crawlSettings.HTTP_CONNECTION_TIMEOUT);
    sparqlService = new CrawlSparqlService(commonSettings.SPARQL_ENDPOINT);
  }

  /**
   * Receives messages with an URI and processes them by requesting the resource,
   * saving it to a triple store, extracting URIs from content and answering the sender.
   *
   * @param msg if type is {@link CrawlUriMessage} then process it
   */
  @Override
  public void onReceive(Object msg) throws RestClientException {

    if (!(msg instanceof CrawlUriMessage)) {
      unhandled(msg);
      return;
    }

    CrawlUriMessage uriMsg = (CrawlUriMessage) msg;
    if (!uriMsg.getStatus().equals(CrawlUriMessage.STATUS.PROCESS)) {
      unhandled(msg);
      return;
    }

    // URI message to process received
    // start the crawling request
    Dataset ds = null;
    try {
      ds = httpRequestService.requestDataset(uriMsg.getUri());
    } catch (RestClientException e) {
      throw new CrawlWrapperException(e, uriMsg);
    }

    // Save dataset to triple store
    sparqlService.updateNamedGraphsOfDataset(ds);
    String wonNodeUri = extractWonNodeUri(ds, uriMsg.getUri());
    if (wonNodeUri == null) {
      wonNodeUri = uriMsg.getWonNodeUri();
    }

    // send extracted non-base URIs back to sender and save meta data about crawling the URI
    log.debug("Extract non-base URIs from message {}", uriMsg);
    Set<String> extractedURIs = sparqlService.extractURIs(
      uriMsg.getUri(), uriMsg.getBaseUri(), crawlSettings.PROPERTYPATHS_NONBASE);
    for (String extractedURI : extractedURIs) {
      CrawlUriMessage newUriMsg = new CrawlUriMessage(
        extractedURI, uriMsg.getBaseUri(), wonNodeUri, CrawlUriMessage.STATUS.PROCESS);
      getSender().tell(newUriMsg, getSelf());
    }

    // send extracted base URIs back to sender and save meta data about crawling the URI
    log.debug("Extract base URIs from message {}", uriMsg);
    extractedURIs = sparqlService.extractURIs(uriMsg.getUri(), uriMsg.getBaseUri(), crawlSettings.PROPERTYPATHS_BASE);
    for (String extractedURI : extractedURIs) {
      CrawlUriMessage newUriMsg = new CrawlUriMessage(
        extractedURI, extractedURI, wonNodeUri, CrawlUriMessage.STATUS.PROCESS);
      getSender().tell(newUriMsg, getSelf());
    }

    // signal sender that this URI is processed and save meta data about crawling the URI.
    // This needs to be done after all extracted URI messages have been sent to guarantee consistency
    // in case of failure
    CrawlUriMessage uriDoneMsg = new CrawlUriMessage(
      uriMsg.getUri(), uriMsg.getBaseUri(), wonNodeUri, CrawlUriMessage.STATUS.DONE);
    log.debug("Crawling done for URI {}", uriDoneMsg.getUri());
    getSender().tell(uriDoneMsg, getSelf());
  }

  /**
   * Extract won node uri from a won resource
   *
   * @param ds resource as dataset
   * @param uri uri that represents resource
   * @return won node uri or null if link to won node is not linked in the resource
   */
  private String extractWonNodeUri(Dataset ds, String uri) {
    try {
      return RdfUtils.findOnePropertyFromResource(ds, URI.create(uri), WON.HAS_WON_NODE).asResource().getURI();
    } catch (IncorrectPropertyCountException e) {
      return null;
    }
  }

}
