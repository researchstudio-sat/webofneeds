package won.matcher.solr.query.factory;

import org.apache.jena.query.Dataset;

/**
 * Created by hfriedrich on 29.08.2017.
 */
public class WhatsNewQueryFactory extends BasicAtomQueryFactory {
    private static final String ATOM_TYPE_DUMMY_FIELD = "_graph._type";
    private static final String ATOM_TYPE_DUMMY_FIELD_CONTENT = "http\\://purl.org/webofneeds/model#Atom";

    public WhatsNewQueryFactory(Dataset atom) {
        super(atom);
        // TODO: Implement a time based matcher/filter/queryfactory
        // add "is" terms/location to "seeks" part of the query and vice versa
        // add "seeks" terms to the "seeks/seeks" part of the query and vice versa
        /*
         * for (Resource contentNode :
         * atomModelWrapper.getSeeksSeeksNodes(AtomContentPropertyType.IS)) {
         * addLocationFilters(contentNode, AtomContentPropertyType.SEEKS); } for
         * (Resource contentNode :
         * atomModelWrapper.getSeeksSeeksNodes(AtomContentPropertyType.SEEKS)) {
         * addLocationFilters(contentNode, AtomContentPropertyType.IS);
         * addLocationFilters(contentNode, AtomContentPropertyType.SEEKS_SEEKS); } for
         * (Resource contentNode :
         * atomModelWrapper.getSeeksSeeksNodes(AtomContentPropertyType.SEEKS_SEEKS)) {
         * addLocationFilters(contentNode, AtomContentPropertyType.SEEKS); }
         */
        // create a dummy query, this is the minimal part of a query that has no search
        // term content
        // so that we at least create some valid query, in this case we just search for
        // "all other atoms"
        MatchFieldQueryFactory dummyQuery = new MatchFieldQueryFactory(ATOM_TYPE_DUMMY_FIELD,
                        ATOM_TYPE_DUMMY_FIELD_CONTENT);
        contentFactories.add(dummyQuery);
    }
}
