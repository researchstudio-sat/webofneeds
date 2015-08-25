package actor;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import common.event.BulkHintEvent;
import common.event.HintEvent;
import common.event.NeedEvent;

/**
 * Created by hfriedrich on 24.08.2015.
 */
public class SirenMatcherActor extends UntypedActor
{
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

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


    // TODO: then send these hints back to sender
    // dummy code
    HintEvent hintEvent1 = new HintEvent("uri1", "uri2", 0.0);
    HintEvent hintEvent2 = new HintEvent("uri3", "uri4", 0.0);
    getSender().tell(hintEvent1, getSelf());

    BulkHintEvent bulkHintEvent = new BulkHintEvent();
    bulkHintEvent.addHintEvent(hintEvent1);
    bulkHintEvent.addHintEvent(hintEvent2);
    getSender().tell(bulkHintEvent, getSelf());
  }

}
