package won.matcher.solr.query;

import java.io.IOException;

import javax.annotation.PostConstruct;

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

/**
 * Created by hfriedrich on 12.08.2016.
 */
@Component
public class DefaultMatcherQueryExecuter implements SolrMatcherQueryExecutor {
    private final Logger log = LoggerFactory.getLogger(getClass());
    @Autowired
    SolrMatcherConfig config;
    SolrClient solrClient;

    @PostConstruct
    private void init() {
        solrClient = new HttpSolrClient.Builder(config.getSolrEndpointUri(false)).build();
    }

    @Override
    public SolrDocumentList executeNeedQuery(String queryString, int maxHints, SolrParams params,
                    String... filterQueries) throws IOException, SolrServerException {
        if (queryString == null) {
            log.debug("query string is null, do execute any query!");
            return null;
        }
        SolrQuery query = new SolrQuery();
        log.debug("use query: {} with filters {}", queryString, filterQueries);
        query.setQuery(queryString);
        query.setFields("id", "score", HintBuilder.WON_NODE_SOLR_FIELD, HintBuilder.HAS_FLAG_SOLR_FIELD,
                        MatchingContextQueryFactory.MATCHING_CONTEXT_SOLR_FIELD);
        query.setRows(maxHints);
        if (filterQueries != null) {
            query.setFilterQueries(filterQueries);
        }
        if (params != null) {
            query.add(params);
        }
        try {
            QueryResponse response = solrClient.query(query);
            SolrDocumentList results = response.getResults();
            // handle special case: if all results have the same score, the rows parameter
            // does not properly restrict the size
            // in order to enforce the restriction, we are doing it here.
            if (results.size() > maxHints) {
                SolrDocumentList cappedResults = new SolrDocumentList();
                for (int i = 0; i < maxHints; i++) {
                    cappedResults.add(results.get(i));
                }
                cappedResults.setMaxScore(results.getMaxScore());
                cappedResults.setNumFound(results.getNumFound());
                cappedResults.setStart(results.getStart());
                return cappedResults;
            }
            return results;
        } catch (SolrException e) {
            log.warn("Exception {} thrown for query: {}", e, queryString);
        }
        return null;
    }
}
