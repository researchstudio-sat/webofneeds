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
import won.protocol.message.WonMessage;
import won.protocol.model.Facet;
import won.protocol.model.Match;
import won.protocol.model.NeedState;
import won.protocol.service.WonNodeInfo;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

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

  public static class WonNodeUtils
  {
    /**
     * Creates a WonNodeInfo object based on the specified dataset. The first model
     * found in the dataset that seems to contain the data needed for a WonNodeInfo
     * object is used.
     * @param wonNodeUri
     * @param dataset
     * @return
     */
    public static WonNodeInfo getWonNodeInfo(final URI wonNodeUri, Dataset dataset){
      return RdfUtils.findFirst(dataset, new RdfUtils.ModelVisitor<WonNodeInfo>()
        {
          @Override
          public WonNodeInfo visit(final Model model) {

            //use the first blank node found for [wonNodeUri] won:hasUriPatternSpecification [blanknode]
            NodeIterator it = model.listObjectsOfProperty(model.getResource(wonNodeUri.toString()),
              WON.HAS_URI_PATTERN_SPECIFICATION);
            if (!it.hasNext()) return null;
            WonNodeInfo wonNodeInfo = new WonNodeInfo();
            RDFNode node = it.next();

            // set the URI prefixes
            it = model.listObjectsOfProperty(node.asResource(), WON.HAS_NEED_URI_PREFIX);
            if (! it.hasNext() ) return null;
            wonNodeInfo.setNeedURIPrefix(it.next().asLiteral().getString());
            it = model.listObjectsOfProperty(node.asResource(), WON.HAS_CONNECTION_URI_PREFIX);
            if (! it.hasNext() ) return null;
            wonNodeInfo.setConnectionURIPrefix(it.next().asLiteral().getString());
            it = model.listObjectsOfProperty(node.asResource(), WON.HAS_EVENT_URI_PREFIX);
            if (! it.hasNext() ) return null;
            wonNodeInfo.setEventURIPrefix(it.next().asLiteral().getString());

            // set the need list URI
            it = model.listObjectsOfProperty(model.getResource(wonNodeUri.toString()), WON.HAS_NEED_LIST);
            if (! it.hasNext() ) return null;
            wonNodeInfo.setNeedListURI(it.next().asNode().getURI());

            // set the supported protocol implementations
            String queryString = "SELECT ?protocol ?param ?value WHERE { ?a <%s> ?c. " +
              "?c <%s> ?protocol. ?c ?param ?value. FILTER ( ?value != ?protocol ) }";
            queryString = String.format(queryString, WON.SUPPORTS_WON_PROTOCOL_IMPL.toString(), RDF.getURI() + "type");
            Query protocolQuery = QueryFactory.create(queryString);
            QueryExecution qexec = QueryExecutionFactory.create(protocolQuery, model);

            ResultSet rs = qexec.execSelect();
            while (rs.hasNext()) {
              QuerySolution qs = rs.nextSolution();
              String protocol = qs.get("protocol").toString();
              String param = qs.get("param").toString();
              String value = qs.get("value").toString();
              wonNodeInfo.setSupportedProtocolImplParamValue(protocol, param, value);
            }

            return wonNodeInfo;
          }
      });
    }

  }

  public static class MessageUtils
  {
    /**
     * Creates an RDF model containing a text message.
     * @param message
     * @return
     */
    public static Model textMessage(String message) {
      Model messageModel = createModelWithBaseResource();
      Resource baseRes = messageModel.createResource(messageModel.getNsPrefixURI(""));
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
      baseRes.addProperty(RDF.type, WONMSG.TYPE_CONNECTION_MESSAGE);
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


    /**
     * Converts the specified hint message into a Match object.
     * @param wonMessage
     * @return a match object or null if the message is not a hint message.
     */
    public static Match toMatch(final WonMessage wonMessage) {
      if (!WONMSG.TYPE_HINT.equals(wonMessage.getMessageType())){
        return null;
      }
      Match match = new Match();
      match.setFromNeed(wonMessage.getReceiverNeedURI());
      match.setToNeed(wonMessage.getSenderURI());
      Dataset messageContent = wonMessage.getMessageContent();

      RDFNode score = RdfUtils.findOnePropertyFromResource(messageContent, wonMessage.getMessageURI(),
        WON.HAS_MATCH_SCORE);
      if (!score.isLiteral()) return null;
      match.setScore(score.asLiteral().getDouble());

      RDFNode counterpart = RdfUtils.findOnePropertyFromResource(messageContent, wonMessage.getMessageURI(),
        WON.HAS_MATCH_COUNTERPART);
      if (!counterpart.isResource()) return null;
      match.setToNeed(URI.create(score.asResource().getURI()));
      return match;
    }


  }

  public static class FacetUtils {



    public static URI getFacet(WonMessage message){
      return getObjectOfMessageProperty(message, WON.HAS_FACET);
    }

    public static URI getRemoteFacet(WonMessage message) {
      return getObjectOfMessageProperty(message, WON.HAS_REMOTE_FACET);
    }

    /**
     * Returns a property of the message (i.e. the object of the first triple ( [message-uri] [property] X )
     * found in one of the content graphs of the specified message.
     */
    private static URI getObjectOfMessageProperty(final WonMessage message, final Property property) {
      List<String> contentGraphUris = message.getContentGraphURIs();
      Dataset contentGraphs = message.getMessageContent();
      URI messageURI = message.getMessageURI();
      for (String graphUri: contentGraphUris) {
        Model contentGraph = contentGraphs.getNamedModel(graphUri);
        StmtIterator smtIter = contentGraph.getResource(messageURI.toString()).listProperties(property);
        if (smtIter.hasNext()) {
          return URI.create(smtIter.nextStatement().getObject().asResource().getURI());
        }
      }
      return null;
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

  public static class NeedUtils
  {


    public static URI queryWonNode(Dataset content) {

      URI wonNodeURI = null;
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
      final Model result = ModelFactory.createDefaultModel();

      RdfUtils.visit(dataset,new RdfUtils.ModelVisitor<Object>() {
        @Override
        public Object visit(Model model) {
          result.add(model);
          return null;
        }
      });
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
