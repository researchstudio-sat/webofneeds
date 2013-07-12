package won.protocol.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * User: atus
 * Date: 23.04.13
 */
public class DateTimeUtils
{
  private static final Logger logger = LoggerFactory.getLogger(DateTimeUtils.class);
  private static SimpleDateFormat sdf;
  private static final String DATE_FORMAT_XSD_DATE_TIME_STAMP = "yyyy-MM-DD'T'hh:mm:ss.sssZ";

  /**
   * Formats the date as xsd:dateTimeStamp (time stamp with timezone info).
   * @param date
   * @return
   */
  public static String format(Date date) {
    sdf = new SimpleDateFormat(DATE_FORMAT_XSD_DATE_TIME_STAMP);
    return sdf.format(date);
  }

  /**
   * Returns the current date as xsd:dateTimeStamp (time stamp with timezone info).
   * @return
   */
  public  static String getCurrentDateTimeStamp() {
    return format(new Date());
  }

  /**
   * Parses the specified date, which is expected to be an xsd:dateTimeStamp (time stamp with timezone info).
   * @param date
   * @return the date or null if the format is not recognized
   */
  public static Date parse(String date) {
    sdf = new SimpleDateFormat(DATE_FORMAT_XSD_DATE_TIME_STAMP);
    try{
      return sdf.parse(date);
    } catch (ParseException e) {
      logger.warn("caught ParseException:", e);
    } finally {
      return null;
    }
  }
}
