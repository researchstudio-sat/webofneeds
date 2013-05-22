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

  public static String format(Date date) {
    sdf = new SimpleDateFormat("YYYY-MM-DD'T'hh:mm:ss.sss");
    return sdf.format(date);
  }

  public  static String getCurrentDateTimeStamp() {
    sdf = new SimpleDateFormat("YYYY-MM-DD'T'hh:mm:ss.sss");
    return sdf.format(new Date());
  }

  //TODO: review method
  public static Date parse(String date) {
    sdf = new SimpleDateFormat("YYYY-MM-DD'T'hh:mm:ss.sss");
    try{
      return sdf.parse(date);
    } catch (ParseException e) {
      logger.warn("caught ParseException:", e);
    } finally {
      return null;
    }

  }
}
