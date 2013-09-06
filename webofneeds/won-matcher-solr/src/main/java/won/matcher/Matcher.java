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

package won.matcher;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.*;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.search.QueryParsing;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.matcher.processor.MatchProcessor;
import won.matcher.protocol.impl.MatcherProtocolNeedServiceClient;
import won.matcher.query.*;
import won.matcher.query.rdf.TriplesQueryFactory;
import won.protocol.solr.SolrFields;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * User: atus
 * Date: 03.07.13
 */
public class Matcher
{
  private Logger logger = LoggerFactory.getLogger(getClass());

  private MatcherProtocolNeedServiceClient client;

  private SolrIndexSearcher solrIndexSearcher;

  private Set<String> hintMemory = new HashSet<String>();

  private ConcurrentLinkedQueue<MatchResult> matches;

  private Set<QueryFactory> queryFactories;

  private List<MatchProcessor> matchProcessors = new ArrayList<MatchProcessor>();

  private static final boolean SUPPRESS_HINTS = true;

  private static final int MAX_MATCHES = 5;
  private static final double MATCH_THRESHOLD = 0.2;
    private static final float MIN_SCORE = 0;
    private static final float MAX_SCORE = 10;


  public Matcher(SolrIndexSearcher solrIndexSearcher)
  {
    logger.debug("Matcher initialized");
    this.solrIndexSearcher = solrIndexSearcher;



    //add all queries
    queryFactories = new HashSet<>();
    queryFactories.add(new BasicNeedTypeQueryFactory(BooleanClause.Occur.MUST, SolrFields.BASIC_NEED_TYPE));
    queryFactories.add(new DoubleRangeQueryFactory(BooleanClause.Occur.SHOULD, SolrFields.LOWER_PRICE_LIMIT, SolrFields.UPPER_PRICE_LIMIT));
    queryFactories.add(new TimeRangeQueryFactory(BooleanClause.Occur.SHOULD, SolrFields.TIME_START, SolrFields.TIME_END));
    queryFactories.add(new TextMatcherQueryFactory(BooleanClause.Occur.SHOULD, new String[]{SolrFields.TITLE, SolrFields.DESCRIPTION}));
    queryFactories.add(new MultipleValueFieldQueryFactory(BooleanClause.Occur.SHOULD, SolrFields.TAG));
    queryFactories.add(new SpatialQueryFactory(BooleanClause.Occur.MUST, SolrFields.LOCATION));
    queryFactories.add(new SelfFilterQueryFactory(SolrFields.URL));
    queryFactories.add(new TriplesQueryFactory(BooleanClause.Occur.SHOULD, SolrFields.NTRIPLE));

    matches = new ConcurrentLinkedQueue<MatchResult>();
  }

  public void addMatchProcessor(MatchProcessor processor) {
    logger.info("adding matchProcessor of type {}", processor.getClass().getName());
    this.matchProcessors.add(processor);
  }

  public void processDocument(SolrInputDocument inputDocument) throws IOException
  {
    //combine all the queries
    BooleanQuery booleanQuery = new BooleanQuery();
    for (QueryFactory queryFactory : queryFactories) {
      Query q = queryFactory.createQuery(solrIndexSearcher, inputDocument);
      if (q != null) {
        q.setBoost(queryFactory.getBoost());
        logger.debug("Simple query: {}", q.toString());
        booleanQuery.add(q, queryFactory.getOccur());
      }
    }

    logger.debug("Final solr query: {}", QueryParsing.toString(booleanQuery, solrIndexSearcher.getSchema()));

    //get top MAX_MATCHES
    TopDocs topDocs = solrIndexSearcher.search(booleanQuery, MAX_MATCHES);

    String url = inputDocument.getFieldValue(SolrFields.URL).toString();

    //if no matches or not good enough skip them
    if (topDocs.scoreDocs.length != 0 && topDocs.getMaxScore() >= MATCH_THRESHOLD) {
      matches.add(new MatchResult(url, topDocs));
      logger.debug("found {} matches for {}", topDocs.totalHits, url);
    } else {
      logger.debug("Found only {} matches with highest score {} for {}. Not sending any hints", new Object[]{topDocs.scoreDocs.length, topDocs.getMaxScore(), url});
    }

    /*
    if (logger.isDebugEnabled()){
      //explain all scored document scores
      for (int i = 0; i < topDocs.scoreDocs.length; i++) {
        Explanation explanation = solrIndexSearcher.explain(booleanQuery,topDocs.scoreDocs[i].doc);
        logger.debug("explanation for score doc {}: \n{}",topDocs.scoreDocs[i].doc, explanation);
      }
    }
    */
  }


  //send matches
  public void callMatchProcessors()
  {
    URI originator = URI.create("http://LDSpiderMatcher.webofneeds");
    IndexReader indexReader = solrIndexSearcher.getIndexReader();

    while (!matches.isEmpty()) {
      MatchResult match = matches.poll();
      URI fromDoc = URI.create(match.getUrl());
      for (ScoreDoc scoreDoc : match.getTopDocs().scoreDocs) {
        try {
          Document document = indexReader.document(scoreDoc.doc);
          URI toDoc = URI.create(document.get(SolrFields.URL));
          logger.debug("preparing to send match between {} and {} with score {}", new Object[]{fromDoc, toDoc,scoreDoc.score});
          if (toDoc.equals(fromDoc)) continue;
          if (scoreDoc.score < MATCH_THRESHOLD) {
            logger.debug("score {} lower than threshold {}, suppressed match between {} and {}", new Object[]{scoreDoc.score, MATCH_THRESHOLD, fromDoc, toDoc});
            continue;
          }
          double normalizedScore = normalizeScore(scoreDoc.score);
          logger.debug("score of {} was normalized to {}", scoreDoc.score, normalizedScore);
          if (isNewHint(fromDoc, toDoc, normalizedScore)) {
            logger.debug("calling MatchProcessors for match {} -> {} :: {}", new Object[]{fromDoc, toDoc, normalizedScore});
            for (MatchProcessor proc: matchProcessors){
              proc.process(fromDoc,toDoc,normalizedScore,originator,null);
            }
          } else {
            logger.debug("Suppressed duplicate hint {} -> {} :: {}", new Object[]{fromDoc, toDoc, normalizedScore});
          }
        } catch (IOException e) {
          logger.error("error reading document from solr index", e);
        }
      }
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

    private boolean isNewHint(URI fromURI, URI toURI, double score){
      return this.hintMemory.add("" + fromURI + toURI + Double.toString(score));
    }

    private class MatchResult {
      private String url;
      private TopDocs topDocs;

      public MatchResult(final String url, final TopDocs topDocs)
      {
        this.url = url;
        this.topDocs = topDocs;
      }

      public String getUrl()
      {
        return url;
      }

      public void setUrl(final String url)
      {
        this.url = url;
      }

      public TopDocs getTopDocs()
      {
        return topDocs;
      }

      public void setTopDocs(final TopDocs topDocs)
      {
        this.topDocs = topDocs;
      }

      @Override
      public boolean equals(final Object o)
      {
        if (this == o) return true;
        if (!(o instanceof MatchResult)) return false;

        final MatchResult that = (MatchResult) o;

        if (topDocs != null ? !topDocs.equals(that.topDocs) : that.topDocs != null) return false;
        if (url != null ? !url.equals(that.url) : that.url != null) return false;

        return true;
      }

      @Override
      public int hashCode()
      {
        int result = url != null ? url.hashCode() : 0;
        result = 31 * result + (topDocs != null ? topDocs.hashCode() : 0);
        return result;
      }
    }
}
