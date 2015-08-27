package actor;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.hp.hpl.jena.query.Dataset;
import common.event.BulkHintEvent;
import common.event.HintEvent;
import common.event.NeedEvent;

/**
 * Created by hfriedrich on 24.08.2015.
 */
public class SirenMatcherActor extends UntypedActor
{
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
  private ActorRef pubSubMediator;

  public SirenMatcherActor() {

    // subscribe to need events
    pubSubMediator = DistributedPubSub.get(getContext().system()).mediator();
    pubSubMediator.tell(new DistributedPubSubMediator.Subscribe(NeedEvent.class.getName(), getSelf()), getSelf());
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

    // TODO: then send these hints back to sender
    // dummy code
    HintEvent hintEvent1 = new HintEvent("uri1", "uri2", 0.0);
    HintEvent hintEvent2 = new HintEvent("uri3", "uri4", 0.0);
    log.info("Publish hint: " + hintEvent1);
    pubSubMediator.tell(new DistributedPubSubMediator.Publish(hintEvent1.getClass().getName(), hintEvent1), getSelf());

    BulkHintEvent bulkHintEvent = new BulkHintEvent();
    bulkHintEvent.addHintEvent(hintEvent1);
    bulkHintEvent.addHintEvent(hintEvent2);
    log.info("Publish bulk hint event: " + bulkHintEvent);
    pubSubMediator.tell(new DistributedPubSubMediator.Publish(bulkHintEvent.getClass().getName(), bulkHintEvent), getSelf());
  }

}
