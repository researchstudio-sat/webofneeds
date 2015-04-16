import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import akka.actor.Props;
import crawler.actor.MasterCrawlerActor;
import crawler.db.SparqlEndpointAccess;
import crawler.message.UriStatusMessage;

import java.io.IOException;
import java.util.Set;

/**
 * User: hfriedrich
 * Date: 27.03.2015
 */
public class AkkaSystemMain
{

  public static void main(String[] args) throws IOException {

    String endpointURI = "http://localhost:9999/bigdata/namespace/needtest2/sparql";

    ActorSystem system = ActorSystem.create("AkkaMatchingService");
    ActorRef master = system.actorOf(
      Props.create(MasterCrawlerActor.class, endpointURI),
      "MasterCrawlerActor");
    ActorRef actor = system.actorOf(Props.create(DeadLetterActor.class), "DeadLetterActor");
    system.eventStream().subscribe(actor, DeadLetter.class);

    // (re-)start crawling
    SparqlEndpointAccess endpoint = new SparqlEndpointAccess(endpointURI);
    String uri = "http://rsa021.researchstudio.at:8080/won/resource/need/";
    uri = "http://rsa021.researchstudio.at:8080/won/resource/need/y1mjzvzlh8avwl6m2tre";
    endpoint.updateCrawlingMetadata(new UriStatusMessage(uri, uri, UriStatusMessage.STATUS.PROCESS));
    Set<UriStatusMessage> msgs = endpoint.getMessagesForCrawling(UriStatusMessage.STATUS.FAILED);
    msgs.addAll(endpoint.getMessagesForCrawling(UriStatusMessage.STATUS.PROCESS));

    for (UriStatusMessage msg : msgs) {
      master.tell(msg, master);
    }
  }

}
