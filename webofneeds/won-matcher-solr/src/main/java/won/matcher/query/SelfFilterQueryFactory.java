package won.matcher.query;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.search.SolrIndexSearcher;

import java.io.IOException;

/**
 * User: atus
 * Date: 17.07.13
 */
public class SelfFilterQueryFactory extends AbstractQueryFactory
{
  private String field;

  public SelfFilterQueryFactory(String field, float boost)
  {
    super(BooleanClause.Occur.MUST_NOT, boost);
    this.field = field;
  }

  public SelfFilterQueryFactory(final String field)
  {
    this(field, 1.0f);
  }

  @Override
  public Query createQuery(final SolrIndexSearcher indexSearcher, final SolrInputDocument inputDocument) throws IOException
  {
    return new TermQuery(new Term(field, inputDocument.getFieldValue(field).toString()));
  }
}
