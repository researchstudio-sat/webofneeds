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
public class TimeRangeFilterQuery extends AbstractQuery
{
  private String lowerBoundField;
  private String upperBoundField;

  public TimeRangeFilterQuery(BooleanClause.Occur occur, String lowerBoundField, String upperBoundField)
  {
    super(occur);
    this.lowerBoundField = lowerBoundField;
    this.upperBoundField = upperBoundField;
  }

  public Query getQuery(SolrIndexSearcher indexSearcher, SolrInputDocument inputDocument)
  {
    if (!inputDocument.containsKey(lowerBoundField) || !inputDocument.containsKey(upperBoundField))
      return null;

    long lower = Long.parseLong(inputDocument.getField(lowerBoundField).toString());
    long upper = Long.parseLong(inputDocument.getField(upperBoundField).getValue().toString());

    Query nq1 = NumericRangeQuery.newLongRange(lowerBoundField, lower, upper, true, true);
    Query nq2 = NumericRangeQuery.newLongRange(upperBoundField, lower, upper, true, true);

    BooleanQuery query = new BooleanQuery();

    //one of the two query must match at least one document
    query.add(nq1, BooleanClause.Occur.SHOULD);
    query.add(nq2, BooleanClause.Occur.SHOULD);

    return query;
  }
}
