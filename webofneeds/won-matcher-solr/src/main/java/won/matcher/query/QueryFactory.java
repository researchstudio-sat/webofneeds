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
import org.apache.solr.search.SolrIndexSearcher;

import java.io.IOException;

/**
 * User: fkleedorfer
 * Date: 27.08.13
 */
public interface QueryFactory
{
  Query createQuery(SolrIndexSearcher indexSearcher, SolrInputDocument inputDocument) throws IOException;

  BooleanClause.Occur getOccur();

  float getBoost();
}
