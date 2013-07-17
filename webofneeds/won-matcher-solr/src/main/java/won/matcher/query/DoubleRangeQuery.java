package won.matcher.query;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.search.SolrIndexSearcher;

/**
 * User: gabriel
 * Date: 03.07.13
 * Time: 12:56
 */
public class DoubleRangeQuery extends AbstractQuery
{
  private String lowerBoundField;
  private String upperBoundField;

  /**
   * Infinite Range Querys problem should not be solved here
   * public enum Infinite { UPPER_BOUND, LOWER_BOUND }
   * <p/>
   * private String boundField;
   * private Infinite boundType;
   * <p/>
   * public DoubleRangeQuery(BooleanClause.Occur occur, String boundField, Infinite boundType) {
   * super(occur);
   * <p/>
   * this.boundField = boundField;
   * this.boundType = boundType;
   * }
   */

  public DoubleRangeQuery(BooleanClause.Occur occur, String lowerBoundField, String upperBoundField)
  {
    super(occur);
    this.lowerBoundField = lowerBoundField;
    this.upperBoundField = upperBoundField;
  }

  public Query getQuery(SolrIndexSearcher indexSearcher, SolrInputDocument inputDocument)
  {

    if (!inputDocument.containsKey(lowerBoundField) || !inputDocument.containsKey(upperBoundField))
      return null;

    double lower = getField(inputDocument, lowerBoundField);
    double upper = getField(inputDocument, upperBoundField);

    Query nq1 = NumericRangeQuery.newDoubleRange(lowerBoundField, lower, upper, true, true);
    Query nq2 = NumericRangeQuery.newDoubleRange(upperBoundField, lower, upper, true, true);

    BooleanQuery query = new BooleanQuery();

    //one of the two query must match at least one document
    query.add(nq1, BooleanClause.Occur.SHOULD);
    query.add(nq2, BooleanClause.Occur.SHOULD);

    return query;
  }

  private double getField(SolrInputDocument inputDocument, String fieldName)
  {
    return Double.parseDouble(inputDocument.getFieldValue(fieldName).toString());
  }
}
