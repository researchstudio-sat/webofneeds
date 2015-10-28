import actor.SirenMatcherActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import akka.actor.Props;
import com.hp.hpl.jena.query.Dataset;
import common.actor.DeadLetterActor;
import common.event.NeedEvent;
import common.service.http.HttpService;
import common.spring.SpringExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import spring.MatcherSirenAppConfiguration;

import java.io.IOException;

/**
 * Created by hfriedrich on 11.09.2015.
 */
public class SirenTest
{
  public static void main(String[] args) throws IOException, InterruptedException {

    // init basic Akka
    AnnotationConfigApplicationContext ctx =
      new AnnotationConfigApplicationContext(MatcherSirenAppConfiguration.class);
    ActorSystem system = ctx.getBean(ActorSystem.class);
    ActorRef sirenMatcherActor = system.actorOf(
      SpringExtension.SpringExtProvider.get(system).props(SirenMatcherActor.class), "SirenMatcherActor");
    ActorRef actor = system.actorOf(Props.create(DeadLetterActor.class), "DeadLetterActor");
    system.eventStream().subscribe(actor, DeadLetter.class);

    // Create a sample need event
    //IMPORTANT! TAKE CARE OF "RESOURCE"; DO NOT USE "PAGE" IN THE URI
    String wonUri = args[0]; // e.g. "http://satsrv07.researchstudio.at:8889/won/resource";
    String testUri = args[1]; // e.g. "http://satsrv07.researchstudio.at:8889/won/resource/need/lwxlqr555dsewtuyx2io";

    HttpService httpRequestService = new HttpService();
    Dataset ds = httpRequestService.requestDataset(testUri);
    NeedEvent needEvent = new NeedEvent(testUri, wonUri, NeedEvent.TYPE.CREATED, ds);

    // send event to matcher implementation
    sirenMatcherActor.tell(needEvent, null);
  }
}
