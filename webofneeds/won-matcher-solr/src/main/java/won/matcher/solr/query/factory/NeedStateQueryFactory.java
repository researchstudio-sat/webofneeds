package won.matcher.solr.query.factory;

import org.apache.jena.query.Dataset;

import won.protocol.vocabulary.WON;

/**
 * Created by hfriedrich on 01.08.2016.
 */
public class NeedStateQueryFactory extends NeedDatasetQueryFactory {
    private static final String NEED_STATE_SOLR_FIELD = "_graph.http___purl.org_webofneeds_model_isInState._id";

    public NeedStateQueryFactory(final Dataset need) {
        super(need);
    }

    @Override
    protected String makeQueryString() {
        return new ExactMatchFieldQueryFactory(NEED_STATE_SOLR_FIELD, WON.NEED_STATE_ACTIVE.toString()).createQuery();
    }
}
