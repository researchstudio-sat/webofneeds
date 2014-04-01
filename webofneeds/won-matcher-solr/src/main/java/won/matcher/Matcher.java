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

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
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
import won.matcher.service.ScoreTransformer;
import won.protocol.solr.SolrFields;
import won.protocol.util.WonRdfUtils;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 */
public class Matcher
{
  private Logger logger = LoggerFactory.getLogger(getClass());

  private MatcherProtocolNeedServiceClient client;

  private SolrIndexSearcher solrIndexSearcher;

  private ScoreTransformer scoreTransformer;

  private URI originatorURI;

  private Set<String> hintMemory = new HashSet<String>();

  private ConcurrentLinkedQueue<MatchResult> matches;

  private Set<QueryFactory> queryFactories;

  private List<MatchProcessor> matchProcessors = new ArrayList<MatchProcessor>();

  private static final boolean SUPPRESS_HINTS = true;

  private static final int MAX_MATCHES = 5;

  /**
   * Creates Solr/SIREn queries for <code>SolrIndexDocument</code>s and delegates actions for identified matches to
   * <code>MatchProcessor</code>s. Keeps an internal memory of matches that have already been processed to avoid
   * re-processing them.
   *
   * @param solrIndexSearcher
   */
  public Matcher(SolrIndexSearcher solrIndexSearcher, ScoreTransformer scoreTransformer, URI originatorURI)
  {
    logger.debug("Matcher initialized");
    this.solrIndexSearcher = solrIndexSearcher;
    this.scoreTransformer = scoreTransformer;
    this.originatorURI = originatorURI;

    //add all queries
    queryFactories = new HashSet<>();
    queryFactories.add(new BasicNeedTypeQueryFactory(BooleanClause.Occur.MUST, SolrFields.BASIC_NEED_TYPE));
    queryFactories.add(new RangeQueryFactory(BooleanClause.Occur.SHOULD, SolrFields.PRICE, "-"));
    queryFactories.add(new RangeQueryFactory(BooleanClause.Occur.SHOULD, SolrFields.DURATION, "/"));
    queryFactories.add(new TextMatcherQueryFactory(BooleanClause.Occur.SHOULD, new String[]{SolrFields.TITLE, SolrFields.DESCRIPTION}));
    queryFactories.add(new MultipleValueFieldQueryFactory(BooleanClause.Occur.SHOULD, SolrFields.TAG));
    queryFactories.add(new SpatialQueryFactory(BooleanClause.Occur.MUST, SolrFields.LOCATION));
    queryFactories.add(new SelfFilterQueryFactory(SolrFields.URL));
    queryFactories.add(new TriplesQueryFactory(BooleanClause.Occur.SHOULD, SolrFields.NTRIPLE, this.solrIndexSearcher.getSchema()));

    matches = new ConcurrentLinkedQueue<MatchResult>();
  }

  /**
   * Adds a <code>MatchProcessor</code> instance to this Matcher.
   * @param processor
   */
  public void addMatchProcessor(MatchProcessor processor) {
    logger.info("adding matchProcessor of type {}", processor.getClass().getName());
    this.matchProcessors.add(processor);
  }

  /**
   * Creates and executes queries for the specified document. Matches are stored in an internal
   * queue and are processed in <code>processMatches()</code>.
   * @param inputDocument
   * @throws IOException
   */
  public void processDocument(SolrInputDocument inputDocument) throws IOException
  {
    String url = inputDocument.getFieldValue(SolrFields.URL).toString();
    try {
      BooleanQuery booleanQuery = createQueryForDocument(inputDocument);
      processQuery(url, booleanQuery, inputDocument);
    } catch (Throwable t) {
      logger.info("caught throwable while processing doc with url {}", url, t);
    }
  }




  /**
   * Executes queries for the specified document. Matches are stored in an internal
   * queue and are processed in <code>processMatches()</code>.
   *
   * @param url
   * @param booleanQuery
   * @param inputDocument - used for passing the rdf triples to the match processing logic
   * @throws IOException
   */
  private void processQuery(final String url, final BooleanQuery booleanQuery, SolrInputDocument inputDocument) throws IOException
  {
    logger.debug("Final solr query: {}", QueryParsing.toString(booleanQuery, solrIndexSearcher.getSchema()));
    //get top MAX_MATCHES
    TopDocs topDocs = solrIndexSearcher.search(booleanQuery, MAX_MATCHES);
    //if no matches or not good enough skip them

    if (topDocs.scoreDocs.length != 0 && scoreTransformer.isAboveInputThreshold(topDocs.getMaxScore())) {
      matches.add(new MatchResult(url, topDocs, (String) inputDocument.getFieldValue(SolrFields.NTRIPLE)));
      logger.debug("found {} matches for {}", topDocs.totalHits, url);
    } else {
      logger.debug("Found only {} matches with highest score {} for {}. Not sending any hints", new Object[]{topDocs.scoreDocs.length, topDocs.getMaxScore(), url});
    }
    if (logger.isDebugEnabled()){
      logger.debug("matches for {}: ", url);
      for (int i = 0; i < topDocs.scoreDocs.length; i++) {
        logger.debug("score {}: {}", topDocs.scoreDocs[i].score, this.solrIndexSearcher.getReader().document(topDocs.scoreDocs[i].doc).get(SolrFields.URL));
      }
    }
  }

  /**
   * Creates and returns a query that is used to identify the most similar documents for the specified document.
   * Used internally from processDocument(), but can safely be used externally.
   * @param inputDocument
   * @return
   * @throws IOException
   */
  public BooleanQuery createQueryForDocument(final SolrInputDocument inputDocument) throws IOException
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
    return booleanQuery;
  }


  /**
   * Processes all matches found by preceding calls to <code>processDocument()</code>. For each match,
   * all <code>MatchProcessor</code>s are called.
   */
  public void processMatches()
  {

    IndexReader indexReader = solrIndexSearcher.getIndexReader();

    while (!matches.isEmpty()) {
      MatchResult match = matches.poll();
      URI fromDocUri = URI.create(match.getUrl());
      Model fromDocModel = null; //lazily initialize this model if a match is found
      for (ScoreDoc scoreDoc : match.getTopDocs().scoreDocs) {
        try {
          Document document = indexReader.document(scoreDoc.doc);
          URI toDocUri = URI.create(document.get(SolrFields.URL));
          logger.debug("preparing to send match between {} and {} with score {}", new Object[]{fromDocUri, toDocUri,scoreDoc.score});
          if (toDocUri.equals(fromDocUri)) continue;
          if (!scoreTransformer.isAboveInputThreshold(scoreDoc.score)) {
            logger.debug("score {} lower than threshold {}, suppressed match between {} and {}", new Object[]{scoreDoc.score, scoreTransformer.getInputThreshold(), fromDocUri, toDocUri});
            continue;
          }
          double normalizedScore = scoreTransformer.transform(scoreDoc.score);
          logger.debug("score of {} was normalized to {}", scoreDoc.score, normalizedScore);
          if (isNewHint(fromDocUri, toDocUri, normalizedScore)) {
            logger.debug("determining which facets to use in hint");
            if (fromDocModel == null) {
              fromDocModel = convertRdfStringToModel(fromDocUri.toString(), match.getRdfContent());
            }
            Model facetModel = determineFacetsForHint(fromDocUri, toDocUri, fromDocModel, readModelFromSolrIndex(toDocUri));
            logger.debug("calling MatchProcessors for match {} -> {} :: {}", new Object[]{fromDocUri, toDocUri, normalizedScore});
            for (MatchProcessor proc: matchProcessors){
              proc.process(fromDocUri,toDocUri,normalizedScore,originatorURI,facetModel);
            }
          } else {
            logger.debug("Suppressed duplicate hint {} -> {} :: {}", new Object[]{fromDocUri, toDocUri, normalizedScore});
          }
        } catch (Throwable t) {
          logger.error("error while processing match result {}",match, t);
        }
      }
    }

  }

  private Model convertRdfStringToModel(String uri, String rdf){
    logger.debug("converting rdf string for need {} to rdf model. Rdf string starts with {}", uri, StringUtils.abbreviate(rdf,20));
    Model model = ModelFactory.createDefaultModel();
    model.setNsPrefix("", uri);
    RDFDataMgr.read(model, new StringReader(rdf), uri, Lang.NTRIPLES);
    return model;
  }


  /**
   * Just takes the first facet from both needs. TODO: Needs refinement once we get more clever with multiple facets.
   * @param fromDocUri
   * @param toDocUri
   * @param fromModel
   * @param toModel
   * @return
   */
  private Model determineFacetsForHint(URI fromDocUri, URI toDocUri, Model fromModel, Model toModel) {
    logger.debug("determining facets for use in hint.");
    if (logger.isDebugEnabled()){
      logger.debug("fromModel:");
      RDFDataMgr.write(System.out, fromModel, Lang.TURTLE);
      logger.debug("[end fromModel]\n\n\n");
    }
    URI fromFacetURI = WonRdfUtils.FacetUtils.getFacet(fromModel);
    logger.debug("'from' need {} has facet {}", fromDocUri, fromFacetURI);
    if (fromFacetURI == null ) return null;
    if (logger.isDebugEnabled()){
      logger.debug("toModel:");
      RDFDataMgr.write(System.out, toModel, Lang.TURTLE);
      logger.debug("[end toModel]\n\n\n");
    }
    URI toFacetURI = WonRdfUtils.FacetUtils.getFacet(toModel);
    logger.debug("'to' need {} has facet {}", toDocUri, toFacetURI);
    if (toFacetURI == null ) return null;
    //for now, just use the first facets for matching. TODO: implement a clever strategy here
    Model facetModel = WonRdfUtils.FacetUtils.createFacetModelForHintOrConnect(fromFacetURI, toFacetURI);
    return facetModel;
  }


  /**
   * Fetches the ntriples content from the solr index and builds a jena Model from it.
   * @param docUri
   * @return
   * @throws IOException
   */
  private Model readModelFromSolrIndex(final URI docUri) throws IOException
  {
    logger.debug("Reading need model from solr index for uri {}", docUri);
    TopDocs searchResult = this.solrIndexSearcher.search(new TermQuery(new Term(SolrFields.URL, docUri.toString())), 1);
    if (searchResult == null || searchResult.totalHits == 0) {
      logger.debug("Found no document in index with URI {}", docUri);
      return null;
    }
    Document doc = this.solrIndexSearcher.getIndexReader().document(searchResult.scoreDocs[0].doc);
    if (doc == null) {
      logger.debug("Could not read document from index for URI {}", docUri);
      return null;
    }
    String rdfAsString = doc.get(SolrFields.NTRIPLE);
    if (rdfAsString == null) {
      logger.debug("Could not read RDF content from index document for URI {}", docUri);
      return null;
    }
    logger.debug("Reading rdf model from string. String starts with {}",StringUtils.abbreviate(rdfAsString,20));
    Model fromModel = ModelFactory.createDefaultModel();
    fromModel.setNsPrefix("", docUri.toString());
    RDFDataMgr.read(fromModel, new StringReader(rdfAsString), docUri.toString(), Lang.NTRIPLES);
    return fromModel;
  }


  private boolean isNewHint(URI fromURI, URI toURI, double score){
      return this.hintMemory.add("" + fromURI + toURI + Double.toString(score));
    }

    private class MatchResult {
      private String url;
      private TopDocs topDocs;


      private String rdfContent; //we use string content, we lazily convert it to RDF when needed

      private MatchResult(String url, TopDocs topDocs, String rdfContent) {
        this.rdfContent = rdfContent;
        this.topDocs = topDocs;
        this.url = url;
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

      public String getRdfContent() {
        return rdfContent;
      }

      public void setRdfContent(String rdfContent) {
        this.rdfContent = rdfContent;
      }

      @Override
      public String toString()
      {
        return "MatchResult{" +
            "url='" + url + '\'' +
            '}';
      }

      @Override
      public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MatchResult that = (MatchResult) o;

        if (rdfContent != null ? !rdfContent.equals(that.rdfContent) : that.rdfContent != null) return false;
        if (topDocs != null ? !topDocs.equals(that.topDocs) : that.topDocs != null) return false;
        if (url != null ? !url.equals(that.url) : that.url != null) return false;

        return true;
      }

      @Override
      public int hashCode() {
        int result = url != null ? url.hashCode() : 0;
        result = 31 * result + (topDocs != null ? topDocs.hashCode() : 0);
        result = 31 * result + (rdfContent != null ? rdfContent.hashCode() : 0);
        return result;
      }
    }
}
