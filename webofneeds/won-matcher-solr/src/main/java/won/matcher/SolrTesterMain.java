package won.matcher;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.xml.sax.SAXException;
import won.protocol.solr.SolrFields;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * User: atus
 * Date: 03.07.13
 */
public class SolrTesterMain
{

  public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException, SolrServerException, InterruptedException
  {
    // Note that the following property could be set through JVM level arguments too
    System.setProperty("solr.solr.home", "won-matcher-solr/solr");
    CoreContainer.Initializer initializer = new CoreContainer.Initializer();
    CoreContainer coreContainer = initializer.initialize();
    EmbeddedSolrServer server = new EmbeddedSolrServer(coreContainer, "webofneeds");

    server.add(getTestData());
    Thread.sleep(75*1000);

    server.add(getTestData2());
  }

  public static Collection<SolrInputDocument> getTestData()
  {
    Collection<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();

    SolrInputDocument doc1 = new SolrInputDocument();
    doc1.addField(SolrFields.URL, "http://www.examle.com/ld/need/1");
    doc1.addField(SolrFields.TITLE, "Sofa");
    doc1.addField(SolrFields.DESCRIPTION, "I have a very nice red sofa to give away.");
    doc1.addField(SolrFields.BASIC_NEED_TYPE, "SUPPLY");
    doc1.addField(SolrFields.TAG, "sofa");
    doc1.addField(SolrFields.TAG, "red");
    doc1.addField(SolrFields.TAG, "leather");
    doc1.addField(SolrFields.TAG, "used");
    doc1.addField(SolrFields.LOWER_PRICE_LIMIT, 10.0);
    doc1.addField(SolrFields.UPPER_PRICE_LIMIT, 100.0);

    docs.add(doc1);

    SolrInputDocument doc2 = new SolrInputDocument();
    doc2.addField(SolrFields.URL, "http://www.examle.com/ld/need/2");
    doc2.addField(SolrFields.TITLE, "Sofa or couch");
    doc2.addField(SolrFields.DESCRIPTION, "I am giving away my couch.");
    doc2.addField(SolrFields.BASIC_NEED_TYPE, "SUPPLY");
    doc2.addField(SolrFields.TAG, "blue");
    doc2.addField(SolrFields.TAG, "dirty");
    doc2.addField(SolrFields.TAG, "couch");
    doc2.addField(SolrFields.LOWER_PRICE_LIMIT, 50.0);

    docs.add(doc2);

    return docs;
  }


  public static Collection<SolrInputDocument> getTestData2()
  {
    Collection<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();

    SolrInputDocument doc1 = new SolrInputDocument();
    doc1.addField(SolrFields.URL, "http://www.examle.com/ld/need/3");
    doc1.addField(SolrFields.TITLE, "Sofa");
    doc1.addField(SolrFields.DESCRIPTION, "I need a sofa.");
    doc1.addField(SolrFields.BASIC_NEED_TYPE, "NEED");
    doc1.addField(SolrFields.TAG, "sofa");
    doc1.addField(SolrFields.TAG, "furniture");
    doc1.addField(SolrFields.UPPER_PRICE_LIMIT, 150.0);

    docs.add(doc1);

    SolrInputDocument doc2 = new SolrInputDocument();
    doc2.addField(SolrFields.URL, "http://www.examle.com/ld/need/4");
    doc2.addField(SolrFields.TITLE, "Looking for sofa or couch");
    doc2.addField(SolrFields.DESCRIPTION, "I am looking for a sofa or a couch for my living room.");
    doc2.addField(SolrFields.BASIC_NEED_TYPE, "NEED");
    doc2.addField(SolrFields.TAG, "sofa");
    doc2.addField(SolrFields.TAG, "blue");
    doc2.addField(SolrFields.TAG, "red");
    doc2.addField(SolrFields.TAG, "couch");
    doc2.addField(SolrFields.TAG, "leather");
    doc2.addField(SolrFields.UPPER_PRICE_LIMIT, 50.0);

    docs.add(doc2);

    return docs;
  }

}
