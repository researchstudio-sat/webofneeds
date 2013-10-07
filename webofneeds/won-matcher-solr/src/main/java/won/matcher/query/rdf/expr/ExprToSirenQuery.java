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

package won.matcher.query.rdf.expr;

import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.expr.*;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.sindice.siren.search.SirenBooleanClause;
import org.sindice.siren.search.SirenBooleanQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.matcher.query.rdf.RdfToSirenQuery;
import won.matcher.query.rdf.expr.library.*;

import java.util.*;

/**
 * User: fkleedorfer
 * Date: 03.09.13
 */
public class ExprToSirenQuery
{
  private static final Logger logger = LoggerFactory.getLogger(ExprToSirenQuery.class);


  private static final Map<Class,ExpressionToQueryMapper> mappers = new HashMap<Class, ExpressionToQueryMapper>();
  static {
    mappers.put(E_GreaterThan.class, new MapE_GreaterThan());
    mappers.put(E_LessThan.class, new MapE_LessThan());
    mappers.put(E_GreaterThanOrEqual.class, new MapE_GreaterThanOrEqual());
    mappers.put(E_LessThanOrEqual.class, new MapE_LessThanOrEqual());
    mappers.put(E_Equals.class, new MapE_Equals());
    mappers.put(E_NotEquals.class, new MapE_NotEquals());
    mappers.put(E_OneOf.class, new MapE_OneOf());
    mappers.put(E_NotOneOf.class, new MapE_NotOneOf());
  }


  public static List<QueriesForVariables> createQueriesForVariables(ExprList expressions){
    return new ArrayList<QueriesForVariables>();
  }

  public static BooleanQuery createQueryForBGPWithExpressions(final OpBGP opBGP, final ExprList expressions, final String field, RdfToSirenQuery rdfToSirenQuery){
    QueryGenerator queryGenerator = new QueryGenerator(opBGP, field, rdfToSirenQuery);
    BooleanQuery ret = new BooleanQuery();
    for (Expr expr: expressions){
      ExprWalker.walk(queryGenerator,expr);
      ret.add(queryGenerator.getQuery(), BooleanClause.Occur.MUST);
    }
    logger.debug("created query for bgp {} and expressions {}", opBGP, expressions);
    logger.debug("query is: {}", ret);
    return ret;
  }

  private static class QueryGenerationContext{
    private Set<String> variables = null;
    private QueriesForVariables queriesForVariables = null;
    private BooleanQuery query = null;
    private LogicalAggregationMode logicalMode = LogicalAggregationMode.NONE;

    public void addVariables(Set<String> otherVars){
      if (this.variables == null){ this.variables = new HashSet<String>(); }
      if (otherVars != null) this.variables.addAll(otherVars);
    }

    public void addBooleanClause(BooleanClause clause){
      if (this.query == null) { this.query = new BooleanQuery(); }
      if (clause != null) this.query.add(clause);
    }

    public void addClauseForVariable(String variable, SirenBooleanClause clause){
      if (this.queriesForVariables == null) this.queriesForVariables = new QueriesForVariables();
      if (clause != null && variable != null) this.queriesForVariables.addClause(variable, clause);
    }

    public void addQueriesForVariablesFromOtherContext(QueriesForVariables other, SirenBooleanClause.Occur occur) {
      if (this.queriesForVariables == null) this.queriesForVariables = new QueriesForVariables();
      if (other != null) this.queriesForVariables.addQueriesFromOther(other,occur);
    }

    public void setLogicalMode(LogicalAggregationMode mode) {
      this.logicalMode = mode;
    }

    public Set<String> getVariables()
    {
      return variables;
    }

    public QueriesForVariables getQueriesForVariables()
    {
      return queriesForVariables;
    }

    public BooleanQuery getQuery()
    {
      return query;
    }

    public LogicalAggregationMode getLogicalMode()
    {
      return logicalMode;
    }

    public void addVariable(final String varName)
    {
      if (this.variables == null ) { this.variables = new HashSet<String>(); }
      if (varName != null) this.variables.add(varName);
    }

    @Override
    public String toString()
    {
      return "QueryGenerationContext{" +
          "variables=" + variables +
          ", queriesForVariables=" + queriesForVariables +
          ", query=" + query +
          ", logicalMode=" + logicalMode +
          '}';
    }
  }


  private enum LogicalAggregationMode
  {
    AND,OR,NONE;
    public boolean isCompatible(LogicalAggregationMode other){
      return this == other || other == NONE || this == NONE;
    }
  }

  /**
   * Collects variable names in a bottom-up direction. Keeps a stack with
   * the set of variable names of the current subtree at its head.
   */
  private static class QueryGenerator extends ExprVisitorBase {
    //keeps track of the variables used in the current subtree
    private Stack<QueryGenerationContext> contextStack;
    private OpBGP opBGP;
    private String field;
    private RdfToSirenQuery rdfToSirenQuery;

    private QueryGenerator(OpBGP opBGP, String field, RdfToSirenQuery rdfToSirenQuery)
    {
      this.contextStack = new Stack<QueryGenerationContext>();
      this.opBGP = opBGP;
      this.field = field;
      this.rdfToSirenQuery = rdfToSirenQuery;
    }

    private BooleanQuery getQuery(){
      if (this.contextStack == null || this.contextStack.empty()) return new BooleanQuery();
      QueryGenerationContext ctx = contextStack.peek();
      logger.debug("getting final query from this context: {}", ctx);
      if (ctx.getQueriesForVariables() != null){
        //we still have variables with restrictions - we have to generate more queries
        return (rdfToSirenQuery.createQueryForTriples(this.opBGP.getPattern().getList(), ctx.getQueriesForVariables(), this.field));
      }
      //no more queries to generate, we're already done
      return ctx.getQuery();

    }

    @Override
    public void visit(final ExprFunction0 func)
    {
      logger.debug("visiting: {}",func);
      logger.debug("current queryGenerationContext: {}", this.contextStack.peek());
    }

    @Override
    public void visit(final ExprFunction1 func)
    {
      logger.debug("visiting: {}",func);
      //don't change the stack - whatever is on top (the variables inside the only function argument) is also
      //what should be propagated upwards
      logger.debug("current queryGenerationContext: {}", this.contextStack.peek());
    }

    @Override
    public void visit(final ExprFunction2 func)
    {
      logger.debug("visiting: {}",func);
      QueryGenerationContext ctx = new QueryGenerationContext();

      QueryGenerationContext leftCtx = contextStack.pop();
      QueryGenerationContext rightCtx = contextStack.pop();

      if (func instanceof E_LogicalAnd){
        ctx.setLogicalMode(LogicalAggregationMode.AND);
        addToContext(ctx,leftCtx);
        addToContext(ctx,rightCtx);
      } else if (func instanceof E_LogicalOr){
        ctx.setLogicalMode(LogicalAggregationMode.OR);
        addToContext(ctx,leftCtx);
        addToContext(ctx,rightCtx);
      } else {
        // it's not a && or || node. If we have exactly one variable, we can try to create a siren query for the function
        ctx.addVariables(leftCtx.getVariables());
        ctx.addVariables(rightCtx.getVariables());
        if (ctx.getVariables().size() == 1) {
          ExpressionToQueryMapper mapper = mappers.get(func.getClass());
          if (mapper != null){
            SirenBooleanQuery query = mapper.mapExpression(func,this.field, SirenBooleanClause.Occur.MUST, rdfToSirenQuery);
            //as we just created ctx, there are no queries for variables. By adding a clause, we set the only such mapping
            for (SirenBooleanClause clause: query.getClauses()){
              ctx.addClauseForVariable(ctx.getVariables().iterator().next(), clause);
            }
          }
        }
      }
      contextStack.push(ctx);
      logger.debug("current queryGenerationContext: {}", this.contextStack.peek());
    }

    @Override
    public void visit(final ExprFunction3 func)
    {
      logger.debug("visiting: {}",func);
      //if(expr,expr,expr) only supported without variables
      logger.debug("current queryGenerationContext: {}", this.contextStack.peek());
    }

    @Override
    public void visit(final ExprFunctionN func)
    {
      logger.debug("visiting: {}",func);
      QueryGenerationContext ctx = new QueryGenerationContext();
      QueryGenerationContext[] subContexts = new QueryGenerationContext[func.numArgs()];
      //pop N times for each argument of func
      logger.debug("processing function {} with {} arguments", func, func.numArgs());
      for (int i = 0; i < func.numArgs(); i++){
        subContexts[i] = contextStack.pop();
        ctx.addVariables(subContexts[i].getVariables());
      }
      if (logger.isDebugEnabled()) logger.debug("found {} variables in subexpressions", ctx.getVariables().size());
      if (ctx.getVariables().size() == 1) {
        //we only support 'in' and 'not in' without variables contained in the expressions.
        ExpressionToQueryMapper mapper = mappers.get(func.getClass());
        if (mapper != null){
          SirenBooleanQuery query = mapper.mapExpression(func, field, SirenBooleanClause.Occur.SHOULD, rdfToSirenQuery);
          for (SirenBooleanClause clause: query.getClauses()){
            ctx.addClauseForVariable(ctx.getVariables().iterator().next(), clause);
          }
        }
      }
      contextStack.push(ctx);
      logger.debug("current queryGenerationContext: {}", this.contextStack.peek());
    }

    @Override
    public void visit(final ExprFunctionOp op)
    {
      logger.debug("visiting: {}",op);
      //TODO: handle EXISTS, NOT EXISTS, and SCALAR(?) properly!
      logger.debug("current queryGenerationContext: {}", this.contextStack.peek());
    }

    @Override
    public void visit(final NodeValue nv)
    {
      logger.debug("visiting: {}",nv);
      contextStack.push(new QueryGenerationContext());
      logger.debug("current queryGenerationContext: {}", this.contextStack.peek());
    }

    @Override
    public void visit(final ExprVar nv)
    {
      logger.debug("visiting: {}",nv);
      QueryGenerationContext ctx = new QueryGenerationContext();
      ctx.addVariable(nv.getVarName());
      contextStack.push(ctx);
      logger.debug("current queryGenerationContext: {}", this.contextStack.peek());
    }

    @Override
    public void visit(final ExprAggregator eAgg)
    {
      logger.debug("visiting: {}",eAgg);
      //TODO: find out what this does and handle properly
      logger.debug("current queryGenerationContext: {}", this.contextStack.peek());
    }

    private void addToContext(QueryGenerationContext ctx, QueryGenerationContext ctxToAdd){

      if (ctx.getLogicalMode().isCompatible(ctxToAdd.getLogicalMode())){
        //if we're in AND (OR) mode and the ctx we want to add also has AND (OR) mode, we can just add the
        //queries for the variables and not build the query right away
        SirenBooleanClause.Occur occur = ctx.getLogicalMode() == LogicalAggregationMode.AND ? SirenBooleanClause.Occur.MUST : SirenBooleanClause.Occur.SHOULD;
        ctx.addQueriesForVariablesFromOtherContext(ctxToAdd.getQueriesForVariables(), occur);
        ctx.addVariables(ctxToAdd.getVariables());
      } else {
        //the subexpression is not compatible with the expression (not another && or || but the opposite):
        //generate main boolean queries from subquery and add results to ctx.queries with the occur determined by the logical mode
        BooleanQuery query =  rdfToSirenQuery.createQueryForTriples(this.opBGP.getPattern().getList(),ctxToAdd.getQueriesForVariables(),this.field);
        BooleanClause.Occur occur = ctx.getLogicalMode() == LogicalAggregationMode.AND ? BooleanClause.Occur.MUST : BooleanClause.Occur.SHOULD;
        ctx.addBooleanClause(new BooleanClause(query,occur));
      }
    }
  }





}
