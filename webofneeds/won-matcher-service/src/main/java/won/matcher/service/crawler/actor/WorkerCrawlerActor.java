package won.matcher.service.crawler.actor;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import org.apache.jena.query.Dataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import won.matcher.service.common.event.NeedEvent;
import won.matcher.service.common.service.sparql.SparqlService;
import won.matcher.service.crawler.config.CrawlConfig;
import won.matcher.service.crawler.exception.CrawlWrapperException;
import won.matcher.service.crawler.msg.CrawlUriMessage;
import won.matcher.service.crawler.msg.ResourceCrawlUriMessage;
import won.matcher.service.crawler.service.CrawlSparqlService;
import won.protocol.exception.DataIntegrityException;
import won.protocol.exception.IncorrectPropertyCountException;
import won.protocol.model.NeedState;
import won.protocol.util.NeedModelWrapper;
import won.protocol.util.RdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
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
@Component
@Scope("prototype")
public class WorkerCrawlerActor extends UntypedActor
{
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

  @Autowired
  private LinkedDataSource linkedDataSource;

  @Autowired
  private CrawlSparqlService sparqlService;

  @Autowired
  private CrawlConfig config;

  private ActorRef pubSubMediator;

  @Override
  public void preStart() {

    // initialize the distributed event bus to send need events to the matchers
    pubSubMediator = DistributedPubSub.get(getContext().system()).mediator();
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
    if (!uriMsg.getStatus().equals(CrawlUriMessage.STATUS.PROCESS) &&
      !uriMsg.getStatus().equals(CrawlUriMessage.STATUS.SAVE)) {
      unhandled(msg);
      return;
    }

    // URI message to process received
    // start the crawling request
    Dataset ds = null;

    // check if resource is already downloaded
    if (uriMsg instanceof ResourceCrawlUriMessage) {
      ResourceCrawlUriMessage resMsg = ((ResourceCrawlUriMessage) uriMsg);
      if (resMsg.getSerializedResource() != null && resMsg.getSerializationFormat() != null) {
        try {
          // TODO: this should be optimized, why deserialize the resource here when we just want to save it in the RDF
          // store? How to insert this serialized resource into the SPARQL endpoint?
          ds = SparqlService.deserializeDataset(resMsg.getSerializedResource(), resMsg.getSerializationFormat());
        } catch (Exception e) {
          throw new CrawlWrapperException(e, uriMsg);
        }
      }
    }

    // download resource if not already downloaded
    if (ds == null) {
      try {
        ds = linkedDataSource.getDataForResource(URI.create(uriMsg.getUri()));
      } catch (RestClientException e) {
        throw new CrawlWrapperException(e, uriMsg);
      }
    }

    // Save dataset to triple store
    sparqlService.updateNamedGraphsOfDataset(ds);
    String wonNodeUri = extractWonNodeUri(ds, uriMsg.getUri());
    if (wonNodeUri == null) {
      wonNodeUri = uriMsg.getWonNodeUri();
    }

    // do nothing more here if the STATUS of the message was SAVE
    if (uriMsg.getStatus().equals(CrawlUriMessage.STATUS.SAVE)) {
      log.debug("processed crawl uri event {} with status 'SAVE'", uriMsg);
      return;
    }

    // send extracted non-base URIs back to sender and save meta data about crawling the URI
    // extract only uris which were crawled at least one recrawl interval ago
    long crawlDate = System.currentTimeMillis();
    log.debug("Extract non-base URIs from message {}", uriMsg);
    Set<String> extractedURIs = sparqlService.extractURIs(
      uriMsg.getUri(), uriMsg.getBaseUri(), config.getCrawlNonBasePropertyPaths(),
      crawlDate - config.getRecrawlIntervalDuration().toMillis());
    for (String extractedURI : extractedURIs) {
      CrawlUriMessage newUriMsg = new CrawlUriMessage(
        extractedURI, uriMsg.getBaseUri(), wonNodeUri, CrawlUriMessage.STATUS.PROCESS, crawlDate);
      getSender().tell(newUriMsg, getSelf());
    }

    // send extracted base URIs back to sender and save meta data about crawling the URI
    // extract only uris which were crawled at least one recrawl interval ago
    log.debug("Extract base URIs from message {}", uriMsg);
    extractedURIs = sparqlService.extractURIs(uriMsg.getUri(), uriMsg.getBaseUri(), config.getCrawlBasePropertyPaths(),
      crawlDate - config.getRecrawlIntervalDuration().toMillis());
    for (String extractedURI : extractedURIs) {
      CrawlUriMessage newUriMsg = new CrawlUriMessage(
        extractedURI, extractedURI, wonNodeUri, CrawlUriMessage.STATUS.PROCESS, crawlDate);
      getSender().tell(newUriMsg, getSelf());
    }

    // signal sender that this URI is processed and save meta data about crawling the URI.
    // This needs to be done after all extracted URI messages have been sent to guarantee consistency
    // in case of failure
    crawlDate = System.currentTimeMillis();
    CrawlUriMessage uriDoneMsg = new CrawlUriMessage(
      uriMsg.getUri(), uriMsg.getBaseUri(), wonNodeUri, CrawlUriMessage.STATUS.DONE, crawlDate);
    log.info("Crawling done for URI {}", uriDoneMsg.getUri());
    getSender().tell(uriDoneMsg, getSelf());

    // if this URI/dataset was a need then send an event to the distributed event bus
    NeedModelWrapper needModelWrapper;
    try {

      // only send active needs right now
      needModelWrapper = new NeedModelWrapper(ds);
      NeedState state = needModelWrapper.getNeedState();
      if (state.equals(NeedState.ACTIVE)) {

        log.debug("Created need event for need uri {}", uriMsg.getUri());
        NeedEvent.TYPE type = NeedEvent.TYPE.CREATED;
        NeedEvent needEvent = new NeedEvent(uriMsg.getUri(), wonNodeUri, type, crawlDate, ds);
        pubSubMediator
          .tell(new DistributedPubSubMediator.Publish(needEvent.getClass().getName(), needEvent), getSelf());
      }

    } catch (DataIntegrityException e) {
      log.debug("no valid need model found in dataset for uri {}", uriMsg.getUri());
    }
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

  public void setSparqlService(final CrawlSparqlService sparqlService) {
    this.sparqlService = sparqlService;
  }
}
