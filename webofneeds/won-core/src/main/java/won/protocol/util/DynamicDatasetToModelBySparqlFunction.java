package won.protocol.util;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.Model;

public class DynamicDatasetToModelBySparqlFunction extends SparqlFunction<Dataset, Model> {

  QuerySolutionMap initialBinding = new QuerySolutionMap();

  public DynamicDatasetToModelBySparqlFunction(String sparqlFile) {
    super(sparqlFile);
  }

  public DynamicDatasetToModelBySparqlFunction(String sparqlFile, QuerySolutionMap initialBinding) {
    super(sparqlFile);
    this.initialBinding = initialBinding;
  }

  @Override
  public Model apply(Dataset dataset) {
    Query query = QueryFactory.create(sparql);
    try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset, initialBinding)) {
      return qexec.execConstruct();
    }
  }

}
