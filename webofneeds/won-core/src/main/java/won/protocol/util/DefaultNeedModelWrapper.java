package won.protocol.util;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;
import won.protocol.model.Coordinate;
import won.protocol.model.NeedContentPropertyType;
import won.protocol.model.NeedGraphType;
import won.protocol.vocabulary.WON;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;

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

    public void setTitle(String title) {
        Resource needNode = getNeedNode(NeedGraphType.NEED);
        needNode.removeAll(DC.title);
        needNode.addLiteral(DC.title, title);
    }

    public void setTitle(NeedContentPropertyType type, String title) {
        createContentNodeIfNonExist(type);
        setContentPropertyStringValue(type, DC.title, title);
    }

    public void setShapesGraphReference(URI shapesGraphReference) {
        createContentNodeIfNonExist(NeedContentPropertyType.GOAL);

        Collection<Resource> nodes = getContentNodes(NeedContentPropertyType.GOAL);
        for (Resource node : nodes) {
            node.removeAll(WON.HAS_SHAPES_GRAPH);
            node.addProperty(WON.HAS_SHAPES_GRAPH, getNeedModel().getResource(shapesGraphReference.toString()));
        }
    }

    public Collection<String> getTitlesFromIsOrAll() {
        return getTitlesFromIsOrAll(null);
    }

    public Collection<String> getTitlesFromIsOrAll(String language) {

        Collection<String> titles = null;
        titles = getContentPropertyStringValues(NeedContentPropertyType.IS, DC.title, language);
        if (titles != null && titles.size() > 0) return titles;
        titles = getContentPropertyStringValues(NeedContentPropertyType.IS, DC.title, null);
        if (titles != null && titles.size() > 0) return titles;
        titles = getContentPropertyStringValues(NeedContentPropertyType.ALL, DC.title, language);
        if (titles != null && titles.size() > 0) return titles;
        titles = getContentPropertyStringValues(NeedContentPropertyType.ALL, DC.title, null);
        if (titles != null && titles.size() > 0) return titles;
        return Collections.emptyList();
    }

    public String getSomeTitleFromIsOrAll(String... preferredLanguages) {
        String title = null;
        title = getSomeContentPropertyStringValue(NeedContentPropertyType.IS, DC.title, preferredLanguages);
        if (title != null) return title;
        title = getSomeContentPropertyStringValue(NeedContentPropertyType.ALL, DC.title, preferredLanguages);
        if (title != null) return title;
        return null;
    }

    public String getSomeTitle(Resource contentNode, String... preferredLanguages) {
        String title = null;
        return getSomeContentPropertyStringValue(contentNode, DC.title, preferredLanguages);
    }

    public Collection<String> getTitles(Resource contentNode){ return getTitles(contentNode, null);}
    public Collection<String> getTitles(Resource contentNode, String language) { return getContentPropertyStringValues(contentNode, DC.title, language);}

    public Collection<String> getTitles(NeedContentPropertyType type) { return getTitles(type, null);}
    public Collection<String> getTitles(NeedContentPropertyType type, String language) { return getContentPropertyStringValues(type, DC.title, language);}

    public void setDescription(NeedContentPropertyType type, String description) {
        createContentNodeIfNonExist(type);
        setContentPropertyStringValue(type, DC.description, description);
    }

    public void setDescription(String description) {
        Resource needNode = getNeedNode(NeedGraphType.NEED);
        needNode.removeAll(DC.description);
        needNode.addLiteral(DC.description, description);
    }

    public String getSomeDescription(NeedContentPropertyType type, String... preferredLanguages) {
        return getSomeContentPropertyStringValue(type, DC.description, preferredLanguages);
    }

    public String getSomeDescription(Resource contentNode, String... preferredLanguages){
        return getSomeContentPropertyStringValue(contentNode, DC.description, preferredLanguages);
    }


    public Collection<String> getDescriptions(Resource contentNode) { return getDescriptions(contentNode, null); }
    public Collection<String> getDescriptions(Resource contentNode, String language) {
        return getContentPropertyStringValues(contentNode,DC.description, language);
    }
    public Collection<String> getDescriptions(NeedContentPropertyType type) { return getDescriptions(type, null);}
    public Collection<String> getDescriptions(NeedContentPropertyType type, String language) {
        return getContentPropertyStringValues(type, DC.description, language);
    }

    public void addTag(String tag) {
        Resource needNode = getNeedNode(NeedGraphType.NEED);
        needNode.addLiteral(WON.HAS_TAG, tag);
    }

    public void addTag(NeedContentPropertyType type, String tag) {
        createContentNodeIfNonExist(type);
        addContentPropertyStringValue(type, WON.HAS_TAG, tag);
    }

    public Collection<String> getTags(Resource contentNode) {
        return getContentPropertyStringValues(contentNode, WON.HAS_TAG, null);
    }

    public Collection<String> getTags(NeedContentPropertyType type) {
        return getContentPropertyStringValues(type, WON.HAS_TAG, null);
    }

    public Coordinate getLocationCoordinate(Resource contentNode) {

        Model needModel = getNeedModel();
        Property geoProperty = needModel.createProperty("http://schema.org/", "geo");
        Property longitudeProperty = needModel.createProperty("http://schema.org/", "longitude");
        Property latitudeProperty = needModel.createProperty("http://schema.org/", "latitude");

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
