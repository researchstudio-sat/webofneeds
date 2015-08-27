package common.event;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

/**
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
}