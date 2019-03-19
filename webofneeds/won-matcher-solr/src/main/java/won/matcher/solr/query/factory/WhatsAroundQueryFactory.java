package won.matcher.solr.query.factory;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Resource;

import won.matcher.solr.utils.MatcherNeedContentPropertyType;

/**
 * Created by hfriedrich on 29.08.2017.
 */
public class WhatsAroundQueryFactory extends BasicNeedQueryFactory {

    private static final String NEED_TYPE_DUMMY_FIELD = "_graph._type";
    private static final String NEED_TYPE_DUMMY_FIELD_CONTENT = "http\\://purl.org/webofneeds/model#Need";

    public WhatsAroundQueryFactory(Dataset need) {
        super(need);

        // add "is" terms/location to "seeks" part of the query and vice versa
        // add "seeks" terms to the "seeks/seeks" part of the query and vice versa

        Resource needContentNode = needModelWrapper.getNeedContentNode();
        addLocationFilters(needContentNode, MatcherNeedContentPropertyType.SEEKS);

        for (Resource contentNode : needModelWrapper.getSeeksNodes()) {
            addLocationFilters(contentNode, MatcherNeedContentPropertyType.IS);
            addLocationFilters(contentNode, MatcherNeedContentPropertyType.SEEKS_SEEKS);
        }

        for (Resource contentNode : needModelWrapper.getSeeksSeeksNodes()) {
            addLocationFilters(contentNode, MatcherNeedContentPropertyType.SEEKS);
        }

        // create a dummy query, this is the minimal part of a query that has no search term content
        // so that we at least create some valid query, in this case we just search for "all other needs"
        MatchFieldQueryFactory dummyQuery = new MatchFieldQueryFactory(NEED_TYPE_DUMMY_FIELD,
                NEED_TYPE_DUMMY_FIELD_CONTENT);
        contentFactories.add(dummyQuery);
    }
}
