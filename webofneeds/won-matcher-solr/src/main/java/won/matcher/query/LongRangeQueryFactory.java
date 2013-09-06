package won.matcher.query;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.search.SolrIndexSearcher;

/**
 * User: gabriel
 * Date: 02.07.13
 * Time: 16:12
 */
public class LongRangeQueryFactory extends AbstractQueryFactory
{
  protected String lowerBoundField;
  protected String upperBoundField;

  public LongRangeQueryFactory(BooleanClause.Occur occur, float boost, String lowerBoundField, String upperBoundField)
  {
    super(occur, boost);
    this.lowerBoundField = lowerBoundField;
    this.upperBoundField = upperBoundField;
  }

  public LongRangeQueryFactory(final BooleanClause.Occur occur, final String lowerBoundField, final String upperBoundField)
  {
    super(occur);
    this.lowerBoundField = lowerBoundField;
    this.upperBoundField = upperBoundField;
  }

  public Query createQuery(SolrIndexSearcher indexSearcher, SolrInputDocument inputDocument)
  {
    if (!inputDocument.containsKey(lowerBoundField) && !inputDocument.containsKey(upperBoundField))
      return null;

    Long lower = getField(inputDocument, lowerBoundField);
    Long upper = getField(inputDocument, upperBoundField);

    Query nq1 = NumericRangeQuery.newLongRange(lowerBoundField, lower, upper, true, true);
    Query nq2 = NumericRangeQuery.newLongRange(upperBoundField, lower, upper, true, true);

    BooleanQuery query = new BooleanQuery();

    //one of the two query must match at least one document
    query.add(nq1, BooleanClause.Occur.SHOULD);
    query.add(nq2, BooleanClause.Occur.SHOULD);

    return query;
  }

  protected Long getField(SolrInputDocument inputDocument, String fieldName)
  {
    if (inputDocument.containsKey(fieldName))
      return Long.parseLong(inputDocument.getFieldValue(fieldName).toString());
    else return null;
  }
}
