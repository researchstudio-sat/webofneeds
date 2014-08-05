package won.cryptography.message;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import java.io.StringWriter;
import java.util.Iterator;

/**
 * User: ypanchenko
 * Date: 04.08.2014
 */
public class WonMessageEncoder
{
  public static String encodeAsJsonLd(WonMessage message) {
    return encode(message, Lang.JSONLD);
  }

  public static String encode(WonMessage message, Lang lang) {
    StringWriter sw = new StringWriter();
    RDFDataMgr.write(sw, encodeAsDataset(message), lang);
    return sw.toString();
  }


  public static Dataset encodeAsDataset(WonMessage wonMessage) {
    Model messageMetadata = wonMessage.getMessageMetadata();
    // TODO create clone instead?
    Dataset dataset = DatasetFactory.createMem();
    dataset.setDefaultModel(ModelFactory.createDefaultModel());
    dataset.getDefaultModel().add(messageMetadata);
    Dataset messageContent = wonMessage.getMessageContent();
    dataset.getDefaultModel().add(messageContent.getDefaultModel());
    Iterator<String> names = messageContent.listNames();
    while (names.hasNext()) {
      String name = names.next();
      dataset.addNamedModel(name, messageContent.getNamedModel(name));
    }
    for (String prefix : messageContent.getDefaultModel().getNsPrefixMap().keySet()) {
      dataset.getDefaultModel().setNsPrefix(prefix, messageContent.getDefaultModel().getNsPrefixMap().get(prefix));
    }
    // TODO need to check if the prefix is already in use
    for (String prefix : messageMetadata.getNsPrefixMap().keySet()) {
      dataset.getDefaultModel().setNsPrefix(prefix, messageMetadata.getNsPrefixMap().get(prefix));
    }
    return dataset;
  }
}
