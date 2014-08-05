package won.cryptography.message;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.RDF;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

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

  private static final RDFNode NULL_NODE = null;

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
    Model messageMeta = ModelFactory.createDefaultModel();
    StmtIterator stmtIterator = defaultModel.listStatements();
    while (stmtIterator.hasNext()) {
      Statement stmt = stmtIterator.nextStatement();
      if (isMessageMeta(stmt)) {
        messageMeta.add(stmt);
      }
    }
    // TODO add all prefixes or only relevant..?
    Model defaultModelDiff = defaultModel.difference(messageMeta);

    Dataset dataset = DatasetFactory.createMem();
    dataset.setDefaultModel(defaultModelDiff);
    List<String> names = getModelNames(message);
    for (String name : names) {
      dataset.addNamedModel(name, message.getNamedModel(name));
    }
    Resource msgBnode = extractMessageBnode(messageMeta);
    WonMessageMethod method = extractMethod(msgBnode, messageMeta);
    String protocolUri = extractProtocol(msgBnode, messageMeta);

    return new WonMessage(protocolUri, method, dataset);
  }

  private static Resource extractMessageBnode(final Model messageMeta) {

    StmtIterator stmtIter = messageMeta.listStatements(null, RDF.type, WonMessageOntology.MESSAGE_TYPE_RESOURCE);

    while (stmtIter.hasNext()) {
      Statement stmt = stmtIter.next();
      return stmt.getSubject().asResource();
      // not more than one type is allowed
    }
    return null;
  }

  private static WonMessageMethod extractMethod(final Resource msgBnode, final Model messageMeta) {
    WonMessageMethod method = new WonMessageMethod();

    // set method uri
    Property methodProp = messageMeta.createProperty(WonMessageOntology.MESSAGE_ONTOLOGY_URI,
                                                     WonMessageOntology.METHOD_PROPERTY);
    StmtIterator stmtIter = messageMeta.listStatements(msgBnode, methodProp, NULL_NODE);
    String methodName = null;
    Resource methodResource = null;
    while (stmtIter.hasNext()) {
      Statement stmt = stmtIter.next();
      methodResource = stmt.getObject().asResource();
      method.setMethodUri(methodResource.getURI());
      break; // not more than one method is allowed, if it changes in spec,
      // then the structure of WonMessage or MessageMethod has to be changed...
    }

    // TODO
    // set method parameters
//    if (methodResource != null) {
//      StmtIterator stmtIter = messageMeta.listStatements(methodResource, methodProp, NULL_NODE);
//
//    }

    return method;
  }

  private static String extractProtocol(final Resource msgBnode, final Model messageMeta) {
    Property protocolProp = messageMeta.createProperty(WonMessageOntology.MESSAGE_ONTOLOGY_URI,
                                                       WonMessageOntology.PROTOCOL_PROPERTY);
    StmtIterator stmtIter = messageMeta.listStatements(msgBnode, protocolProp, NULL_NODE);

    String protocol = null;
    while (stmtIter.hasNext()) {
      Statement stmt = stmtIter.next();
      protocol = stmt.getObject().asResource().getURI();
      break; // not more than one protocol is allowed
    }

    return protocol;
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

  private static boolean isMessageMeta(final Statement stmt) {
    Property prop = stmt.getPredicate();
    if (prop.isResource()) {
      String uri = prop.getURI();
      if (uri.startsWith(WonMessageOntology.MESSAGE_ONTOLOGY_URI)) {
        return true;
      } else if (uri.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
        if (stmt.getObject().isURIResource() && stmt.getObject().asResource().getURI().startsWith(WonMessageOntology
                                                                                                    .MESSAGE_ONTOLOGY_URI)) {
          return true;
        }
      }
    }
    return false;
  }
}
