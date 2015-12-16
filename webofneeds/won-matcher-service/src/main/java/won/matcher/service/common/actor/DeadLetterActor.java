package won.matcher.service.common.actor;

import akka.actor.DeadLetter;
import akka.actor.UntypedActor;

/**
 * Prints messages that could not be delivered
 *
 * User: hfriedrich
 * Date: 09.04.2015
 */
public class DeadLetterActor extends UntypedActor
{
  public void onReceive(Object message) {
    if (message instanceof DeadLetter) {
      System.err.println(message);
    }
  }
}

