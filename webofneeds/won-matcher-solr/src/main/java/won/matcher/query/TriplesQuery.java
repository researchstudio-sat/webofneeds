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

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Query;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.search.SolrIndexSearcher;

import java.io.IOException;
import java.util.Collection;

/**
 * User: fkleedorfer
 * Date: 30.07.13
 */
public class TriplesQuery extends AbstractQuery
{
  private String field;

  public TriplesQuery(final BooleanClause.Occur occur, final String fields)
  {
    super(occur);
    this.field = fields;
  }

  @Override
  public Query getQuery(final SolrIndexSearcher indexSearcher, final SolrInputDocument inputDocument) throws IOException
  {
    Collection<Object> triples =  inputDocument.getFieldValues(field);
    Model model = toModel(triples);
    return null;
  }

  private Model toModel(Collection<Object> fieldValues) {
    return ModelFactory.createDefaultModel();
  }
}
