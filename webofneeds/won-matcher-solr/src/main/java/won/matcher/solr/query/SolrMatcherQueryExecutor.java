package won.matcher.solr.query;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.SolrParams;

/**
 * Created by hfriedrich on 12.08.2016.
 */
public interface SolrMatcherQueryExecutor {
    SolrDocumentList executeNeedQuery(String queryString, int maxHints, SolrParams params, String... filterQueries)
                    throws IOException, SolrServerException;
}
