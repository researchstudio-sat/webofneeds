package won.protocol.util;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;

public class DatasetToModelBySparqlFunction extends SparqlFunction<Dataset, Model> {

    public DatasetToModelBySparqlFunction(String sparqlFile) {
        super(sparqlFile);
    }

    @Override
    public Model apply(Dataset dataset) {
        Query query = QueryFactory.create(sparql);
        try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
            return qexec.execConstruct();
        }
    }

}
