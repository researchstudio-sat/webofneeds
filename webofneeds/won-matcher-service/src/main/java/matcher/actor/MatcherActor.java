package matcher.actor;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import common.event.NeedEvent;

/**
 * User: hfriedrich
 * Date: 04.06.2015
 */
public class MatcherActor extends UntypedActor
{
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

  @Override
  public void preStart() {
    getContext().system().eventStream().subscribe(getSelf(), NeedEvent.class);
  }

  @Override
  public void onReceive(final Object message) throws Exception {

    if (message instanceof NeedEvent) {
      log.info("need event received");
    }
  }
}
