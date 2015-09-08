package node.actor;

import akka.actor.ActorRef;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.hp.hpl.jena.query.Dataset;
import common.event.BulkHintEvent;
import common.event.HintEvent;
import common.event.WonNodeEvent;
import common.service.HttpRequestService;
import common.spring.SpringExtension;
import crawler.actor.MasterCrawlerActor;
import crawler.msg.CrawlUriMessage;
import node.config.ActiveMqWonNodeConnectionFactory;
import node.config.WonNodeControllerConfig;
import node.pojo.WonNodeConnection;
import node.service.WonNodeSparqlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import won.protocol.service.WonNodeInfo;
import won.protocol.service.WonNodeInformationService;

import java.net.URI;
import java.util.*;


/**
 * Actor that knows all won nodes the matching service is communicating. It gets informed
 * about new won nodes over the event stream (e.g. by he crawler) and decides which
 * won nodes to crawl and to register with for receiving need events.
 *
 * User: hfriedrich
 * Date: 27.04.2015
 */
@Component
@Scope("prototype")
public class WonNodeControllerActor extends UntypedActor
{
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
  private ActorRef pubSubMediator;
  private ActorRef crawler;
  private Map<String, WonNodeConnection> crawlWonNodes = new HashMap<>();
  private Set<String> skipWonNodeUris = new HashSet<>();
  private Set<String> failedWonNodeUris = new HashSet<>();
  private static final String TICK = "tick";

  @Autowired
  private WonNodeSparqlService sparqlService;

  @Autowired
  private HttpRequestService httpRequestService;

  @Autowired
  private WonNodeControllerConfig config;

  @Autowired
  private WonNodeInformationService wonNodeInformationService;


  @Override
  public void preStart() {

    // Create a scheduler to execute the life check for each won node regularly
    getContext().system().scheduler().schedule(config.getLifeCheckDuration(), config.getLifeCheckDuration(),
                                               getSelf(), TICK, getContext().dispatcher(), null);

    // Subscribe for won node events
    pubSubMediator = DistributedPubSub.get(getContext().system()).mediator();
    pubSubMediator.tell(new DistributedPubSubMediator.Subscribe(WonNodeEvent.class.getName(), getSelf()), getSelf());

    // Subscribe for hint events
    pubSubMediator.tell(new DistributedPubSubMediator.Subscribe(HintEvent.class.getName(), getSelf()), getSelf());
    pubSubMediator.tell(new DistributedPubSubMediator.Subscribe(BulkHintEvent.class.getName(), getSelf()), getSelf());

    // set won nodes to skip by configuration
    skipWonNodeUris.addAll(config.getSkipWonNodes());

    // get all known won node uris
    Set<WonNodeInfo> wonNodeInfo = sparqlService.retrieveAllWonNodeInfo();
    for (WonNodeInfo nodeInfo : wonNodeInfo) {
      WonNodeConnection con = subscribeNeedUpdates(nodeInfo);
      crawlWonNodes.put(nodeInfo.getWonNodeURI(), con);
    }

    // initialize the won nodes to crawl
    for (String nodeUri : config.getCrawlWonNodes()) {
      if (!skipWonNodeUris.contains(nodeUri)) {
        if (!crawlWonNodes.containsKey(nodeUri)) {
          WonNodeEvent e = new WonNodeEvent(nodeUri, WonNodeEvent.STATUS.NEW_WON_NODE_DISCOVERED);
          pubSubMediator.tell(new DistributedPubSubMediator.Publish(e.getClass().getName(), e), getSelf());
        }
      }
    }

    // initialize the crawler
    crawler = getContext().actorOf(SpringExtension.SpringExtProvider.get(
      getContext().system()).props(MasterCrawlerActor.class), "MasterCrawlerActor");
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
            WonNodeEvent e = new WonNodeEvent(event.getWonNodeUri(), WonNodeEvent.STATUS.CONNECTED_TO_WON_NODE);
            pubSubMediator.tell(new DistributedPubSubMediator.Publish(e.getClass().getName(), e), getSelf());
            return;
        }

        // skip crawling of won nodes in the skip list
        if (skipWonNodeUris.contains(event.getWonNodeUri())) {
          log.debug("Skip crawling won node with uri '{}'", event.getWonNodeUri());
          WonNodeEvent e = new WonNodeEvent(event.getWonNodeUri(), WonNodeEvent.STATUS.SKIP_WON_NODE);
          pubSubMediator.tell(new DistributedPubSubMediator.Publish(e.getClass().getName(), e), getSelf());
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
        WonNodeEvent e = new WonNodeEvent(event.getWonNodeUri(), WonNodeEvent.STATUS.CONNECTED_TO_WON_NODE);
        pubSubMediator.tell(new DistributedPubSubMediator.Publish(e.getClass().getName(), e), getSelf());
        startCrawling(crawlWonNodes.get(event.getWonNodeUri()).getWonNodeInfo());
        return;
      }
    }

    // send back hints to won nodes
    if (message instanceof HintEvent) {
      sendHint((HintEvent) message);
      return;
    } else if(message instanceof BulkHintEvent) {
      BulkHintEvent bulkHintEvent = (BulkHintEvent) message;
      for (HintEvent hint : bulkHintEvent.getHintEvents()) {
        sendHint(hint);
      }
      return;
    }

    unhandled(message);
  }

  /**
   * Send hint event out to won node
   *
   * @param hint
   */
  private void sendHint(HintEvent hint) {

    if (!crawlWonNodes.containsKey(hint.getFromWonNodeUri())) {
      log.warning("cannot send hint to won node {}! Is registered with the won node controller?", hint.getFromWonNodeUri());
      return;
    }

    // send hint to first won node
    URI eventUri = wonNodeInformationService.generateEventURI(URI.create(hint.getFromWonNodeUri()));
    hint.setGeneratedEventUri(eventUri);
    WonNodeConnection fromWonNodeConnection = crawlWonNodes.get(hint.getFromWonNodeUri());
    log.debug("Send hint {} to won node {}", hint, hint.getFromWonNodeUri());
    fromWonNodeConnection.getHintProducer().tell(hint, getSelf());
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
    return ActiveMqWonNodeConnectionFactory.createWonNodeConnection(getContext(), wonNodeInfo);
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
        } else if (con.getHintProducer().equals(t.getActor())) {
          log.error("HintProducer '{}' of won '{}' has been shut down", t.getActor(), uri);
          addFailedWonNode(con.getWonNodeInfo().getWonNodeURI(), con);
        }
      }
    }
  }


}


