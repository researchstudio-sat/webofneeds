package won.utils.im.port;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
//import org.apache.jena.vocabulary.XSD;

public class RealEstateNeedGenerator {

    static Model model = ModelFactory.createDefaultModel();

    static Property won_hasFacet = model.createProperty("http://purl.org/webofneeds/model#hasFacet");
    static Property won_is = model.createProperty("http://purl.org/webofneeds/model#is");
    static Property won_seeks = model.createProperty("http://purl.org/webofneeds/model#seeks");
    static Property won_hasTag = model.createProperty("http://purl.org/webofneeds/model#hasTag");
    static Property won_hasLocation = model.createProperty("http://purl.org/webofneeds/model#hasLocation");
    static Property won_hasBoundingBox = model.createProperty("http://purl.org/webofneeds/model#hasBoundingBox");
    static Property won_hasNorthWestCorner = model
            .createProperty("http://purl.org/webofneeds/model#hasNorthWestCorner");
    static Property won_hasSouthEastCorner = model
            .createProperty("http://purl.org/webofneeds/model#hasSouthEastCorner");

    static Property schema_amenityFeature = model.createProperty("http://schema.org/amenityFeature");
    static Property schema_floorSize = model.createProperty("http://schema.org/floorSize");
    static Property schema_numberOfRooms = model.createProperty("http://schema.org/numberOfRooms");
    static Property schema_priceSpecification = model.createProperty("http://schema.org/priceSpecification");
    static Property schema_geo = model.createProperty("http://schema.org/geo");
    static Property schema_latitude = model.createProperty("http://schema.org/latitude");
    static Property schema_longitude = model.createProperty("http://schema.org/longitude");
    static Property schema_name = model.createProperty("http://schema.org/name");
    static Property schema_description = model.createProperty("http://schema.org/description");
    static Property schema_price = model.createProperty("http://schema.org/price");
    static Property schema_priceCurrency = model.createProperty("http://schema.org/priceCurrency");
    static Property schema_unitCode = model.createProperty("http://schema.org/unitCode");
    static Property schema_value = model.createProperty("http://schema.org/value");

    public static void main(String[] args) {
        generateNeeds();
    }

    private static void generateNeeds() {
        for (int i = 0; i < 9; i++) {
            // TODO: unique URI
            String needURI = "https://localhost:8443/won/resource/event/m3tuwsuahplc#need";

            model = ModelFactory.createDefaultModel();

            model.setNsPrefix("conn", "https://localhost:8443/won/resource/connection/");
            model.setNsPrefix("need", "https://localhost:8443/won/resource/need/");
            model.setNsPrefix("local", "https://localhost:8443/won/resource/");
            model.setNsPrefix("event", "https://localhost:8443/won/resource/event/");
            model.setNsPrefix("msg", "http://purl.org/webofneeds/message#");
            model.setNsPrefix("won", "http://purl.org/webofneeds/model#");
            model.setNsPrefix("woncrypt", "http://purl.org/webofneeds/woncrypt#");
            model.setNsPrefix("cert", "http://www.w3.org/ns/auth/cert#");
            model.setNsPrefix("geo", "http://www.w3.org/2003/01/geo/wgs84_pos#");
            model.setNsPrefix("sig", "http://icp.it-risk.iwvi.uni-koblenz.de/ontologies/signature.owl#");
            model.setNsPrefix("s", "http://schema.org/");
            model.setNsPrefix("sh", "http://www.w3.org/ns/shacl#");
            model.setNsPrefix("ldp", "http://www.w3.org/ns/ldp#");
            model.setNsPrefix("sioc", "http://rdfs.org/sioc/ns#");

            Resource need = model.createResource(needURI);
            Resource isPart = model.createResource();
            Resource seeksPart = model.createResource();
            Resource won_Need = model.createResource("http://purl.org/webofneeds/model#Need");
            Resource won_OwnerFacet = model.createResource("http://purl.org/webofneeds/model#OwnerFacet");

            isPart = addTitle(isPart, i);
            isPart = addDescription(isPart);
            isPart = addLocation(isPart);
            isPart = addAmenities(isPart);
            isPart = addFloorSize(isPart);
            isPart = addNumberOfRooms(isPart);
            isPart = addPriceSpecification(isPart);

            seeksPart.addProperty(won_hasTag, "to-rent");

            need.addProperty(RDF.type, won_Need);
            need.addProperty(won_hasFacet, won_OwnerFacet);
            need.addProperty(won_is, isPart);
            need.addProperty(won_seeks, seeksPart);

            model.write(System.out, "TURTLE");
        }
    }

    // resource is isPart or seeksPart
    private static Resource addTitle(Resource resource, int counter) {
        resource.addProperty(DC.title, "Sample Real Estate Need " + counter);
        return resource;
    }

    private static Resource addDescription(Resource resource) {
        resource.addProperty(DC.description, "This is a sample offer that was automatically generated.");
        return resource;
    }

    private static Resource addLocation(Resource resource) {
        Resource locationResource = model.createResource();
        Resource boundingBoxResource = model.createResource();
        Resource nwCornerResource = model.createResource();
        Resource seCornerResource = model.createResource();
        Resource geoResource = model.createResource();
        Resource schema_Place = model.createResource("http://schema.org/Place");
        Resource schema_GeoCoordinates = model.createResource("http://schema.org/GeoCoordinates");

        resource.addProperty(won_hasLocation, locationResource);
        locationResource.addProperty(RDF.type, schema_Place);
        locationResource.addProperty(schema_name, "Some Location Name");
        locationResource.addProperty(schema_geo, geoResource);
        geoResource.addProperty(RDF.type, schema_GeoCoordinates);
        geoResource.addProperty(schema_latitude, "11.11");
        geoResource.addProperty(schema_longitude, "22.22");
        locationResource.addProperty(won_hasBoundingBox, boundingBoxResource);
        boundingBoxResource.addProperty(won_hasNorthWestCorner, nwCornerResource);
        nwCornerResource.addProperty(RDF.type, schema_GeoCoordinates);
        nwCornerResource.addProperty(schema_latitude, "33.11");
        nwCornerResource.addProperty(schema_longitude, "44.22");
        boundingBoxResource.addProperty(won_hasNorthWestCorner, seCornerResource);
        seCornerResource.addProperty(RDF.type, schema_GeoCoordinates);
        seCornerResource.addProperty(schema_latitude, "55.11");
        seCornerResource.addProperty(schema_longitude, "66.22");
        return resource;
    }

    private static Resource addAmenities(Resource resource) {
    	Resource amenityResource = model.createResource();
        Resource schema_LocationFeatureSpecification = model
                .createResource("http://schema.org/LocationFeatureSpecification");

        resource.addProperty(schema_amenityFeature, amenityResource);
        amenityResource.addProperty(RDF.type, schema_LocationFeatureSpecification);
        amenityResource.addProperty(schema_name, "some amenity");
        amenityResource.addProperty(schema_name, "some amenity2");
        amenityResource.addProperty(schema_name, "some amenity3");
        amenityResource.addProperty(schema_name, "some amenity4");
        return resource;
    }

    private static Resource addFloorSize(Resource resource) {
    	Resource floorSizeResource = model.createResource();
    	Resource schema_QuantitativeValue = model.createResource("http://schema.org/QuantitativeValue");

    	resource.addProperty(schema_floorSize, floorSizeResource);
        floorSizeResource.addProperty(RDF.type, schema_QuantitativeValue);
        floorSizeResource.addProperty(schema_unitCode, "MTK");
        floorSizeResource.addProperty(schema_value, "111");
        return resource;
    }

    private static Resource addNumberOfRooms(Resource resource) {
    	resource.addProperty(schema_numberOfRooms, "5");
        return resource;
    }

    private static Resource addPriceSpecification(Resource resource) {
    	Resource schema_QuantitativeValue = model.createResource("http://schema.org/QuantitativeValue");
        Resource schema_CompoundPriceSpecification = model
                .createResource("http://schema.org/CompoundPriceSpecification");
        Resource priceSpecificationResource = model.createResource();

        resource.addProperty(schema_priceSpecification, priceSpecificationResource);
        priceSpecificationResource.addProperty(RDF.type, schema_CompoundPriceSpecification);
        priceSpecificationResource.addProperty(schema_description, "total rent per month");
        priceSpecificationResource.addProperty(schema_price, "333");
        priceSpecificationResource.addProperty(schema_priceCurrency, "EUR");
        return resource;
    }

}
