package won.matcher.sparql.actor;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelExtract;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StatementBoundary;
import org.apache.jena.rdf.model.StatementBoundaryBase;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpExtend;
import org.apache.jena.sparql.algebra.op.OpFilter;
import org.apache.jena.sparql.algebra.op.OpJoin;
import org.apache.jena.sparql.algebra.op.OpLeftJoin;
import org.apache.jena.sparql.algebra.op.OpProject;
import org.apache.jena.sparql.algebra.op.OpTable;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.util.ExprUtils;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMATCH;

public class QueryByExampleBuilder {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    // the atom's model
    private final Model model;
    // the resource in the model representing the atom
    private final Resource atomResource;
    // the variable for the target atom to find
    private final Var resultVariable;
    // the score to report for the atom
    private final Var scoreVariable;
    private Config config = new Config();

    private class Config {
        // if true, the whole structure must match, if false, parts are allowed not to
        // match
        private boolean matchAllTriples = false;

        public Config() {
        }

        public void setMatchAllTriples(boolean matchAllTriples) {
            this.matchAllTriples = matchAllTriples;
        }

        public boolean isMatchAllTriples() {
            return matchAllTriples;
        }
    }

    public QueryByExampleBuilder(Model model, Resource atomResource, Var resultVariable, Var scoreVariable) {
        this.model = model;
        this.atomResource = atomResource;
        this.resultVariable = resultVariable;
        this.scoreVariable = scoreVariable;
    }

    public QueryByExampleBuilder matchAll() {
        config.setMatchAllTriples(true);
        return this;
    }

    /**
     * For each <code>[atom] match:seeks [seeks-node]</code> triple, extracts the
     * subgraph for the <code>seeks-node</code> and generates a sparql query for
     * each one.
     * 
     * @return
     */
    public Set<Op> build() {
        Set<Op> queries = new HashSet<>();
        StmtIterator seeksIt = model.listStatements(atomResource, WONMATCH.seeks, (RDFNode) null);
        while (seeksIt.hasNext()) {
            Statement seeksStmt = seeksIt.next();
            RDFNode seeksNode = seeksStmt.getObject();
            if (seeksNode.isLiteral()) {
                // ignore any literals as seeks-nodes
                continue;
            }
            Optional<Op> query = createQueryByExample(model, seeksNode.asResource());
            if (query.isPresent()) {
                queries.add(query.get());
            }
        }
        return queries;
    }

    private Optional<Op> createQueryByExample(Model modelForQuery, Resource seeksNode) {
        Set<Var> scoreVars = new HashSet<>();
        Optional<Op> subOp = null;
        try {
            subOp = createQueryForResource(modelForQuery, seeksNode, scoreVars, seeksNode, "v");
            if (subOp.isPresent() && !scoreVars.isEmpty()) {
                Op main = OpJoin.create(subOp.get(),
                                createTripleOp(resultVariable, RDF.type.asNode(), WON.Atom.asNode()));
                main = OpExtend.create(main, scoreVariable, createScoreExpression(scoreVars));
                main = OpFilter.filter(ExprUtils.parse("?" + scoreVariable.getName() + " > 0"), main);
                subOp = Optional.of(main);
            }
        } catch (Exception e) {
            logger.warn("Caught error creating qbe query", e);
            return Optional.empty();
        }
        if (subOp.isPresent()) {
            return Optional.of(new OpProject(subOp.get(), Arrays.asList(resultVariable, scoreVariable)));
        }
        return Optional.empty();
    }

    private Op createTripleOp(Node subject, Node predicate, Node object) {
        BasicPattern bp = new BasicPattern();
        bp.add(new Triple(subject, predicate, object));
        return new OpBGP(bp);
    }

    private Expr createScoreExpression(Set<Var> scoreVars) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        scoreVars.forEach(scoreVar -> {
            // generates: IF(BOUND(?var),?var,0)+
            sb.append("IF(BOUND( ?")
                            .append(scoreVar.getName())
                            .append(" ), ?")
                            .append(scoreVar.getName())
                            .append(", 0)+");
        });
        sb.deleteCharAt(sb.length() - 1); // delete last '+'
        sb.append(") / ");
        sb.append(scoreVars.size());
        return ExprUtils.parse(sb.toString());
    }

    /**
     * For each statement with the spceified node as the subject, create one
     * optional clause and
     * <ul>
     * <li>match for the s/p/o triple, if the object is a literal or an URI resource
     * outside the URI space of the atom.</li>
     * <li>If the object is a blank node or an URI within the URI space of the atom,
     * recurse with the object as the next focusNode</li>
     * </ul>
     * 
     * @param subModel
     * @param scoreVars
     * @param seeksNode
     * @return
     */
    private Optional<Op> createQueryForResource(Model subModel, Resource focusNode, Set<Var> scoreVars,
                    Resource seeksNode, String varPrefixBase) {
        // first, extract submodel for the resource:
        Model qbeModel = extractSubModelForNode(subModel, focusNode.asResource());
        StmtIterator it = qbeModel.listStatements(focusNode, null, (RDFNode) null);
        Optional<Op> op = Optional.empty();
        int index = 0;
        while (it.hasNext()) {
            String scoreVarName = varPrefixBase + "_" + index;
            Statement stmt = it.next();
            Resource subject = stmt.getSubject();
            Property predicate = stmt.getPredicate();
            RDFNode object = stmt.getObject();
            Op opTriple = createTripleOp(makeQueryNode(subject, seeksNode), predicate.asNode(),
                            makeQueryNode(object, seeksNode));
            if (isBlankOrInternalUri(stmt.getObject())) {
                // we recurse
                Optional<Op> subOp = createQueryForResource(subModel, stmt.getObject().asResource(), scoreVars,
                                seeksNode, scoreVarName);
                if (subOp.isPresent()) {
                    op = Optional.of(leftJoinIfNecessary(op, OpLeftJoin.create(opTriple, subOp.get(), (Expr) null),
                                    scoreVars,
                                    scoreVarName));
                } else {
                    op = Optional.of(leftJoinIfNecessary(op, opTriple, scoreVars, scoreVarName));
                }
            } else {
                op = Optional.of(leftJoinIfNecessary(op, opTriple, scoreVars, scoreVarName));
            }
            index++;
        }
        return op;
    }

    /**
     * Creates a left join of opLeft to opRight after adding a score variable to
     * opRight; ; If opLeft is not present, uses the unit table. T
     * 
     * @param opLeft
     * @param opRight
     * @param scoreVars
     * @param scoreVarName
     * @return
     */
    private Op leftJoinIfNecessary(Optional<Op> opLeft, Op opRight, Set<Var> scoreVars, String scoreVarName) {
        opRight = addScoreVar(opRight, scoreVars, scoreVarName);
        if (opLeft.isPresent()) {
            return OpLeftJoin.create(opLeft.get(), opRight, (Expr) null);
        }
        return OpLeftJoin.create(OpTable.unit(), opRight, (Expr) null);
    }

    private Op addScoreVar(Op op, Set<Var> scoreVars, String scoreVarName) {
        Var newScoreVar = Var.alloc(scoreVarName);
        scoreVars.add(newScoreVar);
        return OpExtend.create(op, newScoreVar, ExprUtils.parse("1"));
    }

    private Node makeQueryNode(RDFNode node, Resource seeksNode) {
        if (node.isResource() && node.asResource().equals(seeksNode)) {
            return resultVariable;
        }
        if (isBlankOrInternalUri(node)) {
            return Var.alloc(hashFunction(node.hashCode()));
        }
        return node.asNode();
    }

    private boolean isBlankOrInternalUri(RDFNode node) {
        return node.isAnon() ||
                        (node.isURIResource() && node.asResource().getURI().startsWith(atomResource.getURI()));
    }

    private Model extractSubModelForNode(Model model, Resource node) {
        StatementBoundary boundary = new StatementBoundaryBase() {
            public boolean stopAt(Statement s) {
                return atomResource.equals(s.getSubject());
            }
        };
        return new ModelExtract(boundary).extract(node, model);
    }

    private static String hashFunction(Object input) {
        return Integer.toHexString(input.hashCode());
    }
}
