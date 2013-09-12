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

package won.matcher.query.rdf.expr.library.util;

import com.hp.hpl.jena.sparql.engine.binding.BindingFactory;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.function.FunctionEnvBase;

/**
 * User: fkleedorfer
 * Date: 10.09.13
 */
public class ExprUtils
{
  /**
   * Assumes no variables inside expression, evaluates with empty binding and new function environment.
   * @param expression
   * @return
   */
  public static NodeValue evaluate(Expr expression) {
    return expression.eval(BindingFactory.binding(), new FunctionEnvBase());
  }
}
