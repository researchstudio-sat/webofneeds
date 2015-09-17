package actor;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import common.event.BulkHintEvent;
import common.event.HintEvent;
import common.event.NeedEvent;

/**
 * Created by hfriedrich on 25.08.2015.
 */
public class DummyHintReceiverActor extends UntypedActor
{
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
  private ActorRef matcherActor;

  public DummyHintReceiverActor(ActorRef matcherActor) {
    this.matcherActor = matcherActor;
  }

  @Override
  public void onReceive(final Object o) throws Exception {

    if (o instanceof NeedEvent) {
      log.info("forward need event to matcher implementation: " + ((NeedEvent) o).getUri());
      matcherActor.tell(o, getSelf());
    } else if (o instanceof HintEvent) {
      HintEvent e = (HintEvent) o;
      log.info("received hint event: " + e.getFromNeedUri() + ", " + e.getToNeedUri() + ", " + e.getScore());
    } else if (o instanceof BulkHintEvent) {
      log.info("received bulk hint event: " + o);
      BulkHintEvent b = (BulkHintEvent) o;
      for (HintEvent e : b.getHintEvents()) {
        log.info("hint event: " + e.getFromNeedUri() + ", " + e.getToNeedUri() + ", " + e.getScore());
      }
    } else {
      unhandled(o);
    }
  }
}
