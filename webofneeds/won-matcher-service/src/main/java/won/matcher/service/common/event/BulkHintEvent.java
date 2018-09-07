package won.matcher.service.common.event;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Event can hold multiple {@link HintEvent} objects
 *
 * User: hfriedrich
 * Date: 23.06.2015
 */
public class BulkHintEvent implements Serializable
{
  private Collection<HintEvent> hintEvents;

  public BulkHintEvent() {
    hintEvents = new LinkedList<>();
  }

  public void addHintEvent(HintEvent hintEvent) {
    hintEvents.add(hintEvent);
  }

  public Collection<HintEvent> getHintEvents() {
    return Collections.unmodifiableCollection(hintEvents);
  }
  
  public boolean hasEvent(HintEvent tmpHint) {
      //HintEvent(hintTarget.getWonNodeUri(), hintTarget.getNeedUri(), hint.getWonNodeUri(), hint.getNeedUri(), config.getMatcherUri(), 1)
      //TODO: real filter
      /*
      tmpEvent.getFromNeedUri();
      tmpEvent.getFromWonNodeUri();
      tmpEvent.getToNeedUri();
      tmpEvent.getToWonNodeUri();*/
      for(HintEvent hint : this.hintEvents) {
          if(hint.getFromNeedUri() == tmpHint.getFromNeedUri() && hint.getToNeedUri() == tmpHint.getToNeedUri()) {
              return true;
          }
      };
      return false;
  }
}