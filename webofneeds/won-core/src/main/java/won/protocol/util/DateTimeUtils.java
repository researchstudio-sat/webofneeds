package won.protocol.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * User: atus
 * Date: 23.04.13
 */
public class DateTimeUtils
{
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
      e.printStackTrace();
    } finally {
      return null;
    }

  }
}
