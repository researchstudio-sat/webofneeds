package won.protocol.message;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WONMSG;

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

    Dataset dataset = DatasetFactory.createMem();
    Model messageMetadata = ModelFactory.createDefaultModel();
    messageMetadata.setNsPrefix(WONMSG.DEFAULT_PREFIX, WONMSG.BASE_URI);

    // add pointer to the message event
    Resource envelopeGraphResource = messageMetadata.createResource(
      wonMessage.getMessageEvent().getMessageURI().toString()+ WonRdfUtils.NAMED_GRAPH_SUFFIX);
    envelopeGraphResource.addProperty(RDF.type, WONMSG.ENVELOPE_GRAPH);
    //TODO own message event signature

    // add event of the message
    MessageEventMapper mapper = new MessageEventMapper();
    Model eventModel = mapper.toModel(wonMessage.getMessageEvent());
    dataset.addNamedModel(envelopeGraphResource.getURI().toString(), eventModel);

    // add other content of the message
    // TODO create clone instead?
    dataset.setDefaultModel(ModelFactory.createDefaultModel());
    dataset.getDefaultModel().add(messageMetadata);
    Dataset messageContent = wonMessage.getMessageContent();
    dataset.getDefaultModel().add(messageContent.getDefaultModel());
    Iterator<String> names = messageContent.listNames();
    while (names.hasNext()) {
      String name = names.next();
      dataset.addNamedModel(name, messageContent.getNamedModel(name));
    }

    // TODO signatures from other content should be copied inside event message
    // here or before? when event message is created?

    // TODO improve prefix handling
    RdfUtils.addPrefixMapping(dataset.getDefaultModel(), messageContent.getDefaultModel());
    RdfUtils.addPrefixMapping( dataset.getDefaultModel(), messageMetadata);
    RdfUtils.addPrefixMapping( dataset.getDefaultModel(), eventModel);

    return dataset;
  }
}
