package won.matcher.siren.matcher;

import won.matcher.siren.config.SirenMatcherConfig;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by soheilk on 25.08.2015.
 */
@Component
public class SIREnQueryExecutor {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private SirenMatcherConfig config;

    public SolrDocumentList execute(String queryString, SolrServer server) throws SolrServerException {

        try {
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery(queryString);

            // return all fields until we find out how to return only specific fields
            solrQuery.addField("*");
            solrQuery.addField("score");

            solrQuery.setRows((int) config.getMaxHints()); //This specifies how many results should be produced by Solr
            QueryResponse rsp = server.query(solrQuery);
            SolrDocumentList docs = rsp.getResults();

            return docs;
        } catch (SolrServerException e) {
            log.error("Error executing solr query {}, exception is {}", queryString, e);
        }

        return null;
    }
}
