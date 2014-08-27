package won.protocol.message;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.WONMSG;

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

  //private static final RDFNode NULL_NODE = null;

  public static WonMessage decodeFromJsonLd(String message) {
    return decode(Lang.JSONLD, message);
  }

  public static WonMessage decode(Lang lang, String message) {
    Dataset dataset = DatasetFactory.createMem();
    StringReader sr = new StringReader(message);
    RDFDataMgr.read(dataset, sr, null, lang);
    return decodeFromDataset(dataset);
  }

  public static WonMessage decodeFromDataset(Dataset message) {
    // TODO how to handle copying properly...?
    Model defaultModel = message.getDefaultModel();
    //Model messageMeta = ModelFactory.createDefaultModel();
    String msgEventURI = null;
    StmtIterator stmtIterator = defaultModel.listStatements();
    while (stmtIterator.hasNext()) {
      Statement stmt = stmtIterator.nextStatement();
      if (isMessageEventPointer(stmt)) {
        msgEventURI = stmt.getObject().asResource().getURI().toString();
        break;
      }
    }

    MessageEventMapper mapper = new MessageEventMapper();
    MessageEvent msgEvent = mapper.fromModel(message.getNamedModel(msgEventURI));


    Dataset dataset = DatasetFactory.createMem();
    List<String> names = getModelNames(message);
    for (String name : names) {
      if (!msgEventURI.equals(name)) {
        dataset.addNamedModel(name, message.getNamedModel(name));
      }
    }
    RdfUtils.addPrefixMapping(dataset.getDefaultModel(), message.getDefaultModel());

    return new WonMessage(msgEvent, dataset);
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

  private static boolean isMessageEventPointer(final Statement stmt) {
    Property prop = stmt.getPredicate();
    if (prop.equals(WONMSG.MESSAGE_POINTER_PROPERTY)) {
      return true;
    }
    return false;
  }
}
