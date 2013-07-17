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
public class SelfFilterQuery extends AbstractQuery
{
  private String field;

  public SelfFilterQuery(String field)
  {
    super(BooleanClause.Occur.MUST_NOT);
    this.field = field;
  }

  @Override
  public Query getQuery(final SolrIndexSearcher indexSearcher, final SolrInputDocument inputDocument) throws IOException
  {
    return new TermQuery(new Term(field, inputDocument.getFieldValue(field).toString()));
  }
}
