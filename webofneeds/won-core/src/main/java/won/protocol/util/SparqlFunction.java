package won.protocol.util;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.function.Function;

public abstract class SparqlFunction<T, R> implements Function<T, R> {

  protected final String sparql;
  protected final String sparqlFile;

  public SparqlFunction(String sparqlFile) {
    super();
    // load the query from the sparql file
    InputStream is = getClass().getResourceAsStream(sparqlFile);
    StringWriter writer = new StringWriter();
    try {
      IOUtils.copy(is, writer, Charsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Could not read sparql from file", e);
    }
    this.sparql = writer.toString();
    this.sparqlFile = sparqlFile;
  }

  public String getSparql() {
    return sparql;
  }

}
