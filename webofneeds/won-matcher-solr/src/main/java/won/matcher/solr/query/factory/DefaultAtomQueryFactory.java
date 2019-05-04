package won.matcher.solr.query.factory;

import java.util.Collection;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Resource;

import won.matcher.solr.utils.MatcherAtomContentPropertyType;

/**
 * Created by hfriedrich on 03.08.2016.
 */
public class DefaultAtomQueryFactory extends BasicAtomQueryFactory {
    public DefaultAtomQueryFactory(final Dataset atom) {
        super(atom);
        // add "is" terms/location to "seeks" part of the query and vice versa
        // add "seeks" terms to the "seeks/seeks" part of the query and vice versa
        Resource atomContentNode = atomModelWrapper.getAtomContentNode();
        addTermsToQuery(atomContentNode, MatcherAtomContentPropertyType.SEEKS);
        addLocationFilters(atomContentNode, MatcherAtomContentPropertyType.SEEKS);
        for (Resource contentNode : atomModelWrapper.getSeeksNodes()) {
            addTermsToQuery(contentNode, MatcherAtomContentPropertyType.IS);
            addTermsToQuery(contentNode, MatcherAtomContentPropertyType.SEEKS_SEEKS);
            addLocationFilters(contentNode, MatcherAtomContentPropertyType.IS);
            addLocationFilters(contentNode, MatcherAtomContentPropertyType.SEEKS_SEEKS);
        }
        for (Resource contentNode : atomModelWrapper.getSeeksSeeksNodes()) {
            addTermsToQuery(contentNode, MatcherAtomContentPropertyType.SEEKS);
            addLocationFilters(contentNode, MatcherAtomContentPropertyType.SEEKS);
        }
    }

    private void addTermsToQuery(Resource contentNode, MatcherAtomContentPropertyType fieldType) {
        Collection<String> titles = atomModelWrapper.getTitles(contentNode);
        Collection<String> descriptions = atomModelWrapper.getDescriptions(contentNode);
        Collection<String> tags = atomModelWrapper.getTags(contentNode);
        String tagsTerms = "\"" + String.join("\" \"", tags) + "\"";
        titles.stream().forEach(title -> addTermsToTitleQuery(title, fieldType, 4));
        addTermsToTitleQuery(tagsTerms, fieldType, 2);
        addTermsToTagQuery(tagsTerms, fieldType, 4);
        titles.stream().forEach(title -> addTermsToTagQuery(title, fieldType, 2));
        titles.stream().forEach(title -> addTermsToDescriptionQuery(title, fieldType, 2));
        addTermsToDescriptionQuery(tagsTerms, fieldType, 2);
        descriptions.stream().forEach(descr -> addTermsToDescriptionQuery(descr, fieldType, 1));
    }
}
