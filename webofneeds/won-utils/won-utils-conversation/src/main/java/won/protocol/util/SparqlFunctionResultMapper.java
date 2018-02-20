package won.protocol.util;

import java.util.function.Function;

import org.apache.jena.query.QuerySolution;

public interface SparqlFunctionResultMapper<T> extends Function<QuerySolution, T> {}
