package actor;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.hp.hpl.jena.query.Dataset;
import common.event.HintEvent;
import common.event.NeedEvent;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by hfriedrich on 24.08.2015.
 */
public class SirenMatcherActor extends UntypedActor
{
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
  private ActorRef pubSubMediator;

  private List<NeedEvent> needs;

  public SirenMatcherActor() {

    // subscribe to need events
    pubSubMediator = DistributedPubSub.get(getContext().system()).mediator();
    pubSubMediator.tell(new DistributedPubSubMediator.Subscribe(NeedEvent.class.getName(), getSelf()), getSelf());

    needs = new LinkedList<>();
  }

  @Override
  public void onReceive(final Object o) throws Exception {

    if (o instanceof NeedEvent) {
      NeedEvent needEvent = (NeedEvent) o;
      log.info("NeedEvent received: " + needEvent.getUri());
      processNeedEvent(needEvent);
    } else {
      unhandled(o);
    }
  }

  private void processNeedEvent(NeedEvent needEvent) {

    // TODO: put Siren matching code here and create HintEvent or BulkHintEvent objects
    // ...
    Dataset ds = needEvent.deserializeNeedDataset();

    if (!needs.isEmpty()) {
      NeedEvent matchNeed = needs.get(needs.size()-1);
      HintEvent hintEvent1 = new HintEvent(needEvent.getWonNodeUri(), needEvent.getUri(), matchNeed.getWonNodeUri(),
                                           matchNeed.getUri(), "http://sirenmatcher", 0.1122);
      pubSubMediator.tell(new DistributedPubSubMediator.Publish(hintEvent1.getClass().getName(), hintEvent1), getSelf());
    }

    needs.add(needEvent.clone());

//    // TODO: then send these hints back to sender
//    // dummy code
//    HintEvent hintEvent1 = new HintEvent("uri1", "uri2", "uri3", "uri4", "uri5", 0.0);
//    HintEvent hintEvent2 = new HintEvent("uri11", "uri22", "uri33", "uri44", "uri55", 0.8);
//    log.info("Publish hint: " + hintEvent1);
//    pubSubMediator.tell(new DistributedPubSubMediator.Publish(hintEvent1.getClass().getName(), hintEvent1), getSelf());
//
//    BulkHintEvent bulkHintEvent = new BulkHintEvent();
//    bulkHintEvent.addHintEvent(hintEvent1.clone());
//    bulkHintEvent.addHintEvent(hintEvent2.clone());
//    log.info("Publish bulk hint event: " + bulkHintEvent);
//    pubSubMediator.tell(new DistributedPubSubMediator.Publish(bulkHintEvent.getClass().getName(), bulkHintEvent), getSelf());
  }

}
