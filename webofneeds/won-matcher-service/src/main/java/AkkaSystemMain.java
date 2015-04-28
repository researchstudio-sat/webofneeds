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

    String endpointURI = args[0];
    String uri = args[1];
    SparqlEndpointAccess endpoint = new SparqlEndpointAccess(endpointURI);

    ActorSystem system = ActorSystem.create("AkkaMatchingService");
    ActorRef master = system.actorOf(
      Props.create(MasterCrawlerActor.class, endpointURI),
      "MasterCrawlerActor");
    ActorRef actor = system.actorOf(Props.create(DeadLetterActor.class), "DeadLetterActor");
    system.eventStream().subscribe(actor, DeadLetter.class);

    // (re-)start crawling
    endpoint.updateCrawlingMetadata(new UriStatusMessage(uri, uri, UriStatusMessage.STATUS.PROCESS));
    Set<UriStatusMessage> msgs = endpoint.getMessagesForCrawling(UriStatusMessage.STATUS.PROCESS);
    msgs.addAll(endpoint.getMessagesForCrawling(UriStatusMessage.STATUS.FAILED));

    for (UriStatusMessage msg : msgs) {
      master.tell(msg, master);
    }
  }

}
