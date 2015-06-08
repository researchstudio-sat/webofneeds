import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Iterator;

/**
 * User: hfriedrich
 * Date: 08.06.2015
 */
public class MatcherMain
{
  public static void main(String[] args)
    throws IOException, SolrServerException, ParserConfigurationException, SAXException {

    SolrClient solr = new HttpSolrClient("http://localhost:8983/solr/gettingstarted/");
    SolrQuery parameters = new SolrQuery();
    parameters.set("q", "Epson");
    QueryResponse response = solr.query(parameters);
    System.out.println("Number of results found: " + response.getResults().getNumFound());

    Iterator<SolrDocument> iter = response.getResults().iterator();
    while (iter.hasNext()) {
      System.out.println("Need found with subject: " + iter.next().getFieldValue("subject"));
    }

    SolrInputDocument document = new SolrInputDocument();
    document.addField("subject", "club mate");
    document.addField("content", "i have club mate ice-t to offer");
    document.addField("tags", "club-mate");
    UpdateResponse updateResponse = solr.add(document);
    solr.commit();

    parameters.set("q", "club mate");
    response = solr.query(parameters);
    System.out.println("Number of results found: " + response.getResults().getNumFound());
    iter = response.getResults().iterator();
    while (iter.hasNext()) {
      System.out.println("Need found with subject: " + iter.next().getFieldValue("subject"));
    }
  }
}
