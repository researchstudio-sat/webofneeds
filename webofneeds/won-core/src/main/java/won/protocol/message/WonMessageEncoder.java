package won.protocol.message;

import java.io.StringWriter;

import org.apache.jena.query.Dataset;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

/**
 * User: ypanchenko Date: 04.08.2014
 */
public class WonMessageEncoder {
    public static String encodeAsJsonLd(WonMessage message) {
        return encode(message, Lang.JSONLD);
    }

    /**
     * Encodes the WonMessage object as serialized RDF in the given language. If no
     * WonMessage object is provided an empty string is returned.
     *
     * @param message <code>WonMessage</code> object which will be serialized
     * @param lang defines the serialization language
     * @return <code>String</code> containing the serialized RDF; if no WonMessage
     * is provided an empty string is returned
     */
    public static String encode(WonMessage message, Lang lang) {
        if (message == null)
            return "";
        StringWriter sw = new StringWriter();
        RDFDataMgr.write(sw, message.getCompleteDataset(), lang);
        return sw.toString();
    }

    public static Dataset encodeAsDataset(WonMessage wonMessage) {
        return wonMessage.getCompleteDataset();
    }
}
