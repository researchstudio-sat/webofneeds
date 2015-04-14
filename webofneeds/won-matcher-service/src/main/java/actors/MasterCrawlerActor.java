package actors;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.path.Path;
import com.hp.hpl.jena.sparql.path.PathParser;
import messages.UriActionMessage;
import won.protocol.vocabulary.WON;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Coordinates Crawling of linked data by assigning URIs to crawl to workers of type {@link actors.WorkerCrawlerActor}.
 *
 * User: hfriedrich
 * Date: 30.03.2015
 */
public class MasterCrawlerActor extends UntypedActor
{
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
  private Map<String, UriActionMessage> pendingMessages = null;
  private ActorRef worker = null;
  private int numBaseUrisCrawled = 0;
  private int numNonBaseUrisCrawled = 0;

  public MasterCrawlerActor() {
    pendingMessages = new HashMap<String, UriActionMessage>();
  }

  /**
   * Build the non-base property paths needed for crawling need data
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
   * Build the base property paths needed for crawling need data
   * @return
   */
  private static List<Path> configureBasePropertyPaths(){
    List<Path> propertyPaths = new ArrayList<Path>();
    addPropertyPath(propertyPaths, "<" + WON.HAS_CONNECTIONS + ">" + "/" + "rdfs:member" + "/<" +
      WON.HAS_REMOTE_NEED + ">");
    return propertyPaths;
  }

  private static List<Path> configurePropertyPathAll(){
    List<Path> propertyPaths = new ArrayList<Path>();
    addPropertyPath(propertyPaths, "rdfs:member");
    addPropertyPath(propertyPaths, "rdfs:member/" + "<" + WON.HAS_CONNECTIONS + ">");
    addPropertyPath(propertyPaths, "rdfs:member/" + "<" + WON.HAS_CONNECTIONS + ">" + "/" + "rdfs:member");
    addPropertyPath(propertyPaths, "rdfs:member/" + "<" + WON.HAS_CONNECTIONS + ">" + "/" + "rdfs:member" + "/<" + WON.HAS_REMOTE_CONNECTION + ">");
    addPropertyPath(propertyPaths, "rdfs:member/" + "<" + WON.HAS_CONNECTIONS + ">" + "/" + "rdfs:member" + "/<" + WON.HAS_EVENT_CONTAINER + ">/rdfs:member");
    addPropertyPath(propertyPaths, "rdfs:member/" + "<" + WON.HAS_CONNECTIONS + ">" + "/" + "rdfs:member" + "/<" + WON.HAS_REMOTE_CONNECTION + ">/<" +WON.BELONGS_TO_NEED + ">");
    return propertyPaths;
  }

  private static void addPropertyPath(final List<Path> propertyPaths, String pathString) {
    Path path = PathParser.parse(pathString, PrefixMapping.Standard);
    propertyPaths.add(path);
  }

  @Override
  public void preStart() {

    worker = getContext().actorOf(Props.create(WorkerCrawlerActor.class,
        "http://localhost:9999/bigdata/namespace/needtest1/sparql", configureBasePropertyPaths(),
        configureNonBasePropertyPaths()), "WorkerCrawlerActor");

    String uri = "http://rsa021.researchstudio.at:8080/won/resource/need/y1mjzvzlh8avwl6m2tre";
    UriActionMessage msg = new UriActionMessage(uri, uri, UriActionMessage.ACTION.PROCESS);
    process(msg);
  }

  @Override
  public void onReceive(final Object message) throws Exception {
    if (message instanceof UriActionMessage) {
      UriActionMessage actionMsg = (UriActionMessage) message;
      if (actionMsg.getAction().equals(UriActionMessage.ACTION.REMOVE)) {
        log.debug("Successfully processed URI: {}", actionMsg.getUri());
        if (actionMsg.getUri().equals(actionMsg.getBaseUri())) {
          numBaseUrisCrawled++;
        } else {
          numNonBaseUrisCrawled++;
        }
        pendingMessages.remove(actionMsg.getUri());
      } else if (actionMsg.getAction().equals(UriActionMessage.ACTION.PROCESS)) {
        process(actionMsg);
      }

      log.info("Number of pending messages: {}", pendingMessages.size());
      if (pendingMessages.isEmpty()) {
        log.info("Number of URIs crawled: \n Base URIs: {}\n Non-base URIs: {}", numBaseUrisCrawled, numNonBaseUrisCrawled);
        getContext().system().shutdown();
      }
    } else {
      unhandled(message);
    }
  }

  private void process(UriActionMessage msg) {

    if(pendingMessages.get(msg.getUri()) != null) {
      log.debug("message for URI {} already sent, await answer ...");
    } else {
      log.debug("crawl URI: {}", msg.getUri());
      pendingMessages.put(msg.getUri(), msg);
      worker.tell(msg, getSelf());
    }
  }


}
