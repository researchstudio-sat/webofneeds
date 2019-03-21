package won.matcher.solr.query.factory;

import java.util.Collection;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Resource;

import won.matcher.solr.utils.MatcherNeedContentPropertyType;

/**
 * Created by hfriedrich on 03.08.2016.
 */
public class DefaultNeedQueryFactory extends BasicNeedQueryFactory {

    public DefaultNeedQueryFactory(final Dataset need) {
        super(need);

        // add "is" terms/location to "seeks" part of the query and vice versa
        // add "seeks" terms to the "seeks/seeks" part of the query and vice versa

        Resource needContentNode = needModelWrapper.getNeedContentNode();
        addTermsToQuery(needContentNode, MatcherNeedContentPropertyType.SEEKS);
        addLocationFilters(needContentNode, MatcherNeedContentPropertyType.SEEKS);

        for (Resource contentNode : needModelWrapper.getSeeksNodes()) {
            addTermsToQuery(contentNode, MatcherNeedContentPropertyType.IS);
            addTermsToQuery(contentNode, MatcherNeedContentPropertyType.SEEKS_SEEKS);
            addLocationFilters(contentNode, MatcherNeedContentPropertyType.IS);
            addLocationFilters(contentNode, MatcherNeedContentPropertyType.SEEKS_SEEKS);
        }

        for (Resource contentNode : needModelWrapper.getSeeksSeeksNodes()) {
            addTermsToQuery(contentNode, MatcherNeedContentPropertyType.SEEKS);
            addLocationFilters(contentNode, MatcherNeedContentPropertyType.SEEKS);
        }
    }

    private void addTermsToQuery(Resource contentNode, MatcherNeedContentPropertyType fieldType) {

        Collection<String> titles = needModelWrapper.getTitles(contentNode);
        Collection<String> descriptions = needModelWrapper.getDescriptions(contentNode);
        Collection<String> tags = needModelWrapper.getTags(contentNode);
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
