package node.actor;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.hp.hpl.jena.query.Dataset;
import commons.service.HttpRequestService;
import crawler.msg.CrawlUriMessage;
import node.config.ActiveMqNeedConsumerFactory;
import node.config.WonNodeControllerSettings;
import node.config.WonNodeControllerSettingsImpl;
import node.pojo.WonNodeConnection;
import node.service.WonNodeSparqlService;
import won.protocol.service.WonNodeInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


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
  private HttpRequestService httpRequestService;
  private Map<String, WonNodeInfo> crawlWonNodes;
  private Set<String> skipWonNodeUris;
  private WonNodeSparqlService sparqlService;
  private WonNodeControllerSettingsImpl settings;

  public WonNodeControllerActor() {

    settings = WonNodeControllerSettings.SettingsProvider.get(getContext().system());
    httpRequestService = new HttpRequestService();
    sparqlService = new WonNodeSparqlService(settings.SPARQL_ENDPOINT);
    crawlWonNodes = new HashMap<>();
    skipWonNodeUris = new HashSet<>();
  }

  @Override
  public void preStart() {

    skipWonNodeUris.addAll(settings.WON_NODES_SKIP);

    // get all known won node uris
    Set<WonNodeInfo> wonNodeInfo = sparqlService.retrieveAllWonNodeInfo();
    for (WonNodeInfo nodeInfo : wonNodeInfo) {
      crawlWonNodes.put(nodeInfo.getWonNodeURI(), nodeInfo);
      subscribeNeedUpdates(nodeInfo);
    }

    // initialize the won nodes to crawl or skip
    for (String nodeUri : settings.WON_NODES_CRAWL) {
      if (!skipWonNodeUris.contains(nodeUri)) {
        if (!crawlWonNodes.containsKey(nodeUri)) {
          addWonNodeForCrawling(nodeUri);
        }
      }
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

    if (message instanceof CrawlUriMessage) {
      CrawlUriMessage uriMsg = (CrawlUriMessage) message;
      if (uriMsg.getStatus().equals(CrawlUriMessage.STATUS.PROCESS) &&
          uriMsg.getWonNodeUri() != null && !uriMsg.getWonNodeUri().isEmpty()) {

        if (crawlWonNodes.containsKey(uriMsg.getWonNodeUri())) {
          log.debug("Won node uri '{}' already discovered", uriMsg.getWonNodeUri());
          getSender().tell(uriMsg, getSelf());
          return;
        }

        if (skipWonNodeUris.contains(uriMsg.getWonNodeUri())) {
          log.debug("Skip crawling won node with uri '{}'", uriMsg.getWonNodeUri());
          getSender().tell(skipCrawlMessage(uriMsg), getSelf());
          return;
        }

        // crawl all new discovered won nodes
        addWonNodeForCrawling(uriMsg.getWonNodeUri());
        getSender().tell(uriMsg, getSelf());
        return;
      }
    }

    unhandled(message);
  }

  private static CrawlUriMessage skipCrawlMessage(CrawlUriMessage msg) {
    return new CrawlUriMessage(msg.getUri(), msg.getBaseUri(),
                               msg.getWonNodeUri(), CrawlUriMessage.STATUS.SKIP);
  }

  private void addWonNodeForCrawling(String wonNodeUri) {

    // request the resource and save the data
    Dataset ds = httpRequestService.requestDataset(wonNodeUri);
    sparqlService.updateDataset(ds);
    WonNodeInfo nodeInfo = sparqlService.getWonNodeInfoFromDataset(ds);
    crawlWonNodes.put(nodeInfo.getWonNodeURI(), nodeInfo);

    // subscribe for need updates
    subscribeNeedUpdates(nodeInfo);
  }

  private WonNodeConnection subscribeNeedUpdates(WonNodeInfo wonNodeInfo) {
    return ActiveMqNeedConsumerFactory.createWonNodeConnection(getContext(), wonNodeInfo);
  }

}


