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

import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.OpVisitorBase;
import com.hp.hpl.jena.sparql.algebra.OpWalker;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.algebra.op.OpFilter;
import com.hp.hpl.jena.sparql.expr.ExprList;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.matcher.query.rdf.algebra.expr.ExprToSirenQuery;
import won.matcher.query.rdf.algebra.expr.TransformFilterCNF;

import java.util.Stack;




/**
 * Visits a jena query Op and generates a SIREn query from it. The Op must represent
 * a star-shaped query that is compatible with the type of query SIREn supports.
 */
public class OpToSirenQuery
{
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private Stack<ExprList> expressionStack = new Stack<ExprList>();
  private String field;


  /**
   * Creates a BooleanQuery on the specified lucene index field for the specified jena query op.
   * @param op
   * @param field
   * @return
   */
  public static BooleanQuery createQuery(Op op, String field){
     OpToSirenQuery creator = new OpToSirenQuery(field);
     return creator.doCreateQuery(op);
  }

  private OpToSirenQuery(String field)
  {
    this.field = field;
  }


  private BooleanQuery doCreateQuery(Op op)
  {
    ExpressionCollector collectorVisitor = new ExpressionCollector();
    QueryCreator creatorVisitor = new QueryCreator();
    Popper popper = new Popper();
    OpWalker.walk(op, creatorVisitor, collectorVisitor, popper);
    return creatorVisitor.getQuery();
  }

  /**
   * The before visitor: collects filter expressions and pushes them into the expression stack
   */
  private class ExpressionCollector extends OpVisitorBase {
    @Override
    public void visit(final OpFilter opFilter)
    {
      ExprList expressions = null;
      //peek into the stack and copy the expressions there (if any)
      if (expressionStack.isEmpty()){
        expressions = new ExprList();
      } else {
        //Caution: we're making a shallow copy here
        expressions = new ExprList(expressionStack.peek());
      }
      //add the expressions of the filter, transforming them to CNF first
      expressions.addAll(TransformFilterCNF.translateFilterExpressionsToCNF(opFilter));
      //push onto the stack
      logger.debug("pushing onto stack: " + expressions);
      expressionStack.push(expressions);
    }
  }

  /**
   * The main visitor: turns BGPs into Siren queries using the expressions collected by the before visitor
   */
  private class QueryCreator extends OpVisitorBase
  {
    private BooleanQuery query = new BooleanQuery();

    public BooleanQuery getQuery()
    {
      return query;
    }

    public void visit(final OpBGP opBGP)
    {
      logger.debug("creating query for BGP:\n{}", opBGP);
      //create variable-specific queries for current expressions (from the stack)
      ExprList expressions = null;
      if(!expressionStack.isEmpty()){
        expressions = expressionStack.peek();
      } else {
        expressions = new ExprList();
      }
      logger.debug("expressions:" + expressions);
      if (expressions.isEmpty()) {
        query.add(TriplesToSirenQuery.createQueryForTriples(opBGP.getPattern().getList(),null,field), BooleanClause.Occur.MUST);
      } else {
        query.add(ExprToSirenQuery.createQueryForBGPWithExpressions(opBGP,expressions,field), BooleanClause.Occur.MUST);
      }

    }
  }

  /**
   * The after visitor: pops the expression stack.
   */
  private class Popper extends OpVisitorBase {
    @Override
    public void visit(final OpFilter opFilter)
    {
      if (!expressionStack.isEmpty()) expressionStack.pop();
    }
  }






}
