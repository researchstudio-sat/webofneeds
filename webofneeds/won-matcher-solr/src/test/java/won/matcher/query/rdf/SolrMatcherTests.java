/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package won.matcher.query.rdf;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileUtils;
import junit.framework.Assert;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrIndexReader;
import org.apache.solr.search.SolrIndexSearcher;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sindice.siren.search.*;
import org.xml.sax.SAXException;
import won.matcher.Matcher;
import won.matcher.processor.MatchProcessor;
import won.matcher.service.ScoreTransformer;
import won.protocol.solr.SolrFields;
import won.protocol.vocabulary.WON;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URI;
import java.text.DecimalFormat;
import java.util.*;

/**
 * User: atus
 * Date: 03.07.13
 */
public class SolrMatcherTests
{
  static CoreContainer coreContainer = null;
  static SolrIndexSearcher searcher = null;
  static SolrIndexReader reader = null;
  static Map<String, SolrInputDocument> testDocuments = null;
  private static final String SOLR_CORE_NAME = "webofneeds";
  private static final int SIREN_NUMERIC_FIELD_PRECISION_STEP = 4;

  @BeforeClass
  public static void setup() throws IOException, SAXException, ParserConfigurationException, SolrServerException, InterruptedException
  {


    // Note that the following property could be set through JVM level arguments too
    System.setProperty("solr.solr.home", "won-matcher-solr/src/test/resources/solr");

    //first, delete the index dir if it's there
    File indexDir = new File(System.getProperty("solr.solr.home"), "data/index");
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

    //now start solr
    CoreContainer.Initializer initializer = new CoreContainer.Initializer();
    CoreContainer coreContainer = initializer.initialize();
    EmbeddedSolrServer server = new EmbeddedSolrServer(coreContainer, SOLR_CORE_NAME);
    testDocuments = getTestData();
    server.add(testDocuments.values());
    System.out.println("test documents added to solr server, waiting for commit..");
    server.commit(true, true);
    System.out.println("solr commit done, continuing");
    SolrCore core = coreContainer.getCore(SOLR_CORE_NAME);
    searcher = core.newSearcher("test");
    reader = searcher.getReader();
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



  /*********************** UTILS ************************************/


  /**
   * Test supposed to find some documents by a one-term query
   */
  @Test
  public void testSirenTermQuery() throws IOException
  {
    SirenTermQuery query = new SirenTermQuery(new Term(SolrFields.NTRIPLE, "hasvalue"));
    assertHitCount(query,5,5,"term query");

    SirenCellQuery cellQuery = new SirenCellQuery(query);
    cellQuery.setConstraint(1);
    assertHitCount(cellQuery,5,5,"cell query (constraint 1)");
  }

  /**
   * Test supposed to find some documents by a one-term query
   */
  @Test
  public void testSirenCellQuery() throws IOException
  {
    SirenTermQuery query = new SirenTermQuery(new Term(SolrFields.NTRIPLE, "hasvalue"));
    SirenCellQuery cellQuery = new SirenCellQuery(query);
    cellQuery.setConstraint(1);
    assertHitCount(cellQuery,5,5,"cell query (constraint 1)");
    cellQuery.setConstraint(0);
    assertHitCount(cellQuery,0,5,"cell query (constraint 0)");
  }

  /**
   * Test supposed to find some documents by a one-term query
   */
  @Test
  public void testSirenTupleQuery() throws IOException
  {
    SirenTermQuery predTermQuery = new SirenTermQuery(new Term(SolrFields.NTRIPLE, "color"));
    SirenCellQuery predCellQuery = new SirenCellQuery(predTermQuery);
    predCellQuery.setConstraint(1);
    SirenTermQuery objTermQuery = new SirenTermQuery(new Term(SolrFields.NTRIPLE, "natural"));
    SirenCellQuery objCellQuery = new SirenCellQuery(objTermQuery);
    objCellQuery.setConstraint(2);
    SirenTupleQuery tupleQuery = new SirenTupleQuery();
    tupleQuery.add(predCellQuery, SirenTupleClause.Occur.MUST);
    tupleQuery.add(objCellQuery, SirenTupleClause.Occur.SHOULD);
    assertHitCount(tupleQuery,5,10,"tuple query");
  }


  /**
   * Test supposed to find some documents by numeric attributes
   */
  @Test
  public void testNumericRangeInCellQuery() throws IOException
  {
    SirenNumericRangeQuery numQuery = SirenNumericRangeQuery.newFloatRange(SolrFields.NTRIPLE, SIREN_NUMERIC_FIELD_PRECISION_STEP, 0f, 50f, true, true);
    SirenCellQuery cellQuery = new SirenCellQuery(numQuery);
    cellQuery.setConstraint(2);
    assertHitCount(cellQuery,5,5,"range cell");
  }

  /**
   * Test supposed to find some documents by numeric attributes
   */
  @Test
  public void testNumericRangeQuery() throws IOException
  {
    SirenNumericRangeQuery numQuery = SirenNumericRangeQuery.newIntRange(SolrFields.NTRIPLE, SIREN_NUMERIC_FIELD_PRECISION_STEP, -1000, 1000, true, true);
    assertHitCount(numQuery,0,5,"int range");
    numQuery = SirenNumericRangeQuery.newLongRange(SolrFields.NTRIPLE, SIREN_NUMERIC_FIELD_PRECISION_STEP, -1000L, 1000L, true, true);
    assertHitCount(numQuery,0,5,"long range");
    numQuery = SirenNumericRangeQuery.newDoubleRange(SolrFields.NTRIPLE, SIREN_NUMERIC_FIELD_PRECISION_STEP, -1000d, 1000d, true, true);
    assertHitCount(numQuery,0,5,"double range");
    numQuery = SirenNumericRangeQuery.newFloatRange(SolrFields.NTRIPLE, SIREN_NUMERIC_FIELD_PRECISION_STEP, 0f, 50f, true, true);
    assertHitCount(numQuery,9,10,"float range");
  }

  /**
   *  Test used to find out how to get the equivalent of this query:
   *  ASK where {
   *   ?this gr:color ?color.
   *   FILTER (?color not in ("natural","black"))
   * }
   * @throws Exception
   */
  @Test
  @Ignore
  public void testNegativeMatch() throws Exception {
    SirenBooleanQuery booleanQuery = new SirenBooleanQuery();
    booleanQuery.add(new SirenTermQuery(new Term(SolrFields.NTRIPLE, "natural")), SirenBooleanClause.Occur.MUST_NOT);
    booleanQuery.add(new SirenTermQuery(new Term(SolrFields.NTRIPLE, "black")), SirenBooleanClause.Occur.MUST_NOT);
    //booleanQuery.add(new SirenTermQuery(new Term(SolrFields.NTRIPLE, "birch")), SirenBooleanClause.Occur.SHOULD);
    //booleanQuery.add(new SirenTermQuery(new Term(SolrFields.NTRIPLE, "red")), SirenBooleanClause.Occur.SHOULD);
    booleanQuery.add(new SirenWildcardQuery(new Term(SolrFields.NTRIPLE, "*")), SirenBooleanClause.Occur.MUST);
    BooleanQuery outerQuery = new BooleanQuery();

    outerQuery.add(new SirenTermQuery(new Term(SolrFields.NTRIPLE,"http://purl.org/goodrelations/v1#color")), BooleanClause.Occur.SHOULD);
    outerQuery.add(booleanQuery, BooleanClause.Occur.MUST);
    //outerQuery.add(booleanQuery, SirenBooleanClause.Occur.SHOULD);
    assertHitCount(outerQuery, 17, 20, "negative match");
  }

  @Test
  public void testRdfMatcherWithSpinLe() throws IOException
  {
    assertTopHit("11", "12");
  }


  @Test
  public void testRdfMatcherWithSpinGeLeAnd() throws IOException
  {
    assertTopHit("16", "12");
  }

  @Test
  public void testRdfMatcherWithSpinGeLeAndOr() throws IOException
  {
    assertTopHit("17", "13");
  }

  @Test
  public void testRdfMatcherWithSpinLeSparqlText() throws IOException
  {
    assertTopHit("18", "12");
  }

  @Test
  public void testRdfMatcherExactMatch() throws IOException
  {
    assertTopHit("19", "12");
  }

  @Test
  public void testRdfMatcherExactMatchWithSimpleContent() throws IOException
  {
    assertTopHit("20", "7");
  }

  @Test
  public void testRdfMatcherSpinEqNeText() throws IOException
  {
    assertTopHit("21", "12");
  }

  @Test
  public void testRdfMatcherSpinInText() throws IOException
  {
    assertTopHit("22", "15");
  }

  @Test
  public void testRdfMatcherSpinNotInText() throws IOException
  {
    assertTopHit("23", "15");
  }



  /*********************** UTILS ************************************/



  private void assertTopHit(String docId, String topHitDocNum) throws IOException
  {
    Matcher m = new Matcher(searcher,new ScoreTransformer(), URI.create("http://www.example.com/matcher"));
    HintCountingMatchProcessor proc = new HintCountingMatchProcessor();
    m.addMatchProcessor(proc);
    m.processDocument(testDocuments.get(makeNeedUriString(docId)));
    m.processMatches();
    List<MatchScore> scores = proc.getMatchScores(makeNeedUri(docId));
    Assert.assertTrue(scores.size() > 0);
    //we know that doc 12 is the best match
    URI topHit = scores.get(0).uri;
    for (int i = 0; i < scores.size(); i++) {
      System.out.println(" match " + i + ": " + scores.get(i));
    }
    Assert.assertEquals(makeNeedUri(topHitDocNum), topHit);
  }

  private void assertHitCount(final Query query, int expectedCount, int maxCount, String msg) throws IOException
  {
    TopDocs result = searcher.search(query, maxCount);
    System.out.println(msg +": " + result.totalHits + " docs found");
    DecimalFormat fmt = new DecimalFormat("##.###");
    for (int i = 0; i < result.scoreDocs.length; i++) {
      System.out.println(msg +"[" + i + "]: "+ fmt.format(result.scoreDocs[i].score) +", " + reader.document(result.scoreDocs[i].doc).getValues(SolrFields.URL)[0]);
    }
    Assert.assertEquals(expectedCount, result.scoreDocs.length);
  }


  private URI makeNeedUri(final String id)
  {
    return URI.create(makeNeedUriString(id));
  }

  private String makeNeedUriString(final String id)
  {
    return "http://www.example.com/resource/need/" + id;
  }





  private static void convertTextualSparqlToSpinRDF(final Model model)
  {
    // TODO: SPAQRL queries embedded in the RDF can't use the ns prefixes of the surrounding
    // RDF when it has been converted to ntriples, this leads to an error.
    // The quick fix is to add @prefix declarations in all embedded SPARQL queries, but obviously,
    // that's not ideal. Solution: when a document is added to the index, all sp:text triples should be
    // replaced by their representation in SPIN RDF.
    SPINUtils.replaceSpinTextWithSpinRdf(model);
  }
  private static String getNTriples(String filename, String baseURI) throws FileNotFoundException
  {
    File file = new File(filename);
    if (!file.exists()) {
      System.err.println("file not found: " + file.getAbsolutePath());
    }
    Model model = readTTL(filename, baseURI);
    convertTextualSparqlToSpinRDF(model);
    return toNTriples(model, baseURI);
  }
  private static Model readTTL(String filename, String baseURI) throws FileNotFoundException
  {
    System.out.println("loading ntriples data for " + baseURI + " from " + filename);
    Model ret = ModelFactory.createDefaultModel();
    ret.read(new FileReader(filename), baseURI, FileUtils.langTurtle);
    return ret;
  }

  private static String toNTriples(Model model, String baseURI)
  {
    StringWriter writer = new StringWriter();
    model.write(writer, FileUtils.langNTriple, baseURI);
    return writer.toString();
  }


  private class HintCountingMatchProcessor implements MatchProcessor
  {
    private Map<URI, Integer> hintCounts = new HashMap<URI, Integer>();
    private Map<URI, List<MatchScore>> matchScores = new HashMap<URI, List<MatchScore>>();

    private HintCountingMatchProcessor()
    {
    }

    public int getHintCount(URI uri)
    {
      Integer count = hintCounts.get(uri);
      if (count == null) return 0;
      return count.intValue();
    }

    public List<MatchScore> getMatchScores(URI uri)
    {
      List<MatchScore> scores = matchScores.get(uri);
      if (scores != null) {
        Collections.sort(scores, new Comparator<MatchScore>()
        {
          @Override
          public int compare(final MatchScore o1, final MatchScore o2)
          {
            return (int) Math.signum(o2.score - o1.score);
          }
        });
        return scores;
      }
      return new ArrayList<MatchScore>();
    }

    @Override
    public void process(final URI from, final URI to, final double score, final URI originator, final Model explanation)
    {
      Integer count = hintCounts.get(from);
      if (count == null) {
        count = 0;
      }
      hintCounts.put(from, count + 1);
      List<MatchScore> scores = matchScores.get(from);
      if (scores == null) scores = new ArrayList<MatchScore>();
      scores.add(new MatchScore(to, score));
      matchScores.put(from, scores);
    }
  }

  private class MatchScore
  {
    private URI uri;
    private double score;

    public MatchScore(final URI uri, final double score)
    {
      this.uri = uri;
      this.score = score;
    }

    @Override
    public String toString()
    {
      return "MatchScore{" +
          "uri=" + uri +
          ", score=" + score +
          '}';
    }
  }



  /*********************** TEST DATA ************************************/

  private static Map<String, SolrInputDocument> getTestData() throws FileNotFoundException
  {
    Map<String, SolrInputDocument> docs = new HashMap<String, SolrInputDocument>();

    SolrInputDocument doc1 = new SolrInputDocument();
    String url = "http://www.example.com/resource/need/1";
    doc1.addField(SolrFields.URL, url);
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
    doc1.addField(SolrFields.TIME_START, "2013-08-01T00:01:00.000Z");
    doc1.addField(SolrFields.TIME_END, "2013-08-30T23:00:00.000Z");

    docs.put(url, doc1);

    //11km away from other points
    SolrInputDocument doc2 = new SolrInputDocument();
    url = "http://www.example.com/resource/need/2";
    doc2.addField(SolrFields.URL, url);
    doc2.addField(SolrFields.TITLE, "Sofa or couch");
    doc2.addField(SolrFields.DESCRIPTION, "I am giving away my couch.");
    doc2.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_SUPPLY.getURI());
    doc2.addField(SolrFields.TAG, "blue");
    doc2.addField(SolrFields.TAG, "dirty");
    doc2.addField(SolrFields.TAG, "couch");
    doc2.addField(SolrFields.LOWER_PRICE_LIMIT, 50.0);
    doc2.addField(SolrFields.LOCATION, "48.3089,16.3726");
    doc2.addField(SolrFields.TIME_START, "2013-07-01T00:01:00.000Z");
    doc2.addField(SolrFields.TIME_END, "2013-08-30T23:00:00.000Z");

    docs.put(url, doc2);

    SolrInputDocument doc3 = new SolrInputDocument();
    url = "http://www.example.com/resource/need/3";
    doc3.addField(SolrFields.URL, url);
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

    docs.put(url, doc3);


    SolrInputDocument doc4 = new SolrInputDocument();
    url = "http://www.example.com/resource/need/4";
    doc4.addField(SolrFields.URL, url);
    doc4.addField(SolrFields.TITLE, "Sofa");
    doc4.addField(SolrFields.DESCRIPTION, "I need a sofa.");
    doc4.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc4.addField(SolrFields.TAG, "sofa");
    doc4.addField(SolrFields.TAG, "furniture");
    doc4.addField(SolrFields.UPPER_PRICE_LIMIT, 150.0);
    doc4.addField(SolrFields.LOCATION, "48.2088,16.3726");
    doc4.addField(SolrFields.TIME_START, "2013-06-01T00:01:00.000Z");
    doc4.addField(SolrFields.TIME_END, "2013-07-30T23:00:00.000Z");


    docs.put(url, doc4);

    SolrInputDocument doc5 = new SolrInputDocument();
    url = "http://www.example.com/resource/need/5";
    doc5.addField(SolrFields.URL, url);
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
    doc5.addField(SolrFields.TIME_START, "2013-07-01T00:01:00.000Z");
    doc5.addField(SolrFields.TIME_END, "2013-09-30T23:00:00.000Z");

    docs.put(url, doc5);

    SolrInputDocument doc6 = new SolrInputDocument();
    url = "http://www.example.com/resource/need/6";
    doc6.addField(SolrFields.URL, url);
    doc6.addField(SolrFields.TITLE, "Looking for a place to live");
    doc6.addField(SolrFields.DESCRIPTION, "Me and my family are looking for a house or a large apartment! Thank you.");
    doc6.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc6.addField(SolrFields.TAG, "house");
    doc6.addField(SolrFields.TAG, "apartment");
    doc6.addField(SolrFields.TAG, "family");
    doc6.addField(SolrFields.UPPER_PRICE_LIMIT, 250000.0);
    doc6.addField(SolrFields.LOCATION, "48.2088,16.3726");

    docs.put(url, doc6);

    //a document with ntriples content
    SolrInputDocument doc7 = new SolrInputDocument();
    url = "http://www.example.com/resource/need/7";
    doc7.addField(SolrFields.URL, "http://www.example.com/resource/need/7");
    doc7.addField(SolrFields.TITLE, "Table");
    doc7.addField(SolrFields.DESCRIPTION, "");
    doc7.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_SUPPLY.getURI());
    doc7.addField(SolrFields.TAG, "table");
    doc7.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_content_table1.ttl", url));

    docs.put(url, doc7);

    //another document with ntriples content identical to doc7, the rest different
    SolrInputDocument doc8 = new SolrInputDocument();
    url = "http://www.example.com/resource/need/8";
    doc8.addField(SolrFields.URL, url);
    doc8.addField(SolrFields.TITLE, "Tisch");
    doc8.addField(SolrFields.DESCRIPTION, "");
    doc8.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc8.addField(SolrFields.TAG, "tisch");
    doc8.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_content_cupboard_sameas_1.ttl", url));
    docs.put(url, doc8);

    //another document with ntriples content slightly different from doc 7 and 8, the rest different from all others
    SolrInputDocument doc9 = new SolrInputDocument();
    url = "http://www.example.com/resource/need/9";
    doc9.addField(SolrFields.URL, url);
    doc9.addField(SolrFields.TITLE, "mesa");
    doc9.addField(SolrFields.DESCRIPTION, "");
    doc9.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc9.addField(SolrFields.TAG, "mesa");
    doc9.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_content_cupboard_similar_to_1.ttl", url));
    docs.put(url, doc9);

    //another document with ntriples content identical to doc7 (the blank nodes are named differently), the rest different
    SolrInputDocument doc10 = new SolrInputDocument();
    url = "http://www.example.com/resource/need/10";
    doc10.addField(SolrFields.URL, "http://www.example.com/resource/need/10");
    doc10.addField(SolrFields.TITLE, "Bord");
    doc10.addField(SolrFields.DESCRIPTION, "");
    doc10.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc10.addField(SolrFields.TAG, "bord");
    doc10.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_content_cupboard_similar_to_1b.ttl", url));
    docs.put(url, doc10);


    //another document with ntriples content containing SPIN restrictions
    SolrInputDocument doc11 = new SolrInputDocument();
    url = "http://www.example.com/resource/need/11";
    doc11.addField(SolrFields.URL, url);
    doc11.addField(SolrFields.TITLE, "Kästchen");
    doc11.addField(SolrFields.DESCRIPTION, "");
    doc11.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc11.addField(SolrFields.TAG, "kästchen");
    doc11.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_spin_le_3times.ttl", url));
    docs.put(url, doc11);


    //another document with ntriples content (matches 11)
    SolrInputDocument doc12 = new SolrInputDocument();
    url = "http://www.example.com/resource/need/12";
    doc12.addField(SolrFields.URL, url);
    doc12.addField(SolrFields.TITLE, "Cupboard");
    doc12.addField(SolrFields.DESCRIPTION, "");
    doc12.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_SUPPLY.getURI());
    doc12.addField(SolrFields.TAG, "cupboard");
    doc12.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_content_cupboard_45_45_15.ttl", url));
    docs.put(url, doc12);


    //same as 12, but doesn't match the spin restrictions of 11
    SolrInputDocument doc13 = new SolrInputDocument();
    url = "http://www.example.com/resource/need/13";
    doc13.addField(SolrFields.URL, url);
    doc13.addField(SolrFields.TITLE, "Cupboard");
    doc13.addField(SolrFields.DESCRIPTION, "");
    doc13.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_SUPPLY.getURI());
    doc13.addField(SolrFields.TAG, "cupboard");
    doc13.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_content_cupboard_100_100_100.ttl", url));
    docs.put(url, doc13);


    //same as 12, but doesn't match the spin restrictions of 11, and lacks width
    SolrInputDocument doc14 = new SolrInputDocument();
    url = "http://www.example.com/resource/need/14";
    doc14.addField(SolrFields.URL, url);
    doc14.addField(SolrFields.TITLE, "Cupboard");
    doc14.addField(SolrFields.DESCRIPTION, "");
    doc14.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_SUPPLY.getURI());
    doc14.addField(SolrFields.TAG, "cupboard");
    doc14.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_content_cupboard_200_200.ttl", url));
    docs.put(url, doc14);


    //another document with ntriples content containing SPIN restrictions
    SolrInputDocument doc15 = new SolrInputDocument();
    url = "http://www.example.com/resource/need/15";
    doc15.addField(SolrFields.URL, url);
    doc15.addField(SolrFields.TITLE, "Cupboard");
    doc15.addField(SolrFields.DESCRIPTION, "");
    doc15.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_SUPPLY.getURI());
    doc15.addField(SolrFields.TAG, "cupboard");
    doc15.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_content_cupboard_40_40_18_different_literals.ttl", url));
    docs.put(url, doc15);

    //like 11 (embedded spin), but with restrictions combined with &&
    SolrInputDocument doc16 = new SolrInputDocument();
    url = "http://www.example.com/resource/need/16";
    doc16.addField(SolrFields.URL, url);
    doc16.addField(SolrFields.TITLE, "Kästchen");
    doc16.addField(SolrFields.DESCRIPTION, "");
    doc16.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc16.addField(SolrFields.TAG, "kästchen");
    doc16.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_spin_ge_le_and.ttl", url));
    docs.put(url, doc16);

    //like 11 (embedded spin), but with restrictions combined with && and ||
    SolrInputDocument doc17 = new SolrInputDocument();
    url = "http://www.example.com/resource/need/17";
    doc17.addField(SolrFields.URL, url);
    doc17.addField(SolrFields.TITLE, "Kästchen");
    doc17.addField(SolrFields.DESCRIPTION, "");
    doc17.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc17.addField(SolrFields.TAG, "kästchen");
    doc17.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_spin_ge_le_and_or.ttl", url));
    docs.put(url, doc17);

    //like 11 (embedded spin), but with SPARQL as text (not in SPIN rdf notation)
    SolrInputDocument doc18 = new SolrInputDocument();
    url = "http://www.example.com/resource/need/18";
    doc18.addField(SolrFields.URL, url);
    doc18.addField(SolrFields.TITLE, "");
    doc18.addField(SolrFields.DESCRIPTION, "");
    doc18.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc18.addField(SolrFields.TAG, "");
    doc18.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_spin_le_sp-text.ttl", url));
    docs.put(url, doc18);

    //like 11 (matches 12 exactly) but without SPIN
    SolrInputDocument doc19 = new SolrInputDocument();
    url = "http://www.example.com/resource/need/19";
    doc19.addField(SolrFields.URL, url);
    doc19.addField(SolrFields.TITLE, "");
    doc19.addField(SolrFields.DESCRIPTION, "");
    doc19.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc19.addField(SolrFields.TAG, "");
    doc19.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_content_cupboard_45_45_15_exact-match.ttl", url));
    docs.put(url, doc19);

    // a very simple need, no title, desc etc, just one content triple
    SolrInputDocument doc20 = new SolrInputDocument();
    url = "http://www.example.com/resource/need/20";
    doc20.addField(SolrFields.URL, url);
    doc20.addField(SolrFields.TITLE, "");
    doc20.addField(SolrFields.DESCRIPTION, "");
    doc20.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc20.addField(SolrFields.TAG, "");
    doc20.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_content_table_simple.ttl", url));
    docs.put(url, doc20);

    //like 18 (embedded SPARQL as text) but for testing '=' and '!='
    // a very simple need, no title, desc etc, just one content triple
    SolrInputDocument doc21 = new SolrInputDocument();
    url = "http://www.example.com/resource/need/21";
    doc21.addField(SolrFields.URL, url);
    doc21.addField(SolrFields.TITLE, "");
    doc21.addField(SolrFields.DESCRIPTION, "");
    doc21.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc21.addField(SolrFields.TAG, "");
    doc21.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_spin_eq_ne-text.ttl", url));
    docs.put(url, doc21);

    //testing sparql "in" function
    SolrInputDocument doc22 = new SolrInputDocument();
    url = "http://www.example.com/resource/need/22";
    doc22.addField(SolrFields.URL, url);
    doc22.addField(SolrFields.TITLE, "");
    doc22.addField(SolrFields.DESCRIPTION, "");
    doc22.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc22.addField(SolrFields.TAG, "");
    doc22.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_spin_in-text.ttl", url));
    docs.put(url, doc22);

    //testing sparql "not in" function
    SolrInputDocument doc23 = new SolrInputDocument();
    url = "http://www.example.com/resource/need/23";
    doc23.addField(SolrFields.URL, url);
    doc23.addField(SolrFields.TITLE, "");
    doc23.addField(SolrFields.DESCRIPTION, "");
    doc23.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc23.addField(SolrFields.TAG, "");
    doc23.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_spin_notin-text.ttl", url));
    docs.put(url, doc23);
    return docs;

  }
}
