package won.matcher.service.common.event;

import java.io.Serializable;

/**
 * Created by hfriedrich on 17.10.2016.
 *
 * Event is used to aks the crawler to load need events that were saved during a given date interval
 */
public class LoadNeedEvent implements Serializable
{
  private long fromDate;
  private long toDate;

  public LoadNeedEvent(long fromDate, long toDate) {
    this.fromDate = fromDate;
    this.toDate = toDate;
  }

  public long getFromDate() {
    return fromDate;
  }

  public long getToDate() {
    return toDate;
  }

  @Override
  public String toString() {
    return "[" + fromDate + "," + toDate + "]";
  }
}
