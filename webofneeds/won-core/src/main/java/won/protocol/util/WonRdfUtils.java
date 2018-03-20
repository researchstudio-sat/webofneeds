package won.protocol.util;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.riot.Lang;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathParser;
import org.apache.jena.tdb.TDB;
import org.apache.jena.vocabulary.RDF;
import org.hibernate.cfg.NotYetImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.exception.IncorrectPropertyCountException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonSignatureData;
import won.protocol.model.ConnectionState;
import won.protocol.model.Match;
import won.protocol.model.NeedGraphType;
import won.protocol.service.WonNodeInfo;
import won.protocol.service.WonNodeInfoBuilder;
import won.protocol.vocabulary.SFSIG;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONAGR;
import won.protocol.vocabulary.WONMOD;
import won.protocol.vocabulary.WONMSG;

import java.net.URI;
import java.util.*;

import static won.protocol.util.RdfUtils.findOnePropertyFromResource;
import static won.protocol.util.RdfUtils.findOrCreateBaseResource;
import static won.protocol.util.RdfUtils.visit;

/**
 * Utilities for populating/manipulating the RDF models used throughout the WON application.
 */
public class WonRdfUtils
{

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
      return new WonSignatureData(signedGraphUri, resource.getURI(), signatureValue, hash, fingerprint,
                                  cert);
    }

    /**
     * Adds the triples holding the signature data to the model of the specified resource, using the resource as the
     * subject.
     * @param subject
     * @param wonSignatureData
     */
    public static void addSignature(Resource subject, WonSignatureData wonSignatureData){
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
      subject.addProperty(SFSIG.HAS_VERIFICATION_CERT, containingGraph.createResource(wonSignatureData
                                                                                        .getVerificationCertificateUri()));
    }
  }

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
      assert wonNodeUri != null: "wonNodeUri must not be null";
      assert dataset != null: "dataset must not be null";
      return RdfUtils.findFirst(dataset, new RdfUtils.ModelVisitor<WonNodeInfo>()
        {
          @Override
          public WonNodeInfo visit(final Model model) {

            //use the first blank node found for [wonNodeUri] won:hasUriPatternSpecification [blanknode]
            NodeIterator it = model.listObjectsOfProperty(model.getResource(wonNodeUri.toString()),
              WON.HAS_URI_PATTERN_SPECIFICATION);
            if (!it.hasNext()) return null;
            WonNodeInfoBuilder wonNodeInfoBuilder = new WonNodeInfoBuilder();


            wonNodeInfoBuilder.setWonNodeURI(wonNodeUri.toString());
            RDFNode node = it.next();

            // set the URI prefixes

            it = model.listObjectsOfProperty(node.asResource(), WON.HAS_NEED_URI_PREFIX);
            if (! it.hasNext() ) return null;
            String needUriPrefix = it.next().asLiteral().getString();
              wonNodeInfoBuilder.setNeedURIPrefix(needUriPrefix);
            it = model.listObjectsOfProperty(node.asResource(), WON.HAS_CONNECTION_URI_PREFIX);
            if (! it.hasNext() ) return null;
            wonNodeInfoBuilder.setConnectionURIPrefix(it.next().asLiteral().getString());
            it = model.listObjectsOfProperty(node.asResource(), WON.HAS_EVENT_URI_PREFIX);
            if (! it.hasNext() ) return null;
            wonNodeInfoBuilder.setEventURIPrefix(it.next().asLiteral().getString());

            // set the need list URI
            it = model.listObjectsOfProperty(model.getResource(wonNodeUri.toString()), WON.HAS_NEED_LIST);
            if (it.hasNext() ) {
              wonNodeInfoBuilder.setNeedListURI(it.next().asNode().getURI());
            } else {
              wonNodeInfoBuilder.setNeedListURI(needUriPrefix);
            }

            // set the supported protocol implementations
            String queryString = "SELECT ?protocol ?param ?value WHERE { ?a <%s> ?c. " +
              "?c <%s> ?protocol. ?c ?param ?value. FILTER ( ?value != ?protocol ) }";
            queryString = String.format(queryString, WON.SUPPORTS_WON_PROTOCOL_IMPL.toString(), RDF.getURI() + "type");
            Query protocolQuery = QueryFactory.create(queryString);
            QueryExecution qexec = QueryExecutionFactory.create(protocolQuery, model);

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

  public static class MessageUtils
  {
    /**
     * Adds the specified text as a won:hasTextMessage to the model's base resource.
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
    * Create an RDF model containing a text message and a processing message
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
    	if (toRetract == null) return messageModel;
    	for(URI uri: toRetract) {
    		if (uri != null) {
    			baseRes.addProperty(WONMOD.RETRACTS, baseRes.getModel().getResource(uri.toString()));
    		}
    	}
    	return messageModel;
    }
    
    public static Model addProposes(Model messageModel, URI... toPropose) {
    	Resource baseRes = RdfUtils.findOrCreateBaseResource(messageModel);
    	if (toPropose == null) return messageModel;
    	for(URI uri: toPropose) {
    		if (uri != null) {
    			baseRes.addProperty(WONAGR.PROPOSES, baseRes.getModel().getResource(uri.toString()));
    		}
    	}
    	return messageModel;
    }

    public static Model addRejects(Model messageModel, URI... toReject) {
      Resource baseRes = RdfUtils.findOrCreateBaseResource(messageModel);
      if (toReject == null) return messageModel;
      for(URI uri: toReject) {
          if (uri != null) {
              baseRes.addProperty(WONAGR.REJECT, baseRes.getModel().getResource(uri.toString()));
          }
      }
      return messageModel;
    }

    public static Model addAccepts(Model messageModel, URI... toAccept) {
    	Resource baseRes = RdfUtils.findOrCreateBaseResource(messageModel);
    	if (toAccept == null) return messageModel;
    	for(URI uri: toAccept) {
    		if (uri != null) {
    			if(logger.isDebugEnabled()) {
    	    		logger.debug("checking uri for addProposesToCancel{} with uri {} ({} of {})", new Object[] {uri} );
    	    	}
    			baseRes.addProperty(WONAGR.ACCEPTS, baseRes.getModel().getResource(uri.toString()));
    		}
    	}
    	return messageModel;
    }
    
    public static Model addProposesToCancel(Model messageModel, URI... toProposesToCancel) {
    	Resource baseRes = RdfUtils.findOrCreateBaseResource(messageModel);
    	if (toProposesToCancel == null) return messageModel;
    	for(URI uri: toProposesToCancel) {
    		if (uri != null) {
    			if(logger.isDebugEnabled()) {
    	    		logger.debug("checking uri for addProposesToCancel{} with uri {} ({} of {})", new Object[] {uri} );
    	    	}
    			baseRes.addProperty(WONAGR.PROPOSES_TO_CANCEL, baseRes.getModel().getResource(uri.toString()));
    		}
    	}
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
     * Won't work on WonMessage models, removal depends on refactoring of BA facet code
     * @param model
     * @return
     */
    @Deprecated
    public static String getTextMessage(Model model){
      Statement stmt = model.getProperty(RdfUtils.getBaseResource(model), WON.HAS_TEXT_MESSAGE);
      if (stmt != null) {
        return stmt.getObject().asLiteral().getLexicalForm();
      }
      return null;
    }


      /**
       * Returns the first won:hasTextMessage object, or null if none is found.
       * tries the message, its corresponding remote message, and any forwarded message,
       * if any of those are contained in the dataset
       *
       * @param wonMessage
       * @return
       */
      public static String getTextMessage(final WonMessage wonMessage) {
          URI messageURI = wonMessage.getMessageURI();

          //find the text message in the message, the remote message, or any forwarded message
          String queryString =
                  "prefix msg: <http://purl.org/webofneeds/message#>\n" +
                          "prefix won: <http://purl.org/webofneeds/model#>\n" +
                          "\n" +
                          "SELECT distinct ?txt WHERE {\n" +
                          "  {\n" +
                          "    graph ?gA { ?msg won:hasTextMessage ?txt }\n" +
                          "  } union {\n" +
                          "    graph ?gB { ?msg msg:hasCorrespondingRemoteMessage ?msg2 }\n" +
                          "    graph ?gA { ?msg2 won:hasTextMessage ?txt }\n" +
                          "  } union {\n" +
                          "    graph ?gC { ?msg msg:hasForwardedMessage ?msg2 }\n" +
                          "    graph ?gB { ?msg2 msg:hasCorrespondingRemoteMessage ?msg3 }\n" +
                          "    graph ?gA { ?msg3 won:hasTextMessage ?txt }\n" +
                          "  } union {\n" +
                          "    graph ?gD { ?msg msg:hasCorrespondingRemoteMessage ?msg2 }\n" +
                          "    graph ?gC { ?msg2 msg:hasForwardedMessage ?msg3 }\n" +
                          "    graph ?gB { ?msg3 msg:hasCorrespondingRemoteMessage ?msg4 }\n" +
                          "    graph ?gA { ?msg4 won:hasTextMessage ?txt }\n" +
                          "  } union {\n" +
                          "    graph ?gE { ?msg msg:hasForwardedMessage ?msg2 }\n" +
                          "    graph ?gD { ?msg2 msg:hasCorrespondingRemoteMessage ?msg3 }\n" +
                          "    graph ?gC { ?msg3 msg:hasForwardedMessage ?msg4 }\n" +
                          "    graph ?gB { ?msg4 msg:hasCorrespondingRemoteMessage ?msg5 }\n" +
                          "    graph ?gA { ?msg5 won:hasTextMessage ?txt }\n" +
                          "  } union {\n" +
                          "    graph ?gF { ?msg msg:hasCorrespondingRemoteMessage ?msg2 }\n" +
                          "    graph ?gE { ?msg2 msg:hasForwardedMessage ?msg3 }\n" +
                          "    graph ?gD { ?msg3 msg:hasCorrespondingRemoteMessage ?msg4 }\n" +
                          "    graph ?gC { ?msg4 msg:hasForwardedMessage ?msg5 }\n" +
                          "    graph ?gB { ?msg5 msg:hasCorrespondingRemoteMessage ?msg6 }\n" +
                          "    graph ?gA { ?msg6 won:hasTextMessage ?txt }\n" +
                          "  } union {\n" +
                          "    graph ?gG { ?msg msg:hasForwardedMessage ?msg2 }\n" +
                          "    graph ?gF { ?msg2 msg:hasCorrespondingRemoteMessage ?msg3 }\n" +
                          "    graph ?gE { ?msg3 msg:hasForwardedMessage ?msg4 }\n" +
                          "    graph ?gD { ?msg4 msg:hasCorrespondingRemoteMessage ?msg5 }\n" +
                          "    graph ?gC { ?msg5 msg:hasForwardedMessage ?msg6 }\n" +
                          "    graph ?gB { ?msg6 msg:hasCorrespondingRemoteMessage ?msg7 }\n" +
                          "    graph ?gA { ?msg7 won:hasTextMessage ?txt }\n" +
                          "  }\n" +
                          "\n" +
                          "}";
          Query query = QueryFactory.create(queryString);

          QuerySolutionMap initialBinding = new QuerySolutionMap();
          Model tmpModel = ModelFactory.createDefaultModel();
          initialBinding.add("msg", tmpModel.getResource(messageURI.toString()));
          try (QueryExecution qexec = QueryExecutionFactory
                  .create(query, wonMessage.getCompleteDataset())) {
              qexec.getContext().set(TDB.symUnionDefaultGraph, true);
              ResultSet rs = qexec.execSelect();
              if (rs.hasNext()) {
                  QuerySolution qs = rs.nextSolution();
                  String textMessage = rdfNodeToString(qs.get("txt"));
                  if (rs.hasNext()) {
                      //TODO as soon as we have use cases for multiple messages, we need to refactor this
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
          String queryString =
                  "prefix msg:   <http://purl.org/webofneeds/message#>\n" +
                          "prefix agr:   <http://purl.org/webofneeds/agreement#>\n" +
                          "SELECT ?eventUri where {\n" +
                          " graph ?g {"+
                          "  ?s agr:accepts ?eventUri .\n" +
                          "}}";
          Query query = QueryFactory.create(queryString);

          try (QueryExecution qexec = QueryExecutionFactory.create(query, messageDataset)) {
              qexec.getContext().set(TDB.symUnionDefaultGraph, true);
              ResultSet rs = qexec.execSelect();
              if (rs.hasNext()) {
                  QuerySolution qs = rs.nextSolution();
                  String eventUri = rdfNodeToString(qs.get("eventUri"));

                  if(eventUri != null) {
                      acceptedEvents.add(URI.create(eventUri));
                  }
              }
          }
          return acceptedEvents;
      }

      public static boolean isProcessingMessage(final WonMessage wonMessage) {
          String queryString =
                  "prefix msg:   <http://purl.org/webofneeds/message#>\n" +
                          "prefix won:   <http://purl.org/webofneeds/model#>\n" +
                          "SELECT ?text where {\n" +
                          " graph ?g {"+
                          "  ?s won:isProcessing ?text .\n" +
                          "}}";
          Query query = QueryFactory.create(queryString);

          try (QueryExecution qexec = QueryExecutionFactory.create(query, wonMessage.getCompleteDataset())) {
              qexec.getContext().set(TDB.symUnionDefaultGraph, true);
              ResultSet rs = qexec.execSelect();
              if (rs.hasNext()) {
                  QuerySolution qs = rs.nextSolution();
                  String text = rdfNodeToString(qs.get("text"));

                  if(text != null) {
                      return true;
                  }
              }
          }
          return false;
      }

      /**
       * Adds the specified text as a won:hasTextMessage to the model's base resource.
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
          String queryString =
                  "prefix msg:   <http://purl.org/webofneeds/message#>\n" +
                          "prefix agr:   <http://purl.org/webofneeds/agreement#>\n" +
                          "SELECT ?eventUri where {\n" +
                          " graph ?g {"+
                          "  ?s agr:proposes ?eventUri .\n" +
                          "}}";
          Query query = QueryFactory.create(queryString);


          try (QueryExecution qexec = QueryExecutionFactory.create(query, messageDataset)) {
              qexec.getContext().set(TDB.symUnionDefaultGraph, true);
              ResultSet rs = qexec.execSelect();
              if (rs.hasNext()) {
                  QuerySolution qs = rs.nextSolution();
                  String eventUri = rdfNodeToString(qs.get("eventUri"));

                  if(eventUri != null) {
                      proposesToCancelEvents.add(URI.create(eventUri));
                  }
              }
          }
          return proposesToCancelEvents;
      }

      public static List<URI> getRejectEvents(final WonMessage wonMessage) {
          return getRejectEvents(wonMessage.getCompleteDataset());
      }

      public static List<URI> getRejectEvents(final Dataset messageDataset) {
          List<URI> rejectEvents = new ArrayList<>();
          String queryString =
                  "prefix msg:   <http://purl.org/webofneeds/message#>\n" +
                          "prefix agr:   <http://purl.org/webofneeds/agreement#>\n" +
                          "SELECT ?eventUri where {\n" +
                          " graph ?g {"+
                          "  ?s agr:reject ?eventUri .\n" +
                          "}}";
          Query query = QueryFactory.create(queryString);


          try (QueryExecution qexec = QueryExecutionFactory.create(query, messageDataset)) {
              qexec.getContext().set(TDB.symUnionDefaultGraph, true);
              ResultSet rs = qexec.execSelect();
              if (rs.hasNext()) {
                  QuerySolution qs = rs.nextSolution();
                  String eventUri = rdfNodeToString(qs.get("eventUri"));

                  if(eventUri != null) {
                      rejectEvents.add(URI.create(eventUri));
                  }
              }
          }
          return rejectEvents;
      }

      public static List<URI> getRetractEvents(final WonMessage wonMessage) {
          return getRetractEvents(wonMessage.getCompleteDataset());
      }

      public static List<URI> getRetractEvents(final Dataset messageDataset) {
          List<URI> retractEvents = new ArrayList<>();
          String queryString =
                  "prefix msg:   <http://purl.org/webofneeds/message#>\n" +
                          "prefix agr:   <http://purl.org/webofneeds/agreement#>\n" +
                          "SELECT ?eventUri where {\n" +
                          " graph ?g {"+
                          "  ?s agr:retract ?eventUri .\n" +
                          "}}";
          Query query = QueryFactory.create(queryString);


          try (QueryExecution qexec = QueryExecutionFactory.create(query, messageDataset)) {
              qexec.getContext().set(TDB.symUnionDefaultGraph, true);
              ResultSet rs = qexec.execSelect();
              if (rs.hasNext()) {
                  QuerySolution qs = rs.nextSolution();
                  String eventUri = rdfNodeToString(qs.get("eventUri"));

                  if(eventUri != null) {
                      retractEvents.add(URI.create(eventUri));
                  }
              }
          }
          return retractEvents;
      }


      public static List<URI> getProposesToCancelEvents(final WonMessage wonMessage) {
          return getProposesToCancelEvents(wonMessage.getCompleteDataset());
      }

      public static List<URI> getProposesToCancelEvents(final Dataset messageDataset) {
          List<URI> proposesToCancelEvents = new ArrayList<>();
          String queryString =
                  "prefix msg:   <http://purl.org/webofneeds/message#>\n" +
                          "prefix agr:   <http://purl.org/webofneeds/agreement#>\n" +
                          "SELECT ?eventUri where {\n" +
                          " graph ?g {"+
                          "  ?s agr:proposesToCancel ?eventUri .\n" +
                          "}}";
          Query query = QueryFactory.create(queryString);


          try (QueryExecution qexec = QueryExecutionFactory.create(query, messageDataset)) {
              qexec.getContext().set(TDB.symUnionDefaultGraph, true);
              ResultSet rs = qexec.execSelect();
              if (rs.hasNext()) {
                  QuerySolution qs = rs.nextSolution();
                  String eventUri = rdfNodeToString(qs.get("eventUri"));

                  if(eventUri != null) {
                      proposesToCancelEvents.add(URI.create(eventUri));
                  }
              }
          }
          return proposesToCancelEvents;
      }

      private static String rdfNodeToString(RDFNode node) {
          if (node.isLiteral()) {
              return node.asLiteral().getString();
          } else if (node.isResource()) {
              return node.asResource().getURI();
          }
          return null;
      }

    private static RDFNode getTextMessageForResource(Dataset dataset, URI uri){
      if (uri == null) return  null;
      return RdfUtils.findFirstPropertyFromResource(dataset, uri, WON.HAS_TEXT_MESSAGE);
    }

    private static RDFNode getTextMessageForResource(Dataset dataset, Resource resource){
      if (resource == null) return null;
      return RdfUtils.findFirstPropertyFromResource(dataset, resource, WON.HAS_TEXT_MESSAGE);
    }
    /**
     * Converts the specified hint message into a Match object.
     * @param wonMessage
     * @return a match object or null if the message is not a hint message.
     */
    public static Match toMatch(final WonMessage wonMessage) {
      if (!WONMSG.TYPE_HINT.equals(wonMessage.getMessageType().getResource())){
        return null;
      }
      Match match = new Match();
      match.setFromNeed(wonMessage.getReceiverNeedURI());

      Dataset messageContent = wonMessage.getMessageContent();

      RDFNode score = findOnePropertyFromResource(messageContent, wonMessage.getMessageURI(),
        WON.HAS_MATCH_SCORE);
      if (!score.isLiteral()) return null;
      match.setScore(score.asLiteral().getDouble());

      RDFNode counterpart = findOnePropertyFromResource(messageContent, wonMessage.getMessageURI(),
                                                                 WON.HAS_MATCH_COUNTERPART);
      if (!counterpart.isResource()) return null;
      match.setToNeed(URI.create(counterpart.asResource().getURI()));
      return match;
    }


    public static WonMessage copyByDatasetSerialization(final WonMessage toWrap) {
      WonMessage copied = new WonMessage(RdfUtils.readDatasetFromString(
        RdfUtils.writeDatasetToString(toWrap.getCompleteDataset(),
                                      Lang.TRIG) ,Lang.TRIG));
      return copied;
    }


  }

  public static class FacetUtils {


    /**
     * Returns the facet in a connect message. Attempts to get it from the specified message itself.
     * If no such facet is found there, the remoteFacet of the correspondingRemoteMessage is used.
     * @param message
     * @return
       */
    public static URI getFacet(WonMessage message){
      URI uri = getObjectOfMessageProperty(message, WON.HAS_FACET);
      if (uri == null) {
        uri = getObjectOfRemoteMessageProperty(message, WON.HAS_REMOTE_FACET);
      }
      return uri;
    }

    /**
     * Returns the remoteFacet in a connect message. Attempts to get it from the specified message itself.
     * If no such facet is found there, the facet of the correspondingRemoteMessage is used.
     * @param message
     * @return
     */
    public static URI getRemoteFacet(WonMessage message) {
      URI uri = getObjectOfMessageProperty(message, WON.HAS_REMOTE_FACET);
      if (uri == null) {
        uri = getObjectOfRemoteMessageProperty(message, WON.HAS_FACET);
      }
      return uri;
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
     * Returns a property of the corresponding remote message (i.e. the object of the first triple (
     * [corresponding-remote-message-uri] [property] X )
     * found in one of the content graphs of the specified message.
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

      public static void addFacet(final Dataset dataset, final URI facetURI) {
          visit(dataset, model -> {
              addFacet(model, facetURI);
              return null;
          });
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
     * Creates a model for connecting two facets.CONNECTED.getURI().equals(connectionState)
     * @return
     */
    public static Model createFacetModelForHintOrConnect(URI facet, URI remoteFacet)
    {
      Model model = ModelFactory.createDefaultModel();
      Resource baseResource = findOrCreateBaseResource(model);
      WonRdfUtils.FacetUtils.addFacet(model, facet);
      WonRdfUtils.FacetUtils.addRemoteFacet(model, remoteFacet);
      logger.debug("facet model contains these facets: from:{} to:{}", facet, remoteFacet);
      return model;
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
     * @param dataset <code>Dataset</code> object which contains connection information
     * @param connectionURI
     * @return <code>URI</code> of the need
     */
    public static URI getLocalNeedURIFromConnection(Dataset dataset, final URI connectionURI) {
      return URI.create(findOnePropertyFromResource(
        dataset, connectionURI, WON.BELONGS_TO_NEED).asResource().getURI());
    }

    public static URI getRemoteNeedURIFromConnection(Dataset dataset, final URI connectionURI) {
      return URI.create(findOnePropertyFromResource(
        dataset, connectionURI, WON.HAS_REMOTE_NEED).asResource().getURI());
    }

    public static URI getWonNodeURIFromConnection(Dataset dataset, final URI connectionURI) {
      return URI.create(findOnePropertyFromResource(
        dataset, connectionURI, WON.HAS_WON_NODE).asResource().getURI());
    }

    public static URI getRemoteConnectionURIFromConnection(Dataset dataset, final URI connectionURI) {
      return URI.create(findOnePropertyFromResource(
        dataset, connectionURI, WON.HAS_REMOTE_CONNECTION).asResource().getURI());
    }
    
    public static URI getLastMessageSentByLocalNeed(Dataset dataset, final URI connectionURI) {
    	throw new NotYetImplementedException();
    }
    
    public static URI getLastMessageSentByRemoteNeed(Dataset dataset, final URI connectionURI) {
    	throw new NotYetImplementedException();
    }
    
    public static List<URI> getMessageURIs(){
    	throw new NotYetImplementedException();
    }
    
  }

  private static Model createModelWithBaseResource() {
      Model model = ModelFactory.createDefaultModel();
      model.setNsPrefix("", "no:uri");
      model.createResource(model.getNsPrefixURI(""));
      return model;
    }

  public static class NeedUtils
  {
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
                      throw new IncorrectPropertyCountException(1,2);
              }
              return u;
          }
          else
              return null;
      }

      /**
       * searches for a subject of type won:Need and returns the NeedURI
       *
       * @param dataset <code>Dataset</code> object which will be searched for the NeedURI
       * @return <code>URI</code> which is of type won:Need
       */
      public static Resource getNeedResource(Dataset dataset) {
          Model model = new NeedModelWrapper(dataset).copyNeedModel(NeedGraphType.NEED);
          return getNeedResource(model);
      }

    public static URI getWonNodeURIFromNeed(Dataset dataset, final URI needURI) {
      return URI.create(findOnePropertyFromResource(
        dataset, needURI, WON.HAS_WON_NODE).asResource().getURI());
    }
  }

}
