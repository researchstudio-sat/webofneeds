package actors;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import messages.URIActionMessage;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * TODO: dummy class, will change!!!
 *
 * User: hfriedrich
 * Date: 30.03.2015
 */
public class LinkedDataCrawlerActor extends UntypedActor
{
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
  private Set<String> URIsToCrawl = null;
  private Set<String> URIsAwaitAnswer = null;
  private ActorRef uriProcessor = null;

  public LinkedDataCrawlerActor() {
    URIsToCrawl = new LinkedHashSet<String>();
    URIsAwaitAnswer= new LinkedHashSet<String>();
  }

  @Override
  public void preStart() {
    URIsToCrawl.add("http://rsa021.researchstudio.at:8080/won/resource/need/y1mjzvzlh8avwl6m2tre");
    URIsToCrawl.add("http://rsa021.researchstudio.at:8080/won/resource/need/jes90tcj737s3tljn1s9");
    uriProcessor = getContext().actorOf(Props.create(ProcessLinkedDataUriActor.class,
        "http://localhost:9999/bigdata/namespace/needtest1/sparql"));
    process();
  }

  @Override
  public void onReceive(final Object message) throws Exception {
    if (message instanceof URIActionMessage) {
      URIActionMessage actionMsg = (URIActionMessage) message;
      if (actionMsg.getAction().equals(URIActionMessage.ACTION.REMOVE)) {
        log.debug("Successfully processed URI: {}", actionMsg.getUri());
        URIsAwaitAnswer.remove(actionMsg.getUri());
      }

      if (URIsAwaitAnswer.isEmpty() && URIsToCrawl.isEmpty()) {
        log.info("Nothing more to crawl");
        getContext().system().shutdown();
      }
    } else {
      unhandled(message);
    }
  }

  private void process() {
    for (String uri : URIsToCrawl) {
      URIsAwaitAnswer.add(uri);
      URIActionMessage uriMessage = new URIActionMessage(uri, URIActionMessage.ACTION.PROCESS);
      uriProcessor.tell(uriMessage, getSelf());
    }
    URIsToCrawl.clear();
  }
}
