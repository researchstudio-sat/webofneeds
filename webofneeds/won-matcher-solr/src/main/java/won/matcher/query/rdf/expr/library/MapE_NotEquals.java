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

import com.hp.hpl.jena.sparql.expr.Expr;
import org.apache.lucene.index.Term;
import org.sindice.siren.search.SirenBooleanClause;
import org.sindice.siren.search.SirenBooleanQuery;
import org.sindice.siren.search.SirenPrimitiveQuery;
import org.sindice.siren.search.SirenWildcardQuery;
import won.matcher.query.rdf.RdfToSirenQuery;
import won.matcher.query.rdf.expr.library.util.ExprUtils;

/**
 * Maps '=' to a Siren query, expecting exactly one variable its the expressions.
 * User: fkleedorfer
 * Date: 02.09.13
 */
public class MapE_NotEquals extends BinaryOperator
{
  @Override
  protected SirenPrimitiveQuery mapExpressionWithValueOnRightSide(final Expr value, final String field, final RdfToSirenQuery rdfToSirenQuery)
  {
    SirenBooleanQuery query = new SirenBooleanQuery();
    query.add(rdfToSirenQuery.createSirenPrimitiveQueryForNode(ExprUtils.evaluate(value).asNode(),null, field), SirenBooleanClause.Occur.MUST_NOT);
    //add a wildcard query because a boolean query containing only Occur.MUST_NOT doesn't match anything
    //caution - this may impact performance.
    // TODO: check if lucene's MatchAllDocsQuery can be adapted for SIREn
    query.add(new SirenWildcardQuery(new Term(field, "*")), SirenBooleanClause.Occur.MUST);
    return query;
  }

  @Override
  protected SirenPrimitiveQuery mapExpressionWithValueOnLeftSide(final Expr value, final String field, final RdfToSirenQuery rdfToSirenQuery)
  {
    return mapExpressionWithValueOnRightSide(value,field, rdfToSirenQuery);
  }
}
