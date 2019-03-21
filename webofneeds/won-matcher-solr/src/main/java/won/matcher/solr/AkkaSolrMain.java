package won.matcher.solr;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import won.matcher.service.common.spring.SpringExtension;
import won.matcher.solr.actor.MatcherPubSubActor;
import won.matcher.solr.spring.MatcherSolrAppConfiguration;

import java.io.IOException;

/**
 * Created by hfriedrich on 24.08.2015.
 */
public class AkkaSolrMain {

  public static void main(String[] args) throws IOException {

    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MatcherSolrAppConfiguration.class);
    ActorSystem system = ctx.getBean(ActorSystem.class);
    ActorRef matcherPubSubActor = system
        .actorOf(SpringExtension.SpringExtProvider.get(system).props(MatcherPubSubActor.class), "MatcherPubSubActor");
  }
}
