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

package won.matcher.query;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.search.SolrIndexSearcher;

/**
 * User: gabriel
 * Date: 03.07.13
 * Time: 12:56
 */
public class DoubleRangeQueryFactory extends AbstractQueryFactory
{
  private String lowerBoundField;
  private String upperBoundField;

  public DoubleRangeQueryFactory(BooleanClause.Occur occur, float boost, String lowerBoundField, String upperBoundField)
  {
    super(occur, boost);
    this.lowerBoundField = lowerBoundField;
    this.upperBoundField = upperBoundField;
  }

  public DoubleRangeQueryFactory(final BooleanClause.Occur occur, final String lowerBoundField, final String upperBoundField)
  {
    super(occur);
    this.lowerBoundField = lowerBoundField;
    this.upperBoundField = upperBoundField;
  }

  public Query createQuery(SolrIndexSearcher indexSearcher, SolrInputDocument inputDocument)
  {
    if (!inputDocument.containsKey(lowerBoundField) && !inputDocument.containsKey(upperBoundField))
      return null;

    Double lower = getField(inputDocument, lowerBoundField);
    Double upper = getField(inputDocument, upperBoundField);

    Query nq1 = NumericRangeQuery.newDoubleRange(lowerBoundField, lower, upper, true, true);
    Query nq2 = NumericRangeQuery.newDoubleRange(upperBoundField, lower, upper, true, true);

    BooleanQuery query = new BooleanQuery();

    //one of the two query must match at least one document
    query.add(nq1, BooleanClause.Occur.SHOULD);
    query.add(nq2, BooleanClause.Occur.SHOULD);

    return query;
  }

  private Double getField(SolrInputDocument inputDocument, String fieldName)
  {
    if (inputDocument.containsKey(fieldName))
      return Double.parseDouble(inputDocument.getFieldValue(fieldName).toString());
    else return null;
  }
}
