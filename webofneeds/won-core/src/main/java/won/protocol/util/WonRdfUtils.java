package won.protocol.util;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;
import com.hp.hpl.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.exception.MultipleQueryResultsFoundException;
import won.protocol.model.NeedState;
import won.protocol.vocabulary.WON;


import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Utilities for populating/manipulating the RDF models used throughout the WON application.
 */
public class WonRdfUtils
{

  public static final String NAMED_GRAPH_SUFFIX = "#data";

  private static final Logger logger = LoggerFactory.getLogger(WonRdfUtils.class);
  public static class MessageUtils {
    /**
     * Creates an RDF model containing a text message.
     * @param message
     * @return
     */
    public static Model textMessage(String message) {
      Model messageModel = createModelWithBaseResource();
      Resource baseRes = messageModel.createResource(messageModel.getNsPrefixURI(""));
      baseRes.addProperty(RDF.type, WON.TEXT_MESSAGE);
      baseRes.addProperty(WON.HAS_TEXT_MESSAGE,message, XSDDatatype.XSDstring);
      return messageModel;
    }

    /**
     * Creates an RDF model containing a generic message.
     *
     * @return
     */
    public static Model genericMessage(URI predicate, URI object) {
      return genericMessage(new PropertyImpl(predicate.toString()), new ResourceImpl(object.toString()));
    }

    /**
     * Creates an RDF model containing a generic message.
     *
     * @return
     */
    public static Model genericMessage(Property predicate, Resource object) {
      Model messageModel = createModelWithBaseResource();
      Resource baseRes = RdfUtils.getBaseResource(messageModel);
      baseRes.addProperty(RDF.type, WON.MESSAGE);
      baseRes.addProperty(predicate, object);
      return messageModel;
    }

    /**
     * Creates an RDF model containing a feedback message referring to the specified resource
     * that is either positive or negative.
     * @return
     */
    public static Model binaryFeedbackMessage(URI forResource, boolean isFeedbackPositive) {
      Model messageModel = createModelWithBaseResource();
      Resource baseRes = RdfUtils.getBaseResource(messageModel);
      Resource feedbackNode = messageModel.createResource();
      baseRes.addProperty(WON.HAS_FEEDBACK, feedbackNode);
      feedbackNode.addProperty(WON.HAS_BINARY_RATING, isFeedbackPositive ? WON.GOOD : WON.BAD);
      feedbackNode.addProperty(WON.FOR_RESOURCE, messageModel.createResource(forResource.toString()));
      return messageModel;
    }

    /**
     * Returns the first won:hasTextMessage object, or null if none is found.
     * @param model
     * @return
     */
    public static String getTextMessage(Model model){
      Statement stmt = model.getProperty(RdfUtils.getBaseResource(model),WON.HAS_TEXT_MESSAGE);
      if (stmt != null) {
        return stmt.getObject().asLiteral().getLexicalForm();
      }
      return null;
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
      logger.debug("getFacet(model) called");
      Resource baseRes = RdfUtils.getBaseResource(content);
      StmtIterator stmtIterator = baseRes.listProperties(WON.HAS_FACET);
      if (!stmtIterator.hasNext()) {
        logger.debug("no facet found in model");
        return null;
      }
      URI facetURI = URI.create(stmtIterator.next().getObject().asResource().getURI());
      if (logger.isDebugEnabled()){
        if (stmtIterator.hasNext()){
          logger.debug("returning facet {}, but model has more facets than just this one.");
        }
      }
      return facetURI;
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

    /**
     * Adds a triple to the model of the form <> won:hasRemoteFacet [facetURI].
     * @param content
     * @param facetURI
     */
    public static void addRemoteFacet(final Model content, final URI facetURI)
    {
      Resource baseRes = RdfUtils.getBaseResource(content);
      baseRes.addProperty(WON.HAS_REMOTE_FACET, content.createResource(facetURI.toString()));
    }

    /**
     * Creates a model for connecting two facets.
     * @return
     */
    public static Model createFacetModelForHintOrConnect(URI facet, URI remoteFacet)
    {
      Model model = ModelFactory.createDefaultModel();
      Resource baseResource = RdfUtils.findOrCreateBaseResource(model);
      WonRdfUtils.FacetUtils.addFacet(model, facet);
      WonRdfUtils.FacetUtils.addRemoteFacet(model, remoteFacet);
      logger.debug("facet model contains these facets: from:{} to:{}", facet, remoteFacet);
      return model;
    }

  }

  // ToDo (FS): after the whole system has been adapted to the new message format check if the following methods are still in use and if they are make them pretty!
  public static class NeedUtils {

    public static URI queryOwner(Dataset content)
        throws MultipleQueryResultsFoundException
    {

      URI ownerURI = null;
      final String queryString =
          "PREFIX won: <http://purl.org/webofneeds/model#> " +
              "SELECT * { ?s won:hasOwner ?owner }";
      Query query = QueryFactory.create(queryString);
      try (QueryExecution qexec = QueryExecutionFactory.create(query, content)) {
        ResultSet results = qexec.execSelect();
        boolean foundOneResult = false;
        for (; results.hasNext(); ) {
          if (foundOneResult)
            throw new MultipleQueryResultsFoundException();
          foundOneResult = true;
          QuerySolution solution = results.nextSolution();
          Resource r = solution.getResource("owner");
          try {
            ownerURI = new URI(r.getURI());
          } catch (URISyntaxException e) {
            logger.warn("caught URISyntaxException:", e);
            return null;
          }
        }
      }
      return ownerURI;
    }

    public static URI queryWonNode(Dataset content)
        throws MultipleQueryResultsFoundException
    {

      URI wonNodeURI = null;
      final String queryString =
          "PREFIX won: <http://purl.org/webofneeds/model#> " +
              "SELECT * { ?s won:hasWonNode ?wonNode }";
      Query query = QueryFactory.create(queryString);
      try (QueryExecution qexec = QueryExecutionFactory.create(query, content)) {
        ResultSet results = qexec.execSelect();
        boolean foundOneResult = false;
        for (; results.hasNext(); ) {
          if (foundOneResult)
            throw new MultipleQueryResultsFoundException();
          foundOneResult = true;
          QuerySolution solution = results.nextSolution();
          Resource r = solution.getResource("wonNode");
          try {
            wonNodeURI = new URI(r.getURI());
          } catch (URISyntaxException e) {
            logger.warn("caught URISyntaxException:", e);
            return null;
          }
        }
      }
      return wonNodeURI;
    }

    public static Boolean queryActiveStatus(Dataset content)
        throws MultipleQueryResultsFoundException
    {

      logger.debug("queryActiveStatus - content: " + RdfUtils.toString(content));

      Boolean active = null;
      final String queryString =
          "PREFIX won: <http://purl.org/webofneeds/model#> " +
              "SELECT * { ?s won:isInState ?activeState }";
      Query query = QueryFactory.create(queryString);
      try (QueryExecution qexec = QueryExecutionFactory.create(query, content)) {
        ResultSet results = qexec.execSelect();
        boolean foundOneResult = false;
        for (; results.hasNext(); ) {
          if (foundOneResult)
            throw new MultipleQueryResultsFoundException();
          foundOneResult = true;
          QuerySolution solution = results.nextSolution();
          Resource r = solution.getResource("activeState");
          if (r.getURI().equals(NeedState.ACTIVE.getURI()))
            return true;
          else if (r.getURI().equals(NeedState.INACTIVE.getURI()))
            return false;
        }
      }
      return null;
    }
  }

    private static Model createModelWithBaseResource() {
        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("", "no:uri");
        model.createResource(model.getNsPrefixURI(""));
        return model;
    }


}
