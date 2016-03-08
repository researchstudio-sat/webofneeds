package won.bot.framework.events.action.impl;

import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import won.bot.framework.events.EventListenerContext;
import won.bot.framework.events.action.BaseEventBotAction;
import won.bot.framework.events.event.Event;
import won.bot.framework.events.event.impl.monitor.*;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class MessageLifecycleMonitoringAction extends BaseEventBotAction
{
  Map<String, Split> msgSplitsB = Collections.synchronizedMap(new HashMap<>());
  Map<String, Split> msgSplitsBC = Collections.synchronizedMap(new HashMap<>());
  Map<String, Split> msgSplitsBCD = Collections.synchronizedMap(new HashMap<>());
  Map<String, Split> msgSplitsBCDE = Collections.synchronizedMap(new HashMap<>());

  private long startTestTime = -1;

  public MessageLifecycleMonitoringAction(final EventListenerContext eventListenerContext) {
    super(eventListenerContext);
  }

  @Override
  protected void doRun(final Event event) throws Exception {

    Stopwatch stopwatchB = SimonManager.getStopwatch("messageTripB");
    Stopwatch stopwatchBC = SimonManager.getStopwatch("messageTripBC");
    Stopwatch stopwatchBCD = SimonManager.getStopwatch("messageTripBCD");
    Stopwatch stopwatchBCDE = SimonManager.getStopwatch("messageTripBCDE");

    if (event instanceof MessageSpecificEvent) {

      URI msgURI = ((MessageSpecificEvent) event).getMessageURI();
      logger.debug("RECEIVED EVENT {} for uri {}", event, msgURI);

      if (event instanceof MessageDispatchStartedEvent) {

        Split splitB = stopwatchB.start();
        Split splitBC = stopwatchBC.start();
        Split splitBCD = stopwatchBCD.start();
        Split splitBCDE = stopwatchBCDE.start();
        msgSplitsB.put(msgURI.toString(), splitB);
        msgSplitsBC.put(msgURI.toString(), splitBC);
        msgSplitsBCD.put(msgURI.toString(), splitBCD);
        msgSplitsBCDE.put(msgURI.toString(), splitBCDE);

      } else if (event instanceof MessageDispatchedEvent) {
        msgSplitsB.get(msgURI.toString()).stop();
      } else if (event instanceof MessageDeliveryResponseReceivedEvent) {
        msgSplitsBC.get(msgURI.toString()).stop();
      } else if (event instanceof MessageReceivedByCounterpartEvent) {
        msgSplitsBCD.get(msgURI.toString()).stop();
      } else if (event instanceof MessageDeliveryRemoteResponseReceivedEvent) {
        msgSplitsBCDE.get(msgURI.toString()).stop();
      }

    }

  }

}
