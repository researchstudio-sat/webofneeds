package won.protocol.util;

import static won.protocol.util.RdfUtils.findOnePropertyFromResource;
import static won.protocol.util.RdfUtils.findOrCreateBaseResource;
import static won.protocol.util.RdfUtils.visit;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpGraph;
import org.apache.jena.sparql.algebra.op.OpProject;
import org.apache.jena.sparql.algebra.op.OpUnion;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathParser;
import org.apache.jena.tdb.TDB;
import org.apache.jena.vocabulary.RDF;
import org.hibernate.cfg.NotYetImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.protocol.exception.IncorrectPropertyCountException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonSignatureData;
import won.protocol.model.ConnectionState;
import won.protocol.model.Match;
import won.protocol.model.NeedGraphType;
import won.protocol.service.WonNodeInfo;
import won.protocol.service.WonNodeInfoBuilder;
import won.protocol.vocabulary.SCHEMA;
import won.protocol.vocabulary.SFSIG;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONAGR;
import won.protocol.vocabulary.WONMOD;
import won.protocol.vocabulary.WONMSG;

/**
 * Utilities for populating/manipulating the RDF models used throughout the WON
 * application.
 */
public class WonRdfUtils {

  public static final String NAMED_GRAPH_SUFFIX = "#data";

  private static final Logger logger = LoggerFactory.getLogger(WonRdfUtils.class);

  public static class SignatureUtils {
    public static boolean isSignatureGraph(String graphUri, Model model) {
      // TODO check the presence of all the required triples
      Resource resource = model.getResource(graphUri);
      StmtIterator si = model.listStatements(resource, RDF.type, SFSIG.SIGNATURE);
      if (si.hasNext()) {
        return true;
      }
      return false;
    }

    public static boolean isSignature(Model model, String modelName) {
      // TODO check the presence of all the required triples
      return model.contains(model.getResource(modelName), RDF.type, SFSIG.SIGNATURE);
    }

    public static String getSignedGraphUri(String signatureGraphUri, Model signatureGraph) {
      String signedGraphUri = null;
      Resource resource = signatureGraph.getResource(signatureGraphUri);
      NodeIterator ni = signatureGraph.listObjectsOfProperty(resource, WONMSG.HAS_SIGNED_GRAPH_PROPERTY);
      if (ni.hasNext()) {
        signedGraphUri = ni.next().asResource().getURI();
      }
      return signedGraphUri;
    }

    public static String getSignatureValue(String signatureGraphUri, Model signatureGraph) {
      String signatureValue = null;
      Resource resource = signatureGraph.getResource(signatureGraphUri);
      NodeIterator ni2 = signatureGraph.listObjectsOfProperty(resource, SFSIG.HAS_SIGNATURE_VALUE);
      if (ni2.hasNext()) {
        signatureValue = ni2.next().asLiteral().toString();
      }
      return signatureValue;
    }

    public static WonSignatureData extractWonSignatureData(final String uri, final Model model) {
      return extractWonSignatureData(model.getResource(uri));
    }

    public static WonSignatureData extractWonSignatureData(final Resource resource) {
      Statement stmt = resource.getRequiredProperty(WONMSG.HAS_SIGNED_GRAPH_PROPERTY);
      String signedGraphUri = stmt.getObject().asResource().getURI();
      stmt = resource.getRequiredProperty(SFSIG.HAS_SIGNATURE_VALUE);
      String signatureValue = stmt.getObject().asLiteral().getString();
      stmt = resource.getRequiredProperty(WONMSG.HAS_HASH_PROPERTY);
      String hash = stmt.getObject().asLiteral().getString();
      stmt = resource.getRequiredProperty(WONMSG.HAS_PUBLIC_KEY_FINGERPRINT_PROPERTY);
      String fingerprint = stmt.getObject().asLiteral().getString();
      stmt = resource.getRequiredProperty(SFSIG.HAS_VERIFICATION_CERT);
      String cert = stmt.getObject().asResource().getURI();
      return new WonSignatureData(signedGraphUri, resource.getURI(), signatureValue, hash, fingerprint, cert);
    }

    /**
     * Adds the triples holding the signature data to the model of the specified
     * resource, using the resource as the subject.
     * 
     * @param subject
     * @param wonSignatureData
     */
    public static void addSignature(Resource subject, WonSignatureData wonSignatureData) {
      assert wonSignatureData.getHash() != null;
      assert wonSignatureData.getSignatureValue() != null;
      assert wonSignatureData.getPublicKeyFingerprint() != null;
      assert wonSignatureData.getSignedGraphUri() != null;
      assert wonSignatureData.getVerificationCertificateUri() != null;
      Model containingGraph = subject.getModel();
      subject.addProperty(RDF.type, SFSIG.SIGNATURE);
      subject.addProperty(WONMSG.HAS_HASH_PROPERTY, wonSignatureData.getHash());
      subject.addProperty(SFSIG.HAS_SIGNATURE_VALUE, wonSignatureData.getSignatureValue());
      subject.addProperty(WONMSG.HAS_SIGNED_GRAPH_PROPERTY,
          containingGraph.createResource(wonSignatureData.getSignedGraphUri()));
      subject.addProperty(WONMSG.HAS_PUBLIC_KEY_FINGERPRINT_PROPERTY, wonSignatureData.getPublicKeyFingerprint());
      subject.addProperty(SFSIG.HAS_VERIFICATION_CERT,
          containingGraph.createResource(wonSignatureData.getVerificationCertificateUri()));
    }
  }

  public static class WonNodeUtils {
    /**
     * Creates a WonNodeInfo object based on the specified dataset. The first model
     * found in the dataset that seems to contain the data needed for a WonNodeInfo
     * object is used.
     * 
     * @param wonNodeUri
     * @param dataset
     * @return
     */
    public static WonNodeInfo getWonNodeInfo(final URI wonNodeUri, Dataset dataset) {
      assert wonNodeUri != null : "wonNodeUri must not be null";
      assert dataset != null : "dataset must not be null";
      return RdfUtils.findFirst(dataset, new RdfUtils.ModelVisitor<WonNodeInfo>() {
        @Override
        public WonNodeInfo visit(final Model model) {

          // use the first blank node found for [wonNodeUri]
          // won:hasUriPatternSpecification [blanknode]
          NodeIterator it = model.listObjectsOfProperty(model.getResource(wonNodeUri.toString()),
              WON.HAS_URI_PATTERN_SPECIFICATION);
          if (!it.hasNext())
            return null;
          WonNodeInfoBuilder wonNodeInfoBuilder = new WonNodeInfoBuilder();

          wonNodeInfoBuilder.setWonNodeURI(wonNodeUri.toString());
          RDFNode node = it.next();

          // set the URI prefixes

          it = model.listObjectsOfProperty(node.asResource(), WON.HAS_NEED_URI_PREFIX);
          if (!it.hasNext())
            return null;
          String needUriPrefix = it.next().asLiteral().getString();
          wonNodeInfoBuilder.setNeedURIPrefix(needUriPrefix);
          it = model.listObjectsOfProperty(node.asResource(), WON.HAS_CONNECTION_URI_PREFIX);
          if (!it.hasNext())
            return null;
          wonNodeInfoBuilder.setConnectionURIPrefix(it.next().asLiteral().getString());
          it = model.listObjectsOfProperty(node.asResource(), WON.HAS_EVENT_URI_PREFIX);
          if (!it.hasNext())
            return null;
          wonNodeInfoBuilder.setEventURIPrefix(it.next().asLiteral().getString());

          // set the need list URI
          it = model.listObjectsOfProperty(model.getResource(wonNodeUri.toString()), WON.HAS_NEED_LIST);
          if (it.hasNext()) {
            wonNodeInfoBuilder.setNeedListURI(it.next().asNode().getURI());
          } else {
            wonNodeInfoBuilder.setNeedListURI(needUriPrefix);
          }

          // set the supported protocol implementations
          String queryString = "SELECT ?protocol ?param ?value WHERE { ?a <%s> ?c. "
              + "?c <%s> ?protocol. ?c ?param ?value. FILTER ( ?value != ?protocol ) }";
          queryString = String.format(queryString, WON.SUPPORTS_WON_PROTOCOL_IMPL.toString(), RDF.getURI() + "type");
          Query protocolQuery = QueryFactory.create(queryString);
          try (QueryExecution qexec = QueryExecutionFactory.create(protocolQuery, model)) {

            ResultSet rs = qexec.execSelect();
            while (rs.hasNext()) {
              QuerySolution qs = rs.nextSolution();

              String protocol = rdfNodeToString(qs.get("protocol"));
              String param = rdfNodeToString(qs.get("param"));
              String value = rdfNodeToString(qs.get("value"));
              wonNodeInfoBuilder.addSupportedProtocolImplParamValue(protocol, param, value);
            }

            return wonNodeInfoBuilder.build();
          }
        }
      });
    }

    private static String rdfNodeToString(RDFNode node) {
      if (node.isLiteral()) {
        return node.asLiteral().getString();
      } else if (node.isResource()) {
        return node.asResource().getURI();
      }
      return null;
    }

  }

  public static class MessageUtils {
    /**
     * Adds the specified text as a won:hasTextMessage to the model's base resource.
     * 
     * @param message
     * @return
     */
    public static Model addMessage(Model model, String message) {
      Resource baseRes = RdfUtils.findOrCreateBaseResource(model);
      baseRes.addProperty(WON.HAS_TEXT_MESSAGE, message, XSDDatatype.XSDstring);
      return model;
    }

    /**
     * Creates an RDF model containing a text message.
     * 
     * @param message
     * @return
     */
    public static Model textMessage(String message) {
      Model messageModel = createModelWithBaseResource();
      Resource baseRes = messageModel.createResource(messageModel.getNsPrefixURI(""));
      baseRes.addProperty(WON.HAS_TEXT_MESSAGE, message, XSDDatatype.XSDstring);
      return messageModel;
    }

    /**
     * Create an RDF model containing a text message and a processing message
     * 
     * @param message
     * @return
     */
    public static Model processingMessage(String message) {
      Model messageModel = textMessage(message);
      return addProcessing(messageModel, message);
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

    public static Model addToMessage(Model messageModel, Property predicate, Resource object) {
      Resource baseRes = RdfUtils.findOrCreateBaseResource(messageModel);
      baseRes.addProperty(predicate, object);
      return messageModel;
    }

    public static Model retractsMessage(URI... toRetract) {
      return addRetracts(createModelWithBaseResource(), toRetract);
    }

    public static Model proposesMessage(URI... toPropose) {
      return addProposes(createModelWithBaseResource(), toPropose);
    }

    public static Model rejectMessage(URI... toReject) {
      return addRejects(createModelWithBaseResource(), toReject);
    }

    public static Model acceptsMessage(URI... toAccept) {
      return addAccepts(createModelWithBaseResource(), toAccept);
    }

    public static Model proposesToCancelMessage(URI... toProposesToCancel) {
      return addProposesToCancel(createModelWithBaseResource(), toProposesToCancel);
    }

    public static Model addRetracts(Model messageModel, URI... toRetract) {
      Resource baseRes = RdfUtils.findOrCreateBaseResource(messageModel);
      if (toRetract == null)
        return messageModel;
      for (URI uri : toRetract) {
        if (uri != null) {
          baseRes.addProperty(WONMOD.RETRACTS, baseRes.getModel().getResource(uri.toString()));
        }
      }
      return messageModel;
    }

    public static Model addProposes(Model messageModel, URI... toPropose) {
      Resource baseRes = RdfUtils.findOrCreateBaseResource(messageModel);
      if (toPropose == null)
        return messageModel;
      for (URI uri : toPropose) {
        if (uri != null) {
          baseRes.addProperty(WONAGR.PROPOSES, baseRes.getModel().getResource(uri.toString()));
        }
      }
      return messageModel;
    }

    public static Model addRejects(Model messageModel, URI... toReject) {
      Resource baseRes = RdfUtils.findOrCreateBaseResource(messageModel);
      if (toReject == null)
        return messageModel;
      for (URI uri : toReject) {
        if (uri != null) {
          baseRes.addProperty(WONAGR.REJECTS, baseRes.getModel().getResource(uri.toString()));
        }
      }
      return messageModel;
    }

    public static Model addAccepts(Model messageModel, URI... toAccept) {
      Resource baseRes = RdfUtils.findOrCreateBaseResource(messageModel);
      if (toAccept == null)
        return messageModel;
      for (URI uri : toAccept) {
        if (uri != null) {
          if (logger.isDebugEnabled()) {
            logger.debug("checking uri for addProposesToCancel{} with uri {} ({} of {})", new Object[] { uri });
          }
          baseRes.addProperty(WONAGR.ACCEPTS, baseRes.getModel().getResource(uri.toString()));
        }
      }
      return messageModel;
    }

    public static Model addProposesToCancel(Model messageModel, URI... toProposesToCancel) {
      Resource baseRes = RdfUtils.findOrCreateBaseResource(messageModel);
      if (toProposesToCancel == null)
        return messageModel;
      for (URI uri : toProposesToCancel) {
        if (uri != null) {
          if (logger.isDebugEnabled()) {
            logger.debug("checking uri for addProposesToCancel{} with uri {} ({} of {})", new Object[] { uri });
          }
          baseRes.addProperty(WONAGR.PROPOSES_TO_CANCEL, baseRes.getModel().getResource(uri.toString()));
        }
      }
      return messageModel;
    }

    /**
     * Creates an RDF model containing a feedback message referring to the specified
     * resource that is either positive or negative.
     * 
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
     * Returns the first won:hasTextMessage object, or null if none is found. Won't
     * work on WonMessage models, removal depends on refactoring of BA facet code
     * 
     * @param model
     * @return
     */
    @Deprecated
    public static String getTextMessage(Model model) {
      Statement stmt = model.getProperty(RdfUtils.getBaseResource(model), WON.HAS_TEXT_MESSAGE);
      if (stmt != null) {
        return stmt.getObject().asLiteral().getLexicalForm();
      }
      return null;
    }

    /**
     * Returns all won:hasTextMessage objects, or an empty set if none is found. The
     * specified model has to be a message's content graph.
     * 
     * @param model
     * @return
     */
    public static Set<String> getTextMessages(Model model, URI messageUri) {
      Set<String> ret = new HashSet<>();
      StmtIterator stmtIt = model.listStatements(model.getResource(messageUri.toString()), WON.HAS_TEXT_MESSAGE,
          (RDFNode) null);
      while (stmtIt.hasNext()) {
        RDFNode node = stmtIt.next().getObject();
        if (node.isLiteral()) {
          ret.add(node.asLiteral().getLexicalForm());
        }
      }
      return ret;
    }

    /**
     * Returns the first won:hasTextMessage object, or null if none is found. tries
     * the message, its corresponding remote message, and any forwarded message, if
     * any of those are contained in the dataset
     *
     * @param wonMessage
     * @return
     */
    public static String getTextMessage(final WonMessage wonMessage) {
      URI messageURI = wonMessage.getMessageURI();

      // find the text message in the message, the remote message, or any forwarded
      // message
      String queryString = "prefix msg: <http://purl.org/webofneeds/message#>\n"
          + "prefix won: <http://purl.org/webofneeds/model#>\n" + "\n" + "SELECT distinct ?txt WHERE {\n" + "  {\n"
          + "    graph ?gA { ?msg won:hasTextMessage ?txt }\n" + "  } union {\n"
          + "    graph ?gB { ?msg msg:hasCorrespondingRemoteMessage ?msg2 }\n"
          + "    graph ?gA { ?msg2 won:hasTextMessage ?txt }\n" + "  } union {\n"
          + "    graph ?gC { ?msg msg:hasForwardedMessage ?msg2 }\n"
          + "    graph ?gB { ?msg2 msg:hasCorrespondingRemoteMessage ?msg3 }\n"
          + "    graph ?gA { ?msg3 won:hasTextMessage ?txt }\n" + "  } union {\n"
          + "    graph ?gD { ?msg msg:hasCorrespondingRemoteMessage ?msg2 }\n"
          + "    graph ?gC { ?msg2 msg:hasForwardedMessage ?msg3 }\n"
          + "    graph ?gB { ?msg3 msg:hasCorrespondingRemoteMessage ?msg4 }\n"
          + "    graph ?gA { ?msg4 won:hasTextMessage ?txt }\n" + "  } union {\n"
          + "    graph ?gE { ?msg msg:hasForwardedMessage ?msg2 }\n"
          + "    graph ?gD { ?msg2 msg:hasCorrespondingRemoteMessage ?msg3 }\n"
          + "    graph ?gC { ?msg3 msg:hasForwardedMessage ?msg4 }\n"
          + "    graph ?gB { ?msg4 msg:hasCorrespondingRemoteMessage ?msg5 }\n"
          + "    graph ?gA { ?msg5 won:hasTextMessage ?txt }\n" + "  } union {\n"
          + "    graph ?gF { ?msg msg:hasCorrespondingRemoteMessage ?msg2 }\n"
          + "    graph ?gE { ?msg2 msg:hasForwardedMessage ?msg3 }\n"
          + "    graph ?gD { ?msg3 msg:hasCorrespondingRemoteMessage ?msg4 }\n"
          + "    graph ?gC { ?msg4 msg:hasForwardedMessage ?msg5 }\n"
          + "    graph ?gB { ?msg5 msg:hasCorrespondingRemoteMessage ?msg6 }\n"
          + "    graph ?gA { ?msg6 won:hasTextMessage ?txt }\n" + "  } union {\n"
          + "    graph ?gG { ?msg msg:hasForwardedMessage ?msg2 }\n"
          + "    graph ?gF { ?msg2 msg:hasCorrespondingRemoteMessage ?msg3 }\n"
          + "    graph ?gE { ?msg3 msg:hasForwardedMessage ?msg4 }\n"
          + "    graph ?gD { ?msg4 msg:hasCorrespondingRemoteMessage ?msg5 }\n"
          + "    graph ?gC { ?msg5 msg:hasForwardedMessage ?msg6 }\n"
          + "    graph ?gB { ?msg6 msg:hasCorrespondingRemoteMessage ?msg7 }\n"
          + "    graph ?gA { ?msg7 won:hasTextMessage ?txt }\n" + "  }\n" + "\n" + "}";
      Query query = QueryFactory.create(queryString);

      QuerySolutionMap initialBinding = new QuerySolutionMap();
      Model tmpModel = ModelFactory.createDefaultModel();
      initialBinding.add("msg", tmpModel.getResource(messageURI.toString()));
      try (QueryExecution qexec = QueryExecutionFactory.create(query, wonMessage.getCompleteDataset())) {
        qexec.getContext().set(TDB.symUnionDefaultGraph, true);
        ResultSet rs = qexec.execSelect();
        if (rs.hasNext()) {
          QuerySolution qs = rs.nextSolution();
          String textMessage = rdfNodeToString(qs.get("txt"));
          if (rs.hasNext()) {
            // TODO as soon as we have use cases for multiple messages, we need to refactor
            // this
            throw new IllegalArgumentException("wonMessage has more than one text messages");
          }
          return textMessage;
        }
      }
      return null;
    }

    public static List<URI> getAcceptedEvents(final WonMessage wonMessage) {
      return getAcceptedEvents(wonMessage.getCompleteDataset());
    }

    public static List<URI> getAcceptedEvents(final Dataset messageDataset) {
      List<URI> acceptedEvents = new ArrayList<>();
      String queryString = "prefix msg:   <http://purl.org/webofneeds/message#>\n"
          + "prefix agr:   <http://purl.org/webofneeds/agreement#>\n" + "SELECT ?eventUri where {\n" + " graph ?g {"
          + "  ?s agr:accepts ?eventUri .\n" + "}}";
      Query query = QueryFactory.create(queryString);

      try (QueryExecution qexec = QueryExecutionFactory.create(query, messageDataset)) {
        qexec.getContext().set(TDB.symUnionDefaultGraph, true);
        ResultSet rs = qexec.execSelect();
        if (rs.hasNext()) {
          QuerySolution qs = rs.nextSolution();
          String eventUri = rdfNodeToString(qs.get("eventUri"));

          if (eventUri != null) {
            acceptedEvents.add(URI.create(eventUri));
          }
        }
      }
      return acceptedEvents;
    }

    public static boolean isProcessingMessage(final WonMessage wonMessage) {
      String queryString = "prefix msg:   <http://purl.org/webofneeds/message#>\n"
          + "prefix won:   <http://purl.org/webofneeds/model#>\n" + "SELECT ?text where {\n" + " graph ?g {"
          + "  ?s won:isProcessing ?text .\n" + "}}";
      Query query = QueryFactory.create(queryString);

      try (QueryExecution qexec = QueryExecutionFactory.create(query, wonMessage.getCompleteDataset())) {
        qexec.getContext().set(TDB.symUnionDefaultGraph, true);
        ResultSet rs = qexec.execSelect();
        if (rs.hasNext()) {
          QuerySolution qs = rs.nextSolution();
          String text = rdfNodeToString(qs.get("text"));

          if (text != null) {
            return true;
          }
        }
      }
      return false;
    }

    /**
     * Adds the specified text as a won:hasTextMessage to the model's base resource.
     * 
     * @param message
     * @return
     */
    public static Model addProcessing(Model model, String message) {
      Resource baseRes = RdfUtils.findOrCreateBaseResource(model);
      baseRes.addProperty(WON.IS_PROCESSING, message, XSDDatatype.XSDstring);
      return model;
    }

    public static List<URI> getProposesEvents(final WonMessage wonMessage) {
      return getProposesEvents(wonMessage.getCompleteDataset());
    }

    public static List<URI> getProposesEvents(final Dataset messageDataset) {
      List<URI> proposesToCancelEvents = new ArrayList<>();
      String queryString = "prefix msg:   <http://purl.org/webofneeds/message#>\n"
          + "prefix agr:   <http://purl.org/webofneeds/agreement#>\n" + "SELECT ?eventUri where {\n" + " graph ?g {"
          + "  ?s agr:proposes ?eventUri .\n" + "}}";
      Query query = QueryFactory.create(queryString);

      try (QueryExecution qexec = QueryExecutionFactory.create(query, messageDataset)) {
        qexec.getContext().set(TDB.symUnionDefaultGraph, true);
        ResultSet rs = qexec.execSelect();
        if (rs.hasNext()) {
          QuerySolution qs = rs.nextSolution();
          String eventUri = rdfNodeToString(qs.get("eventUri"));

          if (eventUri != null) {
            proposesToCancelEvents.add(URI.create(eventUri));
          }
        }
      }
      return proposesToCancelEvents;
    }

    public static List<URI> getProposesToCancelEvents(final WonMessage wonMessage) {
      return getProposesToCancelEvents(wonMessage.getCompleteDataset());
    }

    public static List<URI> getProposesToCancelEvents(final Dataset messageDataset) {
      List<URI> proposesToCancelEvents = new ArrayList<>();
      String queryString = "prefix msg:   <http://purl.org/webofneeds/message#>\n"
          + "prefix agr:   <http://purl.org/webofneeds/agreement#>\n" + "SELECT ?eventUri where {\n" + " graph ?g {"
          + "  ?s agr:proposesToCancel ?eventUri .\n" + "}}";
      Query query = QueryFactory.create(queryString);

      try (QueryExecution qexec = QueryExecutionFactory.create(query, messageDataset)) {
        qexec.getContext().set(TDB.symUnionDefaultGraph, true);
        ResultSet rs = qexec.execSelect();
        if (rs.hasNext()) {
          QuerySolution qs = rs.nextSolution();
          String eventUri = rdfNodeToString(qs.get("eventUri"));

          if (eventUri != null) {
            proposesToCancelEvents.add(URI.create(eventUri));
          }
        }
      }
      return proposesToCancelEvents;
    }

    /**
     * Returns previous message URIs for local and remote message.
     * 
     * @param wonMessage
     * @return
     */
    public static List<URI> getPreviousMessageUrisIncludingRemote(final WonMessage wonMessage) {
      List<URI> uris = new ArrayList<>();
      String queryString = "prefix msg:   <http://purl.org/webofneeds/message#>\n"
          + "prefix agr:   <http://purl.org/webofneeds/agreement#>\n" + "SELECT distinct ?prev where {\n" + "   {"
          + "    ?msg msg:hasPreviousMessage ?prev .\n" + "   } union {"
          + "    ?msg msg:hasCorrespondingRemoteMessage/msg:hasPreviousMessage ?prev " + "  }" + "}";
      Query query = QueryFactory.create(queryString);

      try (QueryExecution qexec = QueryExecutionFactory.create(query, wonMessage.getCompleteDataset())) {
        qexec.getContext().set(TDB.symUnionDefaultGraph, true);
        QuerySolutionMap binding = new QuerySolutionMap();
        binding.add("msg", new ResourceImpl(wonMessage.getMessageURI().toString()));
        qexec.setInitialBinding(binding);
        ResultSet rs = qexec.execSelect();
        if (rs.hasNext()) {
          QuerySolution qs = rs.nextSolution();
          String eventUri = rdfNodeToString(qs.get("prev"));
          if (eventUri != null) {
            uris.add(URI.create(eventUri));
          }
        }
      }
      return uris;
    }

    /**
     * Returns the whole review content of a WonMessage
     * 
     * @param wonMessage
     * @return
     */
    public static Map<Property, String> getReviewContent(final WonMessage wonMessage) throws IllegalArgumentException {

      System.out.println("message content: ");
      RDFDataMgr.write(System.out, wonMessage.getMessageContent(), Lang.TRIG);
      System.out.println("whole message: ");
      RDFDataMgr.write(System.out, wonMessage.getCompleteDataset(), Lang.TRIG);

      // find the review data in a wonMessage
      String queryString = "prefix s: <http://schema.org/>\n" + "select * where \n" + "  {graph ?g {\n"
          + "    ?event s:review ?review .\n" + "    ?review s:reviewRating ?rating;\n" + "        s:about ?about;\n"
          + "        s:author ?author .\n" + "    ?rating a s:Rating;\n" + "        s:ratingValue ?ratingValue .\n"
          + "\n" + "}}";
      Query query = QueryFactory.create(queryString);

      try (QueryExecution qexec = QueryExecutionFactory.create(query, wonMessage.getCompleteDataset())) {
        qexec.getContext().set(TDB.symUnionDefaultGraph, true);
        ResultSet rs = qexec.execSelect();
        if (rs.hasNext()) {
          QuerySolution qs = rs.nextSolution();
          Map<Property, String> reviewData = new HashMap<Property, String>();
          reviewData.put(SCHEMA.REVIEW, rdfNodeToString(qs.get("review")));
          reviewData.put(SCHEMA.RATING, rdfNodeToString(qs.get("rating")));
          reviewData.put(SCHEMA.ABOUT, rdfNodeToString(qs.get("about")));
          reviewData.put(SCHEMA.AUTHOR, rdfNodeToString(qs.get("author")));
          reviewData.put(SCHEMA.RATING_VALUE, rdfNodeToString(qs.get("ratingValue")));
          if (rs.hasNext()) {
            // TODO as soon as we have use cases for multiple reviews, we need to refactor
            // this
            throw new IllegalArgumentException("wonMessage has more than one review");
          }
          return reviewData;
        }
      }
      return null;
    }

    private static String rdfNodeToString(RDFNode node) {
      if (node.isLiteral()) {
        return node.asLiteral().getString();
      } else if (node.isResource()) {
        return node.asResource().getURI();
      }
      return null;
    }

    private static RDFNode getTextMessageForResource(Dataset dataset, URI uri) {
      if (uri == null)
        return null;
      return RdfUtils.findFirstPropertyFromResource(dataset, uri, WON.HAS_TEXT_MESSAGE);
    }

    private static RDFNode getTextMessageForResource(Dataset dataset, Resource resource) {
      if (resource == null)
        return null;
      return RdfUtils.findFirstPropertyFromResource(dataset, resource, WON.HAS_TEXT_MESSAGE);
    }

    /**
     * Converts the specified hint message into a Match object.
     * 
     * @param wonMessage
     * @return a match object or null if the message is not a hint message.
     */
    public static Match toMatch(final WonMessage wonMessage) {
      if (!WONMSG.TYPE_HINT.equals(wonMessage.getMessageType().getResource())) {
        return null;
      }
      Match match = new Match();
      match.setFromNeed(wonMessage.getReceiverNeedURI());

      Dataset messageContent = wonMessage.getMessageContent();

      RDFNode score = findOnePropertyFromResource(messageContent, wonMessage.getMessageURI(), WON.HAS_MATCH_SCORE);
      if (!score.isLiteral())
        return null;
      match.setScore(score.asLiteral().getDouble());

      RDFNode counterpart = findOnePropertyFromResource(messageContent, wonMessage.getMessageURI(),
          WON.HAS_MATCH_COUNTERPART);
      if (!counterpart.isResource())
        return null;
      match.setToNeed(URI.create(counterpart.asResource().getURI()));
      return match;
    }

    public static WonMessage copyByDatasetSerialization(final WonMessage toWrap) {
      WonMessage copied = new WonMessage(RdfUtils
          .readDatasetFromString(RdfUtils.writeDatasetToString(toWrap.getCompleteDataset(), Lang.TRIG), Lang.TRIG));
      return copied;
    }

  }

  public static class FacetUtils {

    /**
     * Returns the facet in a connect message. Attempts to get it from the specified
     * message itself. If no such facet is found there, the remoteFacet of the
     * correspondingRemoteMessage is used.
     * 
     * @param message
     * @return
     */
    public static URI getFacet(WonMessage message) {
      if (message.getEnvelopeType() == WonMessageDirection.FROM_EXTERNAL) {
        return message.getReceiverFacetURI();
      } else {
        return message.getSenderFacetURI();
      }
    }

    /**
     * Returns the remoteFacet in a connect message. Attempts to get it from the
     * specified message itself. If no such facet is found there, the facet of the
     * correspondingRemoteMessage is used.
     * 
     * @param message
     * @return
     */
    public static URI getRemoteFacet(WonMessage message) {
      if (message.getEnvelopeType() == WonMessageDirection.FROM_EXTERNAL) {
        return message.getSenderFacetURI();
      } else {
        return message.getReceiverFacetURI();
      }
    }

    /**
     * Returns a property of the message (i.e. the object of the first triple (
     * [message-uri] [property] X ) found in one of the content graphs of the
     * specified message.
     */
    private static URI getObjectOfMessageProperty(final WonMessage message, final Property property) {
      List<String> contentGraphUris = message.getContentGraphURIs();
      Dataset contentGraphs = message.getMessageContent();
      URI messageURI = message.getMessageURI();
      for (String graphUri : contentGraphUris) {
        Model contentGraph = contentGraphs.getNamedModel(graphUri);
        StmtIterator smtIter = contentGraph.getResource(messageURI.toString()).listProperties(property);
        if (smtIter.hasNext()) {
          return URI.create(smtIter.nextStatement().getObject().asResource().getURI());
        }
      }
      return null;
    }

    /**
     * Returns a property of the corresponding remote message (i.e. the object of
     * the first triple ( [corresponding-remote-message-uri] [property] X ) found in
     * one of the content graphs of the specified message.
     */
    private static URI getObjectOfRemoteMessageProperty(final WonMessage message, final Property property) {
      List<String> contentGraphUris = message.getContentGraphURIs();
      Dataset contentGraphs = message.getMessageContent();
      URI messageURI = message.getCorrespondingRemoteMessageURI();
      if (messageURI != null) {
        for (String graphUri : contentGraphUris) {
          Model contentGraph = contentGraphs.getNamedModel(graphUri);
          StmtIterator smtIter = contentGraph.getResource(messageURI.toString()).listProperties(property);
          if (smtIter.hasNext()) {
            return URI.create(smtIter.nextStatement().getObject().asResource().getURI());
          }
        }
      }
      return null;
    }

    /**
     * Returns all facets found in the model, attached to the null relative URI
     * '<>'. Returns an empty collection if there is no such facet.
     * 
     * @param content
     * @return
     */
    public static Collection<URI> getFacets(Model content) {
      Resource baseRes = RdfUtils.getBaseResource(content);
      StmtIterator stmtIterator = baseRes.listProperties(WON.HAS_FACET);
      LinkedList<URI> ret = new LinkedList<URI>();
      while (stmtIterator.hasNext()) {
        RDFNode object = stmtIterator.nextStatement().getObject();
        if (object.isURIResource()) {
          ret.add(URI.create(object.asResource().getURI()));
        }
      }
      return ret;
    }

    /**
     * Returns all facets found in the model, attached to the null relative URI
     * '<>'. Returns an empty collection if there is no such facet.
     * 
     * @param content
     * @return
     */
    public static Optional<URI> getTypeOfFacet(Model content, URI facet) {
      Resource resource = content.getResource(facet.toString());
      Resource facetType = resource.getPropertyResourceValue(RDF.type);
      if (facetType != null && facetType.isURIResource()) {
        return Optional.of(URI.create(facetType.asResource().getURI()));
      }
      return Optional.empty();
    }

    public static Optional<URI> getTypeOfFacet(Dataset content, final URI facet) {
      return Optional.ofNullable(RdfUtils.findFirst(content, m -> getTypeOfFacet(m, facet).orElse(null)));
    }

    /**
     * Returns all facets of the base resource of the given type.
     * 
     * @param model
     * @param subject
     * @param facetType
     * @return
     */
    public static Collection<URI> getFacetsOfType(Model model, URI facetType) {
      return getFacetsOfType(model, RdfUtils.getBaseResource(model), facetType);
    }

    /**
     * Returns all facets of subject with the given type found in the model.
     * 
     * @param model
     * @param facetType
     * @return
     */
    public static Collection<URI> getFacetsOfType(Model model, URI subject, URI facetType) {
      return getFacetsOfType(model, model.getResource(subject.toString()), facetType);
    }

    /**
     * Returns all facets of the given type found in the model.
     * 
     * @param model
     * @param facetType
     * @return
     */
    public static Collection<URI> getFacetsOfType(Model model, Resource subject, URI facetType) {
      StmtIterator stmtIterator = subject.listProperties(WON.HAS_FACET);
      Resource facetTypeResource = model.getResource(facetType.toString());
      LinkedList<URI> ret = new LinkedList<URI>();
      while (stmtIterator.hasNext()) {
        RDFNode facet = stmtIterator.nextStatement().getObject();
        if (facet.isResource() && facet.isURIResource()) {
          if (facet.asResource().hasProperty(RDF.type, facetTypeResource)) {
            ret.add(URI.create(facet.toString()));
          }
        }
      }
      return ret;
    }

    public static Collection<URI> getFacetsOfType(Dataset needDataset, URI needURI, URI facetType) {
      return RdfUtils.visitFlattenedToList(needDataset, m -> getFacetsOfType(m, needURI, facetType));
    }

    public static Optional<URI> getDefaultFacet(Model model, boolean returnAnyIfNoDefaultFound) {
      return getDefaultFacet(model, RdfUtils.getBaseResource(model), returnAnyIfNoDefaultFound);
    }

    public static Optional<URI> getDefaultFacet(Model model, URI subject, boolean returnAnyIfNoDefaultFound) {
      return getDefaultFacet(model, model.getResource(subject.toString()), returnAnyIfNoDefaultFound);
    }

    /**
     * Returns the default facet found in the model. If there is no default facet,
     * the result is empty. unless returnAnyIfNoDefaultFound is true, in which case
     * any facet may be returned. and there is no default facet, any facet may be
     * returned
     * 
     * @param model
     * @param subject
     * @param         boolean returnAnyIfNoDefaultFound
     * @return
     */
    public static Optional<URI> getDefaultFacet(Model model, Resource subject, boolean returnAnyIfNoDefaultFound) {
      RDFNode facet = subject.getPropertyResourceValue(WON.HAS_DEFAULT_FACET);
      if (facet != null && facet.isURIResource()) {
        return Optional.of(URI.create(facet.toString()));
      }
      if (returnAnyIfNoDefaultFound) {
        StmtIterator stmtIterator = subject.listProperties(WON.HAS_FACET);
        while (stmtIterator.hasNext()) {
          facet = stmtIterator.next().getObject();
          if (facet.isResource() && facet.isURIResource()) {
            return Optional.of(URI.create(facet.toString()));
          }
        }
      }
      return Optional.empty();
    }

    public static Optional<URI> getDefaultFacet(Dataset needDataset, URI needURI, boolean returnAnyIfNoDefaultFound) {
      return Optional.ofNullable(
          RdfUtils.findFirst(needDataset, m -> getDefaultFacet(m, needURI, returnAnyIfNoDefaultFound).orElse(null)));
    }

    /**
     * Adds a triple to the model of the form <> won:hasFacet [facetURI].
     * 
     * @param model
     * @param facetURI
     */
    public static void addFacet(final Model model, final URI facetURI, final URI facetTypeURI,
        final boolean isDefaultFacet) {
      Resource baseRes = RdfUtils.getBaseResource(model);
      Resource facet = model.createResource(facetURI.toString());
      baseRes.addProperty(WON.HAS_FACET, facet);
      facet.addProperty(RDF.type, model.createResource(facetTypeURI.toString()));
      if (isDefaultFacet) {
        if (baseRes.hasProperty(WON.HAS_DEFAULT_FACET)) {
          baseRes.removeAll(WON.HAS_DEFAULT_FACET);
        }
        baseRes.addProperty(WON.HAS_DEFAULT_FACET, facet);
      }
    }

    public static void addFacet(final Dataset dataset, final URI facetURI, final URI facetTypeURI,
        final boolean isDefaultFacet) {
      visit(dataset, model -> {
        addFacet(model, facetURI, facetTypeURI, isDefaultFacet);
        return null;
      });
    }

    /**
     * Adds a triple to the model of the form <> won:hasRemoteFacet [facetURI].
     * 
     * @param content
     * @param facetURI
     */
    public static void addRemoteFacet(final Model content, final URI facetURI) {
      Resource baseRes = RdfUtils.getBaseResource(content);
      baseRes.addProperty(WON.HAS_REMOTE_FACET, content.createResource(facetURI.toString()));
    }

    /**
     * Creates a model for connecting two facets. Both facets are optional, if none
     * are given the returned Optional is empty
     * 
     * @return
     */
    public static Optional<Model> createFacetModelForHintOrConnect(Optional<URI> facet, Optional<URI> remoteFacet) {
      if (!facet.isPresent() && !remoteFacet.isPresent()) {
        return Optional.empty();
      }
      Model model = ModelFactory.createDefaultModel();
      Resource baseResource = findOrCreateBaseResource(model);
      if (facet.isPresent()) {
        baseResource.addProperty(WON.HAS_FACET, model.getResource(facet.toString()));
      }
      if (remoteFacet.isPresent()) {
        baseResource.addProperty(WON.HAS_REMOTE_FACET, model.getResource(remoteFacet.toString()));
      }
      return Optional.of(model);
    }

  }

  public static class ConnectionUtils {
    public static boolean isConnected(Dataset connectionDataset, URI connectionUri) {
      URI connectionState = getConnectionState(connectionDataset, connectionUri);

      return ConnectionState.CONNECTED.getURI().equals(connectionState);
    }

    public static URI getConnectionState(Dataset connectionDataset, URI connectionUri) {
      Path statePath = PathParser.parse("won:hasConnectionState", DefaultPrefixUtils.getDefaultPrefixes());

      return RdfUtils.getURIPropertyForPropertyPath(connectionDataset, connectionUri, statePath);
    }

    /**
     * return the needURI of a connection
     *
     * @param dataset       <code>Dataset</code> object which contains connection
     *                      information
     * @param connectionURI
     * @return <code>URI</code> of the need
     */
    public static URI getLocalNeedURIFromConnection(Dataset dataset, final URI connectionURI) {
      return URI.create(findOnePropertyFromResource(dataset, connectionURI, WON.BELONGS_TO_NEED).asResource().getURI());
    }

    public static URI getRemoteNeedURIFromConnection(Dataset dataset, final URI connectionURI) {
      return URI.create(findOnePropertyFromResource(dataset, connectionURI, WON.HAS_REMOTE_NEED).asResource().getURI());
    }

    public static URI getWonNodeURIFromConnection(Dataset dataset, final URI connectionURI) {
      return URI.create(findOnePropertyFromResource(dataset, connectionURI, WON.HAS_WON_NODE).asResource().getURI());
    }

    public static URI getWonNodeURIFromNeed(Dataset dataset, final URI needURI) {
      return URI.create(findOnePropertyFromResource(dataset, needURI, WON.HAS_WON_NODE).asResource().getURI());
    }

    public static URI getRemoteConnectionURIFromConnection(Dataset dataset, final URI connectionURI) {
      return URI
          .create(findOnePropertyFromResource(dataset, connectionURI, WON.HAS_REMOTE_CONNECTION).asResource().getURI());
    }

    public static URI getLastMessageSentByLocalNeed(Dataset dataset, final URI connectionURI) {
      throw new NotYetImplementedException();
    }

    public static URI getLastMessageSentByRemoteNeed(Dataset dataset, final URI connectionURI) {
      throw new NotYetImplementedException();
    }

    public static List<URI> getMessageURIs() {
      throw new NotYetImplementedException();
    }

  }

  private static Model createModelWithBaseResource() {
    Model model = ModelFactory.createDefaultModel();
    model.setNsPrefix("", "no:uri");
    model.createResource(model.getNsPrefixURI(""));
    return model;
  }

  public static class NeedUtils {
    /**
     * searches for a subject of type won:Need and returns the NeedURI
     *
     * @param dataset <code>Dataset</code> object which will be searched for the
     *                NeedURI
     * @return <code>URI</code> which is of type won:Need
     */
    public static URI getNeedURI(Dataset dataset) {
      return RdfUtils.findOne(dataset, new RdfUtils.ModelVisitor<URI>() {
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
      Resource res = getNeedResource(model);
      return res == null ? null : URI.create(res.getURI());
    }

    /**
     * searches for a subject of type won:Need and returns the NeedURI
     *
     * @param model <code>Model</code> object which will be searched for the NeedURI
     * @return <code>URI</code> which is of type won:Need
     */
    public static Resource getNeedResource(Model model) {

      List<Resource> needURIs = new ArrayList<>();

      ResIterator iterator = model.listSubjectsWithProperty(RDF.type, WON.NEED);
      while (iterator.hasNext()) {
        needURIs.add(iterator.next());
      }
      if (needURIs.size() == 0)
        return null;
      else if (needURIs.size() == 1)
        return needURIs.get(0);
      else if (needURIs.size() > 1) {
        Resource u = needURIs.get(0);
        for (Resource uri : needURIs) {
          if (!uri.equals(u))
            throw new IncorrectPropertyCountException(1, 2);
        }
        return u;
      } else
        return null;
    }

    /**
     * searches for a subject of type won:Need and returns the NeedURI
     *
     * @param dataset <code>Dataset</code> object which will be searched for the
     *                NeedURI
     * @return <code>URI</code> which is of type won:Need
     */
    public static Resource getNeedResource(Dataset dataset) {
      Model model = new NeedModelWrapper(dataset).copyNeedModel(NeedGraphType.NEED);
      return getNeedResource(model);
    }

    public static URI getWonNodeURIFromNeed(Dataset dataset, final URI needURI) {
      return URI.create(findOnePropertyFromResource(dataset, needURI, WON.HAS_WON_NODE).asResource().getURI());
    }

    /**
     * 
     */
    public static Iterator<URI> getConnectedNeeds(Dataset dataset, final URI needURI) {
      PrefixMapping pmap = new PrefixMappingImpl();
      pmap.withDefaultMappings(PrefixMapping.Standard);
      pmap.setNsPrefix("won", WON.getURI());
      pmap.setNsPrefix("msg", WONMSG.getURI());
      Path path = PathParser.parse("won:hasConnectionContainer/rdfs:member/won:hasRemoteNeed", pmap);
      return RdfUtils.getURIsForPropertyPath(dataset, needURI, path);
    }

    /**
     * Assumes that the dataset contains a need's connection information, looks for
     * the specified remote needs and returns the remote connection uris. Optionally
     * the result can be filtered by connection state.
     */
    public static Set<URI> getRemoteConnectionURIsForRemoteNeeds(Dataset dataset, final Collection<URI> remoteNeeds,
        final Optional<ConnectionState> state) {
      Optional<Object> unions = remoteNeeds.stream().map(uri -> {
        BasicPattern pattern = new BasicPattern();
        pattern.add(Triple.create(Var.alloc("localCon"),
            NodeFactory.createURI("http://purl.org/webofneeds/model#hasRemoteNeed"),
            NodeFactory.createURI(uri.toString())));
        pattern.add(Triple.create(Var.alloc("localCon"),
            NodeFactory.createURI("http://purl.org/webofneeds/model#hasRemoteConnection"), Var.alloc("remoteCon")));
        if (state.isPresent()) {
          pattern.add(Triple.create(Var.alloc("localCon"),
              NodeFactory.createURI("http://purl.org/webofneeds/model#hasConnectionState"),
              NodeFactory.createURI(state.get().getURI().toString())));
        }
        return pattern;
      }).map(pattern -> new OpBGP(pattern)).map(bgp -> new OpGraph(Var.alloc("g"), bgp)).reduce(Optional.empty(),
          (union, pattern) -> {
            if (!union.isPresent()) {
              return Optional.of(pattern);
            }
            return Optional.of(new OpUnion((Op) union.get(), pattern));
          }, (union1, union2) -> {
            if (!union1.isPresent())
              return union2;
            if (!union2.isPresent())
              return union1;
            return Optional.of(new OpUnion((Op) union1.get(), (Op) union2.get()));
          });
      if (!unions.isPresent()) {
        return Collections.EMPTY_SET;
      }
      Op op = new OpProject((Op) unions.get(), Arrays.asList(Var.alloc("remoteCon")));
      Query q = OpAsQuery.asQuery(op);
      q.setQuerySelectType();
      Set<URI> result = new HashSet();
      try (QueryExecution qexec = QueryExecutionFactory.create(q, dataset)) {
        ResultSet resultSet = qexec.execSelect();
        while (resultSet.hasNext()) {
          QuerySolution solution = resultSet.next();
          result.add(URI.create(solution.get("remoteCon").asResource().getURI()));
        }
      }
      return result;
    }
  }
}
