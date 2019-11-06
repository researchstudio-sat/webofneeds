package won.protocol.message;

import java.io.StringReader;
import java.lang.invoke.MethodHandles;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: ypanchenko Date: 04.08.2014
 */
public class WonMessageDecoder {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static WonMessage decodeFromJsonLd(String message) {
        return decode(Lang.JSONLD, message);
    }

    public static WonMessage decode(Lang lang, String message) {
        if (message == null || message.equals("")) {
            logger.warn("cannot decode empty or null string to message");
            return null;
        }
        Dataset dataset = DatasetFactory.createGeneral();
        StringReader sr = new StringReader(message);
        RDFDataMgr.read(dataset, sr, null, lang);
        return decodeFromDataset(dataset);
    }

    public static WonMessage decodeFromDataset(Dataset message) {
        return WonMessage.of(message);
    }
}
