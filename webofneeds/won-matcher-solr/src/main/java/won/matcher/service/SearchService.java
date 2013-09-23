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

package won.matcher.service;

import com.hp.hpl.jena.rdf.model.Model;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.util.Version;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.matcher.Matcher;
import won.matcher.solr.NeedSolrInputDocumentBuilder;
import won.protocol.solr.SolrFields;
import won.protocol.util.NeedModelBuilder;

import java.net.URI;
import java.util.ArrayList;

/**
 * User: fkleedorfer
 * Date: 12.09.13
 */
public class SearchService
{
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private SolrIndexSearcher solrIndexSearcher;
  private Matcher matcher;
  private ScoreTransformer scoreTransformer;



  /**
   *URI identifying the service publicly
   */
  private URI originatorURI;

  public SearchService(final SolrIndexSearcher solrIndexSearcher, final Matcher matcher, final ScoreTransformer scoreTransformer, final URI originatorURI)
  {
    this.solrIndexSearcher = solrIndexSearcher;
    this.matcher = matcher;
    this.originatorURI = originatorURI;
    this.scoreTransformer = scoreTransformer;
  }

  public SearchResult search(String keywords, int numResults){
    try {
      Query query = createKeywordQuery(keywords);
      return createSearchResult(this.solrIndexSearcher.search(query, numResults), this.originatorURI);
    } catch (Throwable t) {
      logger.info("could not perform keyword search", t);
    }
    return null;
  }



  public SearchResult search(String keywords, Model needModel, int numResults){
    try {
      Query query = createKeywordQuery(keywords);
      NeedSolrInputDocumentBuilder builder = new NeedSolrInputDocumentBuilder();
      NeedModelBuilder needModelBuilder = new NeedModelBuilder();
      needModelBuilder.copyValuesFromProduct(needModel);
      needModelBuilder.copyValuesToBuilder(builder);
      BooleanQuery combinedQuery = matcher.createQueryForDocument(builder.build());
      combinedQuery.add(query, BooleanClause.Occur.MUST);
      return createSearchResult(this.solrIndexSearcher.search(combinedQuery, numResults), this.originatorURI);
    } catch (Throwable t) {
      logger.info("could not perform keyword search", t);
    }
    return null;
  }

  public SearchResult search(Model needModel, int numResults){
    try {
      NeedSolrInputDocumentBuilder builder = new NeedSolrInputDocumentBuilder();
      NeedModelBuilder needModelBuilder = new NeedModelBuilder();
      needModelBuilder.copyValuesFromProduct(needModel);
      needModelBuilder.copyValuesToBuilder(builder);
      BooleanQuery combinedQuery = matcher.createQueryForDocument(builder.build());
      return createSearchResult(this.solrIndexSearcher.search(combinedQuery, numResults), this.originatorURI);
    } catch (Throwable t) {
      logger.info("could not perform need search", t);
    }
    return null;
  }


  private Query createKeywordQuery(final String keywords) throws ParseException
  {
    Analyzer analyzer = this.solrIndexSearcher.getSchema().getField(SolrFields.KEYWORD_SEARCH).getType().getQueryAnalyzer();
    QueryParser parser = new QueryParser(Version.LUCENE_35, SolrFields.KEYWORD_SEARCH, analyzer);
    return parser.parse(keywords);
  }

  private SearchResult createSearchResult(TopDocs topDocs, URI originatorURI){
    try {
      IndexReader indexReader = this.solrIndexSearcher.getIndexReader();
      if (topDocs.totalHits == 0) return new SearchResult(new ArrayList());
      for (int i = 0; i < topDocs.totalHits; i++){
        ScoreDoc scoreDoc = topDocs.scoreDocs[i];
        Document doc = indexReader.document(scoreDoc.doc);
        Model resultContent = createSearchResultModel(doc);
        SearchResultItem item = new SearchResultItem(scoreTransformer.transform(scoreDoc.score),resultContent,URI.create(doc.get(SolrFields.URL)),  null);
      }
    } catch (Throwable t) {
      logger.info("could not create search result", t);
    }
    return new SearchResult(new ArrayList());
  }

  private Model createSearchResultModel(final Document document)
  {
    String locationString = document.get(SolrFields.LOCATION);
    String latitude = null;
    String longitude = null;
    if (locationString != null) {
      String[] latLong = locationString.split(",");
      if (latLong.length == 2){
        latitude = latLong[0];
        longitude = latLong[1];
      }
    }
    return new NeedModelBuilder()
        .setUri(document.get(SolrFields.URL))
        .setBasicNeedType(document.get(SolrFields.BASIC_NEED_TYPE))
        .setTags(document.getValues(SolrFields.TAG))
        .setAvailableAtLocation(latitude, longitude)
        .setTitle(document.get(SolrFields.TITLE))
        .setDescription(document.get(SolrFields.DESCRIPTION))
        .setLowerPriceLimit(document.get(SolrFields.LOWER_PRICE_LIMIT))
        .setUpperPriceLimit(document.get(SolrFields.UPPER_PRICE_LIMIT))
        //.addInterval(document.get(SolrFields.TIME_START), document.get(SolrFields.TIME_END)) TODO: we don't have time intervals in the solr doc yet.
        .build();
  }
}
