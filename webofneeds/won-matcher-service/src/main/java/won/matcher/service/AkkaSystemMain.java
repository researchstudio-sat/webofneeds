package won.matcher.service;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import won.matcher.service.common.spring.MatcherServiceAppConfiguration;
import won.matcher.service.common.spring.SpringExtension;
import won.matcher.service.nodemanager.actor.WonNodeControllerActor;
import won.matcher.service.rematch.actor.RematchActor;

import java.io.IOException;
import java.util.Map;

/**
 * User: hfriedrich Date: 27.03.2015
 */
public class AkkaSystemMain {
    public static void main(String[] args) throws IOException {
        System.out.println("Environment: ");
        System.out.println("------------ ");
        Map<String, String> env = System.getenv();
        for (String envName : env.keySet()) {
            System.out.format("%s=%s%n",
                            envName,
                            env.get(envName));
        }
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
                        MatcherServiceAppConfiguration.class);
        ActorSystem system = ctx.getBean(ActorSystem.class);
        ActorRef wonNodeControllerActor = system.actorOf(
                        SpringExtension.SpringExtProvider.get(system).props(WonNodeControllerActor.class),
                        "WonNodeControllerActor");
        ActorRef rematchActor = system.actorOf(SpringExtension.SpringExtProvider.get(system).props(RematchActor.class),
                        "RematchActor");
    }
}
