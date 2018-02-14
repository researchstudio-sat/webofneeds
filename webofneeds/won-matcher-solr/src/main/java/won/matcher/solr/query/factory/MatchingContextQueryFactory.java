package won.matcher.solr.query.factory;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class MatchingContextQueryFactory extends SolrQueryFactory {

    public static final String MATCHING_CONTEXT_SOLR_FIELD = "_graph.http___purl.org_webofneeds_model_hasMatchingContext";

    Collection<String> matchingContexts;

    public MatchingContextQueryFactory(Collection<String> matchingContexts) {
        this.matchingContexts = matchingContexts;
    }

    @Override
    protected String makeQueryString() {

        if (matchingContexts == null) {
            return "";
        }

        // Matching Context query is a boolean OR query of exact field queries for each context
        List<ExactMatchFieldQueryFactory> contextFactories = new LinkedList<>();
        Iterator<String> contextIterator = matchingContexts.iterator();
        while (contextIterator.hasNext()) {
            String context = contextIterator.next();
            contextFactories.add(new ExactMatchFieldQueryFactory(MATCHING_CONTEXT_SOLR_FIELD, context));
        }

        BooleanQueryFactory booleanQueryFactory = new BooleanQueryFactory(BooleanQueryFactory.BooleanOperator.OR,
                contextFactories.toArray(new ExactMatchFieldQueryFactory[contextFactories.size()]));

        return booleanQueryFactory.makeQueryString();
    }
}
