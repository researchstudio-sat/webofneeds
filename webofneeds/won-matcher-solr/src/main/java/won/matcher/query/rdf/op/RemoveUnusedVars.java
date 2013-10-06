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

package won.matcher.query.rdf.op;

import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.OpVars;
import com.hp.hpl.jena.sparql.algebra.TransformBase;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.algebra.op.OpFilter;
import com.hp.hpl.jena.sparql.algebra.op.OpNull;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.ExprList;

import java.util.Collection;
import java.util.Set;

/**
 * User: fkleedorfer
 * Date: 02.09.13
 */
public class RemoveUnusedVars extends TransformBase
{
  @Override
  public Op transform(final OpFilter opFilter, final Op subOp)
  {
    if (subOp instanceof OpBGP) {
      if (((OpBGP) subOp).getPattern().size() == 0){
        return OpNull.create();
      }
      Collection<Var> varsInPattern = OpVars.mentionedVars(subOp);
      ExprList expressions = opFilter.getExprs();
      ExprList keepExpressions = new ExprList();
      for (Expr expr: expressions){
        Set<Var> varMentioned = expr.getVarsMentioned();
        if (varsInPattern.containsAll(varMentioned)){
          keepExpressions.add(expr);
        }
      }
      return OpFilter.filter(keepExpressions,subOp);
    }
    return opFilter;
  }
}
