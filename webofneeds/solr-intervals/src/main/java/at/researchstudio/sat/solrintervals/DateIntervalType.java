package at.researchstudio.sat.solrintervals;

import org.apache.lucene.document.Fieldable;
import org.apache.solr.common.SolrException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * A date interval implementation of {@link NumericIntervalType} that uses ISO8601.
 * Use {@link org.apache.solr.schema.TrieLongField} as subFieldToInternal type.
 *
 * According to https://en.wikipedia.org/wiki/ISO_8601 the standard date format is <code>YY-MM-DD'T'hh:mm'Z'</code>
 * and the standard separator is <code>/</code>
 * <p/>
 * User: atus
 * Date: 15.10.13
 */
public class DateIntervalType extends LongIntervalType
{
  public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm'Z'";
  public static final String TIME_ZONE = "UTC";
  public static final String SEPARATOR = "/";

  protected SimpleDateFormat dateFormat;

  public DateIntervalType()
  {
    TimeZone tz = TimeZone.getTimeZone(TIME_ZONE);
    dateFormat = new SimpleDateFormat(DATE_FORMAT);
    dateFormat.setTimeZone(tz);
  }

  @Override
  public String toInternal(final String val)
  {
    String[] parts = val.split(SEPARATOR);
    if (parts.length != 2)
      throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "DateIntervalType has the following form: <start>/<stop>");

    try {
      long start = dateFormat.parse(parts[0]).getTime();
      long stop = dateFormat.parse(parts[1]).getTime();

      String result = String.valueOf(start) + LongIntervalType.SEPARATOR + String.valueOf(stop);

      return result;
    } catch (ParseException e) {
      throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Could not parse date", e);
    }
  }

  @Override
  protected String subFieldToInternal(final String external)
  {
    long time;
    try {
      time = dateFormat.parse(external).getTime();
    } catch (ParseException e) {
      throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Cold not parse date", e);
    }

    return String.valueOf(time);
  }

  @Override
  public String toExternal(final Fieldable f)
  {
    String internal = f.stringValue();

    String[] parts = super.splitExternal(internal);

    long start = Long.parseLong(parts[0]);
    long stop = Long.parseLong(parts[1]);

    String startDate = dateFormat.format(new Date(start));
    String stopDate = dateFormat.format(new Date(stop));

    String result = startDate + SEPARATOR + stopDate;

    return result;
  }

}
