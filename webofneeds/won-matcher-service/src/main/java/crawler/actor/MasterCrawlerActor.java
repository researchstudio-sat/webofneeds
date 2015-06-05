package crawler.actor;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import akka.routing.FromConfig;
import crawler.config.CrawlSettings;
import crawler.config.CrawlSettingsImpl;
import crawler.exception.CrawlWrapperException;
import crawler.msg.CrawlUriMessage;
import crawler.service.CrawlSparqlService;
import common.event.WonNodeEvent;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Coordinates recursive crawling of linked data resources by assigning {@link CrawlUriMessage}
 * to workers {@link WorkerCrawlerActor} and one single worker of type {@link UpdateMetadataActor}.
 * The process can be stopped at any time and continued by passing the messages that
 * should be crawled again since meta data about the crawling process is saved
 * in the SPARQL endpoint. This is done by a single actor of type {@link UpdateMetadataActor}
 * which keeps message order to guarantee consistency in case of failure. Unfinished messages can
 * be resend for restarting crawling.
 * Newly discovered won node events are published on the event stream during crawling.
 * When an event is received that indicates that we connected to that won node, crawling
 * this won node can continue.
 *
 * User: hfriedrich
 * Date: 30.03.2015
 */
public class MasterCrawlerActor extends UntypedActor
{
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
  private final CrawlSettingsImpl settings = CrawlSettings.SettingsProvider.get(getContext().system());
  private static final FiniteDuration RESCHEDULE_MESSAGE_DURATION = Duration.create(500, TimeUnit.MILLISECONDS);
  private CrawlSparqlService sparqlService;
  private Map<String, CrawlUriMessage> pendingMessages = null;
  private Map<String, CrawlUriMessage> doneMessages = null;
  private Map<String, CrawlUriMessage> failedMessages = null;
  private Set<String> crawlWonNodeUris = null;
  private Set<String> skipWonNodeUris = null;
  private ActorRef crawlingWorker;
  private ActorRef updateMetaDataWorker;

  public MasterCrawlerActor() {
    pendingMessages = new HashMap<>();
    doneMessages = new HashMap<>();
    failedMessages = new HashMap<>();
    crawlWonNodeUris = new HashSet<>();
    skipWonNodeUris = new HashSet<>();
    sparqlService = new CrawlSparqlService(settings.METADATA_SPARQL_ENDPOINT);
  }

  @Override
  public void preStart() {

    // Create the router/pool with worker actors that do the actual crawling
    Props workerProps = Props.create(WorkerCrawlerActor.class);
    crawlingWorker = getContext().actorOf(new FromConfig().props(workerProps), "CrawlingRouter");

    // create a single meta data update actor for all worker actors
    updateMetaDataWorker = getContext().actorOf(Props.create(UpdateMetadataActor.class), "MetaDataUpdateWorker");
    getContext().watch(updateMetaDataWorker);

    // subscribe for won node events
    getContext().system().eventStream().subscribe(getSelf(), WonNodeEvent.class);

    // load the unfinished uris and start crawling
    for (CrawlUriMessage msg : sparqlService.retrieveMessagesForCrawling(CrawlUriMessage.STATUS.PROCESS)) {
      pendingMessages.put(msg.getUri(), msg);
      crawlingWorker.tell(msg, getSelf());
    }

    for (CrawlUriMessage msg : sparqlService.retrieveMessagesForCrawling(CrawlUriMessage.STATUS.FAILED)) {
      getSelf().tell(msg, getSelf());
    }
  }

  /**
   * set supervision strategy for worker actors and handle failed crawling actions
   *
   * @return
   */
  @Override
  public SupervisorStrategy supervisorStrategy() {

    SupervisorStrategy supervisorStrategy = new OneForOneStrategy(
      0, Duration.Zero(), new Function<Throwable, SupervisorStrategy.Directive>()
    {

      @Override
      public SupervisorStrategy.Directive apply(Throwable t) throws Exception {

        // save the failed status of a crawlingWorker during crawling
        if (t instanceof CrawlWrapperException) {
          CrawlWrapperException e = (CrawlWrapperException) t;
          log.warning("Handled breaking message: {}", e.getBreakingMessage());
          log.warning("Exception was: {}", e.getException());
          processCrawlUriMessage(e.getBreakingMessage());
          return SupervisorStrategy.resume();
        }

        // default behaviour in other cases
        return SupervisorStrategy.escalate();
      }
    });

    return supervisorStrategy;
  }

  /**
   * Process {@link crawler.msg.CrawlUriMessage} objects
   *
   * @param message
   */
  @Override
  public void onReceive(final Object message) {

    if (message instanceof WonNodeEvent) {
      processWonNodeEvent((WonNodeEvent) message);
    } else if (message instanceof CrawlUriMessage) {
      CrawlUriMessage uriMsg = (CrawlUriMessage) message;
      processCrawlUriMessage(uriMsg);
      log.debug("Number of pending messages: {}", pendingMessages.size());
    } else {
      unhandled(message);
    }
  }

  private void logStatus() {
    log.info("Number of URIs\n Crawled: {}\n Failed: {}\n Pending: {}",
             doneMessages.size(), failedMessages.size(), pendingMessages.size());
  }

  private boolean discoveredNewWonNode(String uri) {
    if (uri == null || uri.isEmpty() || crawlWonNodeUris.contains(uri) || skipWonNodeUris.contains(uri)) {
      return false;
    }
    return true;
  }

  /**
   * Pass the messages to process to the workers and update meta data about crawling.
   * Also create an event if a new won node is discovered.
   *
   * @param msg
   */
  private void processCrawlUriMessage(CrawlUriMessage msg) {

    log.debug("Process message: {}", msg);
    if (msg.getStatus().equals(CrawlUriMessage.STATUS.PROCESS)) {

      // multiple extractions of the same URI can happen quite often since the extraction
      // query uses property path from base URI which may return URIs that are already
      // processed. So filter out these messages here
      if (pendingMessages.get(msg.getUri()) != null ||
        doneMessages.get(msg.getUri()) != null ||
        failedMessages.get(msg.getUri()) != null) {
        log.debug("message {} already processing/processed ...", msg);
        return;
      }

      updateMetaDataWorker.tell(msg, getSelf());

      // check if the uri belongs to a known and not skipped won node.
      // if so continue crawling, otherwise first publish an event about a newly
      // discovered won node and reschedule the processing of the current message until
      // we received an answer for the discovered won node event
      if (discoveredNewWonNode(msg.getWonNodeUri())) {
        log.debug("discovered new won node {}", msg.getWonNodeUri());
        getContext().system().eventStream().publish(new WonNodeEvent(msg.getWonNodeUri(), WonNodeEvent.STATUS.NEW_WON_NODE_DISCOVERED));
        getContext().system().scheduler().scheduleOnce(
          RESCHEDULE_MESSAGE_DURATION, getSelf(), msg, getContext().dispatcher(), null);
      } else if (!skipWonNodeUris.contains(msg.getWonNodeUri())) {
        pendingMessages.put(msg.getUri(), msg);
        crawlingWorker.tell(msg, getSelf());
      }

    } else if (msg.getStatus().equals(CrawlUriMessage.STATUS.DONE)) {

      // URI crawled successfully
      log.debug("Successfully processed URI: {}", msg.getUri());
      updateMetaDataWorker.tell(msg, getSelf());
      pendingMessages.remove(msg.getUri());
      if (doneMessages.put(msg.getUri(), msg) != null) {
        log.warning("URI message received twice: {}", msg.getUri());
      }
      logStatus();

    } else if (msg.getStatus().equals(CrawlUriMessage.STATUS.FAILED)) {

      // Crawling failed
      log.debug("Crawling URI failed: {}", msg.getUri());
      updateMetaDataWorker.tell(msg, getSelf());
      pendingMessages.remove(msg.getUri());
      failedMessages.put(msg.getUri(), msg);
      logStatus();
    }
  }

  /**
   * If events about crawling or skipping certain won nodes occur, keep this information in memory
   *
   * @param event
   */
  private void processWonNodeEvent(WonNodeEvent event) {

    if (event.getStatus().equals(WonNodeEvent.STATUS.CONNECTED_TO_WON_NODE)) {
      log.debug("added new won node to set of crawling won nodes: {}", event.getWonNodeUri());
      skipWonNodeUris.remove(event.getWonNodeUri());
      crawlWonNodeUris.add(event.getWonNodeUri());
    } else if (event.getStatus().equals(WonNodeEvent.STATUS.SKIP_WON_NODE)) {
      log.debug("skip crawling won node: {}", event.getWonNodeUri());
      crawlWonNodeUris.remove(event.getWonNodeUri());
      skipWonNodeUris.add(event.getWonNodeUri());
    }
  }


}
