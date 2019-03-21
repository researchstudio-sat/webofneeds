package won.matcher.sparql;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import won.matcher.service.common.spring.SpringExtension;
import won.matcher.sparql.actor.MatcherPubSubActor;
import won.matcher.sparql.spring.MatcherSparqlAppConfiguration;

import java.io.IOException;

/**
 * Created by hfriedrich on 24.08.2015.
 */
public class AkkaSparqlMain {

  public static void main(String[] args) throws IOException {

    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
        MatcherSparqlAppConfiguration.class);
    ActorSystem system = ctx.getBean(ActorSystem.class);
    ActorRef matcherPubSubActor = system
        .actorOf(SpringExtension.SpringExtProvider.get(system).props(MatcherPubSubActor.class), "MatcherPubSubActor");
  }
}
