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

  public DoubleRangeQuery(BooleanClause.Occur occur, String lowerBoundField, String upperBoundField)
  {
    super(occur);
    this.lowerBoundField = lowerBoundField;
    this.upperBoundField = upperBoundField;
  }

  public Query getQuery(SolrIndexSearcher indexSearcher, SolrInputDocument inputDocument)
  {
    if (!inputDocument.containsKey(lowerBoundField) && !inputDocument.containsKey(upperBoundField))
      return null;

    Double lower = getField(inputDocument, lowerBoundField);
    Double upper = getField(inputDocument, upperBoundField);

    Query nq1 = NumericRangeQuery.newDoubleRange(lowerBoundField, lower, upper, true, true);
    Query nq2 = NumericRangeQuery.newDoubleRange(upperBoundField, lower, upper, true, true);

    BooleanQuery query = new BooleanQuery();

    //one of the two query must match at least one document
    query.add(nq1, BooleanClause.Occur.SHOULD);
    query.add(nq2, BooleanClause.Occur.SHOULD);

    return query;
  }

  private Double getField(SolrInputDocument inputDocument, String fieldName)
  {
    if (inputDocument.containsKey(fieldName))
      return Double.parseDouble(inputDocument.getFieldValue(fieldName).toString());
    else return null;
  }
}
