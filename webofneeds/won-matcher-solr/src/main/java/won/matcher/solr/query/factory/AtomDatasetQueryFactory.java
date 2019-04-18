package won.matcher.solr.query.factory;

import org.apache.jena.query.Dataset;

/**
 * Created by hfriedrich on 01.08.2016.
 */
public abstract class AtomDatasetQueryFactory extends SolrQueryFactory {
    protected Dataset atomDataset;

    public AtomDatasetQueryFactory(Dataset atom) {
        this.atomDataset = atom;
    }
}
