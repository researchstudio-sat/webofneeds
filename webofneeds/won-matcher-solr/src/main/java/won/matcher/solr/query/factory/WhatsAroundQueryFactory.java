package won.matcher.solr.query.factory;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Resource;
import won.protocol.model.NeedContentPropertyType;

/**
 * Created by hfriedrich on 29.08.2017.
 */
public class WhatsAroundQueryFactory extends BasicNeedQueryFactory {

    public WhatsAroundQueryFactory(Dataset need) {
        super(need);

        // add "is" terms/location to "seeks" part of the query and vice versa
        // add "seeks" terms to the "seeks/seeks" part of the query and vice versa

        for (Resource contentNode : needModelWrapper.getContentNodes(NeedContentPropertyType.IS)) {
            addLocationFilters(contentNode, NeedContentPropertyType.SEEKS);
        }

        for (Resource contentNode : needModelWrapper.getContentNodes(NeedContentPropertyType.SEEKS)) {
            addLocationFilters(contentNode, NeedContentPropertyType.IS);
            addLocationFilters(contentNode, NeedContentPropertyType.SEEKS_SEEKS);
        }

        for (Resource contentNode : needModelWrapper.getContentNodes(NeedContentPropertyType.SEEKS_SEEKS)) {
            addLocationFilters(contentNode, NeedContentPropertyType.SEEKS);
        }
    }
}
