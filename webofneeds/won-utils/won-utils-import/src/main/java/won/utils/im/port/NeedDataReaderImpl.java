package won.utils.im.port;

import org.apache.jena.rdf.model.Model;

import java.io.IOException;

/**
 * User: ypanchenko
 * Date: 04.09.2014
 */
public class NeedDataReaderImpl implements NeedDataReader<Model> {
  @Override public boolean hasNext() {
    // TODO implement
    return false;
  }

  @Override public Model next() {
    // TODO implement using NeedModelBuilder,
    // see, for example, a DummyNeedsReader in /test
    return null;
  }

  @Override public void close() throws IOException {
    // TODO implement
  }
}
