package won.matcher.service.common.event;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Event can hold multiple {@link HintEvent} objects
 *
 * User: hfriedrich Date: 23.06.2015
 */
public class BulkHintEvent implements Serializable {
  private Collection<HintEvent> hintEvents;

  public BulkHintEvent() {
    hintEvents = new LinkedList<>();
  }

  public void addHintEvent(HintEvent hintEvent) {
    hintEvents.add(hintEvent);
  }

  public void addHintEvents(Collection<HintEvent> hintEvents) {
    this.hintEvents.addAll(hintEvents);
  }

  public Collection<HintEvent> getHintEvents() {
    return Collections.unmodifiableCollection(hintEvents);
  }
}