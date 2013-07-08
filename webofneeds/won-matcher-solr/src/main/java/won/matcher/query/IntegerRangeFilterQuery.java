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
public class IntegerRangeFilterQuery extends AbstractQuery
{
  private String lowerBoundField;
  private String upperBoundField;

  public IntegerRangeFilterQuery(BooleanClause.Occur occur, String lowerBoundField, String upperBoundField)
  {
    super(occur);
    this.lowerBoundField = lowerBoundField;
    this.upperBoundField = upperBoundField;
  }

  public Query getQuery(SolrIndexSearcher indexSearcher, SolrInputDocument inputDocument)
  {
    if (!inputDocument.containsKey(lowerBoundField) || !inputDocument.containsKey(upperBoundField))
      return null;

    double lower = Double.parseDouble(inputDocument.getFieldValue(lowerBoundField).toString());
    double upper = Double.parseDouble(inputDocument.getFieldValue(upperBoundField).toString());

    Query nq1 = NumericRangeQuery.newDoubleRange(lowerBoundField, lower, upper, true, true);
    Query nq2 = NumericRangeQuery.newDoubleRange(upperBoundField, lower, upper, true, true);

    BooleanQuery query = new BooleanQuery();

    //one of the two query must match at least one document
    query.add(nq1, BooleanClause.Occur.SHOULD);
    query.add(nq2, BooleanClause.Occur.SHOULD);

    return query;
  }
}
