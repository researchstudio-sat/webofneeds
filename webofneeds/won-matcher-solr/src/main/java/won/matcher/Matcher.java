package won.matcher;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.*;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import won.matcher.protocol.impl.MatcherProtocolNeedServiceClient;
import won.matcher.query.*;
import won.matcher.solr.DocumentStorage;
import won.protocol.solr.SolrFields;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * User: atus
 * Date: 03.07.13
 */
public class Matcher
{
  private Logger logger = LoggerFactory.getLogger(getClass());

  private MatcherProtocolNeedServiceClient client;

  private SolrCore solrCore;

  private HashMap<String, ScoreDoc[]> knownMatches;

  private Set<AbstractQuery> queries;

  private static final int MAX_MATCHES = 3;
  private static final double MATCH_THRESHOLD = 0.4;

  public Matcher() throws IOException, SAXException, ParserConfigurationException
  {
    logger.debug("Matcher initialized");

    //get solr core
    CoreContainer.Initializer initializer = new CoreContainer.Initializer();
    CoreContainer coreContainer = initializer.initialize();
    solrCore = coreContainer.getCore("webofneeds");


    //setup matcher client
    client = new MatcherProtocolNeedServiceClient();
    client.initializeDefault();

    //add all queries
    queries = new HashSet<>();
    queries.add(new BasicNeedTypeQuery(BooleanClause.Occur.MUST, SolrFields.BASIC_NEED_TYPE));
//    queries.add(new IntegerRangeQuery(BooleanClause.Occur.SHOULD, SolrFields.LOWER_PRICE_LIMIT, SolrFields.UPPER_PRICE_LIMIT));
//    queries.add(new LongRangeQuery(BooleanClause.Occur.SHOULD, SolrFields.START_TIME, SolrFields.END_TIME));
    queries.add(new TextMatcherQuery(BooleanClause.Occur.SHOULD, new String[]{SolrFields.TITLE, SolrFields.DESCRIPTION}));
//    queries.add(new MultipleValueFieldQuery(BooleanClause.Occur.SHOULD, SolrFields.TAG));
//    queries.add(new SpatialQuery(BooleanClause.Occur.MUST, SolrFields.LOCATION, solrCore));

    knownMatches = new HashMap();
  }

  public void processDocument(SolrInputDocument inputDocument) throws IOException
  {
    logger.debug("Matcher add called");

    //init
    BooleanQuery booleanQuery = new BooleanQuery();
    TopDocs topDocs;

    //get index
    SolrIndexSearcher solrIndexSearcher = solrCore.getSearcher().get();

    //get set of documents to compare to (filter + more like this, etc)
    for (AbstractQuery query : queries) {
      Query q = query.getQuery(solrIndexSearcher, inputDocument);
      if (q != null) {
        logger.info("query: "+ q.toString());
        booleanQuery.add(q, query.getOccur());
      }
    }
    logger.info("long query: " + booleanQuery.toString());

    //compare and select best match(es) for hints
    topDocs = solrIndexSearcher.search(booleanQuery, 10);

    if (topDocs.scoreDocs.length != 0)
      knownMatches.put(inputDocument.getFieldValue(SolrFields.URL).toString(), topDocs.scoreDocs);

    for (String match : knownMatches.keySet()) {
      StringBuilder sb = new StringBuilder();
      for (ScoreDoc score : knownMatches.get(match))
        sb.append(String.format("\ndocument: %s\tscore:%2.3f", score.doc, score.score));

      logger.info("document: " + match + " " + sb.toString());
    }
  }

  public void finish() throws IOException
  {
    logger.info("Matcher finish called.");
    //cleanup or send prepared matches

//    try {
    URI originator = URI.create("http://LDSpiderMatcher.webofneeds");

    IndexReader indexReader = solrCore.getSearcher().get().getIndexReader();

    for (String match : knownMatches.keySet()) {
      URI fromDoc = URI.create(match);
      for (ScoreDoc score : knownMatches.get(match)) {
        Document document = indexReader.document(score.doc);
        URI toDoc = URI.create(document.get(SolrFields.URL));
        logger.info(String.format("Sending hint %s -> %s :: %3.2f",fromDoc,toDoc,score.score));
        //client.hint(fromDoc, toDoc, score.score, originator, null);
      }
    }
//    } catch (NoSuchNeedException e) {
//      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//    } catch (IllegalMessageForNeedStateException e) {
//      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//    }

  }
}
