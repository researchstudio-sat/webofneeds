package won.matcher.sparql.actor;

import java.util.Optional;
import java.util.Random;

import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.TransformCopy;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.algebra.op.OpGraph;
import org.apache.jena.sparql.algebra.op.OpProject;
import org.apache.jena.sparql.core.Var;

public class SparqlMatcherUtils {
    /**
     * Adds a graph op to the query, right underneath the project op. Needed
     * for executing a query in an in-memory Dataset.
     * @param queryOp
     * @param graphVarName optional graph name. If not specified, a random variable name is generated.
     * @return
     */
    public static Op addGraphOp(Op queryOp, Optional<String> graphVarName) {
        Random rnd = new Random();
        String graphVariable = graphVarName.orElse(new String("graph"+Long.toHexString(rnd.nextLong())));
        Op queryWithGraphClause = Transformer.transform(new TransformCopy() {
            @Override
            public Op transform(OpProject opProject, Op subOp) {
                return opProject.copy(new OpGraph(Var.alloc(graphVariable), subOp));
            }
        }, queryOp);
        return queryWithGraphClause;
    }
    
    public static Op addGraphOp(Op queryOp) {
        return addGraphOp(queryOp, Optional.empty());
    }
}
