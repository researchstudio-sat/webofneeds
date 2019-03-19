package won.utils.im.port;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDF;

import won.protocol.vocabulary.SCHEMA;
import won.protocol.vocabulary.WON;

public class RealEstateNeedGenerator {

    static Model model = ModelFactory.createDefaultModel();

    static RDFDatatype bigdata_geoSpatialDatatype = new BaseDatatype(
            "http://www.bigdata.com/rdf/geospatial/literals/v1#lat-lon");

    static HashMap<String, String>[] locations = new HashMap[10];
    static String[] amenities = { "Balcony", "Parkingspace", "Garden", "Bathtub", "furnished", "Parquetflooring",
            "Elevator", "Cellar", "Pool", "Sauna", "accessible" };

    public static void main(String[] args) throws Exception {
        initializeLocations();
        generateNeeds();
    }

    private static void generateNeeds() throws Exception {
        File parentFolder = new File("sample_needs");
        parentFolder.mkdirs();
        Arrays.stream(parentFolder.listFiles()).forEach(f -> f.delete());
        final int N = 10000;
        int outputSteps = N / 10 / 10 * 10;
        Random random = new Random();
        for (int i = 0; i < N; i++) {
            String rnd = Long.toHexString(random.nextLong());
            String needURI = "https://localhost:8443/won/resource/event/" + "real_estate_sample_" + rnd;

            model = ModelFactory.createDefaultModel();

            setPrefixes();

            Resource need = model.createResource(needURI);
            Resource seeksPart = model.createResource();

            // method signatures: branch, probability that detail is added, min, max
            need = addTitle(need, 1.0, i);
            need = addDescription(need, 1.0);
            need = addQuery(need);
            need = addLocation(need, 1.0, need);
            need = addAmenities(need, 0.8, 1, 4);
            need = addFloorSize(need, 0.8, 28, 250, need);
            need = addNumberOfRooms(need, 0.8, 1, 9, need);
            need = addPriceSpecification(need, 1.0, 250, 2200, need);
            need.addProperty(WON.HAS_TAG, "RentOutRealEstate");

            seeksPart.addProperty(WON.HAS_TAG, "SearchRealEstateToRent");

            need.addProperty(RDF.type, WON.NEED);

            /*
             * no facets - they are added by the bot Resource won_ChatFacet =
             * model.createResource("http://purl.org/webofneeds/model#ChatFacet"); Resource holdableFacet =
             * need.getModel().getResource(needURI + "#holdableFacet"); holdableFacet.addProperty(RDF.type,
             * holdableFacet.getModel().getResource(FacetType.HoldableFacet.getURI().toString()));
             * need.addProperty(won_hasFacet, holdableFacet);
             * 
             * Resource chatFacet = need.getModel().getResource(needURI + "#chatFacet"); chatFacet.addProperty(RDF.type,
             * chatFacet.getModel().getResource(FacetType.ChatFacet.getURI().toString()));
             * need.addProperty(won_hasFacet, chatFacet);
             */

            need.addProperty(WON.SEEKS, seeksPart);

            try {
                FileOutputStream out = new FileOutputStream(
                        new File(parentFolder, "real_estate_need_" + rnd + ".trig"));
                model.write(out, "TURTLE");
                out.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (i % outputSteps == 0) {
                System.out.println("generated " + i + " sample needs");
            }
        }
        System.out.println("generated " + N + " sample needs");
    }

    private static Resource addQuery(Resource resource) throws Exception {
        String query = RealEstateNeedGenerator.getResourceAsString("realestate-offer-query-template.rq");
        return resource.addLiteral(WON.HAS_QUERY, query);
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

    private static Resource addLocation(Resource resource, double probability, Resource resourceForQuery) {
        if (Math.random() < (1.0 - probability)) {
            return resource;
        }

        // pick a location and change it by a random amount so that the locations are
        // scattered around a point
        int locNr = (int) (Math.random() * 10);
        double rndlat = 0.05 * Math.random();
        double rndlng = 0.05 * Math.random();
        DecimalFormat df = new DecimalFormat("##.######");
        df.setRoundingMode(RoundingMode.HALF_UP);
        df.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));

        String nwlat = df.format(Double.parseDouble(locations[locNr].get("nwlat")) + rndlat);
        String nwlng = df.format(Double.parseDouble(locations[locNr].get("nwlng")) + rndlng);
        String selat = df.format(Double.parseDouble(locations[locNr].get("selat")) + rndlat);
        String selng = df.format(Double.parseDouble(locations[locNr].get("selng")) + rndlng);
        String lat = df.format(Double.parseDouble(locations[locNr].get("lat")) + rndlat);
        String lng = df.format(Double.parseDouble(locations[locNr].get("lng")) + rndlng);
        String name = locations[locNr].get("name");

        Resource locationResource = model.createResource();
        Resource boundingBoxResource = model.createResource();
        Resource nwCornerResource = model.createResource();
        Resource seCornerResource = model.createResource();
        Resource geoResource = model.createResource();
        Resource schema_Place = model.createResource("http://schema.org/Place");
        Resource schema_GeoCoordinates = model.createResource("http://schema.org/GeoCoordinates");

        resource.addProperty(SCHEMA.LOCATION, locationResource);
        locationResource.addProperty(RDF.type, schema_Place);
        locationResource.addProperty(SCHEMA.NAME, name);
        locationResource.addProperty(SCHEMA.GEO, geoResource);
        geoResource.addProperty(RDF.type, schema_GeoCoordinates);
        geoResource.addProperty(SCHEMA.LATITUDE, lat);
        geoResource.addProperty(SCHEMA.LONGITUDE, lng);
        // add bigdata specific value: "<subj> won:geoSpatial
        // "48.225073#16.358398"^^<http://www.bigdata.com/rdf/geospatial/literals/v1#lat-lon>"
        geoResource.addProperty(WON.GEO_SPATIAL, lat + "#" + lng, bigdata_geoSpatialDatatype);
        locationResource.addProperty(WON.HAS_BOUNDING_BOX, boundingBoxResource);
        boundingBoxResource.addProperty(WON.HAS_NORTH_WEST_CORNER, nwCornerResource);
        nwCornerResource.addProperty(RDF.type, schema_GeoCoordinates);
        nwCornerResource.addProperty(SCHEMA.LATITUDE, nwlat);
        nwCornerResource.addProperty(SCHEMA.LONGITUDE, nwlng);
        boundingBoxResource.addProperty(WON.HAS_SOUTH_EAST_CORNER, seCornerResource);
        seCornerResource.addProperty(RDF.type, schema_GeoCoordinates);
        seCornerResource.addProperty(SCHEMA.LATITUDE, selat);
        seCornerResource.addProperty(SCHEMA.LONGITUDE, selng);

        // update query
        replaceInQuery(resourceForQuery, "\\?varLatLng", "\"" + lat + "#" + lng + "\"");
        replaceInQuery(resourceForQuery, "\\?varRadius", "\"5\"");
        return resource;
    }

    private static Resource replaceInQuery(Resource resource, String toReplace, String replacement) {
        String query = resource.getRequiredProperty(WON.HAS_QUERY).getString();
        resource.removeAll(WON.HAS_QUERY);
        return resource.addProperty(WON.HAS_QUERY, query.replaceAll(toReplace, replacement));
    }

    private static Resource addAmenities(Resource resource, double probability, int min, int max) {
        if (Math.random() < (1.0 - probability)) {
            return resource;
        }

        int numberOfAmenities = (int) (Math.random() * Math.abs(max - min + 1) + min);
        Collections.shuffle(Arrays.asList(amenities));

        Resource schema_LocationFeatureSpecification = model
                .createResource("http://schema.org/LocationFeatureSpecification");

        for (int j = 0; j < numberOfAmenities; j++) {
            Resource amenityResource = model.createResource();
            resource.addProperty(SCHEMA.AMENITYFEATURE, amenityResource);
            amenityResource.addProperty(RDF.type, schema_LocationFeatureSpecification);
            amenityResource.addProperty(SCHEMA.VALUE, amenities[j], SCHEMA.TEXT);
        }
        return resource;
    }

    private static Resource addFloorSize(Resource resource, double probability, int min, int max,
            Resource resourceForQuery) {
        if (Math.random() < (1.0 - probability)) {
            return resource;
        }

        int floorSize = (int) (Math.random() * Math.abs(max - min + 1)) + min;

        Resource floorSizeResource = model.createResource();
        Resource schema_QuantitativeValue = model.createResource("http://schema.org/QuantitativeValue");

        resource.addProperty(SCHEMA.FLOORSIZE, floorSizeResource);
        floorSizeResource.addProperty(RDF.type, schema_QuantitativeValue);
        floorSizeResource.addProperty(SCHEMA.UNITCODE, "MTK");
        floorSizeResource.addProperty(SCHEMA.VALUE, Integer.toString(floorSize), XSDDatatype.XSDfloat);
        replaceInQuery(resourceForQuery, "\\?varFloorSize", "\"" + floorSize + "\"");
        return resource;
    }

    private static Resource addNumberOfRooms(Resource resource, double probability, int min, int max,
            Resource resourceForQuery) {
        if (Math.random() < (1.0 - probability)) {
            return resource;
        }

        int numberOfRooms = (int) (Math.random() * Math.abs(max - min + 1)) + min;

        resource.addProperty(SCHEMA.NUMBEROFROOMS, Integer.toString(numberOfRooms), XSDDatatype.XSDfloat);
        replaceInQuery(resourceForQuery, "\\?varNumberOfRooms", "\"" + numberOfRooms + "\"");
        return resource;
    }

    private static Resource addPriceSpecification(Resource resource, double probability, double min, double max,
            Resource resourceForQuery) {
        if (Math.random() < (1.0 - probability)) {
            return resource;
        }

        int price = (int) (Math.random() * Math.abs(max - min + 1) + min);

        Resource schema_CompoundPriceSpecification = model
                .createResource("http://schema.org/CompoundPriceSpecification");
        Resource priceSpecificationResource = model.createResource();

        resource.addProperty(SCHEMA.PRICESPECIFICATION, priceSpecificationResource);
        priceSpecificationResource.addProperty(RDF.type, schema_CompoundPriceSpecification);
        priceSpecificationResource.addProperty(SCHEMA.DESCRIPTION, "total rent per month");
        priceSpecificationResource.addProperty(SCHEMA.PRICE, Integer.toString(price), XSDDatatype.XSDfloat);
        priceSpecificationResource.addProperty(SCHEMA.PRICECURRENCY, "EUR");
        replaceInQuery(resourceForQuery, "\\?varPrice", "\"" + price + "\"");
        replaceInQuery(resourceForQuery, "\\?varCurrency", "\"EUR\"");
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
        loc1.put("name", "Vienna, Austria");
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

    private static InputStream getResourceAsStream(String name) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
    }

    private static String getResourceAsString(String name) throws Exception {
        byte[] buffer = new byte[256];
        StringWriter sw = new StringWriter();
        try (InputStream in = getResourceAsStream(name)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int bytesRead = 0;
            while ((bytesRead = in.read(buffer)) > -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return new String(baos.toByteArray(), Charset.defaultCharset());
        }

    }
}
