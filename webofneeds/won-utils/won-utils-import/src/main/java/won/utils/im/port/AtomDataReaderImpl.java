package won.utils.im.port;

import java.io.IOException;

import org.apache.jena.rdf.model.Model;

/**
 * User: ypanchenko Date: 04.09.2014
 */
public class AtomDataReaderImpl implements AtomDataReader<Model> {
    @Override
    public boolean hasNext() {
        // TODO implement
        return false;
    }

    @Override
    public Model next() {
        // TODO implement using AtomModelBuilder,
        // see, for example, a DummyAtomsReader in /test
        return null;
    }

    @Override
    public void close() throws IOException {
        // TODO implement
    }
}
