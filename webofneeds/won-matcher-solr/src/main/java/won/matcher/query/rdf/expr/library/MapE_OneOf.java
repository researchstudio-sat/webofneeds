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

package won.matcher.query.rdf.expr.library;

import com.hp.hpl.jena.sparql.expr.E_OneOf;
import com.hp.hpl.jena.sparql.expr.Expr;
import org.sindice.siren.search.SirenBooleanClause;
import org.sindice.siren.search.SirenBooleanQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.matcher.query.rdf.RdfToSirenQuery;
import won.matcher.query.rdf.expr.ExpressionToQueryMapper;
import won.matcher.query.rdf.expr.library.util.ExprUtils;

import java.util.Iterator;

/**
 * User: fkleedorfer
 * Date: 11.09.13
 */
public class MapE_OneOf implements ExpressionToQueryMapper
{
  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Maps a SPARQL 'IN' expression. Expects exactly one variable at position 0, all other args must be constants.
   *
   * @param expression
   * @param field
   * @param occur
   * @param rdfToSirenQuery
   * @return
   */
  @Override
  public SirenBooleanQuery mapExpression(final Expr expression, final String field, final SirenBooleanClause.Occur occur, final RdfToSirenQuery rdfToSirenQuery)
  {
    E_OneOf oneOf = (E_OneOf) expression;
    Iterator<Expr> it = oneOf.getArgs().iterator();
    Expr left = it.next();
    if (!left.isVariable()) return null;
    SirenBooleanQuery query = new SirenBooleanQuery();
    while (it.hasNext()){
      Expr expr = it.next();
      query.add(rdfToSirenQuery.createSirenPrimitiveQueryForNode(ExprUtils.evaluate(expr).asNode(),null, field), SirenBooleanClause.Occur.SHOULD);
    }
    if (query.getClauses().length == 0) return null;
    return query;
  }
}
