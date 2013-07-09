package won.matcher.query;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.similar.MoreLikeThis;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.search.SolrIndexSearcher;

import java.io.IOException;
import java.io.StringReader;

/**
 * User: gabriel
 * Date: 02.07.13
 * Time: 16:13
 */
public class TextMatcherQuery extends AbstractQuery
{
  private String[] fields;

  private MoreLikeThis mlt;

  public TextMatcherQuery(BooleanClause.Occur occur, String[] fields)
  {
    super(occur);
    this.fields = fields;
  }

  public Query getQuery(SolrIndexSearcher indexSearcher, SolrInputDocument inputDocument) throws IOException
  {
    mlt = new MoreLikeThis(indexSearcher.getReader());

    mlt.setMinDocFreq(1);
    mlt.setMinTermFreq(1);
    mlt.setMaxDocFreqPct(100);
    mlt.setMinTermFreq(1);
    //mlt.setFieldNames(fields);

    BooleanQuery booleanQuery = new BooleanQuery();
    for (String field : fields)
      if (inputDocument.getField(field) != null) {
        Query query = mlt.like(new StringReader(inputDocument.getFieldValue(field).toString()), field);
        BooleanClause clause = new BooleanClause(query, BooleanClause.Occur.SHOULD);
        booleanQuery.add(clause);
      }

    if (booleanQuery.clauses().size() == 0)
      return null;

    return booleanQuery;
  }
}
