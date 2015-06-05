package node.actor;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.hp.hpl.jena.query.Dataset;
import commons.service.HttpRequestService;
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
 * about new won nodes (e.g. by he crawler) and decides which won nodes to crawl and to
 * register with for receiving need updates.
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

    skipWonNodeUris.addAll(settings.WON_NODES_SKIP);

    // get all known won node uris
    Set<WonNodeInfo> wonNodeInfo = sparqlService.retrieveAllWonNodeInfo();
    for (WonNodeInfo nodeInfo : wonNodeInfo) {
      WonNodeConnection con = subscribeNeedUpdates(nodeInfo);
      crawlWonNodes.put(nodeInfo.getWonNodeURI(), con);
    }

    // initialize the won nodes to crawl or skip
    for (String nodeUri : settings.WON_NODES_CRAWL) {
      if (!skipWonNodeUris.contains(nodeUri)) {
        if (!crawlWonNodes.containsKey(nodeUri)) {
          // add the won node uri that we still have to crawl to the mailbox of this actor
          getSelf().tell(new CrawlUriMessage(nodeUri, nodeUri, nodeUri, CrawlUriMessage.STATUS.PROCESS), getSelf());
        }
      }
    }

    // initialize the crawler and start crawling
    crawler = getContext().system().actorOf(
      Props.create(MasterCrawlerActor.class, getSelf()), "MasterCrawlerActor");

    for (WonNodeConnection con : crawlWonNodes.values()) {
      startCrawling(con.getWonNodeInfo());
    }
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

    if (message instanceof CrawlUriMessage) {
      CrawlUriMessage uriMsg = (CrawlUriMessage) message;
      if (uriMsg.getStatus().equals(CrawlUriMessage.STATUS.PROCESS) &&
          uriMsg.getWonNodeUri() != null && !uriMsg.getWonNodeUri().isEmpty()) {

        // continue crawling of known won nodes
        if (crawlWonNodes.containsKey(uriMsg.getWonNodeUri())) {
            log.debug("Won node uri '{}' already discovered", uriMsg.getWonNodeUri());
            getSender().tell(uriMsg, getSelf());
            return;
        }

        // skip crawling of won nodes in the skip list
        if (skipWonNodeUris.contains(uriMsg.getWonNodeUri())) {
          log.debug("Skip crawling won node with uri '{}'", uriMsg.getWonNodeUri());
          getSender().tell(skipCrawlMessage(uriMsg), getSelf());
          return;
        }

        // try the connect o won node
        addWonNodeForCrawling(uriMsg.getWonNodeUri());

        // connection failed ?
        if (failedWonNodeUris.contains(uriMsg.getWonNodeUri())) {
          log.debug("Still could not connect to won node with uri: {}", uriMsg.getWonNodeUri());
          getSender().tell(failedCrawlMessage(uriMsg), getSelf());
          return;
        }

        // crawl all new discovered won nodes
        getSender().tell(uriMsg, getSelf());
        startCrawling(crawlWonNodes.get(uriMsg.getWonNodeUri()).getWonNodeInfo());
        return;
      }
    }

    unhandled(message);
  }

  private static CrawlUriMessage skipCrawlMessage(CrawlUriMessage msg) {
    return new CrawlUriMessage(msg.getUri(), msg.getBaseUri(),
                               msg.getWonNodeUri(), CrawlUriMessage.STATUS.SKIP);
  }

  private static CrawlUriMessage failedCrawlMessage(CrawlUriMessage msg) {
    return new CrawlUriMessage(msg.getUri(), msg.getBaseUri(),
                               msg.getWonNodeUri(), CrawlUriMessage.STATUS.FAILED);
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
      sparqlService.updateDataset(ds);
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


