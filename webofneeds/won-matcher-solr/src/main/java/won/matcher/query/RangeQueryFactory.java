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
import org.apache.lucene.search.Query;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.SolrIndexSearcher;

/**
 * User: gabriel
 * Date: 03.07.13
 * Time: 12:56
 */
public class RangeQueryFactory extends AbstractQueryFactory
{
  private String field;
  private String separator;

  public RangeQueryFactory(BooleanClause.Occur occur, float boost, String field, String separator)
  {
    super(occur, boost);
    this.field = field;
    this.separator = separator;
  }

  public RangeQueryFactory(final BooleanClause.Occur occur, final String field, String separator)
  {
    super(occur);
    this.field = field;
    this.separator = separator;
  }

  public Query createQuery(SolrIndexSearcher indexSearcher, SolrInputDocument inputDocument)
  {
    if (!inputDocument.containsKey(field))
      return null;

    String range = getField(inputDocument, field);

    SchemaField sf = indexSearcher.getSchema().getField(field);

    String[] parts = range.split(separator);
    if(parts.length != 2)
      return null;

    Query query = sf.getType().getRangeQuery(null, sf, parts[0], parts[1], true, true);

    return query;
  }

  private String getField(SolrInputDocument inputDocument, String fieldName)
  {
    if (inputDocument.containsKey(fieldName))
      return inputDocument.getFieldValue(fieldName).toString();
    else return null;
  }
}
