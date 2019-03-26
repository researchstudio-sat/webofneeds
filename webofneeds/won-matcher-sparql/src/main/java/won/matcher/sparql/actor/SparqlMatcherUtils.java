package won.matcher.sparql.actor;

import java.util.Arrays;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpVisitorBase;
import org.apache.jena.sparql.algebra.OpVisitorByTypeBase;
import org.apache.jena.sparql.algebra.TransformCopy;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.algebra.op.Op2;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpConditional;
import org.apache.jena.sparql.algebra.op.OpDistinct;
import org.apache.jena.sparql.algebra.op.OpFilter;
import org.apache.jena.sparql.algebra.op.OpGraph;
import org.apache.jena.sparql.algebra.op.OpJoin;
import org.apache.jena.sparql.algebra.op.OpLeftJoin;
import org.apache.jena.sparql.algebra.op.OpList;
import org.apache.jena.sparql.algebra.op.OpMinus;
import org.apache.jena.sparql.algebra.op.OpModifier;
import org.apache.jena.sparql.algebra.op.OpOrder;
import org.apache.jena.sparql.algebra.op.OpProject;
import org.apache.jena.sparql.algebra.op.OpReduced;
import org.apache.jena.sparql.algebra.op.OpService;
import org.apache.jena.sparql.algebra.op.OpSlice;
import org.apache.jena.sparql.algebra.op.OpTriple;
import org.apache.jena.sparql.algebra.op.OpUnion;
import org.apache.jena.sparql.algebra.walker.Walker;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.E_LogicalAnd;
import org.apache.jena.sparql.expr.E_LogicalOr;
import org.apache.jena.sparql.expr.E_NotExists;
import org.apache.jena.sparql.expr.E_StrContains;
import org.apache.jena.sparql.expr.E_StrLowerCase;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.nodevalue.NodeValueBoolean;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.vocabulary.RDF;

import won.protocol.vocabulary.WON;

public class SparqlMatcherUtils {
  /**
   * Adds a graph op to the query, right underneath the project op. Needed for
   * executing a query in an in-memory Dataset.
   * 
   * @param queryOp
   * @param graphVarName optional graph name. If not specified, a random variable
   *                     name is generated. Can be a variable (starting with '?')
   *                     or an URI (NOT enclosed by '<' and '>')
   * @return
   */
  public static Op addGraphOp(Op queryOp, Optional<String> graphVarName) {
    Random rnd = new Random();
    String graphVariable = graphVarName.orElse(new String("graph" + Long.toHexString(rnd.nextLong())));
    Op queryWithGraphClause = Transformer.transform(new TransformCopy(true) {
      @Override
      public Op transform(OpProject opProject, Op subOp) {
        if (graphVariable.startsWith("?")) {
          return opProject.copy(new OpGraph(Var.alloc(graphVariable.substring(1)), subOp));
        } else {
          return opProject.copy(new OpGraph(new ResourceImpl(graphVariable).asNode(), subOp));
        }
      }
    }, queryOp);
    return queryWithGraphClause;
  }

  public static Op addGraphOp(Op queryOp) {
    return addGraphOp(queryOp, Optional.empty());
  }

  /**
   * Removes an Op "'SERVICE' '<'{URI}'>'" from the query.
   * 
   * @param serviceURI
   * @return
   */
  public static Op removeServiceOp(Op queryOp, Optional<String> serviceURI) {

    // first, copy the whole op so we don't change the specified one
    // Op queryCopy = Transformer.transform(new TransformCopy(),queryOp);
    // now transform
    return Transformer.transform(new TransformCopy(true) {

      @Override
      public Op transform(OpJoin opJoin, Op left, Op right) {
        return selectSubOpIfOtherIsService(opJoin, left, right, serviceURI);
      }

      @Override
      public Op transform(OpConditional opCond, Op left, Op right) {
        return selectSubOpIfOtherIsService(opCond, left, right, serviceURI);
      }

      @Override
      public Op transform(OpLeftJoin opLeftJoin, Op left, Op right) {
        return selectSubOpIfOtherIsService(opLeftJoin, left, right, serviceURI);
      }

      @Override
      public Op transform(OpMinus opMinus, Op left, Op right) {
        return selectSubOpIfOtherIsService(opMinus, left, right, serviceURI);
      }

      @Override
      public Op transform(OpUnion opUnion, Op left, Op right) {
        return selectSubOpIfOtherIsService(opUnion, left, right, serviceURI);
      }

      private Op selectSubOpIfOtherIsService(Op2 op2, Op left, Op right, Optional<String> serviceURI) {
        if (isSelectedServiceOp(left, serviceURI)) {
          return right;
        }
        if (isSelectedServiceOp(right, serviceURI)) {
          return left;
        }
        return op2;
      }

      private boolean isSelectedServiceOp(Op op, Optional<String> serviceURI) {
        return op instanceof OpService
            && (!serviceURI.isPresent() || serviceURI.get().equals(((OpService) op).getService().toString()));
      }

    }, queryOp);
  }

  public static Op removeServiceOp(Op queryOp) {
    return removeServiceOp(queryOp, Optional.empty());
  }

  /**
   * Finds the top-level projection of the query.
   */
  public static Optional<Op> findToplevelOpProject(Op op) {
    // use a final array to obtain the result of the visit
    final Op[] toplevelOpProject = new Op[] { null };
    Walker.walk(op, new OpVisitorByTypeBase() {
      @Override
      public void visit(OpProject opProject) {
        // the visitor is called after returning from the recursion, so
        // we have to replace any project op we found deeper in the tree
        // to end up with the toplevel one in the end
        toplevelOpProject[0] = opProject;
      }
    });
    return Optional.ofNullable(toplevelOpProject[0]);
  }

  private static Op makePathBGPPattern(Var start, Var end, int hops, Function<Op, Op> postprocess) {
    String tmpPropName = "tmpProp";
    String tmpObjName = "tmpObj";
    Var curSubj = null;
    Var curPred = null;
    Var curObj = start;
    BasicPattern pattern = new BasicPattern();
    for (int i = 0; i < hops; i++) {
      curSubj = curObj;
      curPred = Var.alloc(tmpPropName + "_" + i);
      curObj = (i == hops - 1) ? end : Var.alloc(tmpObjName + "_" + i);
      pattern.add(new Triple(curSubj, curPred, curObj));
    }
    return postprocess.apply(new OpBGP(pattern));
  }

  public static Op createSearchQuery(String searchString, Var resultName, int hops, boolean disjunctive,
      boolean tokenize) {

    Var textSearchTarget = Var.alloc("textSearchTarget");

    Optional<Op> union = IntStream.range(1, hops + 1)
        .mapToObj(hopCount -> makePathBGPPattern(resultName, textSearchTarget, hopCount, op -> {
          Expr filterExpression = Arrays
              .stream(tokenize ? searchString.toLowerCase().split(" ") : new String[] { searchString.toLowerCase() })
              .<Expr>map(searchPart -> new E_StrContains(new E_StrLowerCase(new ExprVar(textSearchTarget)),
                  new NodeValueString(searchPart)))
              .reduce((left, right) -> disjunctive ? new E_LogicalOr(left, right) : new E_LogicalAnd(left, right))
              .orElse(new NodeValueBoolean(true));
          return OpFilter.filterBy(new ExprList(filterExpression), op);
        })).reduce((op1, op2) -> new OpUnion(op1, op2));

    Op maintriple = new OpTriple(new Triple(resultName, RDF.type.asNode(), WON.NEED.asNode()));
    Op mainOp = union.isPresent() ? OpJoin.create(maintriple, union.get()) : maintriple;
    return mainOp;
  }

  public static Op hintForCounterpartQuery(Op q, Var resultName) {
    InsertionTargetFindingVisitor targetFinder = new InsertionTargetFindingVisitor();
    Walker.walk(q, targetFinder);
    OpInserter inserter = targetFinder.getInserter();
    inserter.setNotExistsTriple(new Triple(resultName, WON.HAS_FLAG.asNode(), WON.NO_HINT_FOR_COUNTERPART.asNode()));
    return Transformer.transform(inserter, q);
  }

  public static Op noHintForCounterpartQuery(Op q, Var resultName) {
    InsertionTargetFindingVisitor targetFinder = new InsertionTargetFindingVisitor();
    Walker.walk(q, targetFinder);
    OpInserter inserter = targetFinder.getInserter();
    inserter.setJoinWithTriple(new Triple(resultName, WON.HAS_FLAG.asNode(), WON.NO_HINT_FOR_COUNTERPART.asNode()));
    return Transformer.transform(inserter, q);
  }

  private static class InsertionInfo {
    private Optional<OpModifier> targetOp = Optional.empty();
  }

  private static class InsertionTargetFindingVisitor extends OpVisitorBase {
    private Optional<InsertionInfo> highestCompleteInfo = Optional.empty();
    private InsertionInfo collectingInfo = new InsertionInfo();

    @Override
    public void visit(OpProject op) {
      rememberTreePositionIfFirst(op);
      highestCompleteInfo = Optional.of(collectingInfo);
      collectingInfo = new InsertionInfo();
    }

    private void rememberTreePositionIfFirst(OpModifier mod) {
      if (collectingInfo.targetOp.isPresent())
        return;
      collectingInfo.targetOp = Optional.of(mod);
    }

    @Override
    public void visit(OpOrder op) {
      rememberTreePositionIfFirst(op);
    }

    @Override
    public void visit(OpDistinct op) {
      rememberTreePositionIfFirst(op);
    }

    @Override
    public void visit(OpReduced op) {
      rememberTreePositionIfFirst(op);
    }

    @Override
    public void visit(OpList op) {
      rememberTreePositionIfFirst(op);
    }

    @Override
    public void visit(OpSlice op) {
      rememberTreePositionIfFirst(op);
    }

    public OpInserter getInserter() {
      return new OpInserter(highestCompleteInfo);
    }

  }

  private static class OpInserter extends TransformCopy {
    public OpInserter(Optional<InsertionInfo> insertionInfo) {
      super(true);
      this.insertionInfo = insertionInfo;
    }

    private Optional<Triple> joinWithTriple = Optional.empty();
    private Optional<Triple> notExistsTriple = Optional.empty();
    private Optional<InsertionInfo> insertionInfo = Optional.empty();

    public void setJoinWithTriple(Triple joinWithTriple) {
      this.joinWithTriple = Optional.of(joinWithTriple);
    }

    public void setNotExistsTriple(Triple notExistsTriple) {
      this.notExistsTriple = Optional.of(notExistsTriple);
    }

    private Op insertJoinWith(Op pattern, Triple triple) {
      return OpJoin.create(new OpTriple(triple), pattern);
    }

    private Op insertNotExistsTriple(Op subOp, Triple triple) {
      return OpFilter.filter(new E_NotExists(new OpTriple(triple)), subOp);
    }

    private boolean isTargetOp(Op op) {
      return insertionInfo.isPresent() && insertionInfo.get().targetOp.isPresent()
          && insertionInfo.get().targetOp.get().equals(op);
    }

    public Op performInsertIfAtTarget(OpModifier op, Op subOp) {
      if (isTargetOp(op)) {
        if (joinWithTriple.isPresent()) {
          subOp = insertJoinWith(subOp, joinWithTriple.get());
        }
        if (notExistsTriple.isPresent()) {
          subOp = insertNotExistsTriple(subOp, notExistsTriple.get());
        }
      }
      return op.copy(subOp);
    }

    @Override
    public Op transform(OpOrder op, Op subOp) {
      return performInsertIfAtTarget(op, subOp);
    }

    @Override
    public Op transform(OpDistinct op, Op subOp) {
      return performInsertIfAtTarget(op, subOp);
    }

    @Override
    public Op transform(OpReduced op, Op subOp) {
      return performInsertIfAtTarget(op, subOp);
    }

    @Override
    public Op transform(OpList op, Op subOp) {
      return performInsertIfAtTarget(op, subOp);
    }

    @Override
    public Op transform(OpSlice op, Op subOp) {
      return performInsertIfAtTarget(op, subOp);
    }

    @Override
    public Op transform(OpProject op, Op subOp) {
      return performInsertIfAtTarget(op, subOp);
    }

  }

}
