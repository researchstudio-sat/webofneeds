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
//    ActorRef hintReceiver = system.actorOf(Props.create(DummyHintReceiverActor.class, matcherActor), "HintReceiverActor");
//
//    // Create a sample need event
//    String wonUri = args[0]; // e.g. "http://satsrv07.researchstudio.at:8889/won/resource";
//    String testUri = args[1]; // e.g. "http://satsrv07.researchstudio.at:8889/won/resource/need/lwxlqr555dsewtuyx2io";
//    HttpRequestService httpRequestService = new HttpRequestService();
//    Dataset ds = httpRequestService.requestDataset(testUri);
//    NeedEvent needEvent = new NeedEvent(testUri, wonUri, NeedEvent.TYPE.CREATED, ds);
//
//    // send event to matcher implementation
//    hintReceiver.tell(needEvent, null);
//    system.actorOf(Props.create(Subscriber.class), "subscriber1");
//    system.actorOf(Props.create(Subscriber.class), "subscriber2");



  }
}
