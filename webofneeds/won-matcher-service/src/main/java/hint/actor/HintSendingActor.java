package hint.actor;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import common.event.BulkHintEvent;
import common.event.HintEvent;

/**
 * Created by hfriedrich on 27.08.2015.
 */
public class HintSendingActor extends UntypedActor
{
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
  private ActorRef pubSubMediator;

  public HintSendingActor() {

    // subscribe to hint events
    pubSubMediator = DistributedPubSub.get(getContext().system()).mediator();
    pubSubMediator.tell(new DistributedPubSubMediator.Subscribe(HintEvent.class.getName(), getSelf()), getSelf());
    pubSubMediator.tell(new DistributedPubSubMediator.Subscribe(BulkHintEvent.class.getName(), getSelf()), getSelf());
  }

  @Override
  public void onReceive(final Object o) throws Exception {

    if (o instanceof HintEvent) {
      sendHint((HintEvent) o);
    } else if(o instanceof BulkHintEvent) {
      BulkHintEvent bulkHintEvent = (BulkHintEvent) o;
      for (HintEvent hint : bulkHintEvent.getHintEvents()) {
        sendHint(hint);
      }
    } else {
      unhandled(o);
    }
  }

  private void sendHint(HintEvent hint) {
    log.info("Received hint: " + hint);
  }

}
