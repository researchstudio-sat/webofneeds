package won.matcher.service.common.actor;

import akka.actor.DeadLetter;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * Prints messages that could not be delivered
 *
 * User: hfriedrich
 * Date: 09.04.2015
 */
public class DeadLetterActor extends UntypedActor
{

  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

  public void onReceive(Object message) {
    if (message instanceof DeadLetter) {
      log.error("Received dead letter message {}", message);
    }
  }
}

