package won.matcher.siren;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import akka.actor.Props;
import com.hp.hpl.jena.query.Dataset;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import won.cryptography.ssl.TrustAnyCertificateStrategy;
import won.matcher.service.common.actor.DeadLetterActor;
import won.matcher.service.common.event.NeedEvent;
import won.matcher.service.common.spring.SpringExtension;
import won.matcher.siren.actor.SirenMatcherActor;
import won.matcher.siren.spring.MatcherSirenAppConfiguration;
import won.protocol.rest.LinkedDataRestClientHttpsServerOnly;
import won.protocol.util.linkeddata.LinkedDataSourceBase;

import java.io.IOException;
import java.net.URI;

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

    // if https is used, use https URIs and HTTPS rest client:
    //String wonUri = "https://satsrv05.researchstudio.at:8889/won/resource";
    //String testUri = "https://satsrv05.researchstudio.at:8889/won/resource/need/xbuwdvqk7nkheydlfzwp";
    LinkedDataSourceBase linkedDataSource = new LinkedDataSourceBase();
    LinkedDataRestClientHttpsServerOnly restClient = new LinkedDataRestClientHttpsServerOnly(new TrustAnyCertificateStrategy());
    restClient.initialize();
    linkedDataSource.setLinkedDataRestClient(restClient);

    Dataset ds = linkedDataSource.getDataForResource(URI.create(testUri));
    NeedEvent needEvent = new NeedEvent(testUri, wonUri, NeedEvent.TYPE.CREATED, ds);

    // send event to matcher implementation
    sirenMatcherActor.tell(needEvent, null);
  }
}
