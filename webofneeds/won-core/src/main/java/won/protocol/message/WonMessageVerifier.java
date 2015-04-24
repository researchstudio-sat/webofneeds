package won.protocol.message;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.RDF;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import won.protocol.vocabulary.WONMSG;

import java.io.StringReader;

/**
 * User: fsalcher
 * Date: 02.09.2014
 */

// ToDo (FS): add signature verification

public class WonMessageVerifier
{

  public static WonMessageVerificationResult verifyWonMessage(String wonMessage, Lang lang) {
    Dataset dataset;
    try {
      dataset = DatasetFactory.createMem();
      StringReader sr = new StringReader(wonMessage);
      RDFDataMgr.read(dataset, sr, null, lang);
    } catch (Exception e) {
      WonMessageVerificationResult result = new WonMessageVerificationResult();
      result.couldNotReadSerializedRDF = true;
      return result;
    }
    return checkEverything(dataset);
  }

  public static WonMessageVerificationResult verifyWonMessage(Dataset wonMessage) {
    return checkEverything(wonMessage);
  }

  private static WonMessageVerificationResult checkEverything(Dataset wonMessage) {

    WonMessageVerificationResult result = new WonMessageVerificationResult();

    checkEnvelop(wonMessage, result);

    return result;
  }

  private static WonMessageVerificationResult checkEnvelop(
    Dataset wonMessage, WonMessageVerificationResult result) {

    Model defaultGraph = wonMessage.getDefaultModel();
    ResIterator resIterator = defaultGraph.listResourcesWithProperty(RDF.type, WONMSG.ENVELOPE_GRAPH);
    if (!resIterator.hasNext())
      result.noEnvelopGraphFound = true;
    Resource res = resIterator.nextResource();
    String envelopGraphURI = res.getURI().toString();
    if (resIterator.hasNext())
      result.multipleEnvelopsFound = true;

    Model envelop = wonMessage.getNamedModel(envelopGraphURI);

    // get the messageEvent type
    NodeIterator messageEventTypeIterator = envelop.listObjectsOfProperty(RDF.type);
    if (!messageEventTypeIterator.hasNext())
      result.noMessageEventTypeFound = true;
    boolean messageTypeFound = false;
    while (messageEventTypeIterator.hasNext()) {
      RDFNode nextNode = messageEventTypeIterator.nextNode();
      if (nextNode.isResource()) {
       Resource r = nextNode.asResource();
        (WonMessageType.getWonMessageType(r))


      }
    }

    checkSenderAndReceiver(envelop, result);

    return result;
  }

  private static WonMessageVerificationResult checkSenderAndReceiver(
    Model envelop, WonMessageVerificationResult result) {



    return result;
  }

//  private static WonMessageVerificationResult check

}
