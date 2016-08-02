package won.matcher.solr;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;

import java.io.IOException;

/**
 * Created by hfriedrich on 28.07.2016.
 */
public class SolrTest1
{
  public static void main(String[] args) throws IOException, InterruptedException, SolrServerException {

    String urlString = "http://localhost:8983/solr/gettingstarted/";
    SolrClient solr = new HttpSolrClient.Builder(urlString).build();

    SolrQuery query = new SolrQuery();




    query.setQuery("(_graph.http___purl.org_webofneeds_model_hasContent.http___purl.org_dc_elements_1.1_title:" +
                     "old test) AND " +
                     "(_graph.http___purl.org_webofneeds_model_hasContent.http___purl.org_webofneeds_model_hasTag:" +
                     "adidas,sneakers) AND " +
                     "(_graph.http___purl.org_webofneeds_model_hasBasicNeedType._id: \"http://purl" +
                     ".org/webofneeds/model#Demand\")");


    query.setFields("id", "score");


    QueryResponse response = solr.query(query);

    SolrDocumentList list = response.getResults();

    System.out.println(list.get(0).toString());
  }


}
