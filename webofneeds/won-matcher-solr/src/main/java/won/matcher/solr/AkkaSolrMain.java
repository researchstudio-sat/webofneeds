package won.matcher.solr;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import akka.actor.Props;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import won.matcher.service.common.actor.DeadLetterActor;
import won.matcher.service.common.spring.SpringExtension;
import won.matcher.solr.actor.MatcherPubSubActor;
import won.matcher.solr.spring.MatcherSolrAppConfiguration;

import java.io.IOException;

/**
 * Created by hfriedrich on 24.08.2015.
 */
public class AkkaSolrMain
{

  public static void main(String[] args) throws IOException {

    AnnotationConfigApplicationContext ctx =
      new AnnotationConfigApplicationContext(MatcherSolrAppConfiguration.class);
    ActorSystem system = ctx.getBean(ActorSystem.class);
    ActorRef matcherPubSubActor = system.actorOf(
      SpringExtension.SpringExtProvider.get(system).props(MatcherPubSubActor.class), "MatcherPubSubActor");
    ActorRef actor = system.actorOf(Props.create(DeadLetterActor.class), "DeadLetterActor");
    system.eventStream().subscribe(actor, DeadLetter.class);
  }
}
