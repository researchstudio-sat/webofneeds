package won.matcher.service.crawler.actor;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.jena.query.Dataset;
import org.apache.jena.shared.Lock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import won.matcher.service.common.event.AtomEvent;
import won.matcher.service.common.service.sparql.SparqlService;
import won.matcher.service.crawler.config.CrawlConfig;
import won.matcher.service.crawler.exception.CrawlWrapperException;
import won.matcher.service.crawler.msg.CrawlUriMessage;
import won.matcher.service.crawler.msg.ResourceCrawlUriMessage;
import won.matcher.service.crawler.service.CrawlSparqlService;
import won.protocol.exception.IncorrectPropertyCountException;
import won.protocol.model.AtomState;
import won.protocol.rest.DatasetResponseWithStatusCodeAndHeaders;
import won.protocol.util.AtomModelWrapper;
import won.protocol.util.RdfUtils;
import won.protocol.util.linkeddata.LinkedDataSourceBase;
import won.protocol.vocabulary.WON;

/**
 * Actor requests linked data URI using HTTP and saves it to a triple store
 * using SPARQL UPDATE query. Responds to the sender with extracted URIs from
 * the linked data URI.
 * <p>
 * This class uses property paths to extract URIs from linked data resources.
 * These property paths are executed relative to base URIs. Therefore there are
 * two types of property paths. Base property path extract URIs that are taken
 * as new base URIs. Non-base property paths extract URIs that keep the current
 * base URI.
 * <p>
 * User: hfriedrich Date: 07.04.2015
 */
@Component
@Scope("prototype")
public class WorkerCrawlerActor extends UntypedActor {
    private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    @Autowired
    private LinkedDataSourceBase linkedDataSource;
    @Autowired
    private CrawlSparqlService sparqlService;
    @Autowired
    private CrawlConfig config;
    private ActorRef pubSubMediator;

    @Override
    public void preStart() {
        // initialize the distributed event bus to send atom events to the matchers
        pubSubMediator = DistributedPubSub.get(getContext().system()).mediator();
    }

    /**
     * Receives messages with an URI and processes them by requesting the resource,
     * saving it to a triple store, extracting URIs from content and answering the
     * sender.
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
        if (!uriMsg.getStatus().equals(CrawlUriMessage.STATUS.PROCESS)
                        && !uriMsg.getStatus().equals(CrawlUriMessage.STATUS.SAVE)) {
            unhandled(msg);
            return;
        }
        crawlUri(uriMsg);
    }

    private void crawlUri(CrawlUriMessage uriMsg) {
        Dataset ds = null;
        List<String> etags = null;
        Lock lock = null;
        try {
            // check if resource is already downloaded
            if (uriMsg instanceof ResourceCrawlUriMessage) {
                ResourceCrawlUriMessage resMsg = ((ResourceCrawlUriMessage) uriMsg);
                if (resMsg.getSerializedResource() != null && resMsg.getSerializationFormat() != null) {
                    // TODO: this should be optimized, why deserialize the resource here when we
                    // just want to save it in the RDF
                    // store? How to insert this serialized resource into the SPARQL endpoint?
                    ds = SparqlService.deserializeDataset(resMsg.getSerializedResource(),
                                    resMsg.getSerializationFormat());
                }
            }
            // download resource if not already downloaded
            if (ds == null) {
                // use ETag/If-None-Match Headers to make the process more efficient
                HttpHeaders httpHeaders = new HttpHeaders();
                if (uriMsg.getResourceETagHeaderValues() != null && !uriMsg.getResourceETagHeaderValues().isEmpty()) {
                    String ifNoneMatchHeaderValue = StringUtils
                                    .collectionToDelimitedString(uriMsg.getResourceETagHeaderValues(), ", ");
                    httpHeaders.add("If-None-Match", ifNoneMatchHeaderValue);
                }
                DatasetResponseWithStatusCodeAndHeaders datasetWithHeaders = linkedDataSource
                                .getDatasetWithHeadersForResource(URI.create(uriMsg.getUri()), httpHeaders);
                ds = datasetWithHeaders.getDataset();
                etags = datasetWithHeaders.getResponseHeaders().get("ETag");
                // if dataset was not modified (304) we can treat the current crawl uri as done
                if (ds == null && datasetWithHeaders.getStatusCode() == 304) {
                    sendDoneUriMessage(uriMsg, uriMsg.getWonNodeUri(), etags);
                    return;
                }
                // if there is paging activated and the won node tells us that there is more
                // data (previous link)
                // to be downloaded, then we add this link to the crawling process too
                String prevLink = linkedDataSource.getPreviousLinkFromDatasetWithHeaders(datasetWithHeaders);
                if (prevLink != null) {
                    CrawlUriMessage newUriMsg = new CrawlUriMessage(uriMsg.getBaseUri(), prevLink,
                                    uriMsg.getWonNodeUri(), CrawlUriMessage.STATUS.PROCESS, System.currentTimeMillis(),
                                    null);
                    getSender().tell(newUriMsg, getSelf());
                }
            }
            lock = ds == null ? null : ds.getLock();
            lock.enterCriticalSection(true);
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
            // extract URIs from current resource and send extracted URI messages back to
            // sender
            log.debug("Extract URIs from message {}", uriMsg);
            Set<CrawlUriMessage> newCrawlMessages = sparqlService.extractCrawlUriMessages(uriMsg.getBaseUri(),
                            wonNodeUri);
            for (CrawlUriMessage newMsg : newCrawlMessages) {
                getSender().tell(newMsg, getSelf());
            }
            // signal sender that this URI is processed and save meta data about crawling
            // the URI.
            // This needs to be done after all extracted URI messages have been sent to
            // guarantee consistency
            // in case of failure
            sendDoneUriMessage(uriMsg, wonNodeUri, etags);
            // if this URI/dataset was an atom then send an event to the distributed event
            // bu
            if (AtomModelWrapper.isAAtom(ds)) {
                AtomModelWrapper atomModelWrapper = new AtomModelWrapper(ds, false);
                AtomState state = atomModelWrapper.getAtomState();
                AtomEvent.TYPE type = state.equals(AtomState.ACTIVE) ? AtomEvent.TYPE.ACTIVE : AtomEvent.TYPE.INACTIVE;
                log.debug("Created atom event for atom uri {}", uriMsg.getUri());
                long crawlDate = System.currentTimeMillis();
                AtomEvent atomEvent = new AtomEvent(uriMsg.getUri(), wonNodeUri, type, crawlDate, ds);
                pubSubMediator.tell(new DistributedPubSubMediator.Publish(atomEvent.getClass().getName(), atomEvent),
                                getSelf());
            }
        } catch (RestClientException e1) {
            // usually happens if the fetch of the dataset fails e.g.
            // HttpServerErrorException, HttpClientErrorException
            log.debug("Exception during crawling: " + e1);
            throw new CrawlWrapperException(e1, uriMsg);
        } catch (Exception e) {
            log.debug("Exception during crawling: " + e);
            throw new CrawlWrapperException(e, uriMsg);
        } finally {
            if (lock != null) {
                lock.leaveCriticalSection();
            }
        }
    }

    /**
     * Extract won node uri from a won resource
     *
     * @param ds resource as dataset
     * @param uri uri that represents resource
     * @return won node uri or null if link to won node is not linked in the
     * resource
     */
    private String extractWonNodeUri(Dataset ds, String uri) {
        try {
            return RdfUtils.findOnePropertyFromResource(ds, URI.create(uri), WON.wonNode).asResource().getURI();
        } catch (IncorrectPropertyCountException e) {
            return null;
        }
    }

    private void sendDoneUriMessage(CrawlUriMessage sourceUriMessage, String wonNodeUri, Collection<String> etags) {
        long crawlDate = System.currentTimeMillis();
        CrawlUriMessage uriDoneMsg = new CrawlUriMessage(sourceUriMessage.getUri(), sourceUriMessage.getBaseUri(),
                        wonNodeUri, CrawlUriMessage.STATUS.DONE, crawlDate, etags);
        String ifNoneMatch = sourceUriMessage.getResourceETagHeaderValues() != null
                        ? String.join(", ", sourceUriMessage.getResourceETagHeaderValues())
                        : "<None>";
        String responseETags = etags != null ? String.join(", ", etags) : "<None>";
        log.debug("Crawling done for URI {} with ETag Header Values {} (If-None-Match request value: {})",
                        uriDoneMsg.getUri(), responseETags, ifNoneMatch);
        getSender().tell(uriDoneMsg, getSelf());
    }

    public void setSparqlService(final CrawlSparqlService sparqlService) {
        this.sparqlService = sparqlService;
    }
}
