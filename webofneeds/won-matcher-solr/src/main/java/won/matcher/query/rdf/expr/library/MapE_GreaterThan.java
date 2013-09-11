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
import org.sindice.siren.search.SirenPrimitiveQuery;
import won.matcher.query.rdf.RdfToSirenQuery;
import won.matcher.query.rdf.expr.library.util.NumericRangeUtils;

/**
 * Maps '>' to a Siren query, expecting exactly one variable its the expressions.
 * User: fkleedorfer
 * Date: 02.09.13
 */
public class MapE_GreaterThan extends BinaryOperator
{

  @Override
  protected SirenPrimitiveQuery mapExpressionWithValueOnRightSide(final Expr value, final String field, final RdfToSirenQuery rdfToSirenQuery)
  {
    return NumericRangeUtils.getInstance().newGreaterThanRange(field,value,false);
  }

  @Override
  protected SirenPrimitiveQuery mapExpressionWithValueOnLeftSide(final Expr value, final String field, final RdfToSirenQuery rdfToSirenQuery)
  {
    return NumericRangeUtils.getInstance().newLessThanRange(field, value, false);
  }
}
