package won.protocol.util;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import won.protocol.vocabulary.WON;

/**
 * Utilities for populating the RDF models passed in WON messages.
 */
public class MessageModelUtils {
  /**
   * Creates an RDF model containing a text message.
   * @param message
   * @return
   */
  public static Model textMessage(String message) {
    com.hp.hpl.jena.rdf.model.Model messageModel = ModelFactory.createDefaultModel();
    messageModel.setNsPrefix("", "no:uri");
    Resource baseRes = messageModel.createResource(messageModel.getNsPrefixURI(""));
    baseRes.addProperty(RDF.type, WON.TEXT_MESSAGE);
    baseRes.addProperty(WON.HAS_TEXT_MESSAGE,message, XSDDatatype.XSDstring);
    return messageModel;
  }
}
