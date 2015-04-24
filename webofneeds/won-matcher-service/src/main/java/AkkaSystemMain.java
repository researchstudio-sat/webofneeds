import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import akka.actor.Props;
import crawler.actor.DeadLetterActor;
import crawler.actor.MasterCrawlerActor;
import crawler.config.Settings;
import crawler.config.SettingsImpl;
import crawler.db.SparqlEndpointService;
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

    // setup Akka
    ActorSystem system = ActorSystem.create("AkkaMatchingService");
    SettingsImpl settings = Settings.SettingsProvider.get(system);
    SparqlEndpointService endpoint = new SparqlEndpointService(settings.SPARQL_ENDPOINT);
    ActorRef master = system.actorOf(
      Props.create(MasterCrawlerActor.class),
      "MasterCrawlerActor");
    ActorRef actor = system.actorOf(Props.create(DeadLetterActor.class), "crawler.actor.DeadLetterActor");
    system.eventStream().subscribe(actor, DeadLetter.class);

    // read crawling URIs from cmd line
    for (String arg : args) {
      endpoint.updateCrawlingMetadata(new UriStatusMessage(arg, arg, UriStatusMessage.STATUS.PROCESS));
    }

    // (re-)start crawling
    Set<UriStatusMessage> msgs = endpoint.getMessagesForCrawling(UriStatusMessage.STATUS.PROCESS);
    msgs.addAll(endpoint.getMessagesForCrawling(UriStatusMessage.STATUS.FAILED));
    for (UriStatusMessage msg : msgs) {
      master.tell(msg, master);
    }
  }

}
