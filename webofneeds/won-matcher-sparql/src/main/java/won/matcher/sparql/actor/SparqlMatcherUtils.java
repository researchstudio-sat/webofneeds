package won.matcher.sparql.actor;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpVisitor;
import org.apache.jena.sparql.algebra.OpVisitorByTypeBase;
import org.apache.jena.sparql.algebra.TransformBase;
import org.apache.jena.sparql.algebra.TransformCopy;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.algebra.op.Op0;
import org.apache.jena.sparql.algebra.op.Op1;
import org.apache.jena.sparql.algebra.op.Op2;
import org.apache.jena.sparql.algebra.op.OpConditional;
import org.apache.jena.sparql.algebra.op.OpExt;
import org.apache.jena.sparql.algebra.op.OpFilter;
import org.apache.jena.sparql.algebra.op.OpGraph;
import org.apache.jena.sparql.algebra.op.OpJoin;
import org.apache.jena.sparql.algebra.op.OpLeftJoin;
import org.apache.jena.sparql.algebra.op.OpMinus;
import org.apache.jena.sparql.algebra.op.OpN;
import org.apache.jena.sparql.algebra.op.OpNull;
import org.apache.jena.sparql.algebra.op.OpProject;
import org.apache.jena.sparql.algebra.op.OpSequence;
import org.apache.jena.sparql.algebra.op.OpService;
import org.apache.jena.sparql.algebra.op.OpSlice;
import org.apache.jena.sparql.algebra.op.OpTriple;
import org.apache.jena.sparql.algebra.op.OpUnion;
import org.apache.jena.sparql.algebra.walker.Walker;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.E_NotExists;

import won.protocol.vocabulary.WON;

public class SparqlMatcherUtils {
    /**
     * Adds a graph op to the query, right underneath the project op. Needed for
     * executing a query in an in-memory Dataset.
     * 
     * @param queryOp
     * @param graphVarName
     *            optional graph name. If not specified, a random variable name is
     *            generated. Can be a variable (starting with '?') or an URI (NOT
     *            enclosed by '<' and '>')
     * @return
     */
    public static Op addGraphOp(Op queryOp, Optional<String> graphVarName) {
        Random rnd = new Random();
        String graphVariable = graphVarName.orElse(new String("graph" + Long.toHexString(rnd.nextLong())));
        Op queryWithGraphClause = Transformer.transform(new TransformCopy() {
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
        return Transformer.transform(new TransformCopy() {

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
                return op instanceof OpService && (!serviceURI.isPresent()
                        || serviceURI.get().equals(((OpService) op).getService().toString()));
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
        final Op[] toplevelOpProject = new Op[]{null}; 
        Walker.walk(op, new OpVisitorByTypeBase() {
            @Override
            public void visit(OpProject opProject) {
                //the visitor is called after returning from the recursion, so
                //we have to replace any project op we found deeper in the tree
                //to end up with the toplevel one in the end
                toplevelOpProject[0] = opProject;
            }
        });
        return Optional.ofNullable(toplevelOpProject[0]);
    }
    
    public static Op hintForCounterpartQuery(Op q, Var resultName, long limit) {
        Optional<Op> topLevelProject = findToplevelOpProject(q);
        if (!topLevelProject.isPresent()) {
            return q;
        }
        q = Transformer.transform(new TransformCopy(), q);
        return Transformer.transform(new TransformCopy() {
            public Op transform(OpProject op, Op subOp) {
                if (op == topLevelProject.get()) {
                    //only transform the toplevel projection
                    return new OpSlice(
                            op.copy(
                                    OpFilter.filter(
                                            new E_NotExists(
                                                    new OpTriple(
                                                            new Triple(
                                                                    resultName,
                                                                    WON.HAS_FLAG.asNode(),
                                                                    WON.NO_HINT_FOR_COUNTERPART.asNode()
                                                            )
                                                    )
                                            ),
                                            subOp
                                    )
                            ),
                            0,
                            limit
                    );
                } else {
                    return super.transform(op, subOp);
                }
            }
        }, q);
    }

    public static Op noHintForCounterpartQuery(Op q, Var resultName, long limit) {
        Optional<Op> topLevelProject = findToplevelOpProject(q);
        if (!topLevelProject.isPresent()) {
            return q;
        }
        q = Transformer.transform(new TransformCopy(), q);        
        return Transformer.transform(new TransformCopy() {
            public Op transform(OpProject op, Op subOp) {
                if (op == topLevelProject.get()) {
                    //only transform the toplevel projection
                    return new OpSlice(
                            op.copy(
                                    OpJoin.create(
                                            new OpTriple(
                                                    new Triple(
                                                            resultName,
                                                            WON.HAS_FLAG.asNode(),
                                                            WON.NO_HINT_FOR_COUNTERPART.asNode()
                                                    )
                                            ),
                                            subOp
                                    )
                            ),
                            0,
                            limit);
                } else {
                    return super.transform(op, subOp);
                }
            }
        }, q);
    }
}
