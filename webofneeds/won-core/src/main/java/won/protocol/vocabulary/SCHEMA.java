package won.protocol.vocabulary;

import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;


/**
 * SCHEMA vocabulary.
 * 
 * USER: MS
 * Date: 01.10.2018
 */
public class SCHEMA {

    public static final String BASE_URI = "http://schema.org/";
    public static final String DEFAULT_PREFIX= "s";

    private static Model m = ModelFactory.createDefaultModel();

    public static final Property NAME = m.createProperty(BASE_URI + "name");
    public static final Property TITLE = m.createProperty(BASE_URI + "title");
    public static final Property ORGANIZATION = m.createProperty(BASE_URI + "Organization");
    public static final Property JOBPOSTING = m.createProperty(BASE_URI + "JobPosting");
    public static final Property JOBLOCATION = m.createProperty(BASE_URI + "jobLocation");
    public static final Property PLACE = m.createProperty(BASE_URI + "Place");
    public static final Property BASESALARY = m.createProperty(BASE_URI + "baseSalary");
    public static final Property EMPLYOMENTTYPE = m.createProperty(BASE_URI + "employmentType");
    public static final Property INDUSTRY = m.createProperty(BASE_URI + "industry");
    public static final Property PERSON = m.createProperty(BASE_URI + "Person");
    public static final Property URL = m.createProperty(BASE_URI + "URL");
    public static final Property DATEPOSTED = m.createProperty(BASE_URI + "datePosted");
    public static final Property IMAGE = m.createProperty(BASE_URI + "image");
    public static final Property GEOCOORDINATES = m.createProperty(BASE_URI + "GeoCoordinates");
    
    public static final Property AMENITYFEATURE = m.createProperty(BASE_URI + "amenityFeature");
    public static final Property FLOORSIZE = m.createProperty(BASE_URI + "floorSize");
    public static final Property NUMBEROFROOMS = m.createProperty(BASE_URI + "numberOfRooms");
    public static final Property PRICESPECIFICATION = m.createProperty(BASE_URI + "priceSpecification");
    public static final Property GEO = m.createProperty(BASE_URI + "geo");
    public static final Property LATITUDE = m.createProperty(BASE_URI + "latitude");
    public static final Property LONGITUDE = m.createProperty(BASE_URI + "longitude");
    public static final Property DESCRIPTION = m.createProperty(BASE_URI + "description");
    public static final Property PRICE = m.createProperty(BASE_URI + "price");
    public static final Property PRICECURRENCY = m.createProperty(BASE_URI + "priceCurrency");
    public static final Property UNITCODE = m.createProperty(BASE_URI + "unitCode");
    public static final Property VALUE = m.createProperty(BASE_URI + "value");

    public static final RDFDatatype TEXT = new BaseDatatype(BASE_URI + "Text");
    
    /** returns the URI for this schema
     * @return the URI for this schema
     */
    public static String getURI() {
        return BASE_URI;
    }
}
