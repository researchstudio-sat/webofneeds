package won.protocol.util;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;
import com.hp.hpl.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.exception.DataIntegrityException;
import won.protocol.exception.IncorrectPropertyCountException;
import won.protocol.model.Facet;
import won.protocol.model.NeedState;
import won.protocol.vocabulary.WON;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

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
    public static URI getFacet(URI subject, Model content) {
      logger.debug("getFacet(model) called");
      Resource baseRes = content.getResource(subject.toString());
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
     * Returns the first RemoteFacet found in the model, attached to the specified subject.
     * Returns null if there is no such facet.
     * @param content
     * @return
     */
    public static URI getRemoteFacet(URI subject, Model content) {
      logger.debug("getFacet(model) called");
      Resource baseRes = content.getResource(subject.toString());
      StmtIterator stmtIterator = baseRes.listProperties(WON.HAS_REMOTE_FACET);
      if (!stmtIterator.hasNext()) {
        logger.debug("no RemoteFacet found in model");
        return null;
      }
      URI remoteFacetURI = URI.create(stmtIterator.next().getObject().asResource().getURI());
      if (logger.isDebugEnabled()){
        if (stmtIterator.hasNext()){
          logger.debug("returning RemoteFacet {}, but model has more RemoteFacets than just this one.");
        }
      }
      return remoteFacetURI;
    }

    public static URI getFacet(final URI subject, Dataset content){
      return RdfUtils.findFirst(content, new RdfUtils.ModelVisitor<URI>()
      {
        @Override
        public URI visit(final Model model) {
          return getFacet(subject, model);
        }
      });
    }

    public static URI getRemoteFacet(final URI subject, Dataset content){
      return RdfUtils.findFirst(content, new RdfUtils.ModelVisitor<URI>()
      {
        @Override
        public URI visit(final Model model) {
          return getRemoteFacet(subject, model);
        }
      });
    }

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

    public static URI getFacet(Dataset content){
      return RdfUtils.findFirst(content, new RdfUtils.ModelVisitor<URI>()
      {
        @Override
        public URI visit(final Model model) {
          return getFacet(model);
        }
      });
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

    public static URI queryOwner(Dataset content) {

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
            throw new IncorrectPropertyCountException(1,2);
          foundOneResult = true;
          QuerySolution solution = results.nextSolution();
          Resource r = solution.getResource("owner");
          try {
            ownerURI = new URI(r.getURI());
          } catch (URISyntaxException e) {
            logger.warn("caught URISyntaxException:", e);
            throw new DataIntegrityException("could not parse ownerUri: " + r.getURI(), e);
          }
        }
      }
      return ownerURI;
    }

    public static URI queryWonNode(Dataset content) {

      URI wonNodeURI = null;
      // ToDo (FS): add as much as possible to vocabulary stuff
      final String queryString =
        "PREFIX won: <http://purl.org/webofneeds/model#> " +
          "SELECT * { { ?s won:hasWonNode ?wonNode } UNION { GRAPH ?g { ?s won:hasWonNode ?wonNode } } }";
      Query query = QueryFactory.create(queryString);
      try (QueryExecution qexec = QueryExecutionFactory.create(query, content)) {
        ResultSet results = qexec.execSelect();
        boolean foundOneResult = false;
        for (; results.hasNext(); ) {
          if (foundOneResult)
            throw new IncorrectPropertyCountException(1,2);
          foundOneResult = true;
          QuerySolution solution = results.nextSolution();
          Resource r = solution.getResource("wonNode");
          try {
            wonNodeURI = new URI(r.getURI());
          } catch (URISyntaxException e) {
            logger.warn("caught URISyntaxException:", e);
            throw new DataIntegrityException("could not parse wonNodeUri: " + r.getURI(), e);
          }
        }
      }
      return wonNodeURI;
    }

    public static NeedState queryActiveStatus(Model model, URI needURI) {

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
            throw new IncorrectPropertyCountException("More than one result found, but only one expected", 1,2);
          result = NeedState.ACTIVE;
        } else if (s.getObject().equals(WON.NEED_STATE_INACTIVE)) {
          if (result != null && result.equals(NeedState.ACTIVE))
            throw new IncorrectPropertyCountException("More than one result found, but only one expected", 1,2);
          result = NeedState.INACTIVE;
        }
      }
      return result;
    }

    public static NeedState queryActiveStatus(Dataset content, final URI needURI) {
      return RdfUtils.findOne(content, new RdfUtils.ModelVisitor<NeedState>()
      {
        @Override
        public NeedState visit(final Model model) {
          return queryActiveStatus(model, needURI);
        }
      }, true);
    }

    /**
     * returns a list of Facet objects each set with the NeedURI and the TypeURI
     *
     * @param needURI URI which will be set to the facets
     * @param dataset <code>Dataset</code> object which will be searched for the facets
     * @return list of facets
     */
    public static List<Facet> getFacets(final URI needURI, Dataset dataset) {
      return RdfUtils.visitFlattenedToList(dataset, new RdfUtils.ModelVisitor<List<Facet>>()
      {
        @Override
        public List<Facet> visit(final Model model) {
          return getFacets(needURI, model);
        }
      });
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
    public static URI getNeedURI(Dataset dataset) {
      return RdfUtils.findOne(dataset, new RdfUtils.ModelVisitor<URI>()
      {
        @Override
        public URI visit(final Model model) {
          return getNeedURI(model);
        }
      }, true);
    }
    /**
     * searches for a subject of type won:Need and returns the NeedURI
     *
     * @param model <code>Model</code> object which will be searched for the NeedURI
     * @return <code>URI</code> which is of type won:Need
     */
    public static URI getNeedURI(Model model) {

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
            throw new IncorrectPropertyCountException(1,2);
        }
        return u;
      }
      else
        return null;
    }

    /**
     * return the needURI of a connection
     *
     * @param dataset <code>Dataset</code> object which contains connection information
     * @param connectionURI
     * @return <code>URI</code> of the need
     */
    public static URI getLocalNeedURIFromConnection(Dataset dataset, final URI connectionURI) {
      return URI.create(RdfUtils.findOnePropertyFromResource(
        dataset, connectionURI, WON.BELONGS_TO_NEED).asResource().getURI());
    }

    public static URI getRemoteNeedURIFromConnection(Dataset dataset, final URI connectionURI) {
      return URI.create(RdfUtils.findOnePropertyFromResource(
        dataset, connectionURI, WON.HAS_REMOTE_NEED).asResource().getURI());
    }

    public static URI getWonNodeURIFromConnection(Dataset dataset, final URI connectionURI) {
      return URI.create(RdfUtils.findOnePropertyFromResource(
        dataset, connectionURI, WON.HAS_WON_NODE).asResource().getURI());
    }

    public static URI getRemoteConnectionURIFromConnection(Dataset dataset, final URI connectionURI) {
      return URI.create(RdfUtils.findOnePropertyFromResource(
        dataset, connectionURI, WON.HAS_REMOTE_CONNECTION).asResource().getURI());
    }

    public static URI getWonNodeURIFromNeed(Dataset dataset, final URI needURI) {
      return URI.create(RdfUtils.findOnePropertyFromResource(
        dataset, needURI, WON.HAS_WON_NODE).asResource().getURI());
    }

    /**
     * Extracts all triples from the dataset (which is expected to be a dataset describing
     * one need, expressed in multiple named graphs) and copies them to a new model.
     * @param dataset
     * @return
     */
    public static Model getNeedModelFromNeedDataset(Dataset dataset){
      assert dataset != null : "dataset must not be null";
      Model result = ModelFactory.createDefaultModel();
      Model defaultModel = dataset.getDefaultModel();
      //find the hasGraph triples that should reference graphs in the dataset.
      // Get their data and copy it to the result graph.
      NodeIterator it = defaultModel.listObjectsOfProperty(WON.HAS_GRAPH);
      while(it.hasNext()){
        Model namedModel = dataset.getNamedModel(it.next().toString());
        if (namedModel != null){
          result.add(namedModel);
        }
      }
      return result;
    }

    public static URI queryConnectionContainer(Dataset dataset, final URI needURI) {
      return RdfUtils.findOne(dataset, new RdfUtils.ModelVisitor<URI>()
      {
        @Override
        public URI visit(final Model model) {
          return queryConnectionContainer(model, needURI);
        }
      }, true);
    }

    public static URI queryConnectionContainer(Model model, URI needURI) {

      StmtIterator iterator = model.listStatements(model.createResource(needURI.toString()),
                                                   WON.HAS_CONNECTIONS,
                                                   (RDFNode) null);
      if (!iterator.hasNext()) {
        return null;
      }
      URI result = null;
      while (iterator.hasNext()) {
        Statement s = iterator.nextStatement();
        URI nextURI = URI.create(s.getResource().getURI());
        if (result != null && !result.equals(nextURI))
          throw new IncorrectPropertyCountException(1,2);
        result = nextURI;
      }
      return result;
    }

    public static void removeConnectionContainer(Dataset dataset, final URI needURI) {
      RdfUtils.visit(dataset, new RdfUtils.ModelVisitor<Object>()
      {
        @Override
        public Object visit(final Model model) {
          removeConnectionContainer(model, needURI);
          return null;
        }
      });
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
