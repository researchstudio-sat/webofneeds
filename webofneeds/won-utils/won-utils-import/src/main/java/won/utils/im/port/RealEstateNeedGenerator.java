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

        // namespace won
        Resource won_Need = model.createResource("http://purl.org/webofneeds/model#Need");
        Resource won_OwnerFacet = model.createResource("http://purl.org/webofneeds/model#OwnerFacet");
        Property won_hasFacet = model.createProperty("http://purl.org/webofneeds/model#hasFacet");
        Property won_is = model.createProperty("http://purl.org/webofneeds/model#is");
        Property won_seeks = model.createProperty("http://purl.org/webofneeds/model#seeks");
        Property won_hasTag = model.createProperty("http://purl.org/webofneeds/model#hasTag");
        
        // namespace cert
        Resource cert_PublicKey = model.createResource("http://www.w3.org/ns/auth/cert#PublicKey");
        Property cert_key = model.createProperty("http://www.w3.org/ns/auth/cert#key");

        // namespace woncrypt
        Resource woncrypt_ECCPublicKey = model.createResource("http://purl.org/webofneeds/woncrypt#ECCPublicKey");
        Property woncrypt_eccAlgorithm = model.createProperty("http://purl.org/webofneeds/woncrypt#ecc_algorithm");
        Property woncrypt_eccCurveID = model.createProperty("http://purl.org/webofneeds/woncrypt#ecc_curveId");
        Property woncrypt_eccQx = model.createProperty("http://purl.org/webofneeds/woncrypt#ecc_qx");
        Property woncrypt_eccQy = model.createProperty("http://purl.org/webofneeds/woncrypt#ecc_qy");

        // need.addProperty(RDF.type, model.expandPrefix("won:Need"));
        need.addProperty(RDF.type, won_Need);
        need.addProperty(won_hasFacet, won_OwnerFacet);
        need.addProperty(cert_key, cert_PublicKey);
        cert_PublicKey.addProperty(RDF.type, woncrypt_ECCPublicKey);
        cert_PublicKey.addProperty(woncrypt_eccAlgorithm, "EC");
        cert_PublicKey.addProperty(woncrypt_eccCurveID, "secp384r1");
        cert_PublicKey.addProperty(woncrypt_eccQx, "666cae5e0c8037382924976dea4c0f80279c4277fac6ec8d2fa66249f40dffb9c1d5b2fc87d5dc30eb756800b95cf831");
        cert_PublicKey.addProperty(woncrypt_eccQy, "30b976a67995f1081a3fc3d11df0e7ff3fe76de2833f8917a207cd6c8972a073711f4f3cc3aec2baf1861ab0e02e0674");

        need.addProperty(won_is, isPart);
        need.addProperty(won_seeks, seeksPart);

        seeksPart.addProperty(won_hasTag, "to-rent");
        
        //TODO: building the isPart

        model.write(System.out, "TURTLE");

	}

}
