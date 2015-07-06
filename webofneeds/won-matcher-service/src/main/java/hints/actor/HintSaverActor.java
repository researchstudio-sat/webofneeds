package hints.actor;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import common.event.BulkHintEvent;
import common.event.HintEvent;
import common.event.NeedEvent;

/**
 * Created by hfriedrich on 20.07.2015.
 */
public class HintSaverActor extends UntypedActor
{
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

  @Override
  public void preStart() {
    getContext().system().eventStream().subscribe(getSelf(), NeedEvent.class);
  }

  @Override
  public void onReceive(final Object message) throws Exception {

    if (message instanceof HintEvent) {
      log.info("hint event received");
    } else if (message instanceof BulkHintEvent) {
      log.info("bulk hint event received");
    } else {
      unhandled(message);
    }
  }
}
