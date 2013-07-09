package won.matcher;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.xml.sax.SAXException;
import won.protocol.solr.SolrFields;
import won.protocol.vocabulary.WON;

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

    Thread.sleep(75 * 1000);

    server.add(getTestData2());
  }

  public static Collection<SolrInputDocument> getTestData()
  {
    Collection<SolrInputDocument> docs = new ArrayList<>();

    SolrInputDocument doc1 = new SolrInputDocument();
    doc1.addField(SolrFields.URL, "http://www.examle.com/ld/need/1");
    doc1.addField(SolrFields.TITLE, "Sofa");
    doc1.addField(SolrFields.DESCRIPTION, "I have a very nice red sofa to give away.");
    doc1.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_SUPPLY.getURI());
    doc1.addField(SolrFields.TAG, "sofa");
    doc1.addField(SolrFields.TAG, "red");
    doc1.addField(SolrFields.TAG, "leather");
    doc1.addField(SolrFields.TAG, "used");
    doc1.addField(SolrFields.LOWER_PRICE_LIMIT, 10.0);
    doc1.addField(SolrFields.UPPER_PRICE_LIMIT, 100.0);
    doc1.addField(SolrFields.LOCATION, "48.2088,16.3726");

    docs.add(doc1);

    //11km away from other points
    SolrInputDocument doc2 = new SolrInputDocument();
    doc2.addField(SolrFields.URL, "http://www.examle.com/ld/need/2");
    doc2.addField(SolrFields.TITLE, "Sofa or couch");
    doc2.addField(SolrFields.DESCRIPTION, "I am giving away my couch.");
    doc2.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_SUPPLY.getURI());
    doc2.addField(SolrFields.TAG, "blue");
    doc2.addField(SolrFields.TAG, "dirty");
    doc2.addField(SolrFields.TAG, "couch");
    doc2.addField(SolrFields.LOWER_PRICE_LIMIT, 50.0);
    doc2.addField(SolrFields.LOCATION, "48.3089,16.3726");

    docs.add(doc2);

    SolrInputDocument doc3 = new SolrInputDocument();
    doc3.addField(SolrFields.URL, "http://www.examle.com/ld/need/3");
    doc3.addField(SolrFields.TITLE, "House");
    doc3.addField(SolrFields.DESCRIPTION, "Selling a 3 story house in the suburbs of Vienna. Ideal for a big family with kids.");
    doc3.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_SUPPLY.getURI());
    doc3.addField(SolrFields.TAG, "house");
    doc3.addField(SolrFields.TAG, "family");
    doc3.addField(SolrFields.TAG, "suburbs");
    doc3.addField(SolrFields.TAG, "kids");
    doc3.addField(SolrFields.LOWER_PRICE_LIMIT, 100000.0);
    doc3.addField(SolrFields.UPPER_PRICE_LIMIT, 500000.0);
    doc3.addField(SolrFields.LOCATION, "48.2088,16.3726");

    docs.add(doc3);

    return docs;
  }

  public static Collection<SolrInputDocument> getTestData2()
  {
    Collection<SolrInputDocument> docs = new ArrayList<>();

    SolrInputDocument doc4 = new SolrInputDocument();
    doc4.addField(SolrFields.URL, "http://www.examle.com/ld/need/4");
    doc4.addField(SolrFields.TITLE, "Sofa");
    doc4.addField(SolrFields.DESCRIPTION, "I need a sofa.");
    doc4.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc4.addField(SolrFields.TAG, "sofa");
    doc4.addField(SolrFields.TAG, "furniture");
    doc4.addField(SolrFields.UPPER_PRICE_LIMIT, 150.0);
    doc4.addField(SolrFields.LOCATION, "48.2088,16.3726");

    docs.add(doc4);

    SolrInputDocument doc5 = new SolrInputDocument();
    doc5.addField(SolrFields.URL, "http://www.examle.com/ld/need/5");
    doc5.addField(SolrFields.TITLE, "Looking for sofa or couch");
    doc5.addField(SolrFields.DESCRIPTION, "I am looking for a sofa or a couch for my living room.");
    doc5.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc5.addField(SolrFields.TAG, "sofa");
    doc5.addField(SolrFields.TAG, "blue");
    doc5.addField(SolrFields.TAG, "red");
    doc5.addField(SolrFields.TAG, "couch");
    doc5.addField(SolrFields.TAG, "leather");
    doc5.addField(SolrFields.UPPER_PRICE_LIMIT, 50.0);
    doc5.addField(SolrFields.LOCATION, "48.2088,16.3726");

    docs.add(doc5);

    SolrInputDocument doc6 = new SolrInputDocument();
    doc6.addField(SolrFields.URL, "http://www.examle.com/ld/need/6");
    doc6.addField(SolrFields.TITLE, "Looking for a place to live");
    doc6.addField(SolrFields.DESCRIPTION, "Me and my family are looking for a house or a large apartment! Thank you.");
    doc6.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc6.addField(SolrFields.TAG, "house");
    doc6.addField(SolrFields.TAG, "apartment");
    doc6.addField(SolrFields.TAG, "family");
    doc6.addField(SolrFields.UPPER_PRICE_LIMIT, 250000.0);
    doc6.addField(SolrFields.LOCATION, "48.2088,16.3726");

    docs.add(doc6);

    return docs;
  }

}
