package won.matcher.solr.query.factory;

import org.apache.jena.query.Dataset;

/**
 * Created by hfriedrich on 03.08.2016.
 */
public class TestAtomQueryFactory extends DefaultAtomQueryFactory {
    public TestAtomQueryFactory(Dataset atom) {
        super(atom);
    }
}
