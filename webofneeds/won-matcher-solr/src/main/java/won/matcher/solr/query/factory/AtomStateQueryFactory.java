package won.matcher.solr.query.factory;

import org.apache.jena.query.Dataset;

import won.protocol.vocabulary.WON;

/**
 * Created by hfriedrich on 01.08.2016.
 */
public class AtomStateQueryFactory extends AtomDatasetQueryFactory {
    private static final String ATOM_STATE_SOLR_FIELD = "_graph.http___purl.org_webofneeds_model_atomState._id";

    public AtomStateQueryFactory(final Dataset atom) {
        super(atom);
    }

    @Override
    protected String makeQueryString() {
        return new ExactMatchFieldQueryFactory(ATOM_STATE_SOLR_FIELD, WON.ATOM_STATE_ACTIVE.toString()).createQuery();
    }
}
