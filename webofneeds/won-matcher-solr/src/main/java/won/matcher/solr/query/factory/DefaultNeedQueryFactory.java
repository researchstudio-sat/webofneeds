package won.matcher.solr.query.factory;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Resource;
import won.protocol.model.NeedContentPropertyType;

import java.util.Collection;

/**
 * Created by hfriedrich on 03.08.2016.
 */
public class DefaultNeedQueryFactory extends BasicNeedQueryFactory {

    public DefaultNeedQueryFactory(final Dataset need) {
        super(need);

        // add "is" terms/location to "seeks" part of the query and vice versa
        // add "seeks" terms to the "seeks/seeks" part of the query and vice versa
        
        for (Resource contentNode : needModelWrapper.getContentNodes(NeedContentPropertyType.IS)) {
            addTermsToQuery(contentNode, NeedContentPropertyType.SEEKS);
            addLocationFilters(contentNode, NeedContentPropertyType.SEEKS);
        }

        for (Resource contentNode : needModelWrapper.getContentNodes(NeedContentPropertyType.SEEKS)) {
            addTermsToQuery(contentNode, NeedContentPropertyType.IS);
            addTermsToQuery(contentNode, NeedContentPropertyType.SEEKS_SEEKS);
            addLocationFilters(contentNode, NeedContentPropertyType.IS);
            addLocationFilters(contentNode, NeedContentPropertyType.SEEKS_SEEKS);
        }

        for (Resource contentNode : needModelWrapper.getContentNodes(NeedContentPropertyType.SEEKS_SEEKS)) {
            addTermsToQuery(contentNode, NeedContentPropertyType.SEEKS);
            addLocationFilters(contentNode, NeedContentPropertyType.SEEKS);
        }
    }

    private void addTermsToQuery(Resource contentNode, NeedContentPropertyType fieldType) {

        String title = needModelWrapper.getTitle(contentNode);
        String description = needModelWrapper.getDescription(contentNode);
        Collection<String> tags = needModelWrapper.getTags(contentNode);
        String tagsTerms = "\"" + String.join("\" \"", tags) + "\"";

        addTermsToTitleQuery(title, fieldType, 4);
        addTermsToTitleQuery(tagsTerms, fieldType, 2);
        addTermsToTagQuery(tagsTerms, fieldType, 4);
        addTermsToTagQuery(title, fieldType, 2);
        addTermsToDescriptionQuery(title, fieldType, 2);
        addTermsToDescriptionQuery(tagsTerms, fieldType, 2);
        addTermsToDescriptionQuery(description, fieldType, 1);
    }
}
