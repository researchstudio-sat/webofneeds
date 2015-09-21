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
 * Created by hfriedrich on 11.09.2015.
 */
public class SirenTest
{
//  public static void main(String[] args) throws IOException {
//
//    HttpRequestService httpService = new HttpRequestService();
//    Dataset ds = httpService.requestDataset("http://satsrv04.researchstudio" +
//                                           ".at:8889/won/resource/need/3846967518561904600");
//
//    StringWriter sw = new StringWriter();
//    RDFDataMgr.write(sw, ds, RDFFormat.JSONLD);
//    String jsonData = sw.toString();
//    //httpService.postRequest("http://localhost:8983/solr/won/siren/add?commit=true", jsonData);
//    httpService.postRequest("http://192.168.59.103:8983/solr/won/siren/add?commit=true", jsonData);
//    System.out.println(sw.toString());
//  }

  public static void main(String[] args) throws IOException {

    // init basic Akka
    Config config = ConfigFactory.load();
    ActorSystem system = ActorSystem.create("AkkaMatchingService", config);
    ActorRef matcherActor = system.actorOf(Props.create(SirenMatcherActor.class), "SirenMatcherActor");

    // Create a sample need event
    //IMPORTANT! TAKE CARE OF "RESOURCE"; DO NOT USE "PAGE" IN THE URI
    String wonUri = args[0]; // e.g. "http://satsrv07.researchstudio.at:8889/won/resource";
    String testUri = args[1]; // e.g. "http://satsrv07.researchstudio.at:8889/won/resource/need/lwxlqr555dsewtuyx2io";
    HttpRequestService httpRequestService = new HttpRequestService();
    Dataset ds = httpRequestService.requestDataset(testUri);
    NeedEvent needEvent = new NeedEvent(testUri, wonUri, NeedEvent.TYPE.CREATED, ds);

    // send event to matcher implementation
    matcherActor.tell(needEvent, null);
  }
}
