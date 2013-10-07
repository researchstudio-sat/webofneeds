package won.matcher.query;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.search.SolrIndexSearcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * User: atus
 * Date: 09.07.13
 */
public class MultipleValueFieldQueryFactory extends AbstractQueryFactory
{
  private String tagField;

  public MultipleValueFieldQueryFactory(BooleanClause.Occur occur, float boost, String tagField)
  {
    super(occur, boost);
    this.tagField = tagField;
  }

  public MultipleValueFieldQueryFactory(final BooleanClause.Occur occur, final String tagField)
  {
    super(occur);
    this.tagField = tagField;
  }

  @Override
  public Query createQuery(final SolrIndexSearcher indexSearcher, final SolrInputDocument inputDocument) throws IOException
  {
    if (!inputDocument.containsKey(tagField))
      return null;

    Collection<Object> tags = inputDocument.getFieldValues(tagField);
    List<Query> queries = new ArrayList();
    for (Object tag : tags) {
      TermQuery termQuery = new TermQuery(new Term(tagField, tag.toString()));
      queries.add(termQuery);
    }

    Query query = new BooleanQuery();
    query = query.combine(queries.toArray(new Query[queries.size()]));

    return query;
  }
}
