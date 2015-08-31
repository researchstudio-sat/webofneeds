package main.java;

import actor.DummyHintReceiverActor;
import actor.SirenMatcherActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.hp.hpl.jena.query.Dataset;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import common.event.NeedEvent;
import common.service.HttpRequestService;

import java.io.IOException;

/**
 * Created by hfriedrich on 24.08.2015.
 */
public class AkkaSirenMain
{

  public static void main(String[] args) throws IOException {

    // init basic Akka
    //Config config = ConfigFactory.load();
    final Config config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + 0).
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [matcher]")).
                                         withFallback(ConfigFactory.load());
    ActorSystem system = ActorSystem.create("ClusterSystem", config);
    ActorRef matcherActor = system.actorOf(Props.create(SirenMatcherActor.class), "SirenMatcherActor");

  }
}
