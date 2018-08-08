package won.utils.im.port;

import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDF;

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

    static HashMap<String, String>[] locations = new HashMap[10];
    static String[] amenities = { "Balcony", "Parkingspace", "Garden", "Bathtub", "furnished",
            "Parquetflooring", "Elevator", "Cellar", "Pool", "Sauna", "accessible" };

    public static void main(String[] args) {
    	initializeLocations();
        generateNeeds();
    }

	private static void generateNeeds() {
        for (int i = 0; i < 9; i++) {
            String needURI = "https://localhost:8443/won/resource/event/" + "real_estate_sample_" + i + "#need";

            model = ModelFactory.createDefaultModel();

            setPrefixes();

            Resource need = model.createResource(needURI);
            Resource isPart = model.createResource();
            Resource seeksPart = model.createResource();
            Resource won_Need = model.createResource("http://purl.org/webofneeds/model#Need");
            Resource won_OwnerFacet = model.createResource("http://purl.org/webofneeds/model#OwnerFacet");

            // method signatures: branch, probability that detail is added, min, max
            isPart = addTitle(isPart, 1.0, i);
            isPart = addDescription(isPart, 1.0);
            isPart = addLocation(isPart, 1.0);
            isPart = addAmenities(isPart, 0.8, 1, 4);
            isPart = addFloorSize(isPart, 0.8, 28, 250);
            isPart = addNumberOfRooms(isPart, 0.8, 1, 9);
            isPart = addPriceSpecification(isPart, 1.0, 250, 2200);

            seeksPart.addProperty(won_hasTag, "to-rent");

            need.addProperty(RDF.type, won_Need);
            need.addProperty(won_hasFacet, won_OwnerFacet);
            need.addProperty(won_is, isPart);
            need.addProperty(won_seeks, seeksPart);

            try {
                FileOutputStream out = new FileOutputStream("sample_needs/real_estate_need_" + i + ".trig");
                model.write(out, "TURTLE");
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
	
	private static Resource addTitle(Resource resource, double probability, int counter) {
        if (Math.random() < (1.0 - probability)) {
            return resource;
        }

        resource.addProperty(DC.title, "Sample Real Estate Need " + counter);
        return resource;
    }

    private static Resource addDescription(Resource resource, double probability) {
        if (Math.random() < (1.0 - probability)) {
            return resource;
        }

        resource.addProperty(DC.description, "This is a sample offer that was automatically generated.");
        return resource;
    }

    private static Resource addLocation(Resource resource, double probability) {
        if (Math.random() < (1.0 - probability)) {
            return resource;
        }

        // pick a location
        int locNr = (int) (Math.random() * 10);
        String nwlat = locations[locNr].get("nwlat");
        String nwlng = locations[locNr].get("nwlng");
        String selat = locations[locNr].get("selat");
        String selng = locations[locNr].get("selng");
        String lat = locations[locNr].get("lat");
        String lng = locations[locNr].get("lng");
        String name = locations[locNr].get("name");

        Resource locationResource = model.createResource();
        Resource boundingBoxResource = model.createResource();
        Resource nwCornerResource = model.createResource();
        Resource seCornerResource = model.createResource();
        Resource geoResource = model.createResource();
        Resource schema_Place = model.createResource("http://schema.org/Place");
        Resource schema_GeoCoordinates = model.createResource("http://schema.org/GeoCoordinates");

        resource.addProperty(won_hasLocation, locationResource);
        locationResource.addProperty(RDF.type, schema_Place);
        locationResource.addProperty(schema_name, name);
        locationResource.addProperty(schema_geo, geoResource);
        geoResource.addProperty(RDF.type, schema_GeoCoordinates);
        geoResource.addProperty(schema_latitude, lat);
        geoResource.addProperty(schema_longitude, lng);
        locationResource.addProperty(won_hasBoundingBox, boundingBoxResource);
        boundingBoxResource.addProperty(won_hasNorthWestCorner, nwCornerResource);
        nwCornerResource.addProperty(RDF.type, schema_GeoCoordinates);
        nwCornerResource.addProperty(schema_latitude, nwlat);
        nwCornerResource.addProperty(schema_longitude, nwlng);
        boundingBoxResource.addProperty(won_hasNorthWestCorner, seCornerResource);
        seCornerResource.addProperty(RDF.type, schema_GeoCoordinates);
        seCornerResource.addProperty(schema_latitude, selat);
        seCornerResource.addProperty(schema_longitude, selng);
        return resource;
    }

    private static Resource addAmenities(Resource resource, double probability, int min, int max) {
        if (Math.random() < (1.0 - probability)) {
            return resource;
        }

        int numberOfAmenities = (int) (Math.random() * Math.abs(max - min + 1) + min);
        Collections.shuffle(Arrays.asList(amenities));

        Resource amenityResource = model.createResource();
        Resource schema_LocationFeatureSpecification = model
                .createResource("http://schema.org/LocationFeatureSpecification");

        resource.addProperty(schema_amenityFeature, amenityResource);
        amenityResource.addProperty(RDF.type, schema_LocationFeatureSpecification);
        for (int j = 0; j < numberOfAmenities; j++) {
            amenityResource.addProperty(schema_name, amenities[j]);
        }
        return resource;
    }

    private static Resource addFloorSize(Resource resource, double probability, int min, int max) {
        if (Math.random() < (1.0 - probability)) {
            return resource;
        }

        int floorSize = (int) (Math.random() * Math.abs(max - min + 1)) + min;

        Resource floorSizeResource = model.createResource();
        Resource schema_QuantitativeValue = model.createResource("http://schema.org/QuantitativeValue");

        resource.addProperty(schema_floorSize, floorSizeResource);
        floorSizeResource.addProperty(RDF.type, schema_QuantitativeValue);
        floorSizeResource.addProperty(schema_unitCode, "MTK");
        floorSizeResource.addProperty(schema_value, String.valueOf(floorSize));
        return resource;
    }

    private static Resource addNumberOfRooms(Resource resource, double probability, int min, int max) {
        if (Math.random() < (1.0 - probability)) {
            return resource;
        }

        int numberOfRooms = (int) (Math.random() * Math.abs(max - min + 1)) + min;

        resource.addProperty(schema_numberOfRooms, String.valueOf(numberOfRooms));
        return resource;
    }

    private static Resource addPriceSpecification(Resource resource, double probability, double min, double max) {
        if (Math.random() < (1.0 - probability)) {
            return resource;
        }

        int price = (int) (Math.random() * Math.abs(max - min + 1) + min);

        Resource schema_CompoundPriceSpecification = model
                .createResource("http://schema.org/CompoundPriceSpecification");
        Resource priceSpecificationResource = model.createResource();

        resource.addProperty(schema_priceSpecification, priceSpecificationResource);
        priceSpecificationResource.addProperty(RDF.type, schema_CompoundPriceSpecification);
        priceSpecificationResource.addProperty(schema_description, "total rent per month");
        priceSpecificationResource.addProperty(schema_price, String.valueOf(price));
        priceSpecificationResource.addProperty(schema_priceCurrency, "EUR");
        return resource;
    }

    private static void initializeLocations() {
		HashMap<String, String> loc0 = new HashMap<String, String>();
        loc0.put("nwlat", "48.385349");
        loc0.put("nwlng", "16.821063");
        loc0.put("selat", "48.309745");
        loc0.put("selng", "16.729174");
        loc0.put("lat", "48.288651");
        loc0.put("lng", "16.705195");
        loc0.put("name", "Gemeinde Weikendorf, Bezirk Gänserndorf, Lower Austria, 2253, Austria");
        locations[0] = loc0;

        HashMap<String, String> loc1 = new HashMap<String, String>();
        loc1.put("nwlat", "48.213814");
        loc1.put("nwlng", "16.340870");
        loc1.put("selat", "48.236309");
        loc1.put("selng", "16.370149");
        loc1.put("lat", "48.225073");
        loc1.put("lng", "16.358398");
        loc1.put("name", "Alsergrund, Vienna, 1090, Austria");
        locations[1] = loc1;

        HashMap<String, String> loc2 = new HashMap<String, String>();
        loc2.put("nwlat", "48.145908");
        loc2.put("nwlng", "14.126198");
        loc2.put("selat", "48.465908");
        loc2.put("selng", "14.446198");
        loc2.put("lat", "48.305908");
        loc2.put("lng", "14.286198");
        loc2.put("name", "Linz, Upper Austria, 4010, Austria");
        locations[2] = loc2;

        HashMap<String, String> loc3 = new HashMap<String, String>();
        loc3.put("nwlat", "46.910256");
        loc3.put("nwlng", "15.278572");
        loc3.put("selat", "47.230256");
        loc3.put("selng", "15.598572");
        loc3.put("lat", "47.070256");
        loc3.put("lng", "15.438572");
        loc3.put("name", "Graz, Styria, 8011, Austria");
        locations[3] = loc3;

        HashMap<String, String> loc4 = new HashMap<String, String>();
        loc4.put("nwlat", "47.638135");
        loc4.put("nwlng", "12.886481");
        loc4.put("selat", "47.958135");
        loc4.put("selng", "13.206481");
        loc4.put("lat", "47.798135");
        loc4.put("lng", "13.046481");
        loc4.put("name", "Salzburg, 5020, Austria");
        locations[4] = loc4;

        HashMap<String, String> loc5 = new HashMap<String, String>();
        loc5.put("nwlat", "48.164398");
        loc5.put("nwlng", "15.582912");
        loc5.put("selat", "48.244399");
        loc5.put("selng", "15.662912");
        loc5.put("lat", "48.204399");
        loc5.put("lng", "15.622912");
        loc5.put("name", "St. Pölten, Lower Austria, 3102, Austria");
        locations[5] = loc5;

        HashMap<String, String> loc6 = new HashMap<String, String>();
        loc6.put("nwlat", "47.480016");
        loc6.put("nwlng", "9.654882");
        loc6.put("selat", "47.534581");
        loc6.put("selng", "9.807672");
        loc6.put("lat", "47.502578");
        loc6.put("lng", "9.747292");
        loc6.put("name", "Bregenz, Vorarlberg, Austria");
        locations[6] = loc6;

        HashMap<String, String> loc7 = new HashMap<String, String>();
        loc7.put("nwlat", "46.782816");
        loc7.put("nwlng", "14.467960");
        loc7.put("selat", "46.462816");
        loc7.put("selng", "14.147960");
        loc7.put("lat", "46.622816");
        loc7.put("lng", "14.307960");
        loc7.put("name", "Klagenfurt, Klagenfurt am Wörthersee, Carinthia, 9020, Austria");
        locations[7] = loc7;

        HashMap<String, String> loc8 = new HashMap<String, String>();
        loc8.put("nwlat", "47.425430");
        loc8.put("nwlng", "11.552769");
        loc8.put("selat", "47.105430");
        loc8.put("selng", "11.232769");
        loc8.put("lat", "47.265430");
        loc8.put("lng", "11.392769");
        loc8.put("name", "Innsbruck, Tyrol, 6020, Austria");
        locations[8] = loc8;

        HashMap<String, String> loc9 = new HashMap<String, String>();
        loc9.put("nwlat", "48.145711");
        loc9.put("nwlng", "16.560306");
        loc9.put("selat", "47.951363");
        loc9.put("selng", "16.253757");
        loc9.put("lat", "47.875098");
        loc9.put("lng", "15.866162");
        loc9.put("name", "Bezirk Baden, Lower Austria, Austria");
        locations[9] = loc9;
	}
    
	private static void setPrefixes() {
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
	}
}
