package won.utils.im.port;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
//import org.apache.jena.vocabulary.XSD;

public class RealEstateNeedGenerator {

    public static void main(String[] args) {
        String needURI = "https://localhost:8443/won/resource/event/m3tuwsuahplc#need";

        Model model = ModelFactory.createDefaultModel();

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

        Resource schema_Place = model.createResource("http://schema.org/Place");
        Resource schema_GeoCoordinates = model.createResource("http://schema.org/GeoCoordinates");
        Resource schema_LocationFeatureSpecification = model.createResource("http://schema.org/LocationFeatureSpecification");
        Resource schema_QuantitativeValue = model.createResource("http://schema.org/QuantitativeValue");
        Resource schema_CompoundPriceSpecification = model.createResource("http://schema.org/CompoundPriceSpecification");

        Property won_hasFacet = model.createProperty("http://purl.org/webofneeds/model#hasFacet");
        Property won_is = model.createProperty("http://purl.org/webofneeds/model#is");
        Property won_seeks = model.createProperty("http://purl.org/webofneeds/model#seeks");
        Property won_hasTag = model.createProperty("http://purl.org/webofneeds/model#hasTag");
        Property won_hasLocation = model.createProperty("http://purl.org/webofneeds/model#hasLocation");
        Property won_hasBoundingBox = model.createProperty("http://purl.org/webofneeds/model#hasBoundingBox");
        Property won_hasNorthWestCorner = model.createProperty("http://purl.org/webofneeds/model#hasNorthWestCorner");
        Property won_hasSouthEastCorner = model.createProperty("http://purl.org/webofneeds/model#hasSouthEastCorner");

        Property schema_amenityFeature = model.createProperty("http://schema.org/amenityFeature");
        Property schema_floorSize = model.createProperty("http://schema.org/floorSize");
        Property schema_numberOfRooms = model.createProperty("http://schema.org/numberOfRooms");
        Property schema_priceSpecification = model.createProperty("http://schema.org/priceSpecification");
        Property schema_geo = model.createProperty("http://schema.org/geo");
        Property schema_latitude = model.createProperty("http://schema.org/latitude");
        Property schema_longitude = model.createProperty("http://schema.org/longitude");
        Property schema_name = model.createProperty("http://schema.org/name");
        Property schema_description = model.createProperty("http://schema.org/description");
        Property schema_price = model.createProperty("http://schema.org/price");
        Property schema_priceCurrency = model.createProperty("http://schema.org/priceCurrency");
        Property schema_unitCode = model.createProperty("http://schema.org/unitCode");
        Property schema_value = model.createProperty("http://schema.org/value");

        Resource locationResource = model.createResource();
        Resource boundingBoxResource = model.createResource();
        Resource nwCornerResource = model.createResource();
        Resource seCornerResource = model.createResource();
        Resource geoResource = model.createResource();
        Resource amenityResource = model.createResource();
        Resource floorSizeResource = model.createResource();
        // Resource numberOfRoomsResource = model.createResource();
        Resource priceSpecificationResource = model.createResource();

        need.addProperty(RDF.type, won_Need);
        need.addProperty(won_hasFacet, won_OwnerFacet);
        need.addProperty(won_is, isPart);
        need.addProperty(won_seeks, seeksPart);

        seeksPart.addProperty(won_hasTag, "to-rent");
        
        isPart.addProperty(DC.title, "Some Title");
        isPart.addProperty(DC.description, "Some Description");

        isPart.addProperty(won_hasLocation, locationResource);
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

        isPart.addProperty(schema_amenityFeature, amenityResource);
            amenityResource.addProperty(RDF.type, schema_LocationFeatureSpecification);
            amenityResource.addProperty(schema_name, "some amenity");
            amenityResource.addProperty(schema_name, "some amenity2");
            amenityResource.addProperty(schema_name, "some amenity3");
            amenityResource.addProperty(schema_name, "some amenity4");

        isPart.addProperty(schema_floorSize, floorSizeResource);
            floorSizeResource.addProperty(RDF.type, schema_QuantitativeValue);
            floorSizeResource.addProperty(schema_unitCode, "MTK");
            floorSizeResource.addProperty(schema_value, "111");

        isPart.addProperty(schema_numberOfRooms, "5");

        isPart.addProperty(schema_priceSpecification,priceSpecificationResource);
            priceSpecificationResource.addProperty(RDF.type, schema_CompoundPriceSpecification);
            priceSpecificationResource.addProperty(schema_description, "total rent per month");
            priceSpecificationResource.addProperty(schema_price, "333");
            priceSpecificationResource.addProperty(schema_priceCurrency, "EUR");


        // Public Key Graph
        // namespace cert
        // Resource cert_PublicKey = model.createResource("http://www.w3.org/ns/auth/cert#PublicKey");
        // Property cert_key = model.createProperty("http://www.w3.org/ns/auth/cert#key");

        // namespace woncrypt
        // Resource woncrypt_ECCPublicKey = model.createResource("http://purl.org/webofneeds/woncrypt#ECCPublicKey");
        // Property woncrypt_eccAlgorithm = model.createProperty("http://purl.org/webofneeds/woncrypt#ecc_algorithm");
        // Property woncrypt_eccCurveID = model.createProperty("http://purl.org/webofneeds/woncrypt#ecc_curveId");
        // Property woncrypt_eccQx = model.createProperty("http://purl.org/webofneeds/woncrypt#ecc_qx");
        // Property woncrypt_eccQy = model.createProperty("http://purl.org/webofneeds/woncrypt#ecc_qy");

        // need.addProperty(cert_key, cert_PublicKey);
        // cert_PublicKey.addProperty(RDF.type, woncrypt_ECCPublicKey);
        // cert_PublicKey.addProperty(woncrypt_eccAlgorithm, "EC");
        // cert_PublicKey.addProperty(woncrypt_eccCurveID, "secp384r1");
        // cert_PublicKey.addProperty(woncrypt_eccQx, "666cae5e0c8037382924976dea4c0f80279c4277fac6ec8d2fa66249f40dffb9c1d5b2fc87d5dc30eb756800b95cf831");
        // cert_PublicKey.addProperty(woncrypt_eccQy, "30b976a67995f1081a3fc3d11df0e7ff3fe76de2833f8917a207cd6c8972a073711f4f3cc3aec2baf1861ab0e02e0674");


        model.write(System.out, "TURTLE");

	}

}
