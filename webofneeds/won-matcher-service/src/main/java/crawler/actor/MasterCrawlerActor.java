package crawler.actor;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import akka.routing.FromConfig;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.path.Path;
import com.hp.hpl.jena.sparql.path.PathParser;
import crawler.db.SparqlEndpointAccess;
import crawler.exception.CrawlingWrapperException;
import crawler.message.UriStatusMessage;
import scala.concurrent.duration.Duration;
import won.protocol.vocabulary.WON;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Coordinates recursive crawling of linked data resources by assigning {@link crawler.message.UriStatusMessage} to
 * workers of type {@link crawler.actor.WorkerCrawlerActor}.
 * The process can be stopped at any time and continued since meta data about the crawling process is saved
 * in the SPARQL endpoint and (unfinished) messages can be resend for crawling again.
 *
 * User: hfriedrich
 * Date: 30.03.2015
 */
public class MasterCrawlerActor extends UntypedActor
{
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
  private Map<String, UriStatusMessage> pendingMessages = null;
  private ActorRef worker;
  private SparqlEndpointAccess endpoint;
  private int numBaseUrisCrawled = 0;
  private int numNonBaseUrisCrawled = 0;
  private int numFailedUris = 0;


  public MasterCrawlerActor(String sparqlEndpoint) {
    pendingMessages = new HashMap<String, UriStatusMessage>();
    endpoint = new SparqlEndpointAccess(sparqlEndpoint);
  }

  /**
   * Build the non-base property paths (connections and events) needed for crawling need data.
   * @return
   */
  private static List<Path> configureNonBasePropertyPaths(){
    List<Path> propertyPaths = new ArrayList<Path>();
    addPropertyPath(propertyPaths, "<" + WON.HAS_CONNECTIONS + ">");
    addPropertyPath(propertyPaths, "<" + WON.HAS_CONNECTIONS + ">" + "/" + "rdfs:member");
    addPropertyPath(propertyPaths, "<" + WON.HAS_CONNECTIONS + ">" + "/" + "rdfs:member" + "/<" +
      WON.HAS_EVENT_CONTAINER + ">/rdfs:member");

    return propertyPaths;
  }

  /**
   * Build the base property paths needed for crawling need data. Base can be won node which lists needs or the need
   * document itself
   * @return
   */
  private static List<Path> configureBasePropertyPaths() {
    List<Path> propertyPaths = new ArrayList<Path>();
    addPropertyPath(propertyPaths, "rdfs:member");
    addPropertyPath(propertyPaths, "<" + WON.HAS_CONNECTIONS + ">" + "/" + "rdfs:member" + "/<" +
      WON.HAS_REMOTE_NEED + ">");
    return propertyPaths;
  }

  private static void addPropertyPath(final List<Path> propertyPaths, String pathString) {
    Path path = PathParser.parse(pathString, PrefixMapping.Standard);
    propertyPaths.add(path);
  }

  @Override
  public void preStart() {

    Props workerProps = Props.create(WorkerCrawlerActor.class, endpoint.getSparqlEndpoint(),
                                     configureBasePropertyPaths(), configureNonBasePropertyPaths());
    worker = getContext().actorOf(
      new FromConfig().props(workerProps), "CrawlingRouter");
  }

  @Override
  public SupervisorStrategy supervisorStrategy() {

    SupervisorStrategy supervisorStrategy = new OneForOneStrategy(
      0, Duration.Zero(), new Function<Throwable, SupervisorStrategy.Directive>() {

      @Override
      public SupervisorStrategy.Directive apply(Throwable t) throws Exception {

        // save the failed status of a worker during crawling
        if (t instanceof CrawlingWrapperException) {
          CrawlingWrapperException e = (CrawlingWrapperException) t;
          endpoint.updateCrawlingMetadata(e.getBreakingMessage());
          log.warning("Handled breaking message: {}", e.getBreakingMessage());
          log.warning("Exception was: {}", e.getException());
          numFailedUris++;
          return SupervisorStrategy.resume();
        }

        // default behaviour in other cases
        return SupervisorStrategy.escalate();
      }
    });

    return supervisorStrategy;
  }

  /**
   * Receive {@link crawler.message.UriStatusMessage} messages and pass them over to workers for crawling.
   *
   * @param message
   * @throws Exception
   */
  @Override
  public void onReceive(final Object message) throws Exception {
    if (message instanceof UriStatusMessage) {
      UriStatusMessage uriMsg = (UriStatusMessage) message;
      if (uriMsg.getStatus().equals(UriStatusMessage.STATUS.DONE)) {
        log.debug("Successfully processed URI: {}", uriMsg.getUri());
        if (uriMsg.getUri().equals(uriMsg.getBaseUri())) {
          numBaseUrisCrawled++;
        } else {
          numNonBaseUrisCrawled++;
        }
        endpoint.updateCrawlingMetadata(uriMsg);
        pendingMessages.remove(uriMsg.getUri());
      } else if (uriMsg.getStatus().equals(UriStatusMessage.STATUS.PROCESS)) {
        process(uriMsg);
      }

      log.info("Number of pending messages: {}", pendingMessages.size());
      if (pendingMessages.isEmpty()) {
        log.info("Number of URIs crawled: \nBase URIs: {}\nNon-base URIs: {}\nFailed URIs: {}",
                 numBaseUrisCrawled, numNonBaseUrisCrawled, numFailedUris);
      }
    } else {
      unhandled(message);
    }
  }

  /**
   * Pass the messages to process to the workers
   *
   * @param msg
   */
  private void process(UriStatusMessage msg) {

    if(pendingMessages.get(msg.getUri()) != null) {
      log.debug("message for URI {} already sent, await answer ...", msg.getUri());
    } else {
      log.debug("crawl URI: {}", msg.getUri());
      pendingMessages.put(msg.getUri(), msg);
      endpoint.updateCrawlingMetadata(msg);
      worker.tell(msg, getSelf());
    }
  }
}
