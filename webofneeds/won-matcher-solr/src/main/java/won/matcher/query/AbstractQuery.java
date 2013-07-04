package won.matcher.query;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Query;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.search.SolrIndexSearcher;

import java.io.IOException;

/**
 * User: gabriel
 * Date: 04.07.13
 */
public abstract class AbstractQuery
{
  private BooleanClause.Occur occur;

  protected AbstractQuery(BooleanClause.Occur occur) {
      this.occur = occur;
  }

  public abstract Query getQuery(SolrIndexSearcher indexSearcher, SolrInputDocument inputDocument) throws IOException;

  public BooleanClause.Occur getOccur() {
    return occur;
  }

}
