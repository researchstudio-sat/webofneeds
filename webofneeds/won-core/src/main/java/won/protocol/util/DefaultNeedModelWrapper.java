package won.protocol.util;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;
import won.protocol.exception.IncorrectPropertyCountException;
import won.protocol.model.Coordinate;
import won.protocol.model.NeedContentPropertyType;
import won.protocol.vocabulary.WON;

import java.util.Collection;

/**
 * Extends {@link NeedModelWrapper} to add matchat specific methods to access content fields like title, description, tags, etc.
 * In many methods {@link NeedContentPropertyType} is used as a parameter to specify which content node you want to access.
 * <p>
 * Created by hfriedrich on 16.03.2017.
 */
public class DefaultNeedModelWrapper extends NeedModelWrapper {

    public DefaultNeedModelWrapper(final String needUri) {
        super(needUri);
    }

    public DefaultNeedModelWrapper(final Dataset needDataset) {
        super(needDataset);
    }

    public DefaultNeedModelWrapper(final Model needModel, final Model sysInfoModel) {
        super(needModel, sysInfoModel);
    }

    private void createContentNodeIfNonExist(NeedContentPropertyType type) {
        if (type == null || getContentNodes(type).size() == 0) {
            createContentNode(type, null);
        }
    }

    public void setTitle(NeedContentPropertyType type, String title) {
        createContentNodeIfNonExist(type);
        setContentPropertyStringValue(type, DC.title, title);
    }

    public String getTitleFromIsOrAll() {

        String title = null;
        try {
            title = getContentPropertyStringValue(NeedContentPropertyType.IS, DC.title);
        } catch (IncorrectPropertyCountException e1) {
            title = getContentPropertyStringValue(NeedContentPropertyType.ALL, DC.title);
        }

        return title;
    }

    public Collection<String> getTitles(Resource contentNode) { return getContentPropertyStringValues(contentNode, DC.title);}

    public Collection<String> getTitles(NeedContentPropertyType type) { return getContentPropertyStringValues(type, DC.title);}

    public void setDescription(NeedContentPropertyType type, String description) {
        createContentNodeIfNonExist(type);
        setContentPropertyStringValue(type, DC.description, description);
    }

    public Collection<String> getDescriptions(Resource contentNode) {
        return getContentPropertyStringValues(contentNode,DC.description);
    }

    public String getDescription(NeedContentPropertyType type) {
        return getContentPropertyStringValue(type, DC.description);
    }
    public Collection<String> getDescriptions(NeedContentPropertyType type) {
        return getContentPropertyStringValues(type, DC.description);
    }

    public void addTag(NeedContentPropertyType type, String tag) {
        createContentNodeIfNonExist(type);
        addContentPropertyStringValue(type, WON.HAS_TAG, tag);
    }

    public Collection<String> getTags(Resource contentNode) {
        return getContentPropertyStringValues(contentNode, WON.HAS_TAG);
    }

    public Collection<String> getTags(NeedContentPropertyType type) {
        return getContentPropertyStringValues(type, WON.HAS_TAG);
    }

    public Coordinate getLocationCoordinate(Resource contentNode) {

        Property geoProperty = needModel.createProperty("s:geo");
        Property longitudeProperty = needModel.createProperty("s:longitude");
        Property latitudeProperty = needModel.createProperty("s:latitude");

        RDFNode locationNode = RdfUtils.findOnePropertyFromResource(needModel, contentNode, WON.HAS_LOCATION);
        RDFNode geoNode = (locationNode != null && locationNode.isResource()) ? RdfUtils.findOnePropertyFromResource(needModel, locationNode.asResource(), geoProperty) : null;
        RDFNode lat = (geoNode != null && geoNode.isResource()) ? RdfUtils.findOnePropertyFromResource(needModel, geoNode.asResource(), latitudeProperty) : null;
        RDFNode lon = (geoNode != null && geoNode.isResource()) ? RdfUtils.findOnePropertyFromResource(needModel, geoNode.asResource(), longitudeProperty) : null;
        if (lat == null || lon == null) {
            return null;
        }

        Float latitude = Float.valueOf(lat.asLiteral().getString());
        Float longitude = Float.valueOf(lon.asLiteral().getString());
        return new Coordinate(latitude, longitude);
    }

}
