package won.matcher.service.crawler.actor;

import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.SupervisorStrategy;
import akka.actor.UntypedActor;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import won.matcher.service.common.event.WonNodeEvent;
import won.matcher.service.common.spring.SpringExtension;
import won.matcher.service.crawler.config.CrawlConfig;
import won.matcher.service.crawler.exception.CrawlWrapperException;
import won.matcher.service.crawler.msg.CrawlUriMessage;
import won.matcher.service.crawler.msg.ResourceCrawlUriMessage;
import won.matcher.service.crawler.service.CrawlSparqlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import won.protocol.service.WonNodeInfo;

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
 * this won node can continue and will be triggered regularly by
 * {@link won.matcher.service.nodemanager.actor.WonNodeControllerActor}.
 *
 * User: hfriedrich
 * Date: 30.03.2015
 */
@Component
@Scope("prototype")
public class MasterCrawlerActor extends UntypedActor
{
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
  private static final FiniteDuration RESCHEDULE_MESSAGE_DURATION = Duration.create(500, TimeUnit.MILLISECONDS);
  private Map<String, CrawlUriMessage> pendingMessages = new HashMap<>();
  private Map<String, CrawlUriMessage> doneMessages = new HashMap<>();
  private Map<String, CrawlUriMessage> failedMessages = new HashMap<>();
  private Set<String> crawlWonNodeUris = new HashSet<>();
  private Set<String> skipWonNodeUris = new HashSet<>();
  private ActorRef crawlingWorker;
  private ActorRef updateMetaDataWorker;
  private ActorRef pubSubMediator;
  private static final String RECRAWL_TICK = "recrawl_tick";
  private static final int MIN_PENDING_MESSAGES_TO_SKIP_RECRAWLING = 10;

  @Autowired
  private CrawlConfig config;

  @Autowired
  private CrawlSparqlService sparqlService;

  @Override
  public void preStart() {

    // Create a scheduler to execute the life check for each won node regularly
    getContext().system().scheduler().schedule(config.getRecrawlIntervalDuration(), config.getRecrawlIntervalDuration(),
                                               getSelf(), RECRAWL_TICK, getContext().dispatcher(), null);

    // Create the router/pool with worker actors that do the actual crawling
    crawlingWorker = getContext().actorOf(SpringExtension.SpringExtProvider.get(getContext().system()).fromConfigProps(
      WorkerCrawlerActor.class), "CrawlingRouter");

    // create a single meta data update actor for all worker actors
    updateMetaDataWorker = getContext().actorOf(SpringExtension.SpringExtProvider.get(getContext().system()).props(
      UpdateMetadataActor.class), "MetaDataUpdateWorker");
    getContext().watch(updateMetaDataWorker);

    // create an need loading actor
    getContext().actorOf(SpringExtension.SpringExtProvider.get(getContext().system()).props(
      NeedEventLoaderActor.class), "NeedEventLoader");

    // subscribe for won node events
    pubSubMediator = DistributedPubSub.get(getContext().system()).mediator();
    pubSubMediator.tell(new DistributedPubSubMediator.Subscribe(WonNodeEvent.class.getName(), getSelf()), getSelf());

    // subscribe to crawl events
    pubSubMediator.tell(new DistributedPubSubMediator.Subscribe(CrawlUriMessage.class.getName(), getSelf()), getSelf());
    pubSubMediator.tell(new DistributedPubSubMediator.Subscribe(
      ResourceCrawlUriMessage.class.getName(), getSelf()), getSelf());

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

        log.warning("Actor encountered error: {}", t);
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
   * Process {@link won.matcher.service.crawler.msg.CrawlUriMessage} objects
   *
   * @param message
   */
  @Override
  public void onReceive(final Object message) {

    if (message.equals(RECRAWL_TICK)) {
      askWonNodeInfoForCrawling();
    } else if (message instanceof WonNodeEvent) {
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
    if (msg.getStatus().equals(CrawlUriMessage.STATUS.PROCESS) || msg.getStatus().equals(CrawlUriMessage.STATUS.SAVE)) {

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
        WonNodeEvent event = new WonNodeEvent(msg.getWonNodeUri(), WonNodeEvent.STATUS.NEW_WON_NODE_DISCOVERED);
        pubSubMediator.tell(new DistributedPubSubMediator.Publish(event.getClass().getName(), event), getSelf());
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

      // If we receive a connection event then add the won node to the list of known nodes and start crawling it
      log.debug("added new won node to set of crawling won nodes: {}", event.getWonNodeUri());
      skipWonNodeUris.remove(event.getWonNodeUri());
      crawlWonNodeUris.add(event.getWonNodeUri());
      startCrawling(event.getWonNodeInfo());

    } else if (event.getStatus().equals(WonNodeEvent.STATUS.SKIP_WON_NODE)) {

      // if we should skip this won node remove it from the known won node list and add it to the skip list
      log.debug("skip crawling won node: {}", event.getWonNodeUri());
      crawlWonNodeUris.remove(event.getWonNodeUri());
      skipWonNodeUris.add(event.getWonNodeUri());
    }
  }

  /**
   * Ask for complete won node info of all known won nodes on the event bus. Do this to initiate the crawling process
   * again. Therefore clear the cache of crawled uris so that they can be crawled again.
   */
  private void askWonNodeInfoForCrawling() {

    if (pendingMessages.size() > MIN_PENDING_MESSAGES_TO_SKIP_RECRAWLING) {
      log.warning("Skip crawling cylce since there are currently {} messages in the pending queue. Try to restart " +
                    "crawling again in {} minutes", pendingMessages.size(), config.getRecrawlIntervalDuration().toMinutes());
      return;
    }

    log.info("Start crwaling process again. Clear the cached uris and crawling statistics");
    doneMessages.clear();
    failedMessages.clear();
    pendingMessages.clear();

    for (String wonNodeUri : crawlWonNodeUris) {
      log.info("ask for won node info of {}", wonNodeUri);
      WonNodeEvent event = new WonNodeEvent(wonNodeUri, WonNodeEvent.STATUS.GET_WON_NODE_INFO_FOR_CRAWLING);
      pubSubMediator.tell(new DistributedPubSubMediator.Publish(event.getClass().getName(), event), getSelf());
    }
  }

  /**
   * Start crawling a won node starting at the need list
   *
   * @param wonNodeInfo
   */
  private void startCrawling(WonNodeInfo wonNodeInfo) {

      // get the last needUri we crawled from the rdf store and continue crawling from that point
      String lastNeedUri = sparqlService.retrieveLastCrawledNeedUri(wonNodeInfo.getWonNodeURI());
      String lastNeedId = getNeedOrConnectionIdFromUri(lastNeedUri);
      if (lastNeedId != null) {
          String uri = wonNodeInfo.getNeedListURI() + "?resumeafter=" + lastNeedId;
          self().tell(new CrawlUriMessage(uri, wonNodeInfo.getNeedListURI(), wonNodeInfo.getWonNodeURI(),
                  CrawlUriMessage.STATUS.PROCESS, System.currentTimeMillis()), getSelf());

          // change the base uri here to end with a slash cause we don't know how the uri is saved
          // also have to change the first uri parameter since messages with the same uri parameter get filtered out
          uri = wonNodeInfo.getNeedListURI() + "/?resumeafter=" + lastNeedId;
          self().tell(new CrawlUriMessage(uri, wonNodeInfo.getNeedListURI() + "/", wonNodeInfo.getWonNodeURI(),
                  CrawlUriMessage.STATUS.PROCESS, System.currentTimeMillis()), getSelf());
      } else {

          // or else if we didn't crawl needs yet start crawling the whole won node
          String needListUri = removeEndingSlash(wonNodeInfo.getNeedListURI());
          self().tell(new CrawlUriMessage(needListUri, needListUri, wonNodeInfo.getWonNodeURI(),
                  CrawlUriMessage.STATUS.PROCESS, System.currentTimeMillis()), getSelf());
          self().tell(new CrawlUriMessage(needListUri + "/", needListUri + "/", wonNodeInfo.getWonNodeURI(),
                  CrawlUriMessage.STATUS.PROCESS, System.currentTimeMillis()), getSelf());
      }

      // get the last connectionUri we crawled from the rdf store and continue crawling from that point
      String lastConnectionUri = sparqlService.retrieveLastCrawledConnectionUri(wonNodeInfo.getWonNodeURI());
      String lastConnectionId = getNeedOrConnectionIdFromUri(lastConnectionUri);
      if (lastConnectionId != null) {
          String uri = wonNodeInfo.getConnectionURIPrefix() + "?resumeafter=" + lastConnectionId;
          self().tell(new CrawlUriMessage(uri, wonNodeInfo.getConnectionURIPrefix(), wonNodeInfo.getWonNodeURI(),
                  CrawlUriMessage.STATUS.PROCESS, System.currentTimeMillis()), getSelf());

          // change the base uri here to end with a slash cause we don't know how the uri is saved
          // also have to change the first uri parameter since messages with the same uri parameter get filtered out
          uri = wonNodeInfo.getConnectionURIPrefix() + "/?resumeafter=" + lastConnectionId;
          self().tell(new CrawlUriMessage(uri, wonNodeInfo.getConnectionURIPrefix() + "/", wonNodeInfo.getWonNodeURI(),
                  CrawlUriMessage.STATUS.PROCESS, System.currentTimeMillis()), getSelf());
      }
  }

  private String removeEndingSlash(String uri) {
      if (uri != null && uri.endsWith("/")) {
          return uri.substring(0, uri.length() - 1);
      }
      return uri;
  }

  private String getNeedOrConnectionIdFromUri(String uri) {

      uri = removeEndingSlash(uri);
      if (uri != null) {
          String[] parts = uri.split("/");
          return parts[parts.length - 1];
      }
      return null;
  }


}
