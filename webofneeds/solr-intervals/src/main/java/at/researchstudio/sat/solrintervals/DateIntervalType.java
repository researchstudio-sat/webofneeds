package at.researchstudio.sat.solrintervals;

/**
 * A date interval implementation of {@link NumericIntervalType} that uses ISO8601.
 * Use {@link org.apache.solr.schema.TrieLongField} as subFieldToInternal type.
 *
 * According to ISO_8601 the standard separator between two dates is a forward slash <code>/</code>
 * <p/>
 * User: atus
 * Date: 15.10.13
 */
public class DateIntervalType extends NumericIntervalType
{

  public String getDefaultSeparator() {
    return "/";
  }

}
