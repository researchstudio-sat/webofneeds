package at.researchstudio.sat.solrintervals;

import org.apache.solr.common.SolrException;

/**
 * A long implementation of the abstract NumericIntervalType.
 * Use {@link org.apache.solr.schema.TrieLongField} as subFieldToInternal type.
 * <p/>
 * User: atus
 * Date: 11.10.13
 */
public class LongIntervalType extends NumericIntervalType<Long>
{

  @Override
  public boolean validateExternal(final String[] parts)
  {
    try {
      Long.parseLong(parts[0]);
      Long.parseLong(parts[1]);
    } catch (NumberFormatException e) {
      throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "Could not parse long", e);
    }

    return true;
  }

}
