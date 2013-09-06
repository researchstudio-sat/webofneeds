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

package won.matcher.query.rdf;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.sparql.algebra.*;
import com.hp.hpl.jena.sparql.algebra.op.*;
import com.hp.hpl.jena.sparql.expr.*;
import org.junit.Test;
import won.matcher.query.rdf.algebra.OpToSirenQuery;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/**
 * User: fkleedorfer
 * Date: 02.09.13
 */

public class ExpressionWalkerTests
{
  String queryWithFilter = "PREFIX ex: <http://example.com/m#> SELECT * where {?a ex:hasValue ?c . { ?a ex:hasValue ?d . FILTER (?c > 10)}}";
  String queryWithMultipleFilters = "PREFIX ex: <http://example.com/m#> SELECT * where {?a ex:hasValue ?c . { ?a ex:hasValue ?d  FILTER (?d > 10)} { ?a ex:hasValue ?e}  FILTER (?e > 10)}";
  String queryWithBooleanExpression = "PREFIX ex: <http://example.com/m#> SELECT * where {?a ex:hasValue ?c . ?a ex:hasValue ?d FILTER ((?d > 10 && ?c > 10) || (?d <0 && ?c<0))} ";
  private static final Set<String> EMPTY_SET = new HashSet<String>();

  @Test
  public void testVisitingQueryWithFilter(){
    Query query = QueryFactory.create(queryWithFilter);
    Op op = Algebra.compile(query);
    OpVisitor before = new SysoutVisitor("before");
    OpVisitor after = new SysoutVisitor("after");
    OpVisitor visitor = new SysoutVisitor("visitor");
    OpWalker.walk(op,visitor,before,after);
  }

  @Test
  public void testVisitingQueryWithMultipleFilters(){
    Query query = QueryFactory.create(queryWithMultipleFilters);
    Op op = Algebra.compile(query);
    OpVisitor before = new SysoutVisitor("before");
    OpVisitor after = new SysoutVisitor("after");
    OpVisitor visitor = new SysoutVisitor("visitor");
    OpWalker.walk(op,visitor,before,after);
  }

  @Test
  public void testVisitingQueryWithBooleanExpression(){
    Query query = QueryFactory.create(queryWithBooleanExpression);
    Op op = Algebra.compile(query);
    OpVisitor before = new SysoutVisitor("before");
    OpVisitor after = new SysoutVisitor("after");
    OpVisitor visitor = new SysoutVisitor("visitor");
    OpWalker.walk(op,visitor,before,after);
  }

  @Test
  public void testCreateSirenQueryForQueryWithMultipleFilters(){
    Query query = QueryFactory.create(queryWithMultipleFilters);
    Op op = Algebra.compile(query);
    System.out.println("query: " + op);
    System.out.println(OpToSirenQuery.createQuery(op, "ntriples"));
  }

  @Test
  public void testCreateSirenQueryForQueryWithBooleanExpression(){
    Query query = QueryFactory.create(queryWithBooleanExpression);
    Op op = Algebra.compile(query);
    System.out.println(OpToSirenQuery.createQuery(op, "ntriples"));
  }

  @Test
  public void testExpressionVisitorWithQueryWithBooleanExpression(){
    Query query = QueryFactory.create(queryWithBooleanExpression);
    Op op = Algebra.compile(query);
    OpVisitor before = new SysoutVisitor("before");
    OpVisitor after = new SysoutVisitor("after");
    OpVisitor visitor = new SysoutVisitorWithExpressionVisitor("visitor");
    OpWalker.walk(op,visitor,before,after);
  }

  private class SysoutVisitorWithExpressionVisitor extends SysoutVisitor {
    private SysoutVisitorWithExpressionVisitor(final String message)
    {
      super(message);
    }

    @Override
    protected void visitFilter(final OpFilter op)
    {
      Stack<Set<String>> varStack = new Stack<Set<String>>();
      for (Expr expr: op.getExprs()){
        ExprWalker.walk(new VarNameCollector(varStack), expr);
      }
    }
  }

  private class SysoutVisitor extends OpVisitorByTypeBase{

    private String message;

    private SysoutVisitor(final String message)
    {
      this.message = message;
    }

    @Override
    protected void visitN(final OpN op)
    {
      sysout(op);
    }

    @Override
    protected void visit2(final Op2 op)
    {
      sysout(op);
    }

    @Override
    protected void visit1(final Op1 op)
    {
      sysout(op);
    }

    @Override
    protected void visit0(final Op0 op)
    {
      sysout(op);
    }

    @Override
    protected void visitExt(final OpExt op)
    {
      sysout(op);
    }

    @Override
    protected void visitFilter(final OpFilter op)
    {
      sysout(op);
    }

    @Override
    protected void visitModifer(final OpModifier op)
    {
      sysout(op);
    }

    private void sysout(final Op op)
    {
      System.out.println(message + ": " + op);
    }

  }


  /**
   * Collects variable names in a bottom-up direction. Keeps a stack with
   * the set of variable names of the current subtree at its head.
   */
  private class VarNameCollector extends ExprVisitorBase
  {
    private Stack<Set<String>> vars;

    private VarNameCollector(final Stack<Set<String>> vars)
    {
      this.vars = vars;
    }

    @Override
    public void visit(final ExprFunction0 func)
    {
      this.vars.push(EMPTY_SET);
      System.out.println("func: " + func + ", vars: " + vars.peek());
    }

    @Override
    public void visit(final ExprFunction1 func)
    {
      System.out.println("func: " + func + ", vars: " + vars.peek());
    }

    @Override
    public void visit(final ExprFunction2 func)
    {
      Set<String> combinedVars = new HashSet<String>();
      combinedVars.addAll(vars.pop());
      combinedVars.addAll(vars.pop());
      vars.push(combinedVars);
      System.out.println("func: " + func + ", vars: " + vars.peek());
    }

    @Override
    public void visit(final ExprFunction3 func)
    {
      Set<String> combinedVars = new HashSet<String>();
      combinedVars.addAll(vars.pop());
      combinedVars.addAll(vars.pop());
      combinedVars.addAll(vars.pop());
      vars.push(combinedVars);
      System.out.println("func: " + func + ", vars: " + vars.peek());
    }

    @Override
    public void visit(final ExprFunctionN func)
    {
      Set<String> combinedVars = new HashSet<String>();
      for (int i = 0; i < func.numArgs(); i++){
        combinedVars.addAll(vars.pop());
        combinedVars.addAll(vars.pop());
        combinedVars.addAll(vars.pop());
      }
      vars.push(combinedVars);
      System.out.println("func: " + func + ", vars: " + vars.peek());
    }

    @Override
    public void visit(final ExprFunctionOp op)
    {
      //TODO: handle EXISTS, NOT EXISTS, and SCALAR(?) properly!
      vars.push(EMPTY_SET);
      System.out.println("funcOp: " + op+ ", vars: " + vars.peek());
    }

    @Override
    public void visit(final NodeValue nv)
    {
      vars.push(EMPTY_SET);
      System.out.println("nodevalue: " + nv + ", vars: " + vars.peek());
    }

    @Override
    public void visit(final ExprVar nv)
    {
      HashSet<String> setForVar = new HashSet<String>();
      setForVar.add(nv.getVarName());
      vars.push(setForVar);
      System.out.println("exprVar: " + nv + ", vars: " + vars.peek());
    }

    @Override
    public void visit(final ExprAggregator eAgg)
    {
      //TODO: find out what this does and handle properly
      vars.push(EMPTY_SET); //may not be the best way to handle this situation
      System.out.println("aggregation: " + eAgg + ", vars: " + vars.peek());
    }
  }


  /**
   * prints variable names found on the stack
   */
  private class VarNamePrinter extends ExprVisitorBase
  {
    private Stack<Set<String>> vars;

    private VarNamePrinter(final Stack<Set<String>> vars)
    {
      this.vars = vars;
    }

    @Override
    public void visit(final ExprFunction0 func)
    {
      System.out.println("func: " + func + ", vars: " + vars.peek());
    }

    @Override
    public void visit(final ExprFunction1 func)
    {
      System.out.println("func: " + func + ", vars: " + vars.peek());
    }

    @Override
    public void visit(final ExprFunction2 func)
    {
      System.out.println("func: " + func + ", vars: " + vars.peek());
    }

    @Override
    public void visit(final ExprFunction3 func)
    {
      System.out.println("func: " + func + ", vars: " + vars.peek());
    }

    @Override
    public void visit(final ExprFunctionN func)
    {
      System.out.println("func: " + func + ", vars: " + vars.peek());
    }

    @Override
    public void visit(final ExprFunctionOp op)
    {
      System.out.println("funcOp: " + op + ", vars: " + vars.peek());
    }

    @Override
    public void visit(final NodeValue nv)
    {
      System.out.println("nodevalue: " + nv + ", vars: " + vars.peek());
    }

    @Override
    public void visit(final ExprVar nv)
    {
      System.out.println("exprVar: " + nv + ", vars: " + vars.peek());
    }

    @Override
    public void visit(final ExprAggregator eAgg)
    {
      System.out.println("exprAgg: " + eAgg + ", vars: " + vars.peek());
    }
  }
}
