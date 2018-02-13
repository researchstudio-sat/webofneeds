package won.matcher.solr.query;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.SolrParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.matcher.solr.config.SolrMatcherConfig;
import won.matcher.solr.hints.HintBuilder;
import won.matcher.solr.query.factory.MatchingContextQueryFactory;

import javax.annotation.PostConstruct;
import java.io.IOException;

/**
 * Created by hfriedrich on 12.08.2016.
 */
@Component
public class DefaultMatcherQueryExecuter implements SolrMatcherQueryExecutor
{
  private final Logger log = LoggerFactory.getLogger(getClass());

  @Autowired
  SolrMatcherConfig config;

  SolrClient solrClient;

  @PostConstruct
  private void init() {
    solrClient = new HttpSolrClient.Builder(config.getSolrEndpointUri(false)).build();
  }

  @Override
  public SolrDocumentList executeNeedQuery(String queryString, SolrParams params, String... filterQueries)
    throws IOException, SolrServerException {

    SolrQuery query = new SolrQuery();
    log.debug("use query: {} with filters {}", queryString, filterQueries);
    query.setQuery(queryString);
    query.setFields("id", "score", HintBuilder.WON_NODE_SOLR_FIELD, HintBuilder.HAS_FLAG_SOLR_FIELD, MatchingContextQueryFactory.MATCHING_CONTEXT_SOLR_FIELD);
    query.setRows(config.getMaxHints());

    if (filterQueries != null) {
      query.setFilterQueries(filterQueries);
    }

    if (params != null) {
      query.add(params);
    }

    try {
      QueryResponse response = solrClient.query(query);
      return response.getResults();
    } catch (SolrException e) {
      log.warn("Exception {} thrown for query: {}", e, queryString);
    }

    return null;
  }

}
