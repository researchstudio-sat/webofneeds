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
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.apache.jena.sparql.function.library.leviathan.sec;
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
import won.protocol.model.AtomGraphType;
import won.protocol.model.ConnectionState;
import won.protocol.model.SocketDefinitionImpl;
import won.protocol.service.WonNodeInfo;
import won.protocol.service.WonNodeInfoBuilder;
import won.protocol.util.RdfUtils.Pair;
import won.protocol.vocabulary.SCHEMA;
import won.protocol.vocabulary.SFSIG;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONAGR;
import won.protocol.vocabulary.WONCON;
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
            NodeIterator ni = signatureGraph.listObjectsOfProperty(resource, WONMSG.signedGraph);
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
            Statement stmt = resource.getRequiredProperty(WONMSG.signedGraph);
            String signedGraphUri = stmt.getObject().asResource().getURI();
            stmt = resource.getRequiredProperty(SFSIG.HAS_SIGNATURE_VALUE);
            String signatureValue = stmt.getObject().asLiteral().getString();
            stmt = resource.getRequiredProperty(WONMSG.hash);
            String hash = stmt.getObject().asLiteral().getString();
            stmt = resource.getRequiredProperty(WONMSG.publicKeyFingerprint);
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
            subject.addProperty(WONMSG.hash, wonSignatureData.getHash());
            subject.addProperty(SFSIG.HAS_SIGNATURE_VALUE, wonSignatureData.getSignatureValue());
            subject.addProperty(WONMSG.signedGraph,
                            containingGraph.createResource(wonSignatureData.getSignedGraphUri()));
            subject.addProperty(WONMSG.publicKeyFingerprint, wonSignatureData.getPublicKeyFingerprint());
            subject.addProperty(SFSIG.HAS_VERIFICATION_CERT,
                            containingGraph.createResource(wonSignatureData.getVerificationCertificateUri()));
        }
    }

    public static class WonNodeUtils {
        /**
         * Creates a WonNodeInfo object based on the specified dataset. The first model
         * found in the dataset that seems to contain the datan needed for a WonNodeInfo
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
                                    WON.uriPrefixSpecification);
                    if (!it.hasNext())
                        return null;
                    WonNodeInfoBuilder wonNodeInfoBuilder = new WonNodeInfoBuilder();
                    wonNodeInfoBuilder.setWonNodeURI(wonNodeUri.toString());
                    RDFNode node = it.next();
                    // set the URI prefixes
                    it = model.listObjectsOfProperty(node.asResource(), WON.atomUriPrefix);
                    if (!it.hasNext())
                        return null;
                    String atomUriPrefix = it.next().asLiteral().getString();
                    wonNodeInfoBuilder.setAtomURIPrefix(atomUriPrefix);
                    it = model.listObjectsOfProperty(node.asResource(), WON.connectionUriPrefix);
                    if (!it.hasNext())
                        return null;
                    wonNodeInfoBuilder.setConnectionURIPrefix(it.next().asLiteral().getString());
                    it = model.listObjectsOfProperty(node.asResource(), WON.eventUriPrefix);
                    if (!it.hasNext())
                        return null;
                    wonNodeInfoBuilder.setEventURIPrefix(it.next().asLiteral().getString());
                    // set the atom list URI
                    it = model.listObjectsOfProperty(model.getResource(wonNodeUri.toString()), WON.atomList);
                    if (it.hasNext()) {
                        wonNodeInfoBuilder.setAtomListURI(it.next().asNode().getURI());
                    } else {
                        wonNodeInfoBuilder.setAtomListURI(atomUriPrefix);
                    }
                    // set the supported protocol implementations
                    String queryString = "SELECT ?protocol ?param ?value WHERE { ?a <%s> ?c. "
                                    + "?c <%s> ?protocol. ?c ?param ?value. FILTER ( ?value != ?protocol ) }";
                    queryString = String.format(queryString, WON.supportsWonProtocolImpl.toString(),
                                    RDF.getURI() + "type");
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
         * Adds the specified text as a con:text to the model's base resource.
         * 
         * @param message
         * @return
         */
        public static Model addMessage(Model model, String message) {
            Resource baseRes = RdfUtils.findOrCreateBaseResource(model);
            baseRes.addProperty(WONCON.text, message, XSDDatatype.XSDstring);
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
            baseRes.addProperty(WONCON.text, message, XSDDatatype.XSDstring);
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
            baseRes.addProperty(RDF.type, WONMSG.ConnectionMessage);
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
                    baseRes.addProperty(WONMOD.retracts, baseRes.getModel().getResource(uri.toString()));
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
                    baseRes.addProperty(WONAGR.proposes, baseRes.getModel().getResource(uri.toString()));
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
                    baseRes.addProperty(WONAGR.rejects, baseRes.getModel().getResource(uri.toString()));
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
                        logger.debug("checking uri for addProposesToCancel{} with uri {} ({} of {})",
                                        new Object[] { uri });
                    }
                    baseRes.addProperty(WONAGR.accepts, baseRes.getModel().getResource(uri.toString()));
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
                        logger.debug("checking uri for addProposesToCancel{} with uri {} ({} of {})",
                                        new Object[] { uri });
                    }
                    baseRes.addProperty(WONAGR.proposesToCancel, baseRes.getModel().getResource(uri.toString()));
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
            baseRes.addProperty(WONCON.feedback, feedbackNode);
            feedbackNode.addProperty(WONCON.binaryRating, isFeedbackPositive ? WONCON.Good : WONCON.Bad);
            feedbackNode.addProperty(WONCON.feedbackTarget, messageModel.createResource(forResource.toString()));
            return messageModel;
        }

        /**
         * Returns the first con:text object, or null if none is found. Won't work on
         * WonMessage models, removal depends on refactoring of BA socket code
         * 
         * @param model
         * @return
         */
        @Deprecated
        public static String getTextMessage(Model model) {
            Statement stmt = model.getProperty(RdfUtils.getBaseResource(model), WONCON.text);
            if (stmt != null) {
                return stmt.getObject().asLiteral().getLexicalForm();
            }
            return null;
        }

        /**
         * Returns all con:text objects, or an empty set if none is found. The specified
         * model has to be a message's content graph.
         * 
         * @param model
         * @return
         */
        public static Set<String> getTextMessages(Model model, URI messageUri) {
            Set<String> ret = new HashSet<>();
            StmtIterator stmtIt = model.listStatements(model.getResource(messageUri.toString()), WONCON.text,
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
         * Returns the first con:text object, or null if none is found. tries the
         * message, its corresponding remote message, and any forwarded message, if any
         * of those are contained in the dataset
         *
         * @param wonMessage
         * @return
         */
        public static String getTextMessage(final WonMessage wonMessage) {
            URI messageURI = wonMessage.getMessageURI();
            // find the text message in the message, the remote message, or any forwarded
            // message
            String queryString = "prefix msg: <https://w3id.org/won/message#>\n"
                            + "prefix won: <https://w3id.org/won/core#>\n"
                            + "prefix con: <https://w3id.org/won/content#>\n"
                            + "prefix match: <https://w3id.org/won/matching#>\n"
                            + "\n" + "SELECT distinct ?txt WHERE {\n"
                            + "  {\n" + "    graph ?gA { ?msg con:text ?txt }\n" + "  } union {\n"
                            + "    graph ?gB { ?msg msg:correspondingRemoteMessage ?msg2 }\n"
                            + "    graph ?gA { ?msg2 con:text ?txt }\n" + "  } union {\n"
                            + "    graph ?gC { ?msg msg:forwardedMessage ?msg2 }\n"
                            + "    graph ?gB { ?msg2 msg:correspondingRemoteMessage ?msg3 }\n"
                            + "    graph ?gA { ?msg3 con:text ?txt }\n" + "  } union {\n"
                            + "    graph ?gD { ?msg msg:correspondingRemoteMessage ?msg2 }\n"
                            + "    graph ?gC { ?msg2 msg:forwardedMessage ?msg3 }\n"
                            + "    graph ?gB { ?msg3 msg:correspondingRemoteMessage ?msg4 }\n"
                            + "    graph ?gA { ?msg4 con:text ?txt }\n" + "  } union {\n"
                            + "    graph ?gE { ?msg msg:forwardedMessage ?msg2 }\n"
                            + "    graph ?gD { ?msg2 msg:correspondingRemoteMessage ?msg3 }\n"
                            + "    graph ?gC { ?msg3 msg:forwardedMessage ?msg4 }\n"
                            + "    graph ?gB { ?msg4 msg:correspondingRemoteMessage ?msg5 }\n"
                            + "    graph ?gA { ?msg5 con:text ?txt }\n" + "  } union {\n"
                            + "    graph ?gF { ?msg msg:correspondingRemoteMessage ?msg2 }\n"
                            + "    graph ?gE { ?msg2 msg:forwardedMessage ?msg3 }\n"
                            + "    graph ?gD { ?msg3 msg:correspondingRemoteMessage ?msg4 }\n"
                            + "    graph ?gC { ?msg4 msg:forwardedMessage ?msg5 }\n"
                            + "    graph ?gB { ?msg5 msg:correspondingRemoteMessage ?msg6 }\n"
                            + "    graph ?gA { ?msg6 con:text ?txt }\n" + "  } union {\n"
                            + "    graph ?gG { ?msg msg:forwardedMessage ?msg2 }\n"
                            + "    graph ?gF { ?msg2 msg:correspondingRemoteMessage ?msg3 }\n"
                            + "    graph ?gE { ?msg3 msg:forwardedMessage ?msg4 }\n"
                            + "    graph ?gD { ?msg4 msg:correspondingRemoteMessage ?msg5 }\n"
                            + "    graph ?gC { ?msg5 msg:forwardedMessage ?msg6 }\n"
                            + "    graph ?gB { ?msg6 msg:correspondingRemoteMessage ?msg7 }\n"
                            + "    graph ?gA { ?msg7 con:text ?txt }\n" + "  }\n" + "\n" + "}";
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
            String queryString = "prefix msg:   <https://w3id.org/won/message#>\n"
                            + "prefix agr:   <https://w3id.org/won/agreement#>\n" + "SELECT ?eventUri where {\n"
                            + " graph ?g {" + "  ?s agr:accepts ?eventUri .\n" + "}}";
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
            String queryString = "prefix msg:   <https://w3id.org/won/message#>\n"
                            + "prefix won:   <https://w3id.org/won/core#>\n" + "SELECT ?text where {\n" + " graph ?g {"
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
         * Adds the specified text as a con:text to the model's base resource.
         * 
         * @param message
         * @return
         */
        public static Model addProcessing(Model model, String message) {
            Resource baseRes = RdfUtils.findOrCreateBaseResource(model);
            baseRes.addProperty(WONCON.isProcessing, message, XSDDatatype.XSDstring);
            return model;
        }

        public static List<URI> getProposesEvents(final WonMessage wonMessage) {
            return getProposesEvents(wonMessage.getCompleteDataset());
        }

        public static List<URI> getProposesEvents(final Dataset messageDataset) {
            List<URI> proposesToCancelEvents = new ArrayList<>();
            String queryString = "prefix msg:   <https://w3id.org/won/message#>\n"
                            + "prefix agr:   <https://w3id.org/won/agreement#>\n" + "SELECT ?eventUri where {\n"
                            + " graph ?g {" + "  ?s agr:proposes ?eventUri .\n" + "}}";
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
            String queryString = "prefix msg:   <https://w3id.org/won/message#>\n"
                            + "prefix agr:   <https://w3id.org/won/agreement#>\n" + "SELECT ?eventUri where {\n"
                            + " graph ?g {" + "  ?s agr:proposesToCancel ?eventUri .\n" + "}}";
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
            String queryString = "prefix msg:   <https://w3id.org/won/message#>\n"
                            + "prefix agr:   <https://w3id.org/won/agreement#>\n" + "SELECT distinct ?prev where {\n"
                            + "   {" + "    ?msg msg:previousMessage ?prev .\n" + "   } union {"
                            + "    ?msg msg:correspondingRemoteMessage/msg:previousMessage ?prev " + "  }" + "}";
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
        public static Map<Property, String> getReviewContent(final WonMessage wonMessage)
                        throws IllegalArgumentException {
            System.out.println("message content: ");
            RDFDataMgr.write(System.out, wonMessage.getMessageContent(), Lang.TRIG);
            System.out.println("whole message: ");
            RDFDataMgr.write(System.out, wonMessage.getCompleteDataset(), Lang.TRIG);
            // find the review data in a wonMessage
            String queryString = "prefix s: <http://schema.org/>\n" + "select * where \n" + "  {graph ?g {\n"
                            + "    ?event s:review ?review .\n" + "    ?review s:reviewRating ?rating;\n"
                            + "        s:about ?about;\n" + "        s:author ?author .\n" + "    ?rating a s:Rating;\n"
                            + "        s:ratingValue ?ratingValue .\n" + "\n" + "}}";
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
            return RdfUtils.findFirstPropertyFromResource(dataset, uri, WONCON.text);
        }

        private static RDFNode getTextMessageForResource(Dataset dataset, Resource resource) {
            if (resource == null)
                return null;
            return RdfUtils.findFirstPropertyFromResource(dataset, resource, WONCON.text);
        }

        public static WonMessage copyByDatasetSerialization(final WonMessage toWrap) {
            WonMessage copied = new WonMessage(RdfUtils.readDatasetFromString(
                            RdfUtils.writeDatasetToString(toWrap.getCompleteDataset(), Lang.TRIG), Lang.TRIG));
            return copied;
        }
    }

    public static class SocketUtils {
        /**
         * Returns the socket in a connect message. Attempts to get it from the
         * specified message itself. If no such socket is found there, the targetSocket
         * of the correspondingRemoteMessage is used.
         * 
         * @param message
         * @return
         */
        public static URI getSocket(WonMessage message) {
            if (message.getEnvelopeType() == WonMessageDirection.FROM_EXTERNAL) {
                return message.getRecipientSocketURI();
            } else {
                return message.getSenderSocketURI();
            }
        }

        /**
         * Returns the targetSocket in a connect message. Attempts to get it from the
         * specified message itself. If no such socket is found there, the socket of the
         * correspondingRemoteMessage is used.
         * 
         * @param message
         * @return
         */
        public static URI getTargetSocket(WonMessage message) {
            if (message.getEnvelopeType() == WonMessageDirection.FROM_EXTERNAL) {
                return message.getSenderSocketURI();
            } else {
                return message.getRecipientSocketURI();
            }
        }

        /**
         * Calculates all compatible socket pairs in the two specified atoms defined in
         * the dataset.
         * 
         * @param dataset
         * @param leftAtom
         * @param rightAtom
         * @return
         */
        public static Set<Pair<URI>> getCompatibleSocketsForAtoms(Dataset dataset, URI firstAtom, URI secondAtom) {
            Set<URI> firstAtomSockets = getSocketsOfAtom(dataset, firstAtom);
            Set<URI> secondAtomSockets = getSocketsOfAtom(dataset, secondAtom);
            Set<Pair<URI>> ret = new HashSet<>();
            firstAtomSockets.forEach(firstAtomSocket -> {
                secondAtomSockets.forEach(secondAtomSocket -> {
                    if (isSocketsCompatible(dataset, firstAtomSocket, secondAtomSocket)) {
                        ret.add(new Pair(firstAtomSocket, secondAtomSocket));
                    }
                });
            });
            return ret;
        }

        public static Set<Pair<URI>> getIncompatibleSocketsForAtoms(Dataset dataset, URI firstAtom, URI secondAtom) {
            Set<URI> firstAtomSockets = getSocketsOfAtom(dataset, firstAtom);
            Set<URI> secondAtomSockets = getSocketsOfAtom(dataset, secondAtom);
            Set<Pair<URI>> ret = new HashSet<>();
            firstAtomSockets.forEach(firstAtomSocket -> {
                secondAtomSockets.forEach(secondAtomSocket -> {
                    if (!isSocketsCompatible(dataset, firstAtomSocket, secondAtomSocket)) {
                        ret.add(new Pair(firstAtomSocket, secondAtomSocket));
                    }
                });
            });
            return ret;
        }

        /**
         * Checks if the specified sockets are compatible.
         * 
         * @param dataset
         * @param firstAtomSocket
         * @param secondAtomSocket
         * @return
         */
        public static boolean isSocketsCompatible(Dataset dataset, URI firstAtomSocket, URI secondAtomSocket) {
            Set<URI> firstCompatibleDefs = getCompatibleSocketDefinitions(dataset, firstAtomSocket);
            Optional<URI> secondDef = getSocketDefinition(dataset, secondAtomSocket);
            if (!secondDef.isPresent()) {
                throw new IllegalArgumentException("No socket definition found for " + secondAtomSocket);
            }
            if (!firstCompatibleDefs.isEmpty() && !firstCompatibleDefs.contains(secondDef.get())) {
                return false;
            }
            Set<URI> secondCompatibleDefs = getCompatibleSocketDefinitions(dataset, secondAtomSocket);
            Optional<URI> firstDef = getSocketDefinition(dataset, firstAtomSocket);
            if (!firstDef.isPresent()) {
                throw new IllegalArgumentException("No socket definition found for " + firstAtomSocket);
            }
            if (!secondCompatibleDefs.isEmpty() && !secondCompatibleDefs.contains(firstDef.get())) {
                return false;
            }
            return true;
        }

        public static Optional<URI> getSocketDefinition(Dataset dataset, URI socket) {
            return RdfUtils
                            .getObjectStreamOfProperty(dataset, socket, URI.create(WON.socketDefinition.getURI()),
                                            node -> node.isURIResource() ? URI.create(node.asResource().getURI())
                                                            : null)
                            .findFirst();
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
         * Returns all sockets found in the model, attached to the null relative URI
         * '<>'. Returns an empty collection if there is no such socket.
         * 
         * @param content
         * @return
         */
        public static Collection<URI> getSockets(Model content) {
            Resource baseRes = RdfUtils.getBaseResource(content);
            StmtIterator stmtIterator = baseRes.listProperties(WON.socket);
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
         * Returns all sockets found in the model, attached to the null relative URI
         * '<>'. Returns an empty collection if there is no such socket.
         * 
         * @param content
         * @return
         */
        public static Optional<URI> getTypeOfSocket(Model content, URI socket) {
            Resource resource = content.getResource(socket.toString());
            Resource socketType = resource.getPropertyResourceValue(WON.socketDefinition);
            if (socketType != null && socketType.isURIResource()) {
                return Optional.of(URI.create(socketType.asResource().getURI()));
            }
            return Optional.empty();
        }

        public static Optional<URI> getTypeOfSocket(Dataset content, final URI socket) {
            return Optional.ofNullable(RdfUtils.findFirst(content, m -> getTypeOfSocket(m, socket).orElse(null)));
        }

        /**
         * Returns all sockets of the base resource of the given type.
         * 
         * @param model
         * @param subject
         * @param socketType
         * @return
         */
        public static Collection<URI> getSocketsOfType(Model model, URI socketType) {
            return getSocketsOfType(model, RdfUtils.getBaseResource(model), socketType);
        }

        /**
         * Returns all sockets of subject with the given type found in the model.
         * 
         * @param model
         * @param socketType
         * @return
         */
        public static Collection<URI> getSocketsOfType(Model model, URI subject, URI socketType) {
            return getSocketsOfType(model, model.getResource(subject.toString()), socketType);
        }

        /**
         * Returns all sockets of the given type found in the model.
         * 
         * @param model
         * @param socketType
         * @return
         */
        public static Collection<URI> getSocketsOfType(Model model, Resource subject, URI socketType) {
            StmtIterator stmtIterator = subject.listProperties(WON.socket);
            Resource socketTypeResource = model.getResource(socketType.toString());
            LinkedList<URI> ret = new LinkedList<URI>();
            while (stmtIterator.hasNext()) {
                RDFNode socket = stmtIterator.nextStatement().getObject();
                if (socket.isResource() && socket.isURIResource()) {
                    if (socket.asResource().hasProperty(WON.socketDefinition, socketTypeResource)) {
                        ret.add(URI.create(socket.toString()));
                    }
                }
            }
            return ret;
        }

        public static Collection<URI> getSocketsOfType(Dataset atomDataset, URI atomURI, URI socketType) {
            return RdfUtils.visitFlattenedToList(atomDataset, m -> getSocketsOfType(m, atomURI, socketType));
        }

        public static Optional<URI> getDefaultSocket(Model model, boolean returnAnyIfNoDefaultFound) {
            return getDefaultSocket(model, RdfUtils.getBaseResource(model), returnAnyIfNoDefaultFound);
        }

        public static Optional<URI> getDefaultSocket(Model model, URI subject, boolean returnAnyIfNoDefaultFound) {
            return getDefaultSocket(model, model.getResource(subject.toString()), returnAnyIfNoDefaultFound);
        }

        public static Set<URI> getSocketsOfAtom(Dataset atomDataset, URI atomURI) {
            return getSocketsOfAtomAsStream(atomDataset, atomURI).collect(Collectors.toSet());
        }

        public static Stream<URI> getSocketsOfAtomAsStream(Dataset atomDataset, URI atomURI) {
            return RdfUtils.getObjectStreamOfProperty(atomDataset, atomURI, URI.create(WON.socket.getURI()),
                            node -> node.isURIResource() ? URI.create(node.asResource().getURI())
                                            : null);
        }

        /**
         * Returns the default socket found in the model. If there is no default socket,
         * the result is empty. unless returnAnyIfNoDefaultFound is true, in which case
         * any socket may be returned. and there is no default socket, any socket may be
         * returned
         * 
         * @param model
         * @param subject
         * @param boolean returnAnyIfNoDefaultFound
         * @return
         */
        public static Optional<URI> getDefaultSocket(Model model, Resource subject, boolean returnAnyIfNoDefaultFound) {
            RDFNode socket = subject.getPropertyResourceValue(WON.defaultSocket);
            if (socket != null && socket.isURIResource()) {
                return Optional.of(URI.create(socket.toString()));
            }
            if (returnAnyIfNoDefaultFound) {
                StmtIterator stmtIterator = subject.listProperties(WON.socket);
                while (stmtIterator.hasNext()) {
                    socket = stmtIterator.next().getObject();
                    if (socket.isResource() && socket.isURIResource()) {
                        return Optional.of(URI.create(socket.toString()));
                    }
                }
            }
            return Optional.empty();
        }

        public static Optional<URI> getDefaultSocket(Dataset atomDataset, URI atomURI,
                        boolean returnAnyIfNoDefaultFound) {
            return Optional.ofNullable(RdfUtils.findFirst(atomDataset,
                            m -> getDefaultSocket(m, atomURI, returnAnyIfNoDefaultFound).orElse(null)));
        }

        /**
         * Adds a triple to the model of the form <> won:socket [socketURI].
         * 
         * @param model
         * @param socketURI
         */
        public static void addSocket(final Model model, final URI socketURI, final URI socketTypeURI,
                        final boolean isDefaultSocket) {
            Resource baseRes = RdfUtils.getBaseResource(model);
            Resource socket = model.createResource(socketURI.toString());
            baseRes.addProperty(WON.socket, socket);
            socket.addProperty(WON.socketDefinition, model.createResource(socketTypeURI.toString()));
            if (isDefaultSocket) {
                if (baseRes.hasProperty(WON.defaultSocket)) {
                    baseRes.removeAll(WON.defaultSocket);
                }
                baseRes.addProperty(WON.defaultSocket, socket);
            }
        }

        public static void addSocket(final Dataset dataset, final URI socketURI, final URI socketTypeURI,
                        final boolean isDefaultSocket) {
            visit(dataset, model -> {
                addSocket(model, socketURI, socketTypeURI, isDefaultSocket);
                return null;
            });
        }

        /**
         * Adds a triple to the model of the form <> won:targetSocket [socketURI].
         * 
         * @param content
         * @param socketURI
         */
        public static void addTargetSocket(final Model content, final URI socketURI) {
            Resource baseRes = RdfUtils.getBaseResource(content);
            baseRes.addProperty(WON.targetSocket, content.createResource(socketURI.toString()));
        }

        /**
         * Creates a model for connecting two sockets. Both sockets are optional, if
         * none are given the returned Optional is empty
         * 
         * @return
         */
        public static Optional<Model> createSocketModelForHintOrConnect(Optional<URI> socket,
                        Optional<URI> targetSocket) {
            if (!socket.isPresent() && !targetSocket.isPresent()) {
                return Optional.empty();
            }
            Model model = ModelFactory.createDefaultModel();
            Resource baseResource = findOrCreateBaseResource(model);
            if (socket.isPresent()) {
                baseResource.addProperty(WON.socket, model.getResource(socket.toString()));
            }
            if (targetSocket.isPresent()) {
                baseResource.addProperty(WON.targetSocket, model.getResource(targetSocket.toString()));
            }
            return Optional.of(model);
        }

        public static void setCompatibleSocketDefinitions(SocketDefinitionImpl socketConfiguration, Dataset dataset,
                        URI socketURI) {
            socketConfiguration.setCompatibleSocketTypes(getCompatibleSocketDefinitions(dataset, socketURI));
        }

        public static Set<URI> getCompatibleSocketDefinitions(Dataset dataset, URI socketURI) {
            return RdfUtils
                            .getObjectStreamOfProperty(dataset, socketURI, URI.create(WON.socketDefinition.getURI()),
                                            node -> node.isURIResource() ? URI.create(node.asResource().getURI())
                                                            : null)
                            .flatMap(def -> RdfUtils.getObjectStreamOfProperty(dataset, def,
                                            URI.create(WON.compatibleSocketDefinition.getURI()),
                                            node -> node.isURIResource() ? URI.create(node.asResource().getURI())
                                                            : null))
                            .collect(Collectors.toSet());
        }

        public static void setDerivationProperties(SocketDefinitionImpl socketConfiguration, Dataset dataset,
                        URI socketURI) {
            socketConfiguration.setDerivationProperties(RdfUtils
                            .getObjectStreamOfProperty(dataset, socketURI, URI.create(WON.socketDefinition.getURI()),
                                            node -> node.isURIResource() ? URI.create(node.asResource().getURI())
                                                            : null)
                            .flatMap(def -> RdfUtils.getObjectStreamOfProperty(dataset, def,
                                            URI.create(WON.derivesAtomProperty.getURI()),
                                            node -> node.isURIResource() ? URI.create(node.asResource().getURI())
                                                            : null))
                            .collect(Collectors.toSet()));
        }

        public static void setInverseDerivationProperties(SocketDefinitionImpl socketConfiguration, Dataset dataset,
                        URI socketURI) {
            socketConfiguration.setInverseDerivationProperties(RdfUtils
                            .getObjectStreamOfProperty(dataset, socketURI, URI.create(WON.socketDefinition.getURI()),
                                            node -> node.isURIResource() ? URI.create(node.asResource().getURI())
                                                            : null)
                            .flatMap(def -> RdfUtils.getObjectStreamOfProperty(dataset, def,
                                            URI.create(WON.derivesInverseAtomProperty.getURI()),
                                            node -> node.isURIResource() ? URI.create(node.asResource().getURI())
                                                            : null))
                            .collect(Collectors.toSet()));
        }

        public static void setAutoOpen(SocketDefinitionImpl socketConfiguration, Dataset dataset, URI socketURI) {
            Set<Boolean> autoOpens = RdfUtils
                            .getObjectStreamOfProperty(dataset, socketURI, URI.create(WON.socketDefinition.getURI()),
                                            node -> node.isURIResource() ? URI.create(node.asResource().getURI())
                                                            : null)
                            .flatMap(def -> RdfUtils.getObjectStreamOfProperty(dataset, def,
                                            URI.create(WON.autoOpen.getURI()),
                                            node -> node.isLiteral() ? (Boolean) node.asLiteral().getBoolean() : null))
                            .collect(Collectors.toSet());
            if (autoOpens.size() > 1) {
                socketConfiguration.addInconsistentProperty(URI.create(WON.autoOpen.getURI()));
            } else if (autoOpens.size() == 1) {
                socketConfiguration.setAutoOpen(autoOpens.iterator().next());
            }
        }

        public static void setSocketCapacity(SocketDefinitionImpl socketConfiguration, Dataset dataset, URI socketURI) {
            Set<Integer> socketCapacities = RdfUtils
                            .getObjectStreamOfProperty(dataset, socketURI, URI.create(WON.socketDefinition.getURI()),
                                            node -> node.isURIResource() ? URI.create(node.asResource().getURI())
                                                            : null)
                            .flatMap(def -> RdfUtils.getObjectStreamOfProperty(dataset, def,
                                            URI.create(WON.socketCapacity.getURI()),
                                            node -> node.isLiteral() ? (Integer) node.asLiteral().getInt() : null))
                            .collect(Collectors.toSet());
            if (socketCapacities.size() > 1) {
                socketConfiguration.addInconsistentProperty(URI.create(WON.socketCapacity.getURI()));
            } else if (socketCapacities.size() == 1) {
                socketConfiguration.setCapacity(socketCapacities.iterator().next());
            }
        }

        public static Optional<URI> getAtomOfSocket(Dataset dataset, URI socketURI) {
            return RdfUtils.getFirstStatementMapped(dataset, null, URI.create(WON.socket.getURI()), socketURI,
                            s -> s.getSubject().isURIResource() ? URI.create(s.getSubject().getURI()) : null);
        }
    }

    public static class ConnectionUtils {
        public static boolean isConnected(Dataset connectionDataset, URI connectionUri) {
            URI connectionState = getConnectionState(connectionDataset, connectionUri);
            return ConnectionState.CONNECTED.getURI().equals(connectionState);
        }

        public static URI getConnectionState(Dataset connectionDataset, URI connectionUri) {
            Path statePath = PathParser.parse("won:connectionState", DefaultPrefixUtils.getDefaultPrefixes());
            return RdfUtils.getURIPropertyForPropertyPath(connectionDataset, connectionUri, statePath);
        }

        /**
         * return the atomURI of a connection
         *
         * @param dataset <code>Dataset</code> object which contains connection
         * information
         * @param connectionURI
         * @return <code>URI</code> of the atom
         */
        public static URI getLocalAtomURIFromConnection(Dataset dataset, final URI connectionURI) {
            return URI.create(
                            findOnePropertyFromResource(dataset, connectionURI, WON.sourceAtom).asResource().getURI());
        }

        public static URI getTargetAtomURIFromConnection(Dataset dataset, final URI connectionURI) {
            return URI.create(
                            findOnePropertyFromResource(dataset, connectionURI, WON.targetAtom).asResource().getURI());
        }

        public static URI getWonNodeURIFromConnection(Dataset dataset, final URI connectionURI) {
            return URI.create(findOnePropertyFromResource(dataset, connectionURI, WON.wonNode).asResource().getURI());
        }

        public static URI getWonNodeURIFromAtom(Dataset dataset, final URI atomURI) {
            return URI.create(findOnePropertyFromResource(dataset, atomURI, WON.wonNode).asResource().getURI());
        }

        public static URI getTargetConnectionURIFromConnection(Dataset dataset, final URI connectionURI) {
            return URI.create(findOnePropertyFromResource(dataset, connectionURI, WON.targetConnection).asResource()
                            .getURI());
        }

        public static URI getLastMessageSentByLocalAtom(Dataset dataset, final URI connectionURI) {
            throw new NotYetImplementedException();
        }

        public static URI getLastMessageSentByTargetAtom(Dataset dataset, final URI connectionURI) {
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

    public static class AtomUtils {
        /**
         * searches for a subject of type won:Atom and returns the AtomURI
         *
         * @param dataset <code>Dataset</code> object which will be searched for the
         * AtomURI
         * @return <code>URI</code> which is of type won:Atom
         */
        public static URI getAtomURI(Dataset dataset) {
            return RdfUtils.findOne(dataset, new RdfUtils.ModelVisitor<URI>() {
                @Override
                public URI visit(final Model model) {
                    return getAtomURI(model);
                }
            }, true);
        }

        /**
         * searches for a subject of type won:Atom and returns the AtomURI
         *
         * @param model <code>Model</code> object which will be searched for the AtomURI
         * @return <code>URI</code> which is of type won:Atom
         */
        public static URI getAtomURI(Model model) {
            Resource res = getAtomResource(model);
            return res == null ? null : URI.create(res.getURI());
        }

        /**
         * searches for a subject of type won:Atom and returns the AtomURI
         *
         * @param model <code>Model</code> object which will be searched for the AtomURI
         * @return <code>URI</code> which is of type won:Atom
         */
        public static Resource getAtomResource(Model model) {
            List<Resource> atomURIs = new ArrayList<>();
            ResIterator iterator = model.listSubjectsWithProperty(RDF.type, WON.Atom);
            while (iterator.hasNext()) {
                atomURIs.add(iterator.next());
            }
            if (atomURIs.size() == 0)
                return null;
            else if (atomURIs.size() == 1)
                return atomURIs.get(0);
            else if (atomURIs.size() > 1) {
                Resource u = atomURIs.get(0);
                for (Resource uri : atomURIs) {
                    if (!uri.equals(u))
                        throw new IncorrectPropertyCountException(1, 2);
                }
                return u;
            } else
                return null;
        }

        /**
         * searches for a subject of type won:Atom and returns the AtomURI
         *
         * @param dataset <code>Dataset</code> object which will be searched for the
         * AtomURI
         * @return <code>URI</code> which is of type won:Atom
         */
        public static Resource getAtomResource(Dataset dataset) {
            Model model = new AtomModelWrapper(dataset).copyAtomModel(AtomGraphType.ATOM);
            return getAtomResource(model);
        }

        public static URI getWonNodeURIFromAtom(Dataset dataset, final URI atomURI) {
            return URI.create(findOnePropertyFromResource(dataset, atomURI, WON.wonNode).asResource().getURI());
        }

        /**
         * 
         */
        public static Iterator<URI> getConnectedAtoms(Dataset dataset, final URI atomURI) {
            PrefixMapping pmap = new PrefixMappingImpl();
            pmap.withDefaultMappings(PrefixMapping.Standard);
            pmap.setNsPrefix("won", WON.getURI());
            pmap.setNsPrefix("msg", WONMSG.getURI());
            Path path = PathParser.parse("won:hasConnectionContainer/rdfs:member/won:targetAtom", pmap);
            return RdfUtils.getURIsForPropertyPath(dataset, atomURI, path);
        }

        /**
         * Assumes that the dataset contains an atom's connection information, looks for
         * the specified remote atoms and returns the remote connection uris. Optionally
         * the result can be filtered by connection state.
         */
        public static Set<URI> getTargetConnectionURIsForTargetAtoms(Dataset dataset, final Collection<URI> targetAtoms,
                        final Optional<ConnectionState> state) {
            Optional<Object> unions = targetAtoms.stream().map(uri -> {
                BasicPattern pattern = new BasicPattern();
                pattern.add(Triple.create(Var.alloc("localCon"),
                                NodeFactory.createURI("https://w3id.org/won/core#targetAtom"),
                                NodeFactory.createURI(uri.toString())));
                pattern.add(Triple.create(Var.alloc("localCon"),
                                NodeFactory.createURI("https://w3id.org/won/core#targetConnection"),
                                Var.alloc("remoteCon")));
                if (state.isPresent()) {
                    pattern.add(Triple.create(Var.alloc("localCon"),
                                    NodeFactory.createURI("https://w3id.org/won/core#connectionState"),
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
