package won.cryptography.model;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;

/**
 * User: fsalcher
 * Date: 23.07.2014
 */
public class WONCryptographyModel {


    private static Model model = ModelFactory.createDefaultModel();

    public static final String CRYPT_BASE_URI = "http://purl.org/webofneeds/cryptography#";

    public static final String CRYPT_ECCPublicKey_URI_FRAGMENT = "ECCPublicKey";

    public static final String CRYPT_ecc_curveID_URI_FRAGMENT = "ecc_curveID";
    public static final Property CRYPT_ecc_curveID_Property =
            model.createProperty(CRYPT_BASE_URI + CRYPT_ecc_curveID_URI_FRAGMENT);

    public static final String CRYPT_ecc_algorithm_URI_FRAGMENT = "ecc_algorithm";
    public static final Property CRYPT_ecc_algorithm_Property =
            model.createProperty(CRYPT_BASE_URI + CRYPT_ecc_algorithm_URI_FRAGMENT);

    public static final String CRYPT_ecc_qx_URI_FRAGMENT = "ecc_qx";
    public static final Property CRYPT_ecc_qx_Property =
            model.createProperty(CRYPT_BASE_URI + CRYPT_ecc_qx_URI_FRAGMENT);

    public static final String CRYPT_ecc_qy_URI_FRAGMENT = "ecc_qy";
    public static final Property CRYPT_ecc_qy_Property =
            model.createProperty(CRYPT_BASE_URI + CRYPT_ecc_qy_URI_FRAGMENT);


    public static final String CERT_BASE_URI = "http://www.w3.org/ns/auth/cert";

    public static final String CERT_key_URI_FRAGMENT = "key";
    public static final Property CERT_key_Property = model.createProperty(CERT_BASE_URI + CERT_key_URI_FRAGMENT);





}
