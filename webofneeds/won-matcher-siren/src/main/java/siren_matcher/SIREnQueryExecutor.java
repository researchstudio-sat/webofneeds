package siren_matcher;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;

/**
 * Created by soheilk on 25.08.2015.
 */
public class SIREnQueryExecutor {

    public SolrDocumentList execute(String queryString, SolrServer server) throws SolrServerException {
        String sIREnUri = Configuration.sIREnUri;

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(queryString);
        solrQuery.addField("score");
        solrQuery.addField("@graph.@id");
        //solrQuery.addField("id");

        solrQuery.setRows(Configuration.NUMBER_OF_HINTS); //This specifies how many results should be produced by Solr

        // System.out.println("Query is: "+solrQuery); //For testing

        QueryResponse rsp = server.query(solrQuery);
        // System.out.println(rsp.toString()); //For testing
        SolrDocumentList docs = rsp.getResults();

        // System.out.println(docs.get(1).getFieldValue("@graph.@id")); //For testing

        return docs;
    }
}
