package won.matcher.query;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.vocabulary.WON;

import java.io.IOException;

/**
 * User: atus
 * Date: 05.07.13
 */
public class BasicNeedTypeQuery extends AbstractQuery
{
  private Logger logger = LoggerFactory.getLogger(getClass());

  private final String needTypeField;

  public BasicNeedTypeQuery(BooleanClause.Occur occur, String needTypeField)
  {
    super(occur);
    this.needTypeField = needTypeField;
  }

  @Override
  public Query getQuery(final SolrIndexSearcher indexSearcher, final SolrInputDocument inputDocument) throws IOException
  {
    if (!inputDocument.containsKey(needTypeField))
      return null;

    String matchingNeedType = getMatchingNeedType(inputDocument.getFieldValue(needTypeField).toString());
    if(matchingNeedType == null)
      return null;

    Query query = new TermQuery(new Term(needTypeField, matchingNeedType));
    logger.info("Need type query: " + query.toString());

    return query;
  }

  protected String getMatchingNeedType(String needType)
  {
    if (needType.equals(WON.BASIC_NEED_TYPE_SUPPLY.toString()))
      return WON.BASIC_NEED_TYPE_DEMAND.toString();
    else if (needType.equals(WON.BASIC_NEED_TYPE_DEMAND.toString()))
      return WON.BASIC_NEED_TYPE_SUPPLY.toString();
    else if (needType.equals(WON.BASIC_NEED_TYPE_DO_TOGETHER.toString()))
      return WON.BASIC_NEED_TYPE_DO_TOGETHER.toString();
    else if (needType.equals(WON.BASIC_NEED_TYPE_CRITIQUE.toString()))
      return WON.BASIC_NEED_TYPE_CRITIQUE.toString();
    else
      return null;
  }
}
