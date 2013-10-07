/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.matcher.query;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similar.MoreLikeThis;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.solr.SolrFields;

import java.io.IOException;

/**
 * User: gabriel
 * Date: 02.07.13
 * Time: 16:13
 */
public class TextMatcherQueryFactory extends AbstractQueryFactory
{
  private Logger logger = LoggerFactory.getLogger(getClass());

  private String[] fields;

  public TextMatcherQueryFactory(BooleanClause.Occur occur, float boost, String[] fields)
  {
    super(occur, boost);
    this.fields = fields;
  }

  public TextMatcherQueryFactory(final BooleanClause.Occur occur, final String[] fields)
  {
    super(occur);
    this.fields = fields;
  }

  public Query createQuery(SolrIndexSearcher indexSearcher, SolrInputDocument inputDocument) throws IOException
  {
    int documentId = getDocumentId(indexSearcher, inputDocument);
    if (documentId == -1)
      return null;


    MoreLikeThis mlt = new MoreLikeThis(indexSearcher.getReader());

    mlt.setMinDocFreq(1);
    mlt.setMinTermFreq(1);
    mlt.setMaxDocFreqPct(100);
    mlt.setFieldNames(fields);

    BooleanQuery booleanQuery = new BooleanQuery();
    Query query = mlt.like(documentId);
    BooleanClause clause = new BooleanClause(query, BooleanClause.Occur.SHOULD);
    booleanQuery.add(clause);

    if (booleanQuery.clauses().size() == 0)
      return null;

    return booleanQuery;
  }

  //TODO: this is a workaround since morelikethis requires a documentID
  private int getDocumentId(final SolrIndexSearcher indexSearcher, final SolrInputDocument inputDocument) throws IOException
  {
    Query q = new TermQuery(new Term(SolrFields.URL, inputDocument.getFieldValue(SolrFields.URL).toString()));
    TopDocs docs = indexSearcher.search(q, 1);

    if (docs.scoreDocs.length == 0)
      return -1;

    return docs.scoreDocs[0].doc;
  }
}
