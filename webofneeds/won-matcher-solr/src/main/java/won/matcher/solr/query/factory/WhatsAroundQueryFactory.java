package won.matcher.solr.query.factory;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Resource;

import won.matcher.solr.utils.MatcherAtomContentPropertyType;

/**
 * Created by hfriedrich on 29.08.2017.
 */
public class WhatsAroundQueryFactory extends BasicAtomQueryFactory {
    private static final String ATOM_TYPE_DUMMY_FIELD = "_graph._type";
    private static final String ATOM_TYPE_DUMMY_FIELD_CONTENT = "http\\://purl.org/webofneeds/model#Atom";

    public WhatsAroundQueryFactory(Dataset atom) {
        super(atom);
        // add "is" terms/location to "seeks" part of the query and vice versa
        // add "seeks" terms to the "seeks/seeks" part of the query and vice versa
        Resource atomContentNode = atomModelWrapper.getAtomContentNode();
        addLocationFilters(atomContentNode, MatcherAtomContentPropertyType.SEEKS);
        for (Resource contentNode : atomModelWrapper.getSeeksNodes()) {
            addLocationFilters(contentNode, MatcherAtomContentPropertyType.IS);
            addLocationFilters(contentNode, MatcherAtomContentPropertyType.SEEKS_SEEKS);
        }
        for (Resource contentNode : atomModelWrapper.getSeeksSeeksNodes()) {
            addLocationFilters(contentNode, MatcherAtomContentPropertyType.SEEKS);
        }
        // create a dummy query, this is the minimal part of a query that has no search
        // term content
        // so that we at least create some valid query, in this case we just search for
        // "all other atoms"
        MatchFieldQueryFactory dummyQuery = new MatchFieldQueryFactory(ATOM_TYPE_DUMMY_FIELD,
                        ATOM_TYPE_DUMMY_FIELD_CONTENT);
        contentFactories.add(dummyQuery);
    }
}
