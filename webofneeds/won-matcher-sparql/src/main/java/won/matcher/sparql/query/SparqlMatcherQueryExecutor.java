package won.matcher.sparql.query;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.SolrParams;

import java.io.IOException;

/**
 * Created by hfriedrich on 12.08.2016.
 */
public interface SparqlMatcherQueryExecutor
{
  SolrDocumentList executeNeedQuery(String queryString, int maxHints, SolrParams params, String... filterQueries)
    throws IOException, SolrServerException;
}
