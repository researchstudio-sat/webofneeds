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
import scala.concurrent.duration.Duration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Coordinates recursive crawling of linked data resources by assigning {@link CrawlUriMessage}
 * to workers {@link WorkerCrawlerActor} and one single worker of type {@link UpdateMetadataActor}.
 * The process can be stopped at any time and continued by passing the messages that
 * should be crawled again since meta data about the crawling process is saved
 * in the SPARQL endpoint. This is done by a single actor of type {@link UpdateMetadataActor}
 * which keeps message order to guarantee consistency in case of failure. Unfinished messages can
 * be resend for restarting crawling.
 * {@link node.actor.WonNodeControllerActor} is informed about newly discovered won nodes during crawling.
 *
 * User: hfriedrich
 * Date: 30.03.2015
 */
public class MasterCrawlerActor extends UntypedActor
{
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
  private final CrawlSettingsImpl settings = CrawlSettings.SettingsProvider.get(getContext().system());
  private CrawlSparqlService sparqlService;
  private Map<String, CrawlUriMessage> pendingMessages = null;
  private Map<String, CrawlUriMessage> doneMessages = null;
  private Map<String, CrawlUriMessage> failedMessages = null;
  private Set<String> crawlWonNodeUris = null;
  private Set<String> skipWonNodeUris = null;
  private ActorRef crawlingWorker;
  private ActorRef updateMetaDataWorker;
  private ActorRef wonNodeController;

  public MasterCrawlerActor(ActorRef wonNodeController) {
    pendingMessages = new HashMap<>();
    doneMessages = new HashMap<>();
    failedMessages = new HashMap<>();
    crawlWonNodeUris = new HashSet<>();
    skipWonNodeUris = new HashSet<>();
    this.wonNodeController = wonNodeController;
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

    // load the unfinished uris and start crawling
    for (CrawlUriMessage msg : sparqlService.retrieveMessagesForCrawling(CrawlUriMessage.STATUS.PROCESS)) {
      getSelf().tell(msg, getSelf());
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
          process(e.getBreakingMessage());
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

    // if the update meta data worker terminated the master can be terminated too
    if (message instanceof Terminated) {
      if (((Terminated) message).getActor().equals(updateMetaDataWorker)) {
        log.info("Crawler shut down");
        getContext().system().shutdown();
      }
    }

    if (message instanceof CrawlUriMessage) {
      CrawlUriMessage uriMsg = (CrawlUriMessage) message;
      process(uriMsg);
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
   * Pass the messages to process to the workers and update meta data about crawling
   *
   * @param msg
   */
  private void process(CrawlUriMessage msg) {

    log.debug("Process message: {}", msg);
    if (msg.getStatus().equals(CrawlUriMessage.STATUS.PROCESS)) {

      if (getSender().path().equals(crawlingWorker.path())) {

        // multiple extractions of the same URI can happen quite often since the extraction
        // query uses property path from base URI which may return URIs that are already
        // processed. So filter out these messages here
        if (pendingMessages.get(msg.getUri()) != null ||
          doneMessages.get(msg.getUri()) != null ||
          failedMessages.get(msg.getUri()) != null) {
          log.debug("message {} already processing/processed ...", msg);
          return;
        }
      } else if (getSender().path().equals(wonNodeController.path())) {

        // if a message from won node controller about processing an URI from
        // a certain won node is received, then crawl from that won node in the future
        crawlWonNodeUris.add(msg.getWonNodeUri());
      }

      updateMetaDataWorker.tell(msg, getSelf());
      pendingMessages.put(msg.getUri(), msg);

      // check if the uri belongs to a known and not skipped won node.
      // if so continue crawling, otherwise first ask the won node controller
      if (discoveredNewWonNode(msg.getWonNodeUri())) {
        wonNodeController.tell(msg, getSelf());
      } else if (!skipWonNodeUris.contains(msg.getWonNodeUri())) {
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
      crawlingWorker.tell(msg, getSelf());
      logStatus();

    } else if (msg.getStatus().equals(CrawlUriMessage.STATUS.SKIP)) {

      // Skip crawling this won node
      log.debug("Crawling skipped for URI '{}' of WON node '{}'", msg.getUri(), msg.getWonNodeUri());
      skipWonNodeUris.add(msg.getWonNodeUri());
      pendingMessages.remove(msg.getUri());
    }

    // terminate
    if (pendingMessages.size() == 0) {
      log.info("Terminating crawler ...");
      updateMetaDataWorker.tell(PoisonPill.getInstance(), getSelf());
    }
  }


}
