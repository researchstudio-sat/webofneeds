package won.matcher.service;

import java.io.IOException;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import won.matcher.service.common.spring.MatcherServiceAppConfiguration;
import won.matcher.service.common.spring.SpringExtension;
import won.matcher.service.nodemanager.actor.WonNodeControllerActor;

/**
 * User: hfriedrich Date: 27.03.2015
 */
public class AkkaSystemMain {
    public static void main(String[] args) throws IOException {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
                        MatcherServiceAppConfiguration.class);
        ActorSystem system = ctx.getBean(ActorSystem.class);
        ActorRef wonNodeControllerActor = system.actorOf(
                        SpringExtension.SpringExtProvider.get(system).props(WonNodeControllerActor.class),
                        "WonNodeControllerActor");
    }
}
