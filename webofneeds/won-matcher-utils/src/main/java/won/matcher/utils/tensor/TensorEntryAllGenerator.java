package won.matcher.utils.tensor;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.stream.Collectors;

/**
 * Loads all SPARQL "*.rq" files from a specified directory and executes them
 * (using time range parameters) on a SPARQL endpoint. The result
 * {@link TensorEntry} objects from all queries are returned.
 * <p>
 * Created by hfriedrich on 21.04.2017.
 */
public class TensorEntryAllGenerator implements TensorEntryGenerator {

  private String sparqlEndpoint;
  private String queryDirectory;
  private long from;
  private long to;

  public TensorEntryAllGenerator(String queryDirectory, String sparqlEndpoint, long fromDate, long toDate) {

    this.queryDirectory = queryDirectory;
    this.sparqlEndpoint = sparqlEndpoint;
    from = fromDate;
    to = toDate;
  }

  @Override
  public Collection<TensorEntry> generateTensorEntries() throws IOException {

    Collection<TensorEntry> tensorEntries = new LinkedList<>();
    Collection<TensorEntrySparqlGenerator> queryGenerators = new LinkedList<>();

    // read all sparql queries from target directory and configure them with
    // variable bindings
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    Resource[] resources = resolver.getResources("classpath:" + queryDirectory + "/*.rq");

    for (Resource resource : resources) {
      String query = readFromInputStream(resource.getInputStream());
      TensorEntrySparqlGenerator queryGen = new TensorEntrySparqlGenerator(sparqlEndpoint, query);
      queryGen.addVariableBinding("from", Long.valueOf(from));
      queryGen.addVariableBinding("to", Long.valueOf(to));
      queryGenerators.add(queryGen);
    }

    // execute all sparql query generators
    for (TensorEntrySparqlGenerator queryGen : queryGenerators) {
      tensorEntries.addAll(queryGen.generateTensorEntries());
    }

    return tensorEntries;
  }

  public static String readFromInputStream(InputStream input) throws IOException {
    try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input, "UTF-8"))) {
      return buffer.lines().collect(Collectors.joining("\n"));
    }
  }
}
