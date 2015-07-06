package node.actor;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.hp.hpl.jena.query.Dataset;
import common.event.WonNodeEvent;
import common.service.HttpRequestService;
import crawler.actor.MasterCrawlerActor;
import crawler.msg.CrawlUriMessage;
import node.config.ActiveMqNeedConsumerFactory;
import node.config.WonNodeControllerSettings;
import node.config.WonNodeControllerSettingsImpl;
import node.pojo.WonNodeConnection;
import node.service.WonNodeSparqlService;
import org.springframework.web.client.RestClientException;
import won.protocol.service.WonNodeInfo;

import java.util.*;


/**
 * Actor that knows all won nodes the matching service is communicating. It gets informed
 * about new won nodes over the event stream (e.g. by he crawler) and decides which
 * won nodes to crawl and to register with for receiving need events.
 *
 * User: hfriedrich
 * Date: 27.04.2015
 */
public class WonNodeControllerActor extends UntypedActor
{
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
  private ActorRef crawler;
  private HttpRequestService httpRequestService;
  private Map<String, WonNodeConnection> crawlWonNodes;
  private Set<String> skipWonNodeUris;
  private Set<String> failedWonNodeUris;
  private WonNodeSparqlService sparqlService;
  private WonNodeControllerSettingsImpl settings;
  private static final String TICK = "tick";

  public WonNodeControllerActor() {

    settings = WonNodeControllerSettings.SettingsProvider.get(getContext().system());
    httpRequestService = new HttpRequestService();
    sparqlService = new WonNodeSparqlService(settings.SPARQL_ENDPOINT);
    crawlWonNodes = new HashMap<>();
    skipWonNodeUris = new HashSet<>();
    failedWonNodeUris = new HashSet<>();
  }

  @Override
  public void preStart() {

    // Create a scheduler to execute the life check for each won node regularly
    getContext().system().scheduler().schedule(settings.WON_NODE_LIFE_CHECK_DURATION,
                                               settings.WON_NODE_LIFE_CHECK_DURATION, getSelf(), TICK, getContext().dispatcher(), null);

    // Subscribe for won node events
    getContext().system().eventStream().subscribe(getSelf(), WonNodeEvent.class);

    // set won nodes to skip by configuration
    skipWonNodeUris.addAll(settings.WON_NODES_SKIP);

    // get all known won node uris
    Set<WonNodeInfo> wonNodeInfo = sparqlService.retrieveAllWonNodeInfo();
    for (WonNodeInfo nodeInfo : wonNodeInfo) {
      WonNodeConnection con = subscribeNeedUpdates(nodeInfo);
      crawlWonNodes.put(nodeInfo.getWonNodeURI(), con);
    }

    // initialize the won nodes to crawl
    for (String nodeUri : settings.WON_NODES_CRAWL) {
      if (!skipWonNodeUris.contains(nodeUri)) {
        if (!crawlWonNodes.containsKey(nodeUri)) {
          // publish event (to self) for discovering new won node from config file
          getContext().system().eventStream().publish(new WonNodeEvent(
            nodeUri, WonNodeEvent.STATUS.NEW_WON_NODE_DISCOVERED));
        }
      }
    }

    // initialize the crawler
    crawler = getContext().system().actorOf(
      Props.create(MasterCrawlerActor.class), "MasterCrawlerActor");
  }

  /**
   * Receive messages about newly discovered won node and decide to crawl or skip
   * processing these won nodes.
   *
   * @param message
   * @throws Exception
   */
  @Override
  public void onReceive(final Object message) {

    if (message instanceof Terminated) {
      handleConnectionErrors((Terminated) message);
      return;
    }

    if (message.equals(TICK)) {
      lifeCheck();
      return;
    }

    if (message instanceof WonNodeEvent) {
      WonNodeEvent event = (WonNodeEvent) message;

      if (event.getStatus().equals(WonNodeEvent.STATUS.NEW_WON_NODE_DISCOVERED)) {

        // continue crawling of known won nodes
        if (crawlWonNodes.containsKey(event.getWonNodeUri())) {
            log.debug("Won node uri '{}' already discovered", event.getWonNodeUri());
            getContext().system().eventStream().publish(new WonNodeEvent(
              event.getWonNodeUri(), WonNodeEvent.STATUS.CONNECTED_TO_WON_NODE));
            return;
        }

        // skip crawling of won nodes in the skip list
        if (skipWonNodeUris.contains(event.getWonNodeUri())) {
          log.debug("Skip crawling won node with uri '{}'", event.getWonNodeUri());
          getContext().system().eventStream().publish(new WonNodeEvent(
            event.getWonNodeUri(), WonNodeEvent.STATUS.SKIP_WON_NODE));
          return;
        }

        // try the connect to won node
        WonNodeConnection con = addWonNodeForCrawling(event.getWonNodeUri());

        // connection failed ?
        if (failedWonNodeUris.contains(event.getWonNodeUri())) {
          log.debug("Still could not connect to won node with uri: {}, will retry later ...",
                    event.getWonNodeUri());
          return;
        }

        // crawl all new discovered won nodes
        getContext().system().eventStream().publish(new WonNodeEvent(
          event.getWonNodeUri(), WonNodeEvent.STATUS.CONNECTED_TO_WON_NODE));
        startCrawling(crawlWonNodes.get(event.getWonNodeUri()).getWonNodeInfo());
        return;
      }
    }

    unhandled(message);
  }

  /**
   * Try to add a won node for crawling
   *
   * @param wonNodeUri URI of the won node meta data resource
   * @return won node connection if successfully connected, otherwise null
   */
  private WonNodeConnection addWonNodeForCrawling(String wonNodeUri) {

    WonNodeConnection con = null;
    try {
      // request the resource and save the data
      Dataset ds = httpRequestService.requestDataset(wonNodeUri);
      sparqlService.updateNamedGraphsOfDataset(ds);
      WonNodeInfo nodeInfo = sparqlService.getWonNodeInfoFromDataset(ds);

      // subscribe for need updates
      con = subscribeNeedUpdates(nodeInfo);
      crawlWonNodes.put(nodeInfo.getWonNodeURI(), con);
      failedWonNodeUris.remove(nodeInfo.getWonNodeURI());

    } catch (RestClientException e) {
      log.warning("Error requesting won node information from {}, exception is {}", wonNodeUri, e);
      addFailedWonNode(wonNodeUri, con);
    }

    return con;
  }

  /**
   * Start crawling a won node starting at the need list
   *
   * @param wonNodeInfo
   */
  private void startCrawling(WonNodeInfo wonNodeInfo) {

    // try crawling with and without ending "/" in need list uri
    String needListUri = wonNodeInfo.getNeedListURI();
    if (needListUri.endsWith("/")) {
      needListUri = needListUri.substring(0, needListUri.length() - 1);
    }

    crawler.tell(
      new CrawlUriMessage(needListUri, needListUri, wonNodeInfo.getWonNodeURI(),
                          CrawlUriMessage.STATUS.PROCESS), getSelf());
    crawler.tell(
      new CrawlUriMessage(needListUri + "/", needListUri + "/", wonNodeInfo.getWonNodeURI(),
                          CrawlUriMessage.STATUS.PROCESS), getSelf());
  }

  /**
   * Try to connect to unreachable won nodes from time to time
   */
  private void lifeCheck() {
    Iterator<String> iter = failedWonNodeUris.iterator();
    while (iter.hasNext()) {
      String uri = iter.next();

      // if won node becomes available start crawling the node
      WonNodeConnection con = addWonNodeForCrawling(uri);
      if (con != null) {
        startCrawling(con.getWonNodeInfo());
      }
    }
  }

  /**
   * Add a won node to the failed list and stop all its consumers
   *
   * @param wonNodeUri
   * @param con
   */
  private void addFailedWonNode(String wonNodeUri, WonNodeConnection con) {

    if (con != null) {
      getContext().stop(con.getNeedCreatedConsumer());
      getContext().stop(con.getNeedActivatedConsumer());
      getContext().stop(con.getNeedDeactivatedConsumer());
    }

    crawlWonNodes.remove(wonNodeUri);
    failedWonNodeUris.add(wonNodeUri);
  }

  private WonNodeConnection subscribeNeedUpdates(WonNodeInfo wonNodeInfo) {
    return ActiveMqNeedConsumerFactory.createWonNodeConnection(getContext(), wonNodeInfo);
  }

  /**
   * Handles connections errors that occur when the need consumer actors are terminated.
   *
   * @param t messages that holds a reference to consumer actor that was terminated
   */
  private void handleConnectionErrors(Terminated t) {
    for (String uri : crawlWonNodes.keySet()) {
      WonNodeConnection con = crawlWonNodes.get(uri);
      if (con != null) {
        if (con.getNeedCreatedConsumer().equals(t.getActor())) {
          log.error("NeedCreatedConsumer '{}' of won '{}' has been shut down", t.getActor(), uri);
          addFailedWonNode(con.getWonNodeInfo().getWonNodeURI(), con);
        } else if (con.getNeedActivatedConsumer().equals(t.getActor())) {
          log.error("NeedActivatedConsumer '{}' of won '{}' has been shut down", t.getActor(), uri);
          addFailedWonNode(con.getWonNodeInfo().getWonNodeURI(), con);
        } else if (con.getNeedDeactivatedConsumer().equals(t.getActor())) {
          log.error("NeedDeactivatedConsumer '{}' of won '{}' has been shut down", t.getActor(), uri);
          addFailedWonNode(con.getWonNodeInfo().getWonNodeURI(), con);
        }
      }
    }
  }

}


