package won.protocol.util;

import org.apache.jena.graph.Node;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprVar;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * Expects to be passed a file containing a sparql SELECT query and a function
 * for processing each individual result.
 * 
 * @author fkleedorfer
 */
public class SparqlSelectFunction<T> extends SparqlFunction<Dataset, List<T>> {
    private Long limit = null;
    private Long offset = null;
    private QuerySolutionMap initialBinding = null;
    private Function<QuerySolution, T> resultGenerator;
    private List<Expr> havingCondiditions = null;
    private List<SortCondition> orderBy = null;
    private Query query = null;

    public SparqlSelectFunction(String sparqlFile, Function<QuerySolution, T> resultMapper) {
        super(sparqlFile);
        this.resultGenerator = resultMapper;
    }

    public SparqlSelectFunction<T> limit(long limit) {
        this.limit = limit;
        return this;
    }

    public PrefixMapping getPrefixMapping() {
        return getQuery().getPrefixMapping();
    }

    public SparqlSelectFunction<T> offset(long offset) {
        this.offset = offset;
        return this;
    }

    public SparqlSelectFunction<T> addInitialBinding(String varName, RDFNode value) {
        if (this.initialBinding == null) {
            this.initialBinding = new QuerySolutionMap();
        }
        this.initialBinding.add(varName, value);
        return this;
    }

    public SparqlSelectFunction<T> addInitialBindings(QuerySolution moreInitialBindings) {
        if (this.initialBinding == null) {
            this.initialBinding = new QuerySolutionMap();
        }
        for (Iterator<String> it = moreInitialBindings.varNames(); it.hasNext();) {
            String varName = it.next();
            this.initialBinding.add(varName, moreInitialBindings.get(varName));
        }
        return this;
    }

    public SparqlSelectFunction<T> having(Expr havingCondition) {
        if (this.havingCondiditions == null) {
            this.havingCondiditions = new ArrayList<>();
        }
        this.havingCondiditions.add(havingCondition);
        return this;
    }

    public SparqlSelectFunction<T> addOrderBy(SortCondition condition) {
        if (orderBy == null)
            orderBy = new ArrayList<>();
        orderBy.add(condition);
        return this;
    }

    public SparqlSelectFunction<T> addOrderBy(Expr expr, int direction) {
        SortCondition sc = new SortCondition(expr, direction);
        addOrderBy(sc);
        return this;
    }

    public SparqlSelectFunction<T> addOrderBy(Node var, int direction) {
        if (!var.isVariable())
            throw new QueryException("Not a variable: " + var);
        SortCondition sc = new SortCondition(var, direction);
        addOrderBy(sc);
        return this;
    }

    /**
     * Use Query.ORDER_* for the direction value.
     * 
     * @param varName
     * @param direction
     */
    public SparqlSelectFunction<T> addOrderBy(String varName, int direction) {
        varName = Var.canonical(varName);
        SortCondition sc = new SortCondition(new ExprVar(varName), direction);
        addOrderBy(sc);
        return this;
    }

    private Query getQuery() {
        if (this.query == null) {
            this.query = QueryFactory.create(this.sparql);
        }
        return this.query;
    }

    @Override
    public List<T> apply(Dataset dataset) {
        boolean existingTransaction = dataset.isInTransaction();
        if (!existingTransaction) {
            dataset.begin(ReadWrite.READ);
        }
        Dataset result = DatasetFactory.createGeneral();
        result.begin(ReadWrite.WRITE);
        try {
            Query theQuery = this.getQuery();
            if (this.limit != null) {
                theQuery.setLimit(this.limit);
            }
            if (this.offset != null) {
                theQuery.setOffset(this.offset);
            }
            if (this.orderBy != null) {
                for (SortCondition sortCondition : this.orderBy) {
                    theQuery.addOrderBy(sortCondition);
                }
            }
            if (this.havingCondiditions != null) {
                for (Expr havingCondition : this.havingCondiditions) {
                    theQuery.addHavingCondition(havingCondition);
                }
            }
            QuerySolution binding = this.initialBinding;
            if (binding == null) {
                binding = new QuerySolutionMap();
            }
            List<T> ret = new ArrayList<>();
            try (QueryExecution queryExecution = QueryExecutionFactory.create(theQuery, dataset, binding)) {
                ResultSet resultSet = queryExecution.execSelect();
                while (resultSet.hasNext()) {
                    ret.add(this.resultGenerator.apply(resultSet.next()));
                }
            }
            return ret;
        } finally {
            if (!existingTransaction) {
                dataset.end();
            }
            result.commit();
        }
    }
}
