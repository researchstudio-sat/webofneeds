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
import org.junit.Assert;
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

    //start solr
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
    assertHitCount(query, 5, 5, "term query");

    SirenCellQuery cellQuery = new SirenCellQuery(query);
    cellQuery.setConstraint(1);
    assertHitCount(cellQuery, 5, 5, "cell query (constraint 1)");
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
    assertHitCount(cellQuery, 5, 5, "cell query (constraint 1)");
    cellQuery.setConstraint(0);
    assertHitCount(cellQuery, 0, 5, "cell query (constraint 0)");
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
    assertHitCount(tupleQuery, 5, 10, "tuple query");
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
    assertHitCount(cellQuery, 5, 5, "range cell");
  }

  /**
   * Test supposed to find some documents by numeric attributes
   */
  @Test
  public void testNumericRangeQuery() throws IOException
  {
    SirenNumericRangeQuery numQuery = SirenNumericRangeQuery.newIntRange(SolrFields.NTRIPLE, SIREN_NUMERIC_FIELD_PRECISION_STEP, -1000, 1000, true, true);
    assertHitCount(numQuery, 0, 5, "int range");
    numQuery = SirenNumericRangeQuery.newLongRange(SolrFields.NTRIPLE, SIREN_NUMERIC_FIELD_PRECISION_STEP, -1000L, 1000L, true, true);
    assertHitCount(numQuery, 0, 5, "long range");
    numQuery = SirenNumericRangeQuery.newDoubleRange(SolrFields.NTRIPLE, SIREN_NUMERIC_FIELD_PRECISION_STEP, -1000d, 1000d, true, true);
    assertHitCount(numQuery, 0, 5, "double range");
    numQuery = SirenNumericRangeQuery.newFloatRange(SolrFields.NTRIPLE, SIREN_NUMERIC_FIELD_PRECISION_STEP, 0f, 50f, true, true);
    assertHitCount(numQuery, 9, 10, "float range");
  }

  /**
   * Test used to find out how to get the equivalent of this query:
   * ASK where {
   * ?this gr:color ?color.
   * FILTER (?color not in ("natural","black"))
   * }
   *
   * @throws Exception
   */
  @Test
  @Ignore
  public void testNegativeMatch() throws Exception
  {
    SirenBooleanQuery booleanQuery = new SirenBooleanQuery();
    booleanQuery.add(new SirenTermQuery(new Term(SolrFields.NTRIPLE, "natural")), SirenBooleanClause.Occur.MUST_NOT);
    booleanQuery.add(new SirenTermQuery(new Term(SolrFields.NTRIPLE, "black")), SirenBooleanClause.Occur.MUST_NOT);
    //booleanQuery.add(new SirenTermQuery(new Term(SolrFields.NTRIPLE, "birch")), SirenBooleanClause.Occur.SHOULD);
    //booleanQuery.add(new SirenTermQuery(new Term(SolrFields.NTRIPLE, "red")), SirenBooleanClause.Occur.SHOULD);
    booleanQuery.add(new SirenWildcardQuery(new Term(SolrFields.NTRIPLE, "*")), SirenBooleanClause.Occur.MUST);
    BooleanQuery outerQuery = new BooleanQuery();

    outerQuery.add(new SirenTermQuery(new Term(SolrFields.NTRIPLE, "http://purl.org/goodrelations/v1#color")), BooleanClause.Occur.SHOULD);
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

  @Test
  public void testRdfMatcherForHotel() throws IOException
  {
    assertTopHit("27", "24");
  }

  /**
   * ******************** UTILS ***********************************
   */


  private void assertTopHit(String docId, String topHitDocNum) throws IOException
  {
    Matcher m = new Matcher(searcher, new ScoreTransformer(), URI.create("http://www.example.com/matcher"));
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
    System.out.println(msg + ": " + result.totalHits + " docs found");
    DecimalFormat fmt = new DecimalFormat("##.###");
    for (int i = 0; i < result.scoreDocs.length; i++) {
      System.out.println(msg + "[" + i + "]: " + fmt.format(result.scoreDocs[i].score) + ", " + reader.document(result.scoreDocs[i].doc).getValues(SolrFields.URL)[0]);
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
    // SPAQRL queries embedded in the RDF can't use the ns prefixes of the surrounding
    // RDF when it has been converted to ntriples, this leads to an error.
    // The quick fix is to add @prefix declarations in all embedded SPARQL queries, but obviously,
    // that's not ideal.
    // Solution: when a document is added to the index, all sp:text triples should be
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


  /**
   * ******************** TEST DATA ***********************************
   */

  private static Map<String, SolrInputDocument> getTestData() throws FileNotFoundException
  {
    Map<String, SolrInputDocument> docs = new HashMap<String, SolrInputDocument>();

    SolrInputDocument doc = new SolrInputDocument();
    String url = "http://www.example.com/resource/need/1";
    doc.addField(SolrFields.URL, url);
    doc.addField(SolrFields.TITLE, "Sofa");
    doc.addField(SolrFields.DESCRIPTION, "I have a very nice red sofa to give away.");
    doc.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_SUPPLY.getURI());
    doc.addField(SolrFields.TAG, "sofa");
    doc.addField(SolrFields.TAG, "red");
    doc.addField(SolrFields.TAG, "leather");
    doc.addField(SolrFields.TAG, "used");
    doc.addField(SolrFields.PRICE, "10.0-100.0");
    doc.addField(SolrFields.LOCATION, "48.2088,16.3726");
    doc.addField(SolrFields.DURATION, "2013-08-01T00:01:00.000Z/2013-08-30T23:00:00.000Z");

    docs.put(url, doc);

    //11km away from other points
    doc = new SolrInputDocument();
    url = "http://www.example.com/resource/need/2";
    doc.addField(SolrFields.URL, url);
    doc.addField(SolrFields.TITLE, "Sofa or couch");
    doc.addField(SolrFields.DESCRIPTION, "I am giving away my couch.");
    doc.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_SUPPLY.getURI());
    doc.addField(SolrFields.TAG, "blue");
    doc.addField(SolrFields.TAG, "dirty");
    doc.addField(SolrFields.TAG, "couch");
    doc.addField(SolrFields.PRICE, "50.0-*");
    doc.addField(SolrFields.LOCATION, "48.3089,16.3726");
    doc.addField(SolrFields.DURATION, "2013-07-01T00:01:00.000Z/2013-08-30T23:00:00.000Z");

    docs.put(url, doc);

    doc = new SolrInputDocument();
    url = "http://www.example.com/resource/need/3";
    doc.addField(SolrFields.URL, url);
    doc.addField(SolrFields.TITLE, "House");
    doc.addField(SolrFields.DESCRIPTION, "Selling a 3 story house in the suburbs of Vienna. Ideal for a big family with kids.");
    doc.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_SUPPLY.getURI());
    doc.addField(SolrFields.TAG, "house");
    doc.addField(SolrFields.TAG, "family");
    doc.addField(SolrFields.TAG, "suburbs");
    doc.addField(SolrFields.TAG, "kids");
    doc.addField(SolrFields.PRICE, "100000.0-500000.0");
    doc.addField(SolrFields.LOCATION, "48.2088,16.3726");

    docs.put(url, doc);


    doc = new SolrInputDocument();
    url = "http://www.example.com/resource/need/4";
    doc.addField(SolrFields.URL, url);
    doc.addField(SolrFields.TITLE, "Sofa");
    doc.addField(SolrFields.DESCRIPTION, "I need a sofa.");
    doc.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc.addField(SolrFields.TAG, "sofa");
    doc.addField(SolrFields.TAG, "furniture");
    doc.addField(SolrFields.PRICE, "*-150.0");
    doc.addField(SolrFields.LOCATION, "48.2088,16.3726");
    doc.addField(SolrFields.DURATION, "2013-06-01T00:01:00.000Z/2013-07-30T23:00:00.000Z");


    docs.put(url, doc);

    doc = new SolrInputDocument();
    url = "http://www.example.com/resource/need/5";
    doc.addField(SolrFields.URL, url);
    doc.addField(SolrFields.TITLE, "Looking for sofa or couch");
    doc.addField(SolrFields.DESCRIPTION, "I am looking for a sofa or a couch for my living room.");
    doc.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc.addField(SolrFields.TAG, "sofa");
    doc.addField(SolrFields.TAG, "blue");
    doc.addField(SolrFields.TAG, "red");
    doc.addField(SolrFields.TAG, "couch");
    doc.addField(SolrFields.TAG, "leather");
    doc.addField(SolrFields.PRICE, "*-50.0");
    doc.addField(SolrFields.LOCATION, "48.2088,16.3726");
    doc.addField(SolrFields.DURATION, "2013-07-01T00:01:00.000Z/2013-09-30T23:00:00.000Z");

    docs.put(url, doc);

    doc = new SolrInputDocument();
    url = "http://www.example.com/resource/need/6";
    doc.addField(SolrFields.URL, url);
    doc.addField(SolrFields.TITLE, "Looking for a place to live");
    doc.addField(SolrFields.DESCRIPTION, "Me and my family are looking for a house or a large apartment! Thank you.");
    doc.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc.addField(SolrFields.TAG, "house");
    doc.addField(SolrFields.TAG, "apartment");
    doc.addField(SolrFields.TAG, "family");
    doc.addField(SolrFields.PRICE, "*-250000.0");
    doc.addField(SolrFields.LOCATION, "48.2088,16.3726");

    docs.put(url, doc);

    //a document with ntriples content
    doc = new SolrInputDocument();
    url = "http://www.example.com/resource/need/7";
    doc.addField(SolrFields.URL, "http://www.example.com/resource/need/7");
    doc.addField(SolrFields.TITLE, "Table");
    doc.addField(SolrFields.DESCRIPTION, "");
    doc.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_SUPPLY.getURI());
    doc.addField(SolrFields.TAG, "table");
    doc.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_7_content_table1.ttl", url));

    docs.put(url, doc);

    //another document with ntriples content identical to doc7, the rest different
    doc = new SolrInputDocument();
    url = "http://www.example.com/resource/need/8";
    doc.addField(SolrFields.URL, url);
    doc.addField(SolrFields.TITLE, "Tisch");
    doc.addField(SolrFields.DESCRIPTION, "");
    doc.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc.addField(SolrFields.TAG, "tisch");
    doc.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_8_content_cupboard_sameas_1.ttl", url));
    docs.put(url, doc);

    //another document with ntriples content slightly different from doc 7 and 8, the rest different from all others
    doc = new SolrInputDocument();
    url = "http://www.example.com/resource/need/9";
    doc.addField(SolrFields.URL, url);
    doc.addField(SolrFields.TITLE, "mesa");
    doc.addField(SolrFields.DESCRIPTION, "");
    doc.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc.addField(SolrFields.TAG, "mesa");
    doc.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_9_content_cupboard_similar_to_1.ttl", url));
    docs.put(url, doc);

    //another document with ntriples content identical to doc7 (the blank nodes are named differently), the rest different
    doc = new SolrInputDocument();
    url = "http://www.example.com/resource/need/10";
    doc.addField(SolrFields.URL, "http://www.example.com/resource/need/10");
    doc.addField(SolrFields.TITLE, "Bord");
    doc.addField(SolrFields.DESCRIPTION, "");
    doc.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc.addField(SolrFields.TAG, "bord");
    doc.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_10_content_cupboard_similar_to_1b.ttl", url));
    docs.put(url, doc);


    //another document with ntriples content containing SPIN restrictions
    doc = new SolrInputDocument();
    url = "http://www.example.com/resource/need/11";
    doc.addField(SolrFields.URL, url);
    doc.addField(SolrFields.TITLE, "Kästchen");
    doc.addField(SolrFields.DESCRIPTION, "");
    doc.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc.addField(SolrFields.TAG, "kästchen");
    doc.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_11_spin_le_3times.ttl", url));
    docs.put(url, doc);


    //another document with ntriples content (matches 11)
    doc = new SolrInputDocument();
    url = "http://www.example.com/resource/need/12";
    doc.addField(SolrFields.URL, url);
    doc.addField(SolrFields.TITLE, "Cupboard");
    doc.addField(SolrFields.DESCRIPTION, "");
    doc.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_SUPPLY.getURI());
    doc.addField(SolrFields.TAG, "cupboard");
    doc.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_12_content_cupboard_45_45_15.ttl", url));
    docs.put(url, doc);


    //same as 12, but doesn't match the spin restrictions of 11
    doc = new SolrInputDocument();
    url = "http://www.example.com/resource/need/13";
    doc.addField(SolrFields.URL, url);
    doc.addField(SolrFields.TITLE, "Cupboard");
    doc.addField(SolrFields.DESCRIPTION, "");
    doc.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_SUPPLY.getURI());
    doc.addField(SolrFields.TAG, "cupboard");
    doc.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_13_content_cupboard_100_100_100.ttl", url));
    docs.put(url, doc);


    //same as 12, but doesn't match the spin restrictions of 11, and lacks width
    doc = new SolrInputDocument();
    url = "http://www.example.com/resource/need/14";
    doc.addField(SolrFields.URL, url);
    doc.addField(SolrFields.TITLE, "Cupboard");
    doc.addField(SolrFields.DESCRIPTION, "");
    doc.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_SUPPLY.getURI());
    doc.addField(SolrFields.TAG, "cupboard");
    doc.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_14_content_cupboard_200_200.ttl", url));
    docs.put(url, doc);


    //another document with ntriples content containing SPIN restrictions
    doc = new SolrInputDocument();
    url = "http://www.example.com/resource/need/15";
    doc.addField(SolrFields.URL, url);
    doc.addField(SolrFields.TITLE, "Cupboard");
    doc.addField(SolrFields.DESCRIPTION, "");
    doc.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_SUPPLY.getURI());
    doc.addField(SolrFields.TAG, "cupboard");
    doc.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_15_content_cupboard_40_40_18_different_literals.ttl", url));
    docs.put(url, doc);

    //like 11 (embedded spin), but with restrictions combined with &&
    doc = new SolrInputDocument();
    url = "http://www.example.com/resource/need/16";
    doc.addField(SolrFields.URL, url);
    doc.addField(SolrFields.TITLE, "Kästchen");
    doc.addField(SolrFields.DESCRIPTION, "");
    doc.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc.addField(SolrFields.TAG, "kästchen");
    doc.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_16_spin_ge_le_and.ttl", url));
    docs.put(url, doc);

    //like 11 (embedded spin), but with restrictions combined with && and ||
    doc = new SolrInputDocument();
    url = "http://www.example.com/resource/need/17";
    doc.addField(SolrFields.URL, url);
    doc.addField(SolrFields.TITLE, "Kästchen");
    doc.addField(SolrFields.DESCRIPTION, "");
    doc.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc.addField(SolrFields.TAG, "kästchen");
    doc.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_17_spin_ge_le_and_or.ttl", url));
    docs.put(url, doc);

    //like 11 (embedded spin), but with SPARQL as text (not in SPIN rdf notation)
    doc = new SolrInputDocument();
    url = "http://www.example.com/resource/need/18";
    doc.addField(SolrFields.URL, url);
    doc.addField(SolrFields.TITLE, "");
    doc.addField(SolrFields.DESCRIPTION, "");
    doc.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc.addField(SolrFields.TAG, "");
    doc.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_18_spin_le_sp-text.ttl", url));
    docs.put(url, doc);

    //like 11 (matches 12 exactly) but without SPIN
    doc = new SolrInputDocument();
    url = "http://www.example.com/resource/need/19";
    doc.addField(SolrFields.URL, url);
    doc.addField(SolrFields.TITLE, "");
    doc.addField(SolrFields.DESCRIPTION, "");
    doc.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc.addField(SolrFields.TAG, "");
    doc.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_19_content_cupboard_45_45_15_exact-match.ttl", url));
    docs.put(url, doc);

    // a very simple need, no title, desc etc, just one content triple
    doc = new SolrInputDocument();
    url = "http://www.example.com/resource/need/20";
    doc.addField(SolrFields.URL, url);
    doc.addField(SolrFields.TITLE, "");
    doc.addField(SolrFields.DESCRIPTION, "");
    doc.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc.addField(SolrFields.TAG, "");
    doc.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_20_content_table_simple.ttl", url));
    docs.put(url, doc);

    //like 18 (embedded SPARQL as text) but for testing '=' and '!='
    // a very simple need, no title, desc etc, just one content triple
    doc = new SolrInputDocument();
    url = "http://www.example.com/resource/need/21";
    doc.addField(SolrFields.URL, url);
    doc.addField(SolrFields.TITLE, "");
    doc.addField(SolrFields.DESCRIPTION, "");
    doc.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc.addField(SolrFields.TAG, "");
    doc.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_21_spin_eq_ne-text.ttl", url));
    docs.put(url, doc);

    //testing sparql "in" function
    doc = new SolrInputDocument();
    url = "http://www.example.com/resource/need/22";
    doc.addField(SolrFields.URL, url);
    doc.addField(SolrFields.TITLE, "");
    doc.addField(SolrFields.DESCRIPTION, "");
    doc.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc.addField(SolrFields.TAG, "");
    doc.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_22_spin_in-text.ttl", url));
    docs.put(url, doc);

    //testing sparql "not in" function
    doc = new SolrInputDocument();
    url = "http://www.example.com/resource/need/23";
    doc.addField(SolrFields.URL, url);
    doc.addField(SolrFields.TITLE, "");
    doc.addField(SolrFields.DESCRIPTION, "");
    doc.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc.addField(SolrFields.TAG, "");
    doc.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_23_spin_notin-text.ttl", url));
    docs.put(url, doc);

    //testing a need from the hotel domain
    doc = new SolrInputDocument();
    url = "http://www.example.com/resource/need/24";
    doc.addField(SolrFields.URL, url);
    doc.addField(SolrFields.TITLE, "");
    doc.addField(SolrFields.DESCRIPTION, "");
    doc.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_SUPPLY.getURI());
    doc.addField(SolrFields.TAG, "");
    doc.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_24_content_hotel_1.ttl", url));
    docs.put(url, doc);

    //testing a need from the hotel domain
    doc = new SolrInputDocument();
    url = "http://www.example.com/resource/need/25";
    doc.addField(SolrFields.URL, url);
    doc.addField(SolrFields.TITLE, "");
    doc.addField(SolrFields.DESCRIPTION, "");
    doc.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_SUPPLY.getURI());
    doc.addField(SolrFields.TAG, "");
    doc.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_25_content_hotel_2.ttl", url));
    docs.put(url, doc);

    //testing a need from the hotel domain
    doc = new SolrInputDocument();
    url = "http://www.example.com/resource/need/26";
    doc.addField(SolrFields.URL, url);
    doc.addField(SolrFields.TITLE, "");
    doc.addField(SolrFields.DESCRIPTION, "");
    doc.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_SUPPLY.getURI());
    doc.addField(SolrFields.TAG, "");
    doc.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_26_content_hotel_3.ttl", url));
    docs.put(url, doc);

    //testing a need from the hotel domain
    doc = new SolrInputDocument();
    url = "http://www.example.com/resource/need/27";
    doc.addField(SolrFields.URL, url);
    doc.addField(SolrFields.TITLE, "");
    doc.addField(SolrFields.DESCRIPTION, "");
    doc.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc.addField(SolrFields.TAG, "");
    doc.addField(SolrFields.NTRIPLE, getNTriples("won-matcher-solr/src/test/resources/docs/test_27_content_hotel_4.ttl", url));
    docs.put(url, doc);
    return docs;

  }
}
