package won.matcher.solr.query;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;

import java.io.IOException;

/**
 * Created by hfriedrich on 12.08.2016.
 */
public interface SolrMatcherQueryExecutor
{
  public SolrDocumentList executeNeedQuery(String queryString) throws IOException, SolrServerException;
}
