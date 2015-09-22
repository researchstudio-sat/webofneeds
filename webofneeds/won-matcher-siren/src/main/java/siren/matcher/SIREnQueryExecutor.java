package siren.matcher;

import config.SirenMatcherConfig;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by soheilk on 25.08.2015.
 */
@Component
public class SIREnQueryExecutor {

    @Autowired
    private SirenMatcherConfig config;

    public SolrDocumentList execute(String queryString, SolrServer server) throws SolrServerException {

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(queryString);

        //solrQuery.addField("@graph.@id");
        //solrQuery.addField("@graph.http://purl.org/webofneeds/model#hasWonNode.@id");

        // return all fields until we find out how to return only specific fields
        solrQuery.addField("*");
        solrQuery.addField("score");

        solrQuery.setRows(config.getConsideredQueryTokens()); //This specifies how many results should be produced by Solr
        QueryResponse rsp = server.query(solrQuery);
        SolrDocumentList docs = rsp.getResults();

        return docs;
    }
}
