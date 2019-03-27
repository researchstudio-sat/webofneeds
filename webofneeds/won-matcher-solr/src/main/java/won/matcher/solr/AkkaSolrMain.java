package won.matcher.solr;

import java.io.IOException;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import won.matcher.service.common.spring.SpringExtension;
import won.matcher.solr.actor.MatcherPubSubActor;
import won.matcher.solr.spring.MatcherSolrAppConfiguration;

/**
 * Created by hfriedrich on 24.08.2015.
 */
public class AkkaSolrMain {
    public static void main(String[] args) throws IOException {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
                        MatcherSolrAppConfiguration.class);
        ActorSystem system = ctx.getBean(ActorSystem.class);
        ActorRef matcherPubSubActor = system.actorOf(
                        SpringExtension.SpringExtProvider.get(system).props(MatcherPubSubActor.class),
                        "MatcherPubSubActor");
    }
}
