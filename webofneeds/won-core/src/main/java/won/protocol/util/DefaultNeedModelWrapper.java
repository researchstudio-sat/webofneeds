package won.protocol.util;

import java.net.URI;
import java.util.Collection;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;

import won.protocol.model.Coordinate;
import won.protocol.model.NeedGraphType;
import won.protocol.vocabulary.SCHEMA;
import won.protocol.vocabulary.WON;

/**
 * Extends {@link NeedModelWrapper} to add matchat specific methods to access
 * content fields like title, description, tags, etc.
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

    private void createSeeksNodeIfNonExist() {
        if (getSeeksNodes().size() == 0) {
            createSeeksNode(null);
        }
    }

    public void setTitle(String title) {
        Resource needNode = getNeedNode(NeedGraphType.NEED);
        needNode.removeAll(DC.title);
        needNode.addLiteral(DC.title, title);
    }

    public void setSeeksTitle(String title) {
        createSeeksNodeIfNonExist();
        setSeeksPropertyStringValue(DC.title, title);
    }

    public void setShapesGraphReference(URI shapesGraphReference) {
        if (getGoalNodes().size() == 0) {
            createGoalNode(null);
        }
        Collection<Resource> nodes = getGoalNodes();
        for (Resource node : nodes) {
            node.removeAll(WON.HAS_SHAPES_GRAPH);
            node.addProperty(WON.HAS_SHAPES_GRAPH, getNeedModel().getResource(shapesGraphReference.toString()));
        }
    }

    public String getSomeTitleFromIsOrAll(String... preferredLanguages) {
        String title = getNeedContentPropertyStringValue(DC.title, preferredLanguages);
        if (title != null)
            return title;
        title = getSomeContentPropertyStringValue(DC.title, preferredLanguages);
        if (title != null)
            return title;
        return null;
    }

    public Collection<String> getTitles(Resource contentNode) {
        return getTitles(contentNode, null);
    }

    Collection<String> getTitles(Resource contentNode, String language) {
        return getContentPropertyStringValues(contentNode, DC.title, language);
    }

    public void setSeeksDescription(String description) {
        createSeeksNodeIfNonExist();
        setSeeksPropertyStringValue(DC.description, description);
    }

    public void setDescription(String description) {
        Resource needNode = getNeedNode(NeedGraphType.NEED);
        needNode.removeAll(DC.description);
        needNode.addLiteral(DC.description, description);
    }

    public String getSomeDescription(String... preferredLanguages) {
        return getSomeContentPropertyStringValue(DC.description, preferredLanguages);
    }

    public Collection<String> getDescriptions(Resource contentNode) {
        return getDescriptions(contentNode, null);
    }

    Collection<String> getDescriptions(Resource contentNode, String language) {
        return getContentPropertyStringValues(contentNode, DC.description, language);
    }

    public void addTag(String tag) {
        Resource needNode = getNeedNode(NeedGraphType.NEED);
        needNode.addLiteral(WON.HAS_TAG, tag);
    }

    public void addSeeksTag(String tag) {
        createSeeksNodeIfNonExist();
        addSeeksPropertyStringValue(WON.HAS_TAG, tag);
    }

    public Collection<String> getTags(Resource contentNode) {
        return getContentPropertyStringValues(contentNode, WON.HAS_TAG, null);
    }

    public Collection<String> getAllTags() {
        return getAllContentPropertyStringValues(WON.HAS_TAG, null);
    }

    public Collection<String> getAllTitles() {
        return getAllContentPropertyStringValues(DC.title, null);
    }

    public Coordinate getLocationCoordinate(Resource contentNode) {
        Model needModel = getNeedModel();
        Property geoProperty = needModel.createProperty("http://schema.org/", "geo");
        Property longitudeProperty = needModel.createProperty("http://schema.org/", "longitude");
        Property latitudeProperty = needModel.createProperty("http://schema.org/", "latitude");
        RDFNode locationNode = RdfUtils.findOnePropertyFromResource(needModel, contentNode, SCHEMA.LOCATION);
        if (locationNode == null) {
            locationNode = RdfUtils.findOnePropertyFromResource(needModel, contentNode, WON.HAS_LOCATION);
        }
        RDFNode geoNode = (locationNode != null && locationNode.isResource())
                        ? RdfUtils.findOnePropertyFromResource(needModel, locationNode.asResource(), geoProperty)
                        : null;
        RDFNode lat = (geoNode != null && geoNode.isResource())
                        ? RdfUtils.findOnePropertyFromResource(needModel, geoNode.asResource(), latitudeProperty)
                        : null;
        RDFNode lon = (geoNode != null && geoNode.isResource())
                        ? RdfUtils.findOnePropertyFromResource(needModel, geoNode.asResource(), longitudeProperty)
                        : null;
        if (lat == null || lon == null) {
            return null;
        }
        Float latitude = Float.valueOf(lat.asLiteral().getString());
        Float longitude = Float.valueOf(lon.asLiteral().getString());
        return new Coordinate(latitude, longitude);
    }
}
