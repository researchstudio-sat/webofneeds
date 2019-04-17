package won.matcher.utils.tensor;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;

import won.protocol.exception.DataIntegrityException;

/**
 * Executes a sparql query on a sparql endpoint and returns the data as
 * {@link TensorEntry} objects. Expects that the sparql query returns the
 * variables ?slice, ?atom and ?value. Created by hfriedrich on 21.04.2017.
 */
public class TensorEntrySparqlGenerator implements TensorEntryGenerator {
    private String sparqlEndpoint;
    private String query;
    private Map<String, Object> parameterBindings;
    private static String[] variableNames = { "slice", "atom", "value" };

    public TensorEntrySparqlGenerator(String sparqlEndpoint, String sparqlQuery) {
        this.sparqlEndpoint = sparqlEndpoint;
        query = sparqlQuery;
        parameterBindings = new HashMap<>();
    }

    public void addVariableBinding(String var, Object value) {
        parameterBindings.put(var, value);
    }

    public Collection<TensorEntry> generateTensorEntries() {
        Collection<TensorEntry> tensorEntries = new LinkedList<>();
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(query);
        for (String key : parameterBindings.keySet()) {
            Object value = parameterBindings.get(key);
            if (value instanceof String) {
                pss.setLiteral(key, (String) value);
            } else if (value instanceof Long || value instanceof Integer) {
                pss.setLiteral(key, (Long) value);
            } else if (value instanceof Float || value instanceof Double) {
                pss.setLiteral(key, (Double) value);
            } else {
                throw new IllegalArgumentException("Variable must be of type String/Long/Integer/Float/Double");
            }
        }
        Query q = pss.asQuery();
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(sparqlEndpoint, q)) {
            ResultSet results = qexec.execSelect();
            // check that the query returns the right variables
            if (!results.getResultVars().containsAll(Arrays.asList(variableNames))) {
                throw new DataIntegrityException("sparql query is expected to return variables: " + variableNames);
            }
            while (results.hasNext()) {
                TensorEntry entry = new TensorEntry();
                QuerySolution qs = results.next();
                RDFNode node = qs.get("slice");
                entry.setSliceName(node.isResource() ? node.asResource().getURI() : node.asLiteral().getString());
                node = qs.get("atom");
                entry.setAtomUri(node.isResource() ? node.asResource().getURI() : node.asLiteral().getString());
                node = qs.get("value");
                entry.setValue(node.isResource() ? node.asResource().getURI() : node.asLiteral().getString());
                tensorEntries.add(entry);
            }
            return tensorEntries;
        }
    }
}
