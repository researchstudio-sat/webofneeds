import actor.RescalMatcherActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import akka.actor.Props;
import common.actor.DeadLetterActor;

import java.io.IOException;

/**
 * User: hfriedrich
 * Date: 13.06.2015
 */
public class TestMain
{
  public static void main(String[] args) throws IOException {

//    String endpoint = "http://localhost:9999/bigdata/namespace/needtest3/sparql";
//    RescalSparqlService sparqlService = new RescalSparqlService(endpoint);
//    RescalMatchingData data = new RescalMatchingData();
//
//    BulkHintEvent e = HintReader.readHints("C:/dev/temp/tensor");
//    sparqlService.updateMatchingDataWithActiveNeeds(data, 0, 10000000000000l);
//    sparqlService.updateMatchingDataWithConnections(data, 0, 10000000000000l);
//    data.writeCleanedOutputFiles("C:/dev/temp/tensor");

    ActorSystem system = ActorSystem.create("RescalMatcherActorSystem");
    ActorRef actor = system.actorOf(Props.create(DeadLetterActor.class), "DeadLetterActor");
    system.eventStream().subscribe(actor, DeadLetter.class);
    ActorRef rescal = system.actorOf(Props.create(RescalMatcherActor.class), "RescalMatcherActor");
    rescal.tell("start", null);

  }

}
