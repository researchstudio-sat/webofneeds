package won.matcher.solr.query.factory;

import java.util.Date;

/**
 * Created by hfriedrich on 12.08.2016.
 */
public class DateIntervalQueryFactory extends MatchFieldQuery
{
  public DateIntervalQueryFactory(final String fieldName, final String value) {
    super(fieldName, value);
  }

  public DateIntervalQueryFactory(final String fieldName, final Date date1, final Date date2) {
    this.fieldName = fieldName;
    this.value = "[" + date1.toString() + " TO " + date2.toString() + "]";
  }
}
