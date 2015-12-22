package won.matcher.query;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.search.SolrIndexSearcher;
import won.protocol.vocabulary.WON;

import java.io.IOException;

/**
 * User: atus
 * Date: 05.07.13
 */
public class BasicNeedTypeQueryFactory extends AbstractQueryFactory
{
  private final String needTypeField;
  private final boolean search;

  public BasicNeedTypeQueryFactory(BooleanClause.Occur occur, float boost, String needTypeField, boolean search)
  {
    super(occur, boost);
    this.needTypeField = needTypeField;
    this.search = search;
  }

  public BasicNeedTypeQueryFactory(final BooleanClause.Occur occur, final String needTypeField, final boolean search)
  {
    super(occur);
    this.needTypeField = needTypeField;
    this.search = search;
  }


  @Override
  public Query createQuery(final SolrIndexSearcher indexSearcher, final SolrInputDocument inputDocument) throws IOException
  {
    String matchingNeedType;
    if (!inputDocument.containsKey(needTypeField))
      return null;

    if(!search){
      matchingNeedType = getMatchingNeedType(inputDocument.getFieldValue(needTypeField).toString());
    }else{
      matchingNeedType = inputDocument.getFieldValue(needTypeField).toString();
    }

    if (matchingNeedType == null)
      return null;

    Query query = new TermQuery(new Term(needTypeField, matchingNeedType));

    return query;
  }

  protected String getMatchingNeedType(String needType)
  {
    if (needType.equals(WON.BASIC_NEED_TYPE_SUPPLY.getURI().toString()))
      return WON.BASIC_NEED_TYPE_DEMAND.getURI().toString();
    else if (needType.equals(WON.BASIC_NEED_TYPE_DEMAND.getURI().toString()))
      return WON.BASIC_NEED_TYPE_SUPPLY.getURI().toString();
    else if (needType.equals(WON.BASIC_NEED_TYPE_DO_TOGETHER.getURI().toString()))
      return WON.BASIC_NEED_TYPE_DO_TOGETHER.getURI().toString();
    else if (needType.equals(WON.BASIC_NEED_TYPE_CRITIQUE.getURI().toString()))
      return WON.BASIC_NEED_TYPE_CRITIQUE.getURI().toString();
    else
      return null;
  }
}
