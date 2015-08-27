import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import akka.actor.Props;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import common.actor.DeadLetterActor;
import crawler.config.CrawlSettings;
import crawler.config.CrawlSettingsImpl;
import crawler.service.CrawlSparqlService;
import hint.actor.HintSendingActor;
import node.actor.WonNodeControllerActor;

import java.io.IOException;

/**
 * User: hfriedrich
 * Date: 27.03.2015
 */
public class AkkaSystemMain
{

  public static void main(String[] args) throws IOException {

    // setup Akka
    final Config config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + 2551).
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [core]")).withFallback(ConfigFactory.load());
    ActorSystem system = ActorSystem.create("ClusterSystem", config);
    CrawlSettingsImpl settings = CrawlSettings.SettingsProvider.get(system);
    CrawlSparqlService endpoint = new CrawlSparqlService(settings.METADATA_SPARQL_ENDPOINT);
    ActorRef controller = system.actorOf(Props.create(WonNodeControllerActor.class), "WonNodeControllerActor");
    ActorRef actor = system.actorOf(Props.create(DeadLetterActor.class), "DeadLetterActor");
    system.eventStream().subscribe(actor, DeadLetter.class);

    ActorRef hintSender = system.actorOf(Props.create(HintSendingActor.class), "hintSender");
//    ActorRef mediator = DistributedPubSub.get(system).mediator();
//
//    while (true) {
//
//      try {
//        Thread.sleep(5000);
//      } catch (InterruptedException e) {
//        e.printStackTrace();
//      }
//
//      NeedEvent event = new NeedEvent("uri", "uri2", NeedEvent.TYPE.CREATED);
//      mediator.tell(new DistributedPubSubMediator.Publish(NeedEvent.class.getName(), event), null);
//    }

  }

}
