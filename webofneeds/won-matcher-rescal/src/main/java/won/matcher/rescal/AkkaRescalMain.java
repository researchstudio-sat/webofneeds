package won.matcher.rescal;

import akka.actor.ActorSystem;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import won.matcher.rescal.actor.RescalMatcherActor;
import won.matcher.rescal.spring.MatcherRescalAppConfiguration;
import won.matcher.service.common.spring.SpringExtension;

import java.io.IOException;

/**
 * Created by hfriedrich on 15.09.2015.
 */
public class AkkaRescalMain {
  public static void main(String[] args) throws IOException {

    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
        MatcherRescalAppConfiguration.class);
    ActorSystem system = ctx.getBean(ActorSystem.class);
    system.actorOf(SpringExtension.SpringExtProvider.get(system).props(RescalMatcherActor.class), "RescalMatcherActor");
  }
}
