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

package won.matcher.query.rdf.algebra;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.sindice.siren.search.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.matcher.query.rdf.algebra.expr.QueriesForVariables;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TriplesToSirenQuery
{
  private static final Pattern PATTERN_REMOVE_QUOTES = Pattern.compile("\"(.+)\".*");
  private static final BooleanClause.Occur DEFAULT_OCCUR = BooleanClause.Occur.SHOULD;
  private static final SirenBooleanClause.Occur OCCUR_LITERAL = SirenBooleanClause.Occur.MUST;
  private static final SirenTupleClause.Occur OCCUR_TRIPLE = SirenTupleClause.Occur.MUST;
  private static final BooleanClause.Occur OCCUR_STAR = BooleanClause.Occur.MUST;
  private static final BooleanClause.Occur OCCUR_ALL = BooleanClause.Occur.MUST;

  private static Logger logger = LoggerFactory.getLogger(TriplesToSirenQuery.class);

  public static BooleanQuery createQueryForTriples(List<Triple> triples, QueriesForVariables queriesForVariables, String field) {
    if (queriesForVariables == null) queriesForVariables = new QueriesForVariables();
    BooleanQuery query = new BooleanQuery();
    for (Triple triple: triples){
      SirenTupleQuery tupleQuery = new SirenTupleQuery();
      fillTupleQueryFromTriple(triple, tupleQuery, queriesForVariables, field);
      if (tupleQuery.getClauses().length > 0){
        query.add(tupleQuery, OCCUR_STAR);
      }
    }
    return query;
  }

  private static void fillTupleQueryFromTriple(final Triple triple, final SirenTupleQuery sirenTupleQuery, final QueriesForVariables queriesForVariables, String field)
  {
    //subject
    addQueryIfNotNull(sirenTupleQuery, createSirenBooleanQueryForNode(triple.getSubject(), queriesForVariables, field), 0);
    //predicate
    addQueryIfNotNull(sirenTupleQuery, createSirenBooleanQueryForNode(triple.getPredicate(), queriesForVariables, field), 1);
    //object
    SirenBooleanQuery queryForObject =  createSirenBooleanQueryForNode(triple.getObject(), queriesForVariables, field);
    if (queryForObject != null && queryForObject.getBoost() == 1f) { queryForObject.setBoost(10f); }
    addQueryIfNotNull(sirenTupleQuery, queryForObject , 2);
  }

  private static void addQueryIfNotNull(SirenTupleQuery sirenTupleQuery, SirenBooleanQuery sirenBooleanQueryToAdd, int constraint)
  {
    if (sirenBooleanQueryToAdd == null) return;
    SirenCellQuery cellQuery = new SirenCellQuery(sirenBooleanQueryToAdd);
    cellQuery.setConstraint(constraint);
    sirenTupleQuery.add(cellQuery, OCCUR_TRIPLE);
  }

  /**
   * Returns a SirenBooleanQuery that will match the specified node or null
   * if there should not be a restriction.
   *
   * @param node
   * @param queriesForVariables
   */
  private static SirenBooleanQuery createSirenBooleanQueryForNode(Node node, final QueriesForVariables queriesForVariables, String field)
  {
    if (node.isBlank()) return null;
    if (node.isVariable()) {
      SirenBooleanQuery queryForVar = queriesForVariables.getQueryForVariable(node.getName());
      logger.debug("query for variable {}: {}", node.getName(), queryForVar);
      //TODO: make boost and occur configurable!
      queryForVar.setBoost(10000f);
      return queryForVar;
    }
    final SirenBooleanQuery booleanQuery = new SirenBooleanQuery();
    SirenTermQuery sirenTermQuery = new SirenTermQuery(new Term(field, toQuotedString(node)));
    booleanQuery.add(sirenTermQuery,OCCUR_LITERAL);
    return booleanQuery;
  }

  /**
   * adds <..> around an URI.
   *
   * @param node
   * @return
   */
  private static String toQuotedString(Node node)
  {
    if (node.isURI()) return node.toString().toLowerCase();
    if (node.isLiteral()) {
      String nodeAsString = node.toString().toLowerCase();
      Matcher matcher = PATTERN_REMOVE_QUOTES.matcher(nodeAsString);
      if (matcher.matches()) {
        return matcher.group(1);
      }
      return nodeAsString;
    }
    return node.toString();
  }


}