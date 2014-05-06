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
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import won.matcher.Matcher;
import won.matcher.solr.NeedSolrInputDocumentBuilder;
import won.protocol.solr.SolrFields;
import won.protocol.util.NeedModelBuilder;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

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

  /**
   * Constructor that instantiates a CoreContainer with specified config file and solr directory, then creates a new
   * SolrIndexSearcher for the specified core.
   * @param solrConfigFile
   * @param solrHome
   * @param coreName
   * @param scoreTransformer
   * @param originatorURI
   * @throws IOException
   * @throws SAXException
   * @throws ParserConfigurationException
   */
  public SearchService(File solrHome, File solrConfigFile, String coreName, final ScoreTransformer scoreTransformer, final URI originatorURI) throws IOException, SAXException, ParserConfigurationException
  {
    CoreContainer coreContainer = new CoreContainer();
    coreContainer.load(solrHome.getAbsolutePath(), solrConfigFile);
    this.solrIndexSearcher = coreContainer.getCore(coreName).newSearcher(coreName);
    this.matcher = new Matcher(this.solrIndexSearcher, scoreTransformer, originatorURI);
    this.originatorURI = originatorURI;
    this.scoreTransformer = scoreTransformer;
  }

  public SearchResult search(String keywords, int numResults){
   return search(keywords, null, numResults);
  }

  public SearchResult search(String keywords, Model needModel, int numResults){
    try {
      BooleanQuery combinedQuery = new BooleanQuery();
      if (keywords != null && keywords.length() > 0) {
        combinedQuery.add(createKeywordQuery(keywords), BooleanClause.Occur.MUST);
      }
      if (needModel != null && needModel.size() > 0) {
        NeedSolrInputDocumentBuilder builder = new NeedSolrInputDocumentBuilder();
        NeedModelBuilder needModelBuilder = new NeedModelBuilder();
        needModelBuilder.copyValuesFromProduct(needModel);
        needModelBuilder.copyValuesToBuilder(builder);
        SolrInputDocument solrDoc = builder.build();

      }
      if (combinedQuery.getClauses().length > 0) {
        return createSearchResult(this.solrIndexSearcher.search(combinedQuery, numResults), this.originatorURI);
      }
    } catch (Throwable t) {
      logger.info("could not perform keyword search", t);
    }
    return null;
  }

  public SearchResult search(Model needModel, int numResults){
    return search(null, needModel, numResults);
  }



  private Query createKeywordQuery(final String keywords) throws ParseException
  {
    Analyzer analyzer = this.solrIndexSearcher.getSchema().getField(SolrFields.KEYWORD_SEARCH).getType().getQueryAnalyzer();
    QueryParser parser = new QueryParser(Version.LUCENE_35, SolrFields.KEYWORD_SEARCH, analyzer);
    return parser.parse(keywords);
  }

  private SearchResult createSearchResult(TopDocs topDocs, URI originatorURI){
    List<SearchResultItem> items = new ArrayList();
    try {
      IndexReader indexReader = this.solrIndexSearcher.getIndexReader();
      if (topDocs.totalHits == 0) return new SearchResult(this.originatorURI, items);
      logger.debug("search yields {} results", topDocs.totalHits);
      for (int i = 0; i < topDocs.totalHits; i++){
        ScoreDoc scoreDoc = topDocs.scoreDocs[i];
        logger.debug("processing result {} with score {}", i, scoreDoc.score);
        if (scoreTransformer.isAboveInputThreshold(scoreDoc.score)){
          float transformedScore = scoreTransformer.transform(scoreDoc.score);
          logger.debug("score {} transformed to {}", scoreDoc.score, transformedScore);
          Document doc = indexReader.document(scoreDoc.doc);
          Model resultContent = createSearchResultModel(doc);
          SearchResultItem item = new SearchResultItem(scoreTransformer.transform(scoreDoc.score),resultContent,URI.create(doc.get(SolrFields.URL)),  null);
          items.add(item);
        }
      }
    } catch (Throwable t) {
      logger.info("could not create search result", t);
    }
    return new SearchResult(this.originatorURI,items);
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
        .setPriceLimit(document.get(SolrFields.PRICE))
        .addInterval(document.get(SolrFields.DURATION))// TODO: we don't have time intervals in the solr doc yet. AT: now we do, hope it works
        .build();
  }


}
