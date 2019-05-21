package won.protocol.util;

import java.net.URI;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDF;
import won.protocol.model.Coordinate;
import won.protocol.model.AtomGraphType;
import won.protocol.vocabulary.SCHEMA;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONCON;
import won.protocol.vocabulary.WONMATCH;

/**
 * Extends {@link AtomModelWrapper} to add matchat specific methods to access
 * content fields like title, description, tags, etc.
 * <p>
 * Created by hfriedrich on 16.03.2017.
 */
public class DefaultAtomModelWrapper extends AtomModelWrapper {
    public DefaultAtomModelWrapper(final String atomUri) {
        super(atomUri);
    }

    public DefaultAtomModelWrapper(final Dataset atomDataset) {
        super(atomDataset);
    }

    public DefaultAtomModelWrapper(final Model atomModel, final Model sysInfoModel) {
        super(atomModel, sysInfoModel);
    }

    private void createSeeksNodeIfNonExist() {
        if (getSeeksNodes().size() == 0) {
            createSeeksNode(null);
        }
    }

    public void setTitle(String title) {
        Resource atomNode = getAtomNode(AtomGraphType.ATOM);
        atomNode.removeAll(DC.title);
        atomNode.addLiteral(DC.title, title);
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
            node.removeAll(WON.shapesGraph);
            node.addProperty(WON.shapesGraph, getAtomModel().getResource(shapesGraphReference.toString()));
        }
    }

    public String getSomeTitleFromIsOrAll(String... preferredLanguages) {
        String title = getAtomContentPropertyStringValue(DC.title, preferredLanguages);
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
        Resource atomNode = getAtomNode(AtomGraphType.ATOM);
        atomNode.removeAll(DC.description);
        atomNode.addLiteral(DC.description, description);
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
        Resource atomNode = getAtomNode(AtomGraphType.ATOM);
        atomNode.addLiteral(WONCON.tag, tag);
    }

    public void addSeeksTag(String tag) {
        createSeeksNodeIfNonExist();
        addSeeksPropertyStringValue(WONCON.tag, tag);
    }

    public Collection<String> getTags(Resource contentNode) {
        return getContentPropertyStringValues(contentNode, WONCON.tag, null);
    }

    public Collection<String> getAllTags() {
        return getAllContentPropertyStringValues(WONCON.tag, null);
    }

    public Collection<URI> getAllFlags() {
        Collection<RDFNode> rdfFlags = getContentPropertyObjects(WONMATCH.flag);
        Collection<URI> uriFlags = new LinkedList<>();
        for (RDFNode rdfFlag : rdfFlags) {
            if (rdfFlag.isURIResource()) {
                uriFlags.add(URI.create(rdfFlag.asResource().getURI()));
            }
        }
        return uriFlags;
    }

    public Collection<URI> getContentTypes() {
        Collection<RDFNode> rdfTypes = getContentPropertyObjects(RDF.type);
        Collection<URI> uriTypes = new LinkedList<>();
        for (RDFNode rdfType : rdfTypes) {
            if (rdfType.isURIResource()) {
                uriTypes.add(URI.create(rdfType.asResource().getURI()));
            }
        }
        return uriTypes;
    }

    public Collection<URI> getSeeksTypes() {
        Collection<RDFNode> rdfTypes = getSeeksPropertyObjects(RDF.type);
        Collection<URI> uriTypes = new LinkedList<>();
        for (RDFNode rdfType : rdfTypes) {
            if (rdfType.isURIResource()) {
                uriTypes.add(URI.create(rdfType.asResource().getURI()));
            }
        }
        return uriTypes;
    }

    public Collection<String> getAllTitles() {
        return getAllContentPropertyStringValues(DC.title, null);
    }

    public Coordinate getLocationCoordinate() {
        return getLocationCoordinate(getAtomContentNode());
    }

    public Coordinate getLocationCoordinate(Resource contentNode) {
        return getLocationCoordinate(contentNode, SCHEMA.LOCATION, WON.location);
    }

    public Coordinate getJobLocationCoordinate() {
        return getJobLocationCoordinate(getAtomContentNode());
    }

    public Coordinate getJobLocationCoordinate(Resource contentNode) {
        return getLocationCoordinate(contentNode, SCHEMA.JOBLOCATION);
    }

    private Coordinate getLocationCoordinate(Resource contentNode, Property locationProperty) {
        return getLocationCoordinate(contentNode, locationProperty, null);
    }

    /**
     * Tries to retrieve the coordinates of the location stored in the contentNode
     * within the given locationProperty
     * 
     * @param contentNode contentNode in which the locationProperty is searched for
     * @param locationProperty e.g SCHEMA.LOCATION or SCHEMA.JOBLOCATION
     * @param fallbackProperty nullable, will be checked if locationProperty itself
     * is not present in the contentNode
     * @return Coordinate if found otherwise null
     */
    private Coordinate getLocationCoordinate(Resource contentNode, Property locationProperty,
                    Property fallbackProperty) {
        Model atomModel = getAtomModel();
        Property geoProperty = atomModel.createProperty("http://schema.org/", "geo");
        Property longitudeProperty = atomModel.createProperty("http://schema.org/", "longitude");
        Property latitudeProperty = atomModel.createProperty("http://schema.org/", "latitude");
        RDFNode locationNode = RdfUtils.findOnePropertyFromResource(atomModel, contentNode, SCHEMA.LOCATION);
        RDFNode geoNode = (locationNode != null && locationNode.isResource())
                        ? RdfUtils.findOnePropertyFromResource(atomModel, locationNode.asResource(), geoProperty)
                        : null;
        RDFNode lat = (geoNode != null && geoNode.isResource())
                        ? RdfUtils.findOnePropertyFromResource(atomModel, geoNode.asResource(), latitudeProperty)
                        : null;
        RDFNode lon = (geoNode != null && geoNode.isResource())
                        ? RdfUtils.findOnePropertyFromResource(atomModel, geoNode.asResource(), longitudeProperty)
                        : null;
        if (lat == null || lon == null) {
            return null;
        }
        Float latitude = Float.valueOf(lat.asLiteral().getString());
        Float longitude = Float.valueOf(lon.asLiteral().getString());
        return new Coordinate(latitude, longitude);
    }
}
