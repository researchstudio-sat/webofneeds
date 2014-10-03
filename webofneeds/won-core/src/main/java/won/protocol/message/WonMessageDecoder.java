package won.protocol.message;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * User: ypanchenko
 * Date: 04.08.2014
 */

public class WonMessageDecoder
{

  private static final Logger logger = LoggerFactory.getLogger(WonMessageDecoder.class);

  public static WonMessage decodeFromJsonLd(String message) {
    return decode(Lang.JSONLD, message);
  }

  public static WonMessage decode(Lang lang, String message) {
    if (message == null || message.equals("")) {
      logger.warn("cannot decode empty or null string to message");
      return null;
    }
    Dataset dataset = DatasetFactory.createMem();
    StringReader sr = new StringReader(message);
    RDFDataMgr.read(dataset, sr, null, lang);
    return decodeFromDataset(dataset);
  }

  // ToDo (FS): build the WonMessage and MessageEvent with the Builder in order to avoid redundant code
  public static WonMessage decodeFromDataset(Dataset message) {

    return new WonMessage(message);
  }

  // TODO move to a GateUtils...
  private static List<String> getModelNames(final Dataset dataset) {

    List<String> modelNames = new ArrayList<String>();
    Iterator<String> names = dataset.listNames();
    while (names.hasNext()) {
      modelNames.add(names.next());
    }
    return modelNames;
  }

}
