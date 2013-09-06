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
import junit.framework.Assert;
import org.apache.lucene.index.Term;
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
import org.junit.Test;
import org.sindice.siren.search.*;
import org.xml.sax.SAXException;
import won.matcher.Matcher;
import won.matcher.processor.MatchProcessor;
import won.protocol.solr.SolrFields;
import won.protocol.vocabulary.WON;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
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
  private static final String SOLR_CORE_NAME = "webofneeds";
  private static final int SIREN_NUMERIC_FIELD_PRECISION_STEP = 4;

  @BeforeClass
  public static void setup() throws IOException, SAXException, ParserConfigurationException, SolrServerException, InterruptedException
  {


    // Note that the following property could be set through JVM level arguments too
    System.setProperty("solr.solr.home", "won-matcher-solr/src/test/resources/solr");

    //first, delete the index dir if it's there
    File indexDir = new File(System.getProperty("solr.solr.home"),"data/index");
    System.out.println("deleting index dir: " + indexDir);
    File[] indexDirContents = indexDir.listFiles();
    boolean deleteSuccessful = true;
    for(int i =0; i < indexDirContents.length && deleteSuccessful; i++){
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

    server.add(getTestData());
    System.out.println("test documents added to solr server, waiting for commit..");
    server.commit(true,true);
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

  /**
   * Test supposed to find some documents by a one-term query
   */
  @Test
  public void testSirenTermQuery() throws IOException
  {
    SirenTermQuery query = new SirenTermQuery(new Term(SolrFields.NTRIPLE,"hasvalue"));
    TopDocs result = searcher.search(query, 5);
    Assert.assertEquals(5, result.scoreDocs.length);
    for (int i =0; i < result.scoreDocs.length; i++){
      System.out.println("doc " + i + ": " + reader.document(result.scoreDocs[i].doc).getValues(SolrFields.URL)[0]);
    }

    SirenCellQuery cellQuery = new SirenCellQuery(query);
    cellQuery.setConstraint(1);
    result = searcher.search(cellQuery, 5);
    Assert.assertEquals(5, result.scoreDocs.length);
    for (int i =0; i < result.scoreDocs.length; i++){
      System.out.println("doc " + i + ": " + reader.document(result.scoreDocs[i].doc).getValues(SolrFields.URL)[0]);
    }
  }

  /**
   * Test supposed to find some documents by a one-term query
   */
  @Test
  public void testSirenCellQuery() throws IOException
  {
    SirenTermQuery query = new SirenTermQuery(new Term(SolrFields.NTRIPLE,"hasvalue"));
    SirenCellQuery cellQuery = new SirenCellQuery(query);
    cellQuery.setConstraint(1);
    TopDocs result = searcher.search(cellQuery, 5);
    Assert.assertEquals(5, result.scoreDocs.length);

    cellQuery.setConstraint(0);
    result = searcher.search(cellQuery, 5);
    for (int i =0; i < result.scoreDocs.length; i++){
      System.out.println("cell query doc " + i + ": " + reader.document(result.scoreDocs[i].doc).getValues(SolrFields.URL)[0]);
    }
    Assert.assertEquals(0, result.scoreDocs.length);
  }

  /**
   * Test supposed to find some documents by a one-term query
   */
  @Test
  public void testSirenTupleQuery() throws IOException
  {
    SirenTermQuery predTermQuery = new SirenTermQuery(new Term(SolrFields.NTRIPLE,"color"));
    SirenCellQuery predCellQuery = new SirenCellQuery(predTermQuery);
    predCellQuery.setConstraint(1);
    SirenTermQuery objTermQuery = new SirenTermQuery(new Term(SolrFields.NTRIPLE,"natural"));
    SirenCellQuery objCellQuery = new SirenCellQuery(objTermQuery);
    objCellQuery.setConstraint(2);
    SirenTupleQuery tupleQuery = new SirenTupleQuery();
    tupleQuery.add(predCellQuery, SirenTupleClause.Occur.MUST);
    tupleQuery.add(objCellQuery, SirenTupleClause.Occur.SHOULD);
    TopDocs result = searcher.search(tupleQuery, 5);
    Assert.assertEquals(1, result.scoreDocs.length);
    for (int i =0; i < result.scoreDocs.length; i++){
      System.out.println("doc " + i + ": " + reader.document(result.scoreDocs[i].doc).getValues(SolrFields.URL)[0]);
    }
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
    TopDocs result = searcher.search(cellQuery, 5);
    Assert.assertEquals(3, result.scoreDocs.length);
    for (int i =0; i < result.scoreDocs.length; i++){
      System.out.println("doc " + i + ": " + reader.document(result.scoreDocs[i].doc).getValues(SolrFields.URL)[0]);
    }
  }

  /**
   * Test supposed to find some documents by numeric attributes
   */
  @Test
  public void testNumericRangeQuery() throws IOException
  {
    SirenNumericRangeQuery numQuery = SirenNumericRangeQuery.newIntRange(SolrFields.NTRIPLE, SIREN_NUMERIC_FIELD_PRECISION_STEP, -1000, 1000, true, true);
    TopDocs result = searcher.search(numQuery, 5);
    Assert.assertEquals(0, result.scoreDocs.length);
    System.out.println("int query finds " + result.totalHits + " docs");
    for (int i =0; i < result.scoreDocs.length; i++){
      System.out.println("int docs: " + i + ": " + reader.document(result.scoreDocs[i].doc).getValues(SolrFields.URL)[0]);
    }

    numQuery = SirenNumericRangeQuery.newLongRange(SolrFields.NTRIPLE, SIREN_NUMERIC_FIELD_PRECISION_STEP, -1000L, 1000L, true, true);
    result = searcher.search(numQuery, 5);
    System.out.println("long query finds " + result.totalHits + " docs");
    Assert.assertEquals(0, result.scoreDocs.length);
    for (int i =0; i < result.scoreDocs.length; i++){
      System.out.println("long docs: " + i + ": " + reader.document(result.scoreDocs[i].doc).getValues(SolrFields.URL)[0]);
    }

    numQuery = SirenNumericRangeQuery.newDoubleRange(SolrFields.NTRIPLE, SIREN_NUMERIC_FIELD_PRECISION_STEP, -1000d, 1000d, true, true);
    result = searcher.search(numQuery, 5);
    System.out.println("double query finds " + result.totalHits + " docs");
    Assert.assertEquals(0, result.scoreDocs.length);
    for (int i =0; i < result.scoreDocs.length; i++){
      System.out.println("double docs: " + i + ": " + reader.document(result.scoreDocs[i].doc).getValues(SolrFields.URL)[0]);
    }

    numQuery = SirenNumericRangeQuery.newFloatRange(SolrFields.NTRIPLE, SIREN_NUMERIC_FIELD_PRECISION_STEP, 0f, 50f, true, true);
    result = searcher.search(numQuery, 5);
    System.out.println("float query finds " + result.totalHits + " docs");
    Assert.assertEquals(3, result.scoreDocs.length);
    for (int i =0; i < result.scoreDocs.length; i++){
      System.out.println("float docs: " + i + ": " + reader.document(result.scoreDocs[i].doc).getValues(SolrFields.URL)[0]);
    }
  }

  @Test
  public void testComplexRdfMatcherWithEmbeddedSparql() throws IOException
  {
    Matcher m = new Matcher(searcher);
    HintCountingMatchProcessor proc = new HintCountingMatchProcessor();
    m.addMatchProcessor(proc);
    List<SolrInputDocument> docs = getTestData();
    m.processDocument(docs.get(10)); //doc nr 11 (index 10) is the one containing sparql
    m.callMatchProcessors();
    List<MatchScore> scores = proc.getMatchScores(URI.create(docs.get(10).getFieldValue(SolrFields.URL).toString()));
    URI bestMatchURL = URI.create(docs.get(11).getFieldValue(SolrFields.URL).toString()); //we know that doc 12 is the best match
    URI topHit = scores.get(0).uri;
    Assert.assertEquals(topHit, bestMatchURL);
  }


  private static List<SolrInputDocument> getTestData()
  {
    List<SolrInputDocument> docs = new ArrayList<>();

    SolrInputDocument doc1 = new SolrInputDocument();
    doc1.addField(SolrFields.URL, "http://www.example.com/ld/need/1");
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

    docs.add(doc1);

    //11km away from other points
    SolrInputDocument doc2 = new SolrInputDocument();
    doc2.addField(SolrFields.URL, "http://www.example.com/ld/need/2");
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

    docs.add(doc2);

    SolrInputDocument doc3 = new SolrInputDocument();
    doc3.addField(SolrFields.URL, "http://www.example.com/ld/need/3");
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


    SolrInputDocument doc4 = new SolrInputDocument();
    doc4.addField(SolrFields.URL, "http://www.example.com/ld/need/4");
    doc4.addField(SolrFields.TITLE, "Sofa");
    doc4.addField(SolrFields.DESCRIPTION, "I need a sofa.");
    doc4.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc4.addField(SolrFields.TAG, "sofa");
    doc4.addField(SolrFields.TAG, "furniture");
    doc4.addField(SolrFields.UPPER_PRICE_LIMIT, 150.0);
    doc4.addField(SolrFields.LOCATION, "48.2088,16.3726");
    doc4.addField(SolrFields.TIME_START, "2013-06-01T00:01:00.000Z");
    doc4.addField(SolrFields.TIME_END, "2013-07-30T23:00:00.000Z");


    docs.add(doc4);

    SolrInputDocument doc5 = new SolrInputDocument();
    doc5.addField(SolrFields.URL, "http://www.example.com/ld/need/5");
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

    docs.add(doc5);

    SolrInputDocument doc6 = new SolrInputDocument();
    doc6.addField(SolrFields.URL, "http://www.example.com/ld/need/6");
    doc6.addField(SolrFields.TITLE, "Looking for a place to live");
    doc6.addField(SolrFields.DESCRIPTION, "Me and my family are looking for a house or a large apartment! Thank you.");
    doc6.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc6.addField(SolrFields.TAG, "house");
    doc6.addField(SolrFields.TAG, "apartment");
    doc6.addField(SolrFields.TAG, "family");
    doc6.addField(SolrFields.UPPER_PRICE_LIMIT, 250000.0);
    doc6.addField(SolrFields.LOCATION, "48.2088,16.3726");

    docs.add(doc6);

    //a document with ntriples content
    SolrInputDocument doc7 = new SolrInputDocument();
    doc7.addField(SolrFields.URL, "http://www.example.com/ld/need/7");
    doc7.addField(SolrFields.TITLE, "Table");
    doc7.addField(SolrFields.DESCRIPTION, "");
    doc7.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_SUPPLY.getURI());
    doc7.addField(SolrFields.TAG, "table");
    doc7.addField(SolrFields.NTRIPLE, "_:b3 <http://furniture.com/ontology/productionYear> \"1974\" .\n" +
        "_:b3 <http://dbpedia.org/property/material> <http://dbpedia.org/resource/Oak> .\n" +
        "_:b3 <http://dbpedia.org/property/material> <http://dbpedia.org/resource/Wood> .\n" +
        "_:b3 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/resource/Table> .\n" +
        "<http://www.example.com/ld/need/7> <http://purl.org/webofneeds/model#hasContent> _:b2 .\n" +
        "_:b2 <http://purl.org/webofneeds/model#hasContentDescription> _:b3 .\n" +
        "_:b2 <http://purl.org/dc/elements/1.1/title> \"Table\"^^<http://www.w3.org/2001/XMLSchema#string> .\n" +
        "_:b2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/webofneeds/model#NeedContent> .\n");

    docs.add(doc7);

    //another document with ntriples content identical to doc7, the rest different
    SolrInputDocument doc8 = new SolrInputDocument();
    doc8.addField(SolrFields.URL, "http://www.example.com/ld/need/8");
    doc8.addField(SolrFields.TITLE, "Tisch");
    doc8.addField(SolrFields.DESCRIPTION, "");
    doc8.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc8.addField(SolrFields.TAG, "tisch");
    doc8.addField(SolrFields.NTRIPLE, "_:b3 <http://furniture.com/ontology/productionYear> \"1974\" .\n" +
        "_:b3 <http://dbpedia.org/property/material> <http://dbpedia.org/resource/Oak> .\n" +
        "_:b3 <http://dbpedia.org/property/material> <http://dbpedia.org/resource/Wood> .\n" +
        "_:b3 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/resource/Table> .\n" +
        "<http://www.example.com/ld/need/8> <http://purl.org/webofneeds/model#hasContent> _:b2 .\n" +
        "_:b2 <http://purl.org/webofneeds/model#hasContentDescription> _:b3 .\n" +
        "_:b2 <http://purl.org/dc/elements/1.1/title> \"Table\"^^<http://www.w3.org/2001/XMLSchema#string> .\n" +
        "_:b2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/webofneeds/model#NeedContent> .\n");
//    doc8.addField(SolrFields.NTRIPLE,
 //        "<http://www.example.com/something> <http://furniture.com/ontology/productionYear> \"1974\" .\n"
    //        +
    //    "<http://www.example.com/something> <http://dbpedia.org/property/material> <http://dbpedia.org/resource/Wood> . \n"
    //);
    docs.add(doc8);

    //another document with ntriples content slightly different from doc 7 and 8, the rest different from all others
    SolrInputDocument doc9= new SolrInputDocument();
    doc9.addField(SolrFields.URL, "http://www.example.com/ld/need/9");
    doc9.addField(SolrFields.TITLE, "mesa");
    doc9.addField(SolrFields.DESCRIPTION, "");
    doc9.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc9.addField(SolrFields.TAG, "mesa");
    doc9.addField(SolrFields.NTRIPLE, "_:b3 <http://furniture.com/ontology/productionYear> \"1974\" .\n" +
        "_:b3 <http://dbpedia.org/property/material> <http://dbpedia.org/resource/Pine> .\n" +
        "_:b3 <http://dbpedia.org/property/material> <http://dbpedia.org/resource/Wood> .\n" +
        "_:b3 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/resource/Table> .\n" +
        "<http://www.example.com/ld/need/9> <http://purl.org/webofneeds/model#hasContent> _:b2 .\n" +
        "_:b2 <http://purl.org/webofneeds/model#hasContentDescription> _:b3 .\n" +
        "_:b2 <http://purl.org/dc/elements/1.1/title> \"Table\"^^<http://www.w3.org/2001/XMLSchema#string> .\n" +
        "_:b2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/webofneeds/model#NeedContent> .\n");
//    doc8.addField(SolrFields.NTRIPLE,
    //        "<http://www.example.com/something> <http://furniture.com/ontology/productionYear> \"1974\" .\n"
    //        +
    //    "<http://www.example.com/something> <http://dbpedia.org/property/material> <http://dbpedia.org/resource/Wood> . \n"
    //);
    docs.add(doc9);

    //another document with ntriples content identical to doc7 (the blank nodes are named differently), the rest different
    SolrInputDocument doc10 = new SolrInputDocument();
    doc10.addField(SolrFields.URL, "http://www.example.com/ld/need/10");
    doc10.addField(SolrFields.TITLE, "Bord");
    doc10.addField(SolrFields.DESCRIPTION, "");
    doc10.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc10.addField(SolrFields.TAG, "bord");
    doc10.addField(SolrFields.NTRIPLE, "_:bl3 <http://furniture.com/ontology/productionYear> \"1974\" .\n" +
        "_:bl3 <http://dbpedia.org/property/material> <http://dbpedia.org/resource/Oak> .\n" +
        "_:bl3 <http://dbpedia.org/property/material> <http://dbpedia.org/resource/Wood> .\n" +
        "_:bl3 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/resource/Table> .\n" +
        "<http://www.example.com/ld/need/10> <http://purl.org/webofneeds/model#hasContent> _:bl2 .\n" +
        "_:bl2 <http://purl.org/webofneeds/model#hasContentDescription> _:bl3 .\n" +
        "_:bl2 <http://purl.org/dc/elements/1.1/title> \"Table\"^^<http://www.w3.org/2001/XMLSchema#string> .\n" +
        "_:bl2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/webofneeds/model#NeedContent> .\n");
    docs.add(doc10);

    String ntriples = null;

    ntriples =
        "<http://www.example.com/ld/need/11> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/webofneeds/model#Need> .\n" +
            "<http://www.example.com/ld/need/11> <http://purl.org/webofneeds/model#hasBasicNeedType> <http://purl.org/webofneeds/model#Demand> .\n" +
            "<http://www.example.com/ld/need/11> <http://purl.org/webofneeds/model#hasConnections> <http://www.example.com/ld/need/11/connections/> .\n" +
            "_:node1837l6657x1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/webofneeds/model#NeedContent> .\n" +
            "_:node1837l6657x2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/resource/Cupboard> .\n" +
            "_:node1837l6657x2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/goodrelations/v1#Individual> .\n" +
            "_:node1837l6657x3 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/goodrelations/v1#QuantitativeValueFloat> .\n" +
            "_:node1837l6657x3 <http://purl.org/goodrelations/v1#hasUnitOfMeasurement> \"CMT\"^^<http://www.w3.org/2001/XMLSchema#string> .\n" +
            "_:node1837l6657x4 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://spinrdf.org/sp#Ask> .\n" +
            "_:node1837l6657x7 <http://spinrdf.org/sp#varName> \"depth\" .\n" +
            "_:node1837l6657x6 <http://spinrdf.org/sp#object> _:node1837l6657x7 .\n" +
            "_:node1837l6657x6 <http://spinrdf.org/sp#predicate> <http://purl.org/goodrelations/v1#hasValue> .\n" +
            "_:node1837l6657x8 <http://spinrdf.org/sp#varName> \"this\" .\n" +
            "_:node1837l6657x6 <http://spinrdf.org/sp#subject> _:node1837l6657x8 .\n" +
            "_:node1837l6657x5 <http://www.w3.org/1999/02/22-rdf-syntax-ns#first> _:node1837l6657x6 .\n" +
            "_:node1837l6657x5 <http://www.w3.org/1999/02/22-rdf-syntax-ns#rest> _:node1837l6657x9 .\n" +
            "_:node1837l6657x10 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://spinrdf.org/sp#Filter> .\n" +
            "_:node1837l6657x11 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://spinrdf.org/sp#le> .\n" +
            "_:node1837l6657x12 <http://spinrdf.org/sp#varName> \"depth\" .\n" +
            "_:node1837l6657x11 <http://spinrdf.org/sp#arg1> _:node1837l6657x12 .\n" +
            "_:node1837l6657x11 <http://spinrdf.org/sp#arg2> \"50\"^^<http://www.w3.org/2001/XMLSchema#float> .\n" +
            "_:node1837l6657x10 <http://spinrdf.org/sp#expression> _:node1837l6657x11 .\n" +
            "_:node1837l6657x9 <http://www.w3.org/1999/02/22-rdf-syntax-ns#first> _:node1837l6657x10 .\n" +
            "_:node1837l6657x9 <http://www.w3.org/1999/02/22-rdf-syntax-ns#rest> <http://www.w3.org/1999/02/22-rdf-syntax-ns#nil> .\n" +
            "_:node1837l6657x4 <http://spinrdf.org/sp#where> _:node1837l6657x5 .\n" +
            "_:node1837l6657x3 <http://purl.org/webofneeds/model#embedSpinAsk> _:node1837l6657x4 .\n" +
            "_:node1837l6657x2 <http://purl.org/goodrelations/v1#depth> _:node1837l6657x3 .\n" +
            "_:node1837l6657x13 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/goodrelations/v1#QuantitativeValueFloat> .\n" +
            "_:node1837l6657x13 <http://purl.org/goodrelations/v1#hasUnitOfMeasurement> \"CMT\"^^<http://www.w3.org/2001/XMLSchema#string> .\n" +
            "_:node1837l6657x14 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://spinrdf.org/sp#Ask> .\n" +
            "_:node1837l6657x17 <http://spinrdf.org/sp#varName> \"height\" .\n" +
            "_:node1837l6657x16 <http://spinrdf.org/sp#object> _:node1837l6657x17 .\n" +
            "_:node1837l6657x16 <http://spinrdf.org/sp#predicate> <http://purl.org/goodrelations/v1#hasValue> .\n" +
            "_:node1837l6657x18 <http://spinrdf.org/sp#varName> \"this\" .\n" +
            "_:node1837l6657x16 <http://spinrdf.org/sp#subject> _:node1837l6657x18 .\n" +
            "_:node1837l6657x15 <http://www.w3.org/1999/02/22-rdf-syntax-ns#first> _:node1837l6657x16 .\n" +
            "_:node1837l6657x15 <http://www.w3.org/1999/02/22-rdf-syntax-ns#rest> _:node1837l6657x19 .\n" +
            "_:node1837l6657x20 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://spinrdf.org/sp#Filter> .\n" +
            "_:node1837l6657x21 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://spinrdf.org/sp#le> .\n" +
            "_:node1837l6657x22 <http://spinrdf.org/sp#varName> \"height\" .\n" +
            "_:node1837l6657x21 <http://spinrdf.org/sp#arg1> _:node1837l6657x22 .\n" +
            "_:node1837l6657x21 <http://spinrdf.org/sp#arg2> \"50\"^^<http://www.w3.org/2001/XMLSchema#float> .\n" +
            "_:node1837l6657x20 <http://spinrdf.org/sp#expression> _:node1837l6657x21 .\n" +
            "_:node1837l6657x19 <http://www.w3.org/1999/02/22-rdf-syntax-ns#first> _:node1837l6657x20 .\n" +
            "_:node1837l6657x19 <http://www.w3.org/1999/02/22-rdf-syntax-ns#rest> <http://www.w3.org/1999/02/22-rdf-syntax-ns#nil> .\n" +
            "_:node1837l6657x14 <http://spinrdf.org/sp#where> _:node1837l6657x15 .\n" +
            "_:node1837l6657x13 <http://purl.org/webofneeds/model#embedSpinAsk> _:node1837l6657x14 .\n" +
            "_:node1837l6657x2 <http://purl.org/goodrelations/v1#height> _:node1837l6657x13 .\n" +
            "_:node1837l6657x23 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/goodrelations/v1#QuantitativeValueFloat> .\n" +
            "_:node1837l6657x23 <http://purl.org/goodrelations/v1#hasUnitOfMeasurement> \"CMT\"^^<http://www.w3.org/2001/XMLSchema#string> .\n" +
            "_:node1837l6657x24 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://spinrdf.org/sp#Ask> .\n" +
            "_:node1837l6657x27 <http://spinrdf.org/sp#varName> \"width\" .\n" +
            "_:node1837l6657x26 <http://spinrdf.org/sp#object> _:node1837l6657x27 .\n" +
            "_:node1837l6657x26 <http://spinrdf.org/sp#predicate> <http://purl.org/goodrelations/v1#hasValue> .\n" +
            "_:node1837l6657x28 <http://spinrdf.org/sp#varName> \"this\" .\n" +
            "_:node1837l6657x26 <http://spinrdf.org/sp#subject> _:node1837l6657x28 .\n" +
            "_:node1837l6657x25 <http://www.w3.org/1999/02/22-rdf-syntax-ns#first> _:node1837l6657x26 .\n" +
            "_:node1837l6657x25 <http://www.w3.org/1999/02/22-rdf-syntax-ns#rest> _:node1837l6657x29 .\n" +
            "_:node1837l6657x30 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://spinrdf.org/sp#Filter> .\n" +
            "_:node1837l6657x31 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://spinrdf.org/sp#le> .\n" +
            "_:node1837l6657x32 <http://spinrdf.org/sp#varName> \"width\" .\n" +
            "_:node1837l6657x31 <http://spinrdf.org/sp#arg1> _:node1837l6657x32 .\n" +
            "_:node1837l6657x31 <http://spinrdf.org/sp#arg2> \"50\"^^<http://www.w3.org/2001/XMLSchema#float> .\n" +
            "_:node1837l6657x30 <http://spinrdf.org/sp#expression> _:node1837l6657x31 .\n" +
            "_:node1837l6657x29 <http://www.w3.org/1999/02/22-rdf-syntax-ns#first> _:node1837l6657x30 .\n" +
            "_:node1837l6657x29 <http://www.w3.org/1999/02/22-rdf-syntax-ns#rest> <http://www.w3.org/1999/02/22-rdf-syntax-ns#nil> .\n" +
            "_:node1837l6657x24 <http://spinrdf.org/sp#where> _:node1837l6657x25 .\n" +
            "_:node1837l6657x23 <http://purl.org/webofneeds/model#embedSpinAsk> _:node1837l6657x24 .\n" +
            "_:node1837l6657x2 <http://purl.org/goodrelations/v1#width> _:node1837l6657x23 .\n" +
            "_:node1837l6657x1 <http://purl.org/webofneeds/model#hasContentDescription> _:node1837l6657x2 .\n" +
            "<http://www.example.com/ld/need/11> <http://purl.org/webofneeds/model#hasContent> _:node1837l6657x1 .\n" +
            "_:node1837l6657x33 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/webofneeds/model#NeedModality> .\n" +
            "<http://www.example.com/ld/need/11> <http://purl.org/webofneeds/model#hasNeedModality> _:node1837l6657x33 .\n" +
            "<http://www.example.com/ld/need/11> <http://purl.org/webofneeds/model#isInState> <http://purl.org/webofneeds/model#Active> .\n" +
            "<http://www.example.com/ld/need/11> <http://purl.org/webofneeds/model#matcherProtocolEndpoint> <http://www.example.com/won/protocol/matcher> .\n" +
            "<http://www.example.com/ld/need/11> <http://purl.org/webofneeds/model#needCreationDate> \"2013-08-242T01:04:48.048+0000\" .\n" +
            "<http://www.example.com/ld/need/11> <http://purl.org/webofneeds/model#needProtocolEndpoint> <http://www.example.com/won/protocol/need> .\n" +
            "<http://www.example.com/ld/need/11> <http://purl.org/webofneeds/model#ownerProtocolEndpoint> <http://www.example.com/won/protocol/owner> .\n" +
            "<http://www.example.com/ld/need/11/connections/> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/ns/ldp#Container> .\n" +
            "\n";
    //another document with ntriples content containing SPIN restrictions
    SolrInputDocument doc11 = new SolrInputDocument();
    doc11.addField(SolrFields.URL, "http://www.example.com/ld/need/11");
    doc11.addField(SolrFields.TITLE, "Kästchen");
    doc11.addField(SolrFields.DESCRIPTION, "");
    doc11.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_DEMAND.getURI());
    doc11.addField(SolrFields.TAG, "kästchen");
    doc11.addField(SolrFields.NTRIPLE, ntriples);
    docs.add(doc11);


    ntriples =
      "<http://www.example.com/ld/need/12/connections/> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/ns/ldp#Container> .\n" +
          "<http://www.example.com/ld/need/12> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/webofneeds/model#Need> .\n" +
          "<http://www.example.com/ld/need/12> <http://purl.org/webofneeds/model#hasBasicNeedType> <http://purl.org/webofneeds/model#Supply> .\n" +
          "<http://www.example.com/ld/need/12> <http://purl.org/webofneeds/model#hasConnections> <http://www.example.com/ld/need/12/connections/> .\n" +
          "_:node1835a1pjfx1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/webofneeds/model#NeedContent> .\n" +
          "_:node1835a1pjfx1 <http://purl.org/dc/elements/1.1/title> \"cupboard\"^^<http://www.w3.org/2001/XMLSchema#string> .\n" +
          "_:node1835a1pjfx2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/resource/Cupboard> .\n" +
          "_:node1835a1pjfx2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/goodrelations/v1#Individual> .\n" +
          "_:node1835a1pjfx2 <http://purl.org/goodrelations/v1#color> \"natural birch\" .\n" +
          "_:node1835a1pjfx3 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/goodrelations/v1#QuantitativeValueFloat> .\n" +
          "_:node1835a1pjfx3 <http://purl.org/goodrelations/v1#hasUnitOfMeasurement> \"CMT\"^^<http://www.w3.org/2001/XMLSchema#string> .\n" +
          "_:node1835a1pjfx3 <http://purl.org/goodrelations/v1#hasValue> \"15\"^^<http://www.w3.org/2001/XMLSchema#float> .\n" +
          "_:node1835a1pjfx2 <http://purl.org/goodrelations/v1#depth> _:node1835a1pjfx3 .\n" +
          "_:node1835a1pjfx4 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/goodrelations/v1#QuantitativeValueFloat> .\n" +
          "_:node1835a1pjfx4 <http://purl.org/goodrelations/v1#hasUnitOfMeasurement> \"CMT\"^^<http://www.w3.org/2001/XMLSchema#string> .\n" +
          "_:node1835a1pjfx4 <http://purl.org/goodrelations/v1#hasValue> \"45\"^^<http://www.w3.org/2001/XMLSchema#float> .\n" +
          "_:node1835a1pjfx2 <http://purl.org/goodrelations/v1#height> _:node1835a1pjfx4 .\n" +
          "_:node1835a1pjfx5 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/goodrelations/v1#QuantitativeValueFloat> .\n" +
          "_:node1835a1pjfx5 <http://purl.org/goodrelations/v1#hasUnitOfMeasurement> \"CMT\"^^<http://www.w3.org/2001/XMLSchema#string> .\n" +
          "_:node1835a1pjfx5 <http://purl.org/goodrelations/v1#hasValue> \"45\"^^<http://www.w3.org/2001/XMLSchema#float> .\n" +
          "_:node1835a1pjfx2 <http://purl.org/goodrelations/v1#width> _:node1835a1pjfx5 .\n" +
          "_:node1835a1pjfx1 <http://purl.org/webofneeds/model#hasContentDescription> _:node1835a1pjfx2 .\n" +
          "<http://www.example.com/ld/need/12> <http://purl.org/webofneeds/model#hasContent> _:node1835a1pjfx1 .\n" +
          "_:node1835a1pjfx6 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/webofneeds/model#NeedModality> .\n" +
          "<http://www.example.com/ld/need/12> <http://purl.org/webofneeds/model#hasNeedModality> _:node1835a1pjfx6 .\n" +
          "<http://www.example.com/ld/need/12> <http://purl.org/webofneeds/model#isInState> <http://purl.org/webofneeds/model#Active> .\n" +
          "<http://www.example.com/ld/need/12> <http://purl.org/webofneeds/model#matcherProtocolEndpoint> <http://www.example.com/won/protocol/matcher> .\n" +
          "<http://www.example.com/ld/need/12> <http://purl.org/webofneeds/model#needCreationDate> \"2013-08-231T10:42:17.017+0000\" .\n" +
          "<http://www.example.com/ld/need/12> <http://purl.org/webofneeds/model#needProtocolEndpoint> <http://www.example.com/won/protocol/need> .\n" +
          "<http://www.example.com/ld/need/12> <http://purl.org/webofneeds/model#ownerProtocolEndpoint> <http://www.example.com/won/protocol/owner> .\n" +
          "\n";
    //another document with ntriples content containing SPIN restrictions
    SolrInputDocument doc12 = new SolrInputDocument();
    doc12.addField(SolrFields.URL, "http://www.example.com/ld/need/12");
    doc12.addField(SolrFields.TITLE, "Cupboard");
    doc12.addField(SolrFields.DESCRIPTION, "");
    doc12.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_SUPPLY.getURI());
    doc12.addField(SolrFields.TAG, "cupboard");
    doc12.addField(SolrFields.NTRIPLE, ntriples);
    docs.add(doc12);


    //same as 12, but doesn't match the spin restrictions of 11
    ntriples =
        "<http://www.example.com/ld/need/13/connections/> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/ns/ldp#Container> .\n" +
            "<http://www.example.com/ld/need/13> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/webofneeds/model#Need> .\n" +
            "<http://www.example.com/ld/need/13> <http://purl.org/webofneeds/model#hasBasicNeedType> <http://purl.org/webofneeds/model#Supply> .\n" +
            "<http://www.example.com/ld/need/13> <http://purl.org/webofneeds/model#hasConnections> <http://www.example.com/ld/need/13/connections/> .\n" +
            "_:node1835a1pjfx1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/webofneeds/model#NeedContent> .\n" +
            "_:node1835a1pjfx1 <http://purl.org/dc/elements/1.1/title> \"cupboard\"^^<http://www.w3.org/2001/XMLSchema#string> .\n" +
            "_:node1835a1pjfx2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/resource/Cupboard> .\n" +
            "_:node1835a1pjfx2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/goodrelations/v1#Individual> .\n" +
            "_:node1835a1pjfx2 <http://purl.org/goodrelations/v1#color> \"natural birch\" .\n" +
            "_:node1835a1pjfx3 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/goodrelations/v1#QuantitativeValueFloat> .\n" +
            "_:node1835a1pjfx3 <http://purl.org/goodrelations/v1#hasUnitOfMeasurement> \"CMT\"^^<http://www.w3.org/2001/XMLSchema#string> .\n" +
            "_:node1835a1pjfx3 <http://purl.org/goodrelations/v1#hasValue> \"100\"^^<http://www.w3.org/2001/XMLSchema#float> .\n" +
            "_:node1835a1pjfx2 <http://purl.org/goodrelations/v1#depth> _:node1835a1pjfx3 .\n" +
            "_:node1835a1pjfx4 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/goodrelations/v1#QuantitativeValueFloat> .\n" +
            "_:node1835a1pjfx4 <http://purl.org/goodrelations/v1#hasUnitOfMeasurement> \"CMT\"^^<http://www.w3.org/2001/XMLSchema#string> .\n" +
            "_:node1835a1pjfx4 <http://purl.org/goodrelations/v1#hasValue> \"100\"^^<http://www.w3.org/2001/XMLSchema#float> .\n" +
            "_:node1835a1pjfx2 <http://purl.org/goodrelations/v1#height> _:node1835a1pjfx4 .\n" +
            "_:node1835a1pjfx5 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/goodrelations/v1#QuantitativeValueFloat> .\n" +
            "_:node1835a1pjfx5 <http://purl.org/goodrelations/v1#hasUnitOfMeasurement> \"CMT\"^^<http://www.w3.org/2001/XMLSchema#string> .\n" +
            "_:node1835a1pjfx5 <http://purl.org/goodrelations/v1#hasValue> \"100\"^^<http://www.w3.org/2001/XMLSchema#float> .\n" +
            "_:node1835a1pjfx2 <http://purl.org/goodrelations/v1#width> _:node1835a1pjfx5 .\n" +
            "_:node1835a1pjfx1 <http://purl.org/webofneeds/model#hasContentDescription> _:node1835a1pjfx2 .\n" +
            "<http://www.example.com/ld/need/13> <http://purl.org/webofneeds/model#hasContent> _:node1835a1pjfx1 .\n" +
            "_:node1835a1pjfx6 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/webofneeds/model#NeedModality> .\n" +
            "<http://www.example.com/ld/need/13> <http://purl.org/webofneeds/model#hasNeedModality> _:node1835a1pjfx6 .\n" +
            "<http://www.example.com/ld/need/13> <http://purl.org/webofneeds/model#isInState> <http://purl.org/webofneeds/model#Active> .\n" +
            "<http://www.example.com/ld/need/13> <http://purl.org/webofneeds/model#matcherProtocolEndpoint> <http://www.example.com/won/protocol/matcher> .\n" +
            "<http://www.example.com/ld/need/13> <http://purl.org/webofneeds/model#needCreationDate> \"2013-08-231T10:42:17.017+0000\" .\n" +
            "<http://www.example.com/ld/need/13> <http://purl.org/webofneeds/model#needProtocolEndpoint> <http://www.example.com/won/protocol/need> .\n" +
            "<http://www.example.com/ld/need/13> <http://purl.org/webofneeds/model#ownerProtocolEndpoint> <http://www.example.com/won/protocol/owner> .\n" +
            "\n";
    //another document with ntriples content containing SPIN restrictions
    SolrInputDocument doc13 = new SolrInputDocument();
    doc13.addField(SolrFields.URL, "http://www.example.com/ld/need/13");
    doc13.addField(SolrFields.TITLE, "Cupboard");
    doc13.addField(SolrFields.DESCRIPTION, "");
    doc13.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_SUPPLY.getURI());
    doc13.addField(SolrFields.TAG, "cupboard");
    doc13.addField(SolrFields.NTRIPLE, ntriples);
    docs.add(doc13);



    //same as 12, but doesn't match the spin restrictions of 11 and lacks width
    ntriples =
        "<http://www.example.com/ld/need/14/connections/> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/ns/ldp#Container> .\n" +
            "<http://www.example.com/ld/need/14> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/webofneeds/model#Need> .\n" +
            "<http://www.example.com/ld/need/14> <http://purl.org/webofneeds/model#hasBasicNeedType> <http://purl.org/webofneeds/model#Supply> .\n" +
            "<http://www.example.com/ld/need/14> <http://purl.org/webofneeds/model#hasConnections> <http://www.example.com/ld/need/14/connections/> .\n" +
            "_:node1835a1pjfx1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/webofneeds/model#NeedContent> .\n" +
            "_:node1835a1pjfx1 <http://purl.org/dc/elements/1.1/title> \"cupboard\"^^<http://www.w3.org/2001/XMLSchema#string> .\n" +
            "_:node1835a1pjfx2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/resource/Cupboard> .\n" +
            "_:node1835a1pjfx2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/goodrelations/v1#Individual> .\n" +
            "_:node1835a1pjfx2 <http://purl.org/goodrelations/v1#color> \"natural birch\" .\n" +
            "_:node1835a1pjfx3 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/goodrelations/v1#QuantitativeValueFloat> .\n" +
            "_:node1835a1pjfx3 <http://purl.org/goodrelations/v1#hasUnitOfMeasurement> \"CMT\"^^<http://www.w3.org/2001/XMLSchema#string> .\n" +
            "_:node1835a1pjfx3 <http://purl.org/goodrelations/v1#hasValue> \"10000\"^^<http://www.w3.org/2001/XMLSchema#float> .\n" +
            "_:node1835a1pjfx2 <http://purl.org/goodrelations/v1#depth> _:node1835a1pjfx3 .\n" +
            "_:node1835a1pjfx4 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/goodrelations/v1#QuantitativeValueFloat> .\n" +
            "_:node1835a1pjfx4 <http://purl.org/goodrelations/v1#hasUnitOfMeasurement> \"CMT\"^^<http://www.w3.org/2001/XMLSchema#string> .\n" +
            "_:node1835a1pjfx4 <http://purl.org/goodrelations/v1#hasValue> \"10000\"^^<http://www.w3.org/2001/XMLSchema#float> .\n" +
            "_:node1835a1pjfx2 <http://purl.org/goodrelations/v1#height> _:node1835a1pjfx4 .\n" +
            "_:node1835a1pjfx1 <http://purl.org/webofneeds/model#hasContentDescription> _:node1835a1pjfx2 .\n" +
            "<http://www.example.com/ld/need/14> <http://purl.org/webofneeds/model#hasContent> _:node1835a1pjfx1 .\n" +
            "_:node1835a1pjfx6 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/webofneeds/model#NeedModality> .\n" +
            "<http://www.example.com/ld/need/14> <http://purl.org/webofneeds/model#hasNeedModality> _:node1835a1pjfx6 .\n" +
            "<http://www.example.com/ld/need/14> <http://purl.org/webofneeds/model#isInState> <http://purl.org/webofneeds/model#Active> .\n" +
            "<http://www.example.com/ld/need/14> <http://purl.org/webofneeds/model#matcherProtocolEndpoint> <http://www.example.com/won/protocol/matcher> .\n" +
            "<http://www.example.com/ld/need/14> <http://purl.org/webofneeds/model#needCreationDate> \"2013-08-231T10:42:17.017+0000\" .\n" +
            "<http://www.example.com/ld/need/14> <http://purl.org/webofneeds/model#needProtocolEndpoint> <http://www.example.com/won/protocol/need> .\n" +
            "<http://www.example.com/ld/need/14> <http://purl.org/webofneeds/model#ownerProtocolEndpoint> <http://www.example.com/won/protocol/owner> .\n" +
            "\n";
    //another document with ntriples content containing SPIN restrictions
    SolrInputDocument doc14 = new SolrInputDocument();
    doc14.addField(SolrFields.URL, "http://www.example.com/ld/need/14");
    doc14.addField(SolrFields.TITLE, "Cupboard");
    doc14.addField(SolrFields.DESCRIPTION, "");
    doc14.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_SUPPLY.getURI());
    doc14.addField(SolrFields.TAG, "cupboard");
    doc14.addField(SolrFields.NTRIPLE, ntriples);
    docs.add(doc14);

    //like 12 but with the values restricted in 11 completely off-limits
    ntriples =
        "<http://www.example.com/ld/need/15/connections/> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/ns/ldp#Container> .\n" +
            "<http://www.example.com/ld/need/15> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/webofneeds/model#Need> .\n" +
            "<http://www.example.com/ld/need/15> <http://purl.org/webofneeds/model#hasBasicNeedType> <http://purl.org/webofneeds/model#Supply> .\n" +
            "<http://www.example.com/ld/need/15> <http://purl.org/webofneeds/model#hasConnections> <http://www.example.com/ld/need/15/connections/> .\n" +
            "_:node1835a1pjfx1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/webofneeds/model#NeedContent> .\n" +
            "_:node1835a1pjfx1 <http://purl.org/dc/elements/1.1/title> \"cupboard\"^^<http://www.w3.org/2001/XMLSchema#string> .\n" +
            "_:node1835a1pjfx2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/resource/Cupboard> .\n" +
            "_:node1835a1pjfx2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/goodrelations/v1#Individual> .\n" +
            "_:node1835a1pjfx2 <http://purl.org/goodrelations/v1#color> \"natural birch\" .\n" +
            "_:node1835a1pjfx3 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/goodrelations/v1#QuantitativeValueFloat> .\n" +
            "_:node1835a1pjfx3 <http://purl.org/goodrelations/v1#hasUnitOfMeasurement> \"CMT\"^^<http://www.w3.org/2001/XMLSchema#string> .\n" +
            "_:node1835a1pjfx3 <http://purl.org/goodrelations/v1#hasValue> \"10000\"^^<http://www.w3.org/2001/XMLSchema#float> .\n" +
            "_:node1835a1pjfx2 <http://purl.org/goodrelations/v1#depth> _:node1835a1pjfx3 .\n" +
            "_:node1835a1pjfx4 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/goodrelations/v1#QuantitativeValueFloat> .\n" +
            "_:node1835a1pjfx4 <http://purl.org/goodrelations/v1#hasUnitOfMeasurement> \"CMT\"^^<http://www.w3.org/2001/XMLSchema#string> .\n" +
            "_:node1835a1pjfx4 <http://purl.org/goodrelations/v1#hasValue> \"10000\"^^<http://www.w3.org/2001/XMLSchema#float> .\n" +
            "_:node1835a1pjfx2 <http://purl.org/goodrelations/v1#height> _:node1835a1pjfx4 .\n" +
            "_:node1835a1pjfx5 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/goodrelations/v1#QuantitativeValueFloat> .\n" +
            "_:node1835a1pjfx5 <http://purl.org/goodrelations/v1#hasUnitOfMeasurement> \"CMT\"^^<http://www.w3.org/2001/XMLSchema#string> .\n" +
            "_:node1835a1pjfx5 <http://purl.org/goodrelations/v1#hasValue> \"10000\"^^<http://www.w3.org/2001/XMLSchema#float> .\n" +
            "_:node1835a1pjfx2 <http://purl.org/goodrelations/v1#width> _:node1835a1pjfx5 .\n" +
            "_:node1835a1pjfx1 <http://purl.org/webofneeds/model#hasContentDescription> _:node1835a1pjfx2 .\n" +
            "<http://www.example.com/ld/need/15> <http://purl.org/webofneeds/model#hasContent> _:node1835a1pjfx1 .\n" +
            "_:node1835a1pjfx6 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/webofneeds/model#NeedModality> .\n" +
            "<http://www.example.com/ld/need/15> <http://purl.org/webofneeds/model#hasNeedModality> _:node1835a1pjfx6 .\n" +
            "<http://www.example.com/ld/need/15> <http://purl.org/webofneeds/model#isInState> <http://purl.org/webofneeds/model#Active> .\n" +
            "<http://www.example.com/ld/need/15> <http://purl.org/webofneeds/model#matcherProtocolEndpoint> <http://www.example.com/won/protocol/matcher> .\n" +
            "<http://www.example.com/ld/need/15> <http://purl.org/webofneeds/model#needCreationDate> \"2013-08-231T10:42:17.017+0000\" .\n" +
            "<http://www.example.com/ld/need/15> <http://purl.org/webofneeds/model#needProtocolEndpoint> <http://www.example.com/won/protocol/need> .\n" +
            "<http://www.example.com/ld/need/15> <http://purl.org/webofneeds/model#ownerProtocolEndpoint> <http://www.example.com/won/protocol/owner> .\n" +
            "\n";
    //another document with ntriples content containing SPIN restrictions
    SolrInputDocument doc15 = new SolrInputDocument();
    doc15.addField(SolrFields.URL, "http://www.example.com/ld/need/15");
    doc15.addField(SolrFields.TITLE, "Cupboard");
    doc15.addField(SolrFields.DESCRIPTION, "");
    doc15.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_SUPPLY.getURI());
    doc15.addField(SolrFields.TAG, "cupboard");
    doc15.addField(SolrFields.NTRIPLE, ntriples);
    docs.add(doc15);


    //like 12 but has different values in literals
    ntriples =
        "<http://www.example.com/ld/need/16/connections/> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/ns/ldp#Container> .\n" +
            "<http://www.example.com/ld/need/16> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/webofneeds/model#Need> .\n" +
            "<http://www.example.com/ld/need/16> <http://purl.org/webofneeds/model#hasBasicNeedType> <http://purl.org/webofneeds/model#Supply> .\n" +
            "<http://www.example.com/ld/need/16> <http://purl.org/webofneeds/model#hasConnections> <http://www.example.com/ld/need/16/connections/> .\n" +
            "_:node1835a1pjfx1 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/webofneeds/model#NeedContent> .\n" +
            "_:node1835a1pjfx1 <http://purl.org/dc/elements/1.1/title> \"kästchen\"^^<http://www.w3.org/2001/XMLSchema#string> .\n" +
            "_:node1835a1pjfx2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://dbpedia.org/resource/Kaestchen> .\n" +
            "_:node1835a1pjfx2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/goodrelations/v1#Individual> .\n" +
            "_:node1835a1pjfx2 <http://purl.org/goodrelations/v1#color> \"birke\" .\n" +
            "_:node1835a1pjfx3 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/goodrelations/v1#QuantitativeValueFloat> .\n" +
            "_:node1835a1pjfx3 <http://purl.org/goodrelations/v1#hasUnitOfMeasurement> \"CMT\"^^<http://www.w3.org/2001/XMLSchema#string> .\n" +
            "_:node1835a1pjfx3 <http://purl.org/goodrelations/v1#hasValue> \"40\"^^<http://www.w3.org/2001/XMLSchema#float> .\n" +
            "_:node1835a1pjfx2 <http://purl.org/goodrelations/v1#depth> _:node1835a1pjfx3 .\n" +
            "_:node1835a1pjfx4 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/goodrelations/v1#QuantitativeValueFloat> .\n" +
            "_:node1835a1pjfx4 <http://purl.org/goodrelations/v1#hasUnitOfMeasurement> \"CMT\"^^<http://www.w3.org/2001/XMLSchema#string> .\n" +
            "_:node1835a1pjfx4 <http://purl.org/goodrelations/v1#hasValue> \"40\"^^<http://www.w3.org/2001/XMLSchema#float> .\n" +
            "_:node1835a1pjfx2 <http://purl.org/goodrelations/v1#height> _:node1835a1pjfx4 .\n" +
            "_:node1835a1pjfx5 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/goodrelations/v1#QuantitativeValueFloat> .\n" +
            "_:node1835a1pjfx5 <http://purl.org/goodrelations/v1#hasUnitOfMeasurement> \"CMT\"^^<http://www.w3.org/2001/XMLSchema#string> .\n" +
            "_:node1835a1pjfx5 <http://purl.org/goodrelations/v1#hasValue> \"40\"^^<http://www.w3.org/2001/XMLSchema#float> .\n" +
            "_:node1835a1pjfx2 <http://purl.org/goodrelations/v1#width> _:node1835a1pjfx5 .\n" +
            "_:node1835a1pjfx1 <http://purl.org/webofneeds/model#hasContentDescription> _:node1835a1pjfx2 .\n" +
            "<http://www.example.com/ld/need/16> <http://purl.org/webofneeds/model#hasContent> _:node1835a1pjfx1 .\n" +
            "_:node1835a1pjfx6 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://purl.org/webofneeds/model#NeedModality> .\n" +
            "<http://www.example.com/ld/need/16> <http://purl.org/webofneeds/model#hasNeedModality> _:node1835a1pjfx6 .\n" +
            "<http://www.example.com/ld/need/16> <http://purl.org/webofneeds/model#isInState> <http://purl.org/webofneeds/model#Active> .\n" +
            "<http://www.example.com/ld/need/16> <http://purl.org/webofneeds/model#matcherProtocolEndpoint> <http://www.example.com/won/protocol/matcher> .\n" +
            "<http://www.example.com/ld/need/16> <http://purl.org/webofneeds/model#needCreationDate> \"2013-08-231T10:42:17.017+0000\" .\n" +
            "<http://www.example.com/ld/need/16> <http://purl.org/webofneeds/model#needProtocolEndpoint> <http://www.example.com/won/protocol/need> .\n" +
            "<http://www.example.com/ld/need/16> <http://purl.org/webofneeds/model#ownerProtocolEndpoint> <http://www.example.com/won/protocol/owner> .\n" +
            "\n";
    //another document with ntriples content containing SPIN restrictions
    SolrInputDocument doc16 = new SolrInputDocument();
    doc16.addField(SolrFields.URL, "http://www.example.com/ld/need/16");
    doc16.addField(SolrFields.TITLE, "Cupboard");
    doc16.addField(SolrFields.DESCRIPTION, "");
    doc16.addField(SolrFields.BASIC_NEED_TYPE, WON.BASIC_NEED_TYPE_SUPPLY.getURI());
    doc16.addField(SolrFields.TAG, "cupboard");
    doc16.addField(SolrFields.NTRIPLE, ntriples);
    docs.add(doc16);

    return docs;
  }

  private class HintCountingMatchProcessor implements MatchProcessor{
    private Map<URI,Integer> hintCounts = new HashMap<URI, Integer>();
    private Map<URI,List<MatchScore>> matchScores = new HashMap<URI, List<MatchScore>>();

    private HintCountingMatchProcessor()
    {}

    public int getHintCount(URI uri){
      Integer count = hintCounts.get(uri);
      if (count == null) return 0;
      return count.intValue();
    }

    public List<MatchScore> getMatchScores(URI uri){
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
      hintCounts.put(from,count+1);
      List<MatchScore> scores = matchScores.get(from);
      if (scores == null) scores = new ArrayList<MatchScore>();
      scores.add(new MatchScore(to,score));
      matchScores.put(from,scores);
    }
  }

  private class MatchScore {
    private URI uri;
    private double score;

    public MatchScore(final URI uri, final double score)
    {
      this.uri = uri;
      this.score = score;
    }
  }

}
