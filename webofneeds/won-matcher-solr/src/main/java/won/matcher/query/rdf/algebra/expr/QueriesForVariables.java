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

package won.matcher.query.rdf.algebra.expr;

import org.sindice.siren.search.SirenBooleanClause;
import org.sindice.siren.search.SirenBooleanQuery;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Stores an 'environment' of siren queries bound to variable names. The siren queries
 * represent the expressions found in sparql filters.
 */
public class QueriesForVariables
{
  private Map<String, SirenBooleanQuery> queryMap;

  public QueriesForVariables() {
    this.queryMap = new HashMap<String, SirenBooleanQuery>();
  }

  public void addClause(String varName, SirenBooleanClause clause){
    SirenBooleanQuery query = this.queryMap.get(varName);
    if (query == null) {
      query = new SirenBooleanQuery();
    }
    query.add(clause);
    this.queryMap.put(varName,query);
  }

  /**
   * Returns a siren query for the given variable or null if no such query is registered.
   * @param varName
   * @return
   */
  public SirenBooleanQuery getQueryForVariable(String varName){
    return this.queryMap.get(varName);
  }

  /**
   * Adds all variable/query bindings from the other QFV object. If a variable is present in both, the
   * queries are combined in a new SirenBooleanQuery, with the specified occur.
   * @param other
   * @param addWithOccur
   */
  public void addQueriesFromOther(QueriesForVariables other, SirenBooleanClause.Occur addWithOccur){
    Set<Map.Entry<String, SirenBooleanQuery>> entries = other.queryMap.entrySet();
    for(Map.Entry<String, SirenBooleanQuery> entry: entries){
      SirenBooleanQuery query = this.queryMap.get(entry.getKey());
      if (query == null){
        //variable not yet contained,
        this.queryMap.put(entry.getKey(), entry.getValue());
      } else {
        //variable is contained, create new booleanquery for both
        SirenBooleanQuery newQuery = new SirenBooleanQuery();
        newQuery.add(query,addWithOccur);
        newQuery.add(entry.getValue(),addWithOccur);
      }
    }
  }

}
