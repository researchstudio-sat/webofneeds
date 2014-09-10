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
import won.protocol.model.Facet;
import won.protocol.model.NeedState;
import won.protocol.vocabulary.WON;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

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
  public static class NeedUtils
  {

    public static URI queryOwner(Dataset content)
      throws MultipleQueryResultsFoundException {

      URI ownerURI = null;
      // ToDo (FS): add as much as possible to vocabulary stuff
      final String queryString =
        "PREFIX won: <http://purl.org/webofneeds/model#> " +
          "SELECT * { { ?s won:hasOwner ?owner } UNION { GRAPH ?g { ?s won:hasOwner ?owner } } }";
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
      throws MultipleQueryResultsFoundException {

      URI wonNodeURI = null;
      // ToDo (FS): add as much as possible to vocabulary stuff
      final String queryString =
        "PREFIX won: <http://purl.org/webofneeds/model#> " +
          "SELECT * { { ?s won:hasWonNode ?wonNode } UNION { GRAPH ?g { ?s won:hasWonNode ?wonNod } } }";
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

    public static NeedState queryActiveStatus(Model model, URI needURI)
      throws MultipleQueryResultsFoundException {

      StmtIterator iterator = model.listStatements(model.createResource(needURI.toString()),
                                                   WON.IS_IN_STATE,
                                                   (RDFNode) null);
      if (!iterator.hasNext())
        return null;

      NeedState result = null;
      while (iterator.hasNext()) {
        Statement s = iterator.nextStatement();
        if (s.getObject().equals(WON.NEED_STATE_ACTIVE)) {
          if (result != null && result.equals(NeedState.INACTIVE))
            throw new MultipleQueryResultsFoundException();
          result = NeedState.ACTIVE;
        } else if (s.getObject().equals(WON.NEED_STATE_INACTIVE)) {
          if (result != null && result.equals(NeedState.ACTIVE))
            throw new MultipleQueryResultsFoundException();
          result = NeedState.INACTIVE;
        }
      }
      return result;
    }

    public static NeedState queryActiveStatus(Dataset content, URI needURI)
      throws MultipleQueryResultsFoundException {
      NeedState result = null;
      result = queryActiveStatus(content.getDefaultModel(), needURI);

      Iterator<String> nameIt = content.listNames();
      while (nameIt.hasNext()) {
        NeedState tempResult = queryActiveStatus(content.getNamedModel(nameIt.next()), needURI);
        if (tempResult != null && result != null && !result.equals(tempResult))
          throw new MultipleQueryResultsFoundException();
        result = tempResult;
      }

      return result;
    }

    /**
     * returns a list of Facet objects each set with the NeedURI and the TypeURI
     *
     * @param needURI URI which will be set to the facets
     * @param dataset <code>Dataset</code> object which will be searched for the facets
     * @return list of facets
     */
    public static List<Facet> getFacets(URI needURI, Dataset dataset) {
      List<Facet> result = new ArrayList<Facet>();
      Iterator<String> i = dataset.listNames();
      while (i.hasNext()) {
        result.addAll(getFacets(needURI, dataset.getNamedModel(i.next())));
      }
      result.addAll(getFacets(needURI, dataset.getDefaultModel()));
      return result;
    }

    /**
     * returns a list of Facet objects each set with the NeedURI and the TypeURI
     *
     * @param needURI URI which will be set to the facets
     * @param model <code>Model</code> object which will be searched for the facets
     * @return list of facets
     */
    public static List<Facet> getFacets(URI needURI, Model model) {
      List<Facet> result = new ArrayList<Facet>();

      StmtIterator iterator = model.listStatements(model.createResource(needURI.toString()),
                                                   WON.HAS_FACET,
                                                   (RDFNode) null);
      while (iterator.hasNext()) {
        Facet f = new Facet();
        f.setNeedURI(needURI);
        f.setTypeURI(URI.create(iterator.nextStatement().getObject().asResource().getURI()));
        result.add(f);
      }
      return result;
    }

    /**
     * searches for a subject of type won:Need and returns the NeedURI
     *
     * @param dataset <code>Dataset</code> object which will be searched for the NeedURI
     * @return <code>URI</code> which is of type won:Need
     */
    public static URI getNeedURI(Dataset dataset)
      throws MultipleQueryResultsFoundException {

      List<URI> needURIs = new ArrayList<URI>();

      Iterator<String> i = dataset.listNames();
      while (i.hasNext()) {
        URI newURI = getNeedURI(dataset.getNamedModel(i.next()));
        if (newURI != null)
          needURIs.add(newURI);
      }
      URI newURI = getNeedURI(dataset.getDefaultModel());
      if (newURI != null)
        needURIs.add(newURI);

      if (needURIs.size() == 0)
        return null;
      else if (needURIs.size() == 1)
        return needURIs.get(0);
      else if (needURIs.size() > 1) {
        URI u = needURIs.get(0);
        for (URI uri : needURIs) {
          if (!uri.equals(u))
            throw new MultipleQueryResultsFoundException();
        }
        return u;
      }
      else
        return null;
    }
    /**
     * searches for a subject of type won:Need and returns the NeedURI
     *
     * @param model <code>Model</code> object which will be searched for the NeedURI
     * @return <code>URI</code> which is of type won:Need
     */
    public static URI getNeedURI(Model model)
      throws MultipleQueryResultsFoundException {

      List<URI> needURIs = new ArrayList<URI>();

      ResIterator iterator = model.listSubjectsWithProperty(RDF.type, WON.NEED);
      while (iterator.hasNext()) {
        needURIs.add(URI.create(iterator.next().getURI()));
      }

      if (needURIs.size() == 0)
        return null;
      else if (needURIs.size() == 1)
        return needURIs.get(0);
      else if (needURIs.size() > 1) {
        URI u = needURIs.get(0);
        for (URI uri : needURIs) {
          if (!uri.equals(u))
            throw new MultipleQueryResultsFoundException();
        }
        return u;
      }
      else
        return null;
    }

    public static URI queryConnectionContainer(Dataset dataset, URI needURI)
      throws MultipleQueryResultsFoundException {

      URI result = null;
      result = queryConnectionContainer(dataset.getDefaultModel(), needURI);

      Iterator<String> nameIt = dataset.listNames();
      while (nameIt.hasNext()) {
        URI tempResult = queryConnectionContainer(dataset.getNamedModel(nameIt.next()), needURI);
        if (tempResult != null && result != null && !result.equals(tempResult))
          throw new MultipleQueryResultsFoundException();
        result = tempResult;
      }
      return result;
    }

    public static URI queryConnectionContainer(Model model, URI needURI)
      throws MultipleQueryResultsFoundException {

      StmtIterator iterator = model.listStatements(model.createResource(needURI.toString()),
                                                   WON.HAS_CONNECTIONS,
                                                   (RDFNode) null);
      if (!iterator.hasNext())
        return null;

      URI result = null;
      while (iterator.hasNext()) {
        Statement s = iterator.nextStatement();
        URI nextURI = URI.create(s.getResource().getURI());
        if (result != null && !result.equals(nextURI))
          throw new MultipleQueryResultsFoundException();
        result = nextURI;
      }
      return result;
    }

    public static void removeConnectionContainer(Dataset dataset, URI needURI) {
      removeConnectionContainer(dataset.getDefaultModel(), needURI);
      Iterator<String> nameIt = dataset.listNames();
      while (nameIt.hasNext()) {
        removeConnectionContainer(dataset.getNamedModel(nameIt.next()), needURI);
      }
    }

    public static void removeConnectionContainer(Model model, URI needURI) {

      StmtIterator iterator = model.listStatements(model.createResource(needURI.toString()),
                                                   WON.HAS_CONNECTIONS,
                                                   (RDFNode) null);

      URI result = null;
      while (iterator.hasNext()) {
        model.remove(iterator.nextStatement());
      }
    }

  }

  private static Model createModelWithBaseResource() {
      Model model = ModelFactory.createDefaultModel();
      model.setNsPrefix("", "no:uri");
      model.createResource(model.getNsPrefixURI(""));
      return model;
    }
}
