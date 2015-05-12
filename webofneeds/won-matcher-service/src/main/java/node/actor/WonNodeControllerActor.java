package node.actor;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.hp.hpl.jena.query.Dataset;
import commons.service.HttpRequestService;
import crawler.msg.CrawlUriMessage;
import node.config.WonNodeControllerSettings;
import node.config.WonNodeControllerSettingsImpl;
import node.service.WonNodeSparqlService;

import java.util.HashSet;
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
  private Set<String> crawlWonNodeUris;
  private Set<String> skipWonNodeUris;
  private WonNodeSparqlService sparqlService;

  public WonNodeControllerActor() {

    WonNodeControllerSettingsImpl settings =
      WonNodeControllerSettings.SettingsProvider.get(getContext().system());
    crawlWonNodeUris = new HashSet<>();
    crawlWonNodeUris.addAll(settings.WON_NODES_CRAWL);
    skipWonNodeUris = new HashSet<>();
    skipWonNodeUris.addAll(settings.WON_NODES_SKIP);
    httpRequestService = new HttpRequestService();
    sparqlService = new WonNodeSparqlService(settings.SPARQL_ENDPOINT);
  }

  @Override
  public void preStart() {

    // get all known won node uris
    Set<String> wonNodeUris = sparqlService.retrieveWonNodeUris();

    // initialize the won nodes to crawl or skip
    for (String nodeUri : crawlWonNodeUris) {
      if (!skipWonNodeUris.contains(nodeUri)) {
        if (!wonNodeUris.contains(nodeUri)) {
          addWonNodeForCrawling(nodeUri);
        } else {
          crawlWonNodeUris.add(nodeUri);
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

        if (crawlWonNodeUris.contains(uriMsg.getWonNodeUri())) {
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
    Dataset ds = httpRequestService.requestDataset(wonNodeUri);
    sparqlService.updateDataset(ds);
    crawlWonNodeUris.add(wonNodeUri);
  }

}
