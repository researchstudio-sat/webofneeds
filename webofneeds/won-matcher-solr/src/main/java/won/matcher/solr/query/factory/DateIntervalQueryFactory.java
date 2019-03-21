package won.matcher.solr.query.factory;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Created by hfriedrich on 12.08.2016.
 */
public class DateIntervalQueryFactory extends MatchFieldQueryFactory {
  public DateIntervalQueryFactory(final String fieldName, final String value) {
    super(fieldName, value);
  }

  public DateIntervalQueryFactory(final String fieldName, final ZonedDateTime dateTime1,
      final ZonedDateTime dateTime2) {

    this.fieldName = fieldName;
    this.value = "[" + dateTime1.format(DateTimeFormatter.ISO_DATE_TIME) + " TO " +
        dateTime2.format(DateTimeFormatter.ISO_DATE_TIME) + "]";
  }
}
