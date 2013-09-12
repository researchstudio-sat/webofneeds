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
import org.apache.solr.search.SolrIndexSearcher;
import won.matcher.Matcher;

/**
 * User: fkleedorfer
 * Date: 12.09.13
 */
public class SearchService
{
  private SolrIndexSearcher solrIndexSearcher;
  private Matcher matcher;

  public SearchService(final SolrIndexSearcher solrIndexSearcher)
  {
    this.solrIndexSearcher = solrIndexSearcher;
    this.matcher = new Matcher(solrIndexSearcher);
  }

  public SearchResult search(String keywords){
    return null;
  }

  public SearchResult search(String keywords, Model need){
    return null;
  }

  public SearchResult search(Model need){
    return null;
  }
}
