package won.matcher;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.*;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.matcher.protocol.impl.MatcherProtocolNeedServiceClient;
import won.matcher.query.*;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.solr.SolrFields;

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

  private SolrIndexSearcher solrIndexSearcher;

  private HashMap<String, TopDocs> matches;

  private Set<AbstractQuery> queries;

  private static final int MAX_MATCHES = 5;
  private static final double MATCH_THRESHOLD = 0.4;
    private static final float MIN_SCORE = 0;
    private static final float MAX_SCORE = 100;


    public Matcher(SolrIndexSearcher solrIndexSearcher)
  {
    logger.debug("Matcher initialized");
    this.solrIndexSearcher = solrIndexSearcher;

    //setup matcher client
    client = new MatcherProtocolNeedServiceClient();
    client.initializeDefault();

    //add all queries
    queries = new HashSet<>();
    queries.add(new BasicNeedTypeQuery(BooleanClause.Occur.MUST, SolrFields.BASIC_NEED_TYPE));
    queries.add(new DoubleRangeQuery(BooleanClause.Occur.SHOULD, SolrFields.LOWER_PRICE_LIMIT, SolrFields.UPPER_PRICE_LIMIT));
    queries.add(new TimeRangeQuery(BooleanClause.Occur.SHOULD, SolrFields.TIME_START, SolrFields.TIME_END));
    queries.add(new TextMatcherQuery(BooleanClause.Occur.SHOULD, new String[]{SolrFields.TITLE, SolrFields.DESCRIPTION}));
    queries.add(new MultipleValueFieldQuery(BooleanClause.Occur.SHOULD, SolrFields.TAG));
    queries.add(new SpatialQuery(BooleanClause.Occur.MUST, SolrFields.LOCATION));
    queries.add(new SelfFilterQuery(SolrFields.URL));
    queries.add(new TriplesQuery(BooleanClause.Occur.SHOULD, SolrFields.NTRIPLE));

    matches = new HashMap<>();
  }

  public void processDocument(SolrInputDocument inputDocument) throws IOException
  {
    //combine all the queries
    BooleanQuery booleanQuery = new BooleanQuery();
    for (AbstractQuery query : queries) {
      Query q = query.getQuery(solrIndexSearcher, inputDocument);
      if (q != null) {
        logger.debug("Simple query: {}", q.toString());
        booleanQuery.add(q, query.getOccur());
      }
    }
    logger.debug("Final solr query: {}", booleanQuery.toString());

    //get top MAX_MATCHES
    TopDocs topDocs = solrIndexSearcher.search(booleanQuery, MAX_MATCHES);

    logger.debug("found {} matches", topDocs.totalHits);

    //if no matches or not good enough skip them
    if (topDocs.scoreDocs.length != 0 && topDocs.getMaxScore() >= MATCH_THRESHOLD)
      matches.put(inputDocument.getFieldValue(SolrFields.URL).toString(), topDocs);
  }

  //send matches and stuff
  public void finish()
  {
    try {
      URI originator = URI.create("http://LDSpiderMatcher.webofneeds");
      IndexReader indexReader = solrIndexSearcher.getIndexReader();

      for (String match : matches.keySet()) {
        URI fromDoc = URI.create(match);
        for (ScoreDoc scoreDoc : matches.get(match).scoreDocs) {
          Document document = indexReader.document(scoreDoc.doc);
          URI toDoc = URI.create(document.get(SolrFields.URL));
          client.hint(fromDoc, toDoc, normalizeScore(scoreDoc.score), originator, null);
          logger.debug("Sending hint {} -> {} :: {}", new Object[] {fromDoc, toDoc, scoreDoc.score});
        }
      }
    } catch (NoSuchNeedException | IllegalMessageForNeedStateException | IOException e) {
      logger.error(e.getMessage(), e);
    }
  }

    /**
     * Maps the input value between MIN_SCORE and MAX_SCORE linearly to [0,1]
     * @param score
     * @return
     */
    private double normalizeScore(float score) {
        return Math.min(1,Math.max(0,(score - MIN_SCORE)/(MAX_SCORE-MIN_SCORE)));
    }
}
