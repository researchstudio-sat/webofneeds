package won.protocol.util;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.RDF;
import won.protocol.vocabulary.WON;

import java.net.URI;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Utilities for populating/manipulating the RDF models used throughout the WON application.
 */
public class WonRdfUtils
{
  public static class MessageUtils {
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

  public static class FacetUtils {

    /**
     * Returns the first facet found in the model, attached to the null relative URI '<>'.
     * Returns null if there is no such facet.
     * @param content
     * @return
     */
    public static URI getFacet(Model content) {
      Resource baseRes = RdfUtils.getBaseResource(content);
      StmtIterator stmtIterator = baseRes.listProperties(WON.HAS_FACET);
      if (!stmtIterator.hasNext()) return null;
      return URI.create(stmtIterator.next().getObject().asResource().getURI());
    }

    /**
     * Returns all facets found in the model, attached to the null relative URI '<>'.
     * Returns an empty collection if there is no such facet.
     * @param content
     * @return
     */
    public static Collection<URI> getFacets(Model content) {
      Resource baseRes = RdfUtils.getBaseResource(content);
      StmtIterator stmtIterator = baseRes.listProperties(WON.HAS_FACET);
      LinkedList<URI> ret = new LinkedList<URI>();
      while (stmtIterator.hasNext()){
        RDFNode object = stmtIterator.nextStatement().getObject();
        if (object.isURIResource()){
          ret.add(URI.create(object.asResource().getURI()));
        }
      }
      return ret;
    }

    /**
     * Adds a triple to the model of the form <> won:hasFacet [facetURI].
     * @param content
     * @param facetURI
     */
    public static void addFacet(final Model content, final URI facetURI)
    {
      Resource baseRes = RdfUtils.getBaseResource(content);
      baseRes.addProperty(WON.HAS_FACET, content.createResource(facetURI.toString()));
    }
  }


}
