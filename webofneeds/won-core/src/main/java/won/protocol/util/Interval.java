package won.protocol.util;

import java.util.Date;

public class Interval
{
  final Date from;
  final Date to;

  public Interval(final Date from, final Date to)
  {
    if (from == null) {
      if (to == null) throw new IllegalArgumentException("At least one date must be specified!");
      this.from = new Date(0);
      this.to = to;
    } else if (to == null) {
      this.from = from;
      this.to = new Date(Long.MAX_VALUE);
    } else if (from.after(to)) {
      this.from = to;
      this.to = from;
    } else {
      this.to = to;
      this.from = from;
    }
  }
}