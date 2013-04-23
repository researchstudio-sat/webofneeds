package won.protocol.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * User: atus
 * Date: 23.04.13
 */
public class DateTimeUtils
{
  private static SimpleDateFormat sdf;

  static {
    sdf = new SimpleDateFormat("YYYY-MM-DD'T'hh:mm:ss.sss");
  }

  public static String format(Date date) {
    return sdf.format(date);
  }

  public  static String getCurrentDateTimeStamp() {
    return sdf.format(new Date());
  }
}
