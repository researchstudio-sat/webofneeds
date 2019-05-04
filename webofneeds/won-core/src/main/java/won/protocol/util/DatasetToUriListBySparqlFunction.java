package won.protocol.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;

/**
 * Expects to be passed a file containing a sparql query that projects a
 * variable ?uri, which must be an RDF resource that can be converted to a
 * {@link URI}.
 * 
 * @author fkleedorfer
 */
public class DatasetToUriListBySparqlFunction extends SparqlFunction<Dataset, List<URI>> {
    public DatasetToUriListBySparqlFunction(String sparqlFile) {
        super(sparqlFile);
    }

    @Override
    public List<URI> apply(Dataset dataset) {
        dataset.begin(ReadWrite.READ);
        Dataset result = DatasetFactory.createGeneral();
        result.begin(ReadWrite.WRITE);
        Query query = QueryFactory.create(sparql);
        List<URI> ret = new ArrayList<URI>();
        try (QueryExecution queryExecution = QueryExecutionFactory.create(query, dataset)) {
            ResultSet resultSet = queryExecution.execSelect();
            if (!resultSet.getResultVars().contains("uri")) {
                throw new IllegalStateException("Query has no variable named 'uri' (read from: " + sparqlFile + ")");
            }
            while (resultSet.hasNext()) {
                QuerySolution solution = resultSet.next();
                RDFNode uriNode = solution.get("uri");
                if (uriNode == null) {
                    throw new IllegalStateException(
                                    "Query has no variable named 'uri' (read from: " + sparqlFile + ")");
                }
                if (!uriNode.isURIResource()) {
                    throw new IllegalStateException(
                                    "Value of result variable 'uri' is not a resource (read from: " + sparqlFile + ")");
                }
                ret.add(URI.create(uriNode.asResource().getURI().toString()));
            }
        }
        return ret;
    }
}
