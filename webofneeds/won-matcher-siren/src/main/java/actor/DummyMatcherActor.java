package actor;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import common.event.BulkHintEvent;
import common.event.HintEvent;
import common.event.NeedEvent;
import config.SirenMatcherConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by hfriedrich on 06.10.2015.
 *
 * Simplest kind of matcher implementation just for testing purpose
 */
@Component
@Scope("prototype")
public class DummyMatcherActor extends UntypedActor
{
  private LoggingAdapter log = Logging.getLogger(getContext().system(), this);
  private List<NeedEvent> needs = new LinkedList<>();

  @Autowired
  private SirenMatcherConfig config;

  @Override
  public void onReceive(final Object o) throws Exception {

    if (o instanceof NeedEvent) {

      // Simplest kind of dummy matching - create just max hints with latest needs created
      NeedEvent needEvent = (NeedEvent) o;
      BulkHintEvent bulkHintEvent = new BulkHintEvent();
      int startIndex = ((needs.size() - config.getMaxHints()) > 0 ) ? (int) (needs.size() - config.getMaxHints()) : 0;
      for (NeedEvent need : needs.subList(startIndex , needs.size())) {
        HintEvent hintEvent = new HintEvent(needEvent.getWonNodeUri(), needEvent.getUri(), need.getWonNodeUri(),
                                            need.getUri(), config.getSolrServerPublicUri(), 1.0);
        bulkHintEvent.addHintEvent(hintEvent);
      }
      needs.add(needEvent);
      log.info("Create {} hints for need {}", bulkHintEvent.getHintEvents().size(), needEvent);
      getSender().tell(bulkHintEvent, getSelf());
    }
  }
}
