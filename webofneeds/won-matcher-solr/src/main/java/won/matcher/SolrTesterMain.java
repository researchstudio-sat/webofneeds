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

  public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException, SolrServerException
  {
    // Note that the following property could be set through JVM level arguments too
    System.setProperty("solr.solr.home", "won-matcher-solr/solr");
    CoreContainer.Initializer initializer = new CoreContainer.Initializer();
    CoreContainer coreContainer = initializer.initialize();
    EmbeddedSolrServer server = new EmbeddedSolrServer(coreContainer, "webofneeds");

    server.add(getTestData());
  }

  public static Collection<SolrInputDocument> getTestData()
  {
    Collection<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();

    SolrInputDocument doc1 = new SolrInputDocument();
    doc1.addField(SolrFields.FIELD_URL, "http://www.examle.com/ld/need/1");
    doc1.addField(SolrFields.FIELD_TITLE, "Sofa");
    doc1.addField(SolrFields.FIELD_DESCRIPTION, "I have a very nice red sofa to give away.");
    doc1.addField(SolrFields.FIELD_BASIC_NEED_TYPE, "SUPPLY");
    doc1.addField(SolrFields.FIELD_TAG, "sofa");
    doc1.addField(SolrFields.FIELD_TAG, "red");
    doc1.addField(SolrFields.FIELD_TAG, "leather");
    doc1.addField(SolrFields.FIELD_TAG, "used");
    doc1.addField(SolrFields.FIELD_LOWERPRICE, 10.0);
    doc1.addField(SolrFields.FIELD_UPPERPRICE, 100.0);

    docs.add(doc1);

    SolrInputDocument doc2 = new SolrInputDocument();
    doc2.addField(SolrFields.FIELD_URL, "http://www.examle.com/ld/need/2");
    doc2.addField(SolrFields.FIELD_TITLE, "Sofa or couch");
    doc2.addField(SolrFields.FIELD_DESCRIPTION, "I am looking for a sofa or a couch for my living room.");
    doc2.addField(SolrFields.FIELD_BASIC_NEED_TYPE, "NEED");
    doc2.addField(SolrFields.FIELD_TAG, "sofa");
    doc2.addField(SolrFields.FIELD_TAG, "blue");
    doc2.addField(SolrFields.FIELD_TAG, "red");
    doc2.addField(SolrFields.FIELD_TAG, "couch");
    doc2.addField(SolrFields.FIELD_LOWERPRICE, 50.0);
    //doc2.addField(SolrFields.FIELD_UPPERPRICE, 100.0);

    docs.add(doc2);

    return docs;
  }

}
