package actor;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import common.event.BulkHintEvent;
import common.event.HintEvent;
import common.event.NeedEvent;
import common.spring.SpringExtension;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Created by hfriedrich on 30.09.2015.
 */
@Component
@Scope("prototype")
public class MatcherPubSubActor extends UntypedActor
{
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
  private ActorRef pubSubMediator;
  private ActorRef matcherActor;

  @Override
  public void preStart() {

    // subscribe to need events
    pubSubMediator = DistributedPubSub.get(getContext().system()).mediator();
    pubSubMediator.tell(new DistributedPubSubMediator.Subscribe(NeedEvent.class.getName(), getSelf()), getSelf());

    // create the querying and indexing actors that do the actual work
    matcherActor = getContext().actorOf(SpringExtension.SpringExtProvider.get(
      getContext().system()).fromConfigProps(SirenMatcherActor.class), "SirenMatcherPool");
  }

  @Override
  public void onReceive(final Object o) throws Exception {

    if (o instanceof NeedEvent) {

      NeedEvent needEvent = (NeedEvent) o;
      log.info("NeedEvent received: " + needEvent);
      matcherActor.tell(needEvent, getSelf());

    } else if (o instanceof HintEvent) {

      HintEvent hintEvent = (HintEvent) o;
      log.info("Publish hint event: " + hintEvent);
      pubSubMediator.tell(new DistributedPubSubMediator.Publish(
        hintEvent.getClass().getName(), hintEvent), getSelf());

    } else if (o instanceof BulkHintEvent) {

      BulkHintEvent bulkHintEvent = (BulkHintEvent) o;
      log.info("Publish bulk hint event: " + bulkHintEvent);
      pubSubMediator.tell(new DistributedPubSubMediator.Publish(
        bulkHintEvent.getClass().getName(), bulkHintEvent), getSelf());

    } else {
      unhandled(o);
    }
  }
}
