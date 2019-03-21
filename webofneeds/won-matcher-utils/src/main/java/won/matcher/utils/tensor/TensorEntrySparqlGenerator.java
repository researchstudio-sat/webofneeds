package won.matcher.utils.tensor;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.RDFNode;
import won.protocol.exception.DataIntegrityException;

import java.util.*;

/**
 * Executes a sparql query on a sparql endpoint and returns the data as
 * {@link TensorEntry} objects. Expects that the sparql query returns the
 * variables ?slice, ?need and ?value.
 * <p>
 * Created by hfriedrich on 21.04.2017.
 */
public class TensorEntrySparqlGenerator implements TensorEntryGenerator {

  private String sparqlEndpoint;
  private String query;
  private Map<String, Object> parameterBindings;

  private static String[] variableNames = { "slice", "need", "value" };

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
        node = qs.get("need");
        entry.setNeedUri(node.isResource() ? node.asResource().getURI() : node.asLiteral().getString());
        node = qs.get("value");
        entry.setValue(node.isResource() ? node.asResource().getURI() : node.asLiteral().getString());
        tensorEntries.add(entry);
      }
      return tensorEntries;
    }
  }
}
