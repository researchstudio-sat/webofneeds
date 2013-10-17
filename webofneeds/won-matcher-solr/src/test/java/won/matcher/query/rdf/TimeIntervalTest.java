package won.matcher.query.rdf;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.SolrIndexReader;
import org.apache.solr.search.SolrIndexSearcher;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: atus
 * Date: 11.10.13
 */
public class TimeIntervalTest
{
  private static final String SOLR_CORE_NAME = "main";

  static CoreContainer coreContainer = null;
  static SolrIndexSearcher searcher = null;
  static SolrIndexReader reader = null;

  static List<SolrInputDocument> testDocuments = null;

  @BeforeClass
  public static void setup() throws IOException, SAXException, ParserConfigurationException, SolrServerException, InterruptedException
  {
    System.setProperty("solr.solr.home", "src/test/resources/solr-intervals");

    deleteIndexDir();

    //now start solr
    CoreContainer.Initializer initializer = new CoreContainer.Initializer();
    CoreContainer coreContainer = initializer.initialize();

    EmbeddedSolrServer server = new EmbeddedSolrServer(coreContainer, SOLR_CORE_NAME);

    testDocuments = getTestData();
    server.add(testDocuments);
    System.out.println("test documents added to solr server, waiting for commit..");

    server.commit(true, true);
    System.out.println("solr commit done, continuing");

    SolrCore core = coreContainer.getCore(SOLR_CORE_NAME);
    searcher = core.newSearcher("test");
    reader = searcher.getReader();
  }

  private static void deleteIndexDir()
  {
    //first, delete the index dir if it's there
    File indexDir = new File(System.getProperty("solr.solr.home"), "data/index");
    if (indexDir.exists()) {
      System.out.println("deleting index dir: " + indexDir);
      File[] indexDirContents = indexDir.listFiles();
      boolean deleteSuccessful = true;
      for (int i = 0; i < indexDirContents.length && deleteSuccessful; i++) {
        deleteSuccessful = indexDirContents[i].delete();
      }
      deleteSuccessful = indexDir.delete();
      if (deleteSuccessful) {
        System.out.println("index dir deleted");
      } else {
        System.out.println("failed to delete index dir");
      }
    }
  }

  @AfterClass
  public static void tearDown() throws InterruptedException
  {
    if (searcher != null) {
      try {
        searcher.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    if (coreContainer != null) {
      coreContainer.shutdown();
    }
  }

  @Test
  public void readIndex() throws IOException
  {
    int docs = reader.numDocs();

    System.out.println("Printing index:");

    for (int i = 0; i < docs; i++) {
      Document doc = reader.document(i);
      String longInterval = doc.get(Fields.longInterval);
      String dateInterval = doc.get(Fields.dateInterval);

      System.out.println(doc.get(Fields.title));
      System.out.println(longInterval);
      System.out.println(dateInterval);
      System.out.println("======================");

      Assert.assertFalse(longInterval == null);
      Assert.assertFalse(dateInterval == null);
    }
  }

  @Test
  public void longIntervalQuery() throws IOException
  {
    SchemaField schemaField = searcher.getSchema().getField(Fields.longInterval);

    Query q1 = schemaField.getType().getRangeQuery(null, schemaField, "10", "15", true, true);
    TopDocs results1 = searcher.search(q1, 5);
    Assert.assertTrue(results1.totalHits == 0);

    Query q2 = schemaField.getType().getRangeQuery(null, schemaField, "60", "110", true, true);
    TopDocs results2 = searcher.search(q2, 5);
    Assert.assertTrue(results2.totalHits == 2);

    Query q3 = schemaField.getType().getRangeQuery(null, schemaField, "140", "145", true, true);
    TopDocs results3 = searcher.search(q3, 5);
    Assert.assertTrue(results3.totalHits == 1);
  }

  @Test
  public void dateIntervalQuery() throws IOException
  {
    SchemaField schemaField = searcher.getSchema().getField(Fields.dateInterval);

    Query q1 = schemaField.getType().getRangeQuery(null, schemaField, "2013-08-15T00:01:00Z", "2013-09-10T00:01:00Z", true, true);
    TopDocs results1 = searcher.search(q1, 5);
    Assert.assertTrue(results1.totalHits == 2);

    Query q2 = schemaField.getType().getRangeQuery(null, schemaField, "2016-09-15T00:01:00Z", "2017-09-05T00:01:00Z", true, true);
    TopDocs results2 = searcher.search(q2, 5);
    Assert.assertTrue(results2.totalHits == 0);

    Query q3 = schemaField.getType().getRangeQuery(null, schemaField, "2013-07-15T00:01:00Z", "2013-08-05T00:01:00Z", true, true);
    TopDocs results3 = searcher.search(q3, 5);
    Assert.assertTrue(results3.totalHits == 1);

    Query q4 = schemaField.getType().getRangeQuery(null, schemaField, "2013-08-15T00:01:00Z", "2013-08-20T00:01:00Z", true, true);
    TopDocs results4 = searcher.search(q4, 5);
    Assert.assertTrue(results4.totalHits == 1);
  }

  //not used because we don't have a custom query to manage this with
  public void inclusionsTest() throws IOException
  {
    SchemaField sf = searcher.getSchema().getField(Fields.longInterval);

    //search exact, both included
    Query q01 = sf.getType().getRangeQuery(null, sf, "50", "75", true, true);
    Assert.assertTrue(searcher.search(q01,5).totalHits == 1);

    //search exact, both excluded
    Query q02 = sf.getType().getRangeQuery(null, sf, "50", "75", false, false);
    Assert.assertTrue(searcher.search(q02,5).totalHits == 1);

    //search over left, left included
    Query q11 = sf.getType().getRangeQuery(null, sf, "45", "55", true, true);
    Assert.assertTrue(searcher.search(q11,5).totalHits == 1);

    //search over left, left excluded
    Query q12 = sf.getType().getRangeQuery(null, sf, "45", "55", false, true);
    Assert.assertTrue(searcher.search(q12,5).totalHits == 0);

    //search internal, both included
    Query q21 = sf.getType().getRangeQuery(null, sf, "55", "57", true, true);
    Assert.assertTrue(searcher.search(q21,5).totalHits == 1);

    //search internal both excluded
    Query q22 = sf.getType().getRangeQuery(null, sf, "55", "57", false, false);
    Assert.assertTrue(searcher.search(q22,5).totalHits == 1);

    //search over right, right included
    Query q31 = sf.getType().getRangeQuery(null, sf, "70", "80", true, true);
    Assert.assertTrue(searcher.search(q31,5).totalHits == 1);

    //search over right, right excluded
    Query q32 = sf.getType().getRangeQuery(null, sf, "70", "80", true, false);
    Assert.assertTrue(searcher.search(q32,5).totalHits == 0);

    //search over both, both included
    Query q41 = sf.getType().getRangeQuery(null, sf, "40", "80", true, true);
    Assert.assertTrue(searcher.search(q41,5).totalHits == 1);

    //search over right, right excluded
    Query q42 = sf.getType().getRangeQuery(null, sf, "40", "80", false, false);
    Assert.assertTrue(searcher.search(q42,5).totalHits == 0);

    //search over right, right excluded
    Query q51 = sf.getType().getRangeQuery(null, sf, "90", "100", true, true);
    Assert.assertTrue(searcher.search(q51,5).totalHits == 0);

  }

  private static List<SolrInputDocument> getTestData()
  {
    List<SolrInputDocument> docs = new ArrayList<>();

    SolrInputDocument doc1 = new SolrInputDocument();
    doc1.addField(Fields.id, 1);
    doc1.addField(Fields.title, "Example 1");
    doc1.addField(Fields.dateInterval, "2013-08-01T00:01:00Z/2013-08-30T00:01:00Z");
    doc1.addField(Fields.longInterval, "100-200");
    docs.add(doc1);

    SolrInputDocument doc2 = new SolrInputDocument();
    doc2.addField(Fields.id, 2);
    doc2.addField(Fields.title, "Example 2");
    doc2.addField(Fields.dateInterval, "2013-09-05T00:01:00Z/2013-09-30T00:01:00Z");
    doc2.addField(Fields.longInterval, "50-75");
    docs.add(doc2);

    return docs;
  }

  public class Fields
  {
    public static final String id = "id";
    public static final String title = "title";
    public static final String longInterval = "longInterval";
    public static final String dateInterval = "dateInterval";
  }

}
