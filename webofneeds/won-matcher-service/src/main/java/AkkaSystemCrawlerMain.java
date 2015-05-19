import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import akka.actor.Props;
import commons.actor.DeadLetterActor;
import crawler.actor.MasterCrawlerActor;
import crawler.config.CrawlSettings;
import crawler.config.CrawlSettingsImpl;
import crawler.msg.CrawlUriMessage;
import crawler.service.CrawlSparqlService;
import node.actor.WonNodeControllerActor;

import java.io.IOException;
import java.util.Set;

/**
 * User: hfriedrich
 * Date: 27.03.2015
 */
public class AkkaSystemCrawlerMain
{

  public static void main(String[] args) throws IOException {

    // setup Akka
    ActorSystem system = ActorSystem.create("AkkaMatchingService");
    CrawlSettingsImpl settings = CrawlSettings.SettingsProvider.get(system);
    CrawlSparqlService endpoint = new CrawlSparqlService(settings.SPARQL_ENDPOINT);
    ActorRef controller = system.actorOf(Props.create(WonNodeControllerActor.class), "WonNodeConrollerActor");
    ActorRef master = system.actorOf(
      Props.create(MasterCrawlerActor.class, controller),
      "MasterCrawlerActor");
    ActorRef actor = system.actorOf(Props.create(DeadLetterActor.class), "crawler.actor.DeadLetterActor");
    system.eventStream().subscribe(actor, DeadLetter.class);

    // read crawling URIs from cmd line
    for (String arg : args) {
      if (arg.endsWith("/")) {
        endpoint.updateCrawlingMetadata(new CrawlUriMessage(
          arg.substring(0, arg.length() - 1), arg.substring(0, arg.length() - 1), CrawlUriMessage.STATUS.PROCESS));
      } else {
        endpoint.updateCrawlingMetadata(new CrawlUriMessage(arg + "/", arg + "/", CrawlUriMessage.STATUS.PROCESS));
      }
      endpoint.updateCrawlingMetadata(new CrawlUriMessage(arg, arg, CrawlUriMessage.STATUS.PROCESS));
    }

    // (re-)start crawling
    Set<CrawlUriMessage> msgs = endpoint.retrieveMessagesForCrawling(CrawlUriMessage.STATUS.PROCESS);
    msgs.addAll(endpoint.retrieveMessagesForCrawling(CrawlUriMessage.STATUS.FAILED));
    for (CrawlUriMessage msg : msgs) {
      master.tell(msg, master);
    }
  }

}
