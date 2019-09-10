/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.protocol.util.linkeddata;

import org.apache.jena.graph.Node;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.*;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathParser;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.model.AtomState;
import won.protocol.model.SocketDefinition;
import won.protocol.model.SocketDefinitionImpl;
import won.protocol.service.WonNodeInfo;
import won.protocol.util.RdfUtils;
import won.protocol.util.RdfUtils.Pair;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Utilitiy functions for common linked data lookups.
 */
public class WonLinkedDataUtils {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static URI getConnectionStateforConnectionURI(URI connectionURI, LinkedDataSource linkedDataSource) {
        assert linkedDataSource != null : "linkedDataSource must not be null";
        Dataset dataset = getDataForResource(connectionURI, linkedDataSource);
        Path propertyPath = PathParser.parse("<" + WON.connectionState + ">", PrefixMapping.Standard);
        return RdfUtils.getURIPropertyForPropertyPath(dataset, connectionURI, propertyPath);
    }

    public static URI getAtomURIforConnectionURI(URI connectionURI, LinkedDataSource linkedDataSource) {
        assert linkedDataSource != null : "linkedDataSource must not be null";
        Dataset dataset = getDataForResource(connectionURI, linkedDataSource);
        Path propertyPath = PathParser.parse("<" + WON.sourceAtom + ">", PrefixMapping.Standard);
        return RdfUtils.getURIPropertyForPropertyPath(dataset, connectionURI, propertyPath);
    }

    public static URI getTargetConnectionURIforConnectionURI(URI connectionURI, LinkedDataSource linkedDataSource) {
        assert linkedDataSource != null : "linkedDataSource must not be null";
        Dataset dataset = getDataForResource(connectionURI, linkedDataSource);
        Path propertyPath = PathParser.parse("<" + WON.targetConnection + ">", PrefixMapping.Standard);
        return RdfUtils.getURIPropertyForPropertyPath(dataset, connectionURI, propertyPath);
    }

    public static URI getTargetAtomURIforConnectionURI(URI connectionURI, LinkedDataSource linkedDataSource) {
        assert linkedDataSource != null : "linkedDataSource must not be null";
        Dataset dataset = getDataForResource(connectionURI, linkedDataSource);
        Path propertyPath = PathParser.parse("<" + WON.targetAtom + ">", PrefixMapping.Standard);
        return RdfUtils.getURIPropertyForPropertyPath(dataset, connectionURI, propertyPath);
    }

    public static URI getMessageContainerURIforConnectionURI(URI connectionURI, LinkedDataSource linkedDataSource) {
        assert linkedDataSource != null : "linkedDataSource must not be null";
        Dataset dataset = getDataForResource(connectionURI, linkedDataSource);
        Path propertyPath = PathParser.parse("<" + WON.messageContainer + ">", PrefixMapping.Standard);
        return RdfUtils.getURIPropertyForPropertyPath(dataset, connectionURI, propertyPath);
    }

    public static URI getMessageContainerURIforAtomURI(URI atomURI, LinkedDataSource linkedDataSource) {
        assert linkedDataSource != null : "linkedDataSource must not be null";
        Dataset dataset = getDataForResource(atomURI, linkedDataSource);
        Path propertyPath = PathParser.parse("<" + WON.messageContainer + ">", PrefixMapping.Standard);
        return RdfUtils.getURIPropertyForPropertyPath(dataset, atomURI, propertyPath);
    }

    public static Dataset getConversationAndAtomsDataset(String connectionURI, LinkedDataSource linkedDataSource) {
        return getConversationAndAtomsDataset(URI.create(connectionURI), linkedDataSource);
    }

    public static List<URI> getNodeAtomUris(URI nodeURI, LinkedDataSource linkedDataSource) {
        return getNodeAtomUris(nodeURI, null, null, null, null, null, linkedDataSource);
    }

    public static List<URI> getNodeAtomUris(URI nodeURI, ZonedDateTime modifiedAfter, ZonedDateTime createdAfter,
                    AtomState atomState, URI filterBySocketTypeUri, URI filterByAtomTypeUri,
                    LinkedDataSource linkedDataSource) {
        Dataset nodeDataset = getDataForResource(nodeURI, linkedDataSource);
        WonNodeInfo wonNodeInfo = WonRdfUtils.WonNodeUtils.getWonNodeInfo(nodeURI, nodeDataset);
        URI atomListUri = URI.create(wonNodeInfo.getAtomListURI());
        String newQuery = atomListUri.getQuery();
        if (atomState != null) {
            String queryPart = "state=" + atomState;
            newQuery = (newQuery == null) ? queryPart : (newQuery + "&" + queryPart);
        }
        if (modifiedAfter != null) {
            String queryPart = "modifiedafter=" + modifiedAfter;
            newQuery = (newQuery == null) ? queryPart : (newQuery + "&" + queryPart);
        }
        if (createdAfter != null) {
            String queryPart = "createdafter=" + createdAfter;
            newQuery = (newQuery == null) ? queryPart : (newQuery + "&" + queryPart);
        }
        if (filterByAtomTypeUri != null) {
            String queryPart = "filterByAtomTypeUri=" + filterByAtomTypeUri;
            newQuery = (newQuery == null) ? queryPart : (newQuery + "&" + queryPart);
        }
        if (filterBySocketTypeUri != null) {
            String queryPart = "filterBySocketTypeUri=" + filterBySocketTypeUri;
            newQuery = (newQuery == null) ? queryPart : (newQuery + "&" + queryPart);
        }
        try {
            atomListUri = new URI(atomListUri.getScheme(), atomListUri.getAuthority(), atomListUri.getPath(), newQuery,
                            atomListUri.getFragment());
        } catch (URISyntaxException e) {
            logger.warn("Could not append parameters to nodeURI, proceeding request without parameters");
        }
        Dataset atomListDataset = getDataForResource(atomListUri, linkedDataSource);
        return RdfUtils.visitFlattenedToList(atomListDataset, model -> {
            StmtIterator it = model.listStatements((Resource) null, RDFS.member, (RDFNode) null);
            List<URI> ret = new ArrayList<>();
            while (it.hasNext()) {
                ret.add(URI.create(it.next().getObject().toString()));
            }
            return ret;
        });
    }

    public static Dataset getFullAtomDataset(URI atomURI, LinkedDataSource linkedDataSource) {
        assert linkedDataSource != null : "linkedDataSource must not be null";
        int depth = 7;
        int maxRequests = 1000;
        List<Path> propertyPaths = new ArrayList<>();
        PrefixMapping pmap = new PrefixMappingImpl();
        pmap.withDefaultMappings(PrefixMapping.Standard);
        pmap.setNsPrefix("won", WON.getURI());
        pmap.setNsPrefix("msg", WONMSG.getURI());
        propertyPaths.add(PathParser.parse("won:connections", pmap));
        propertyPaths.add(PathParser.parse("won:connections/rdfs:member", pmap));
        propertyPaths.add(PathParser.parse("won:connections/rdfs:member/won:messageContainer", pmap));
        propertyPaths.add(PathParser.parse("won:connections/rdfs:member/won:messageContainer/rdfs:member", pmap));
        propertyPaths.add(PathParser.parse(
                        "won:connections/rdfs:member/won:messageContainer/rdfs:member/msg:correspondingRemoteMessage",
                        pmap));
        propertyPaths.add(PathParser.parse(
                        "won:connections/rdfs:member/won:messageContainer/rdfs:member/msg:previousMessage", pmap));
        propertyPaths.add(PathParser.parse("won:connections/rdfs:member/won:sourceAtom", pmap));
        propertyPaths.add(PathParser.parse("won:connections/rdfs:member/won:sourceAtom/won:messageContainer", pmap));
        propertyPaths.add(PathParser
                        .parse("won:connections/rdfs:member/won:sourceAtom/won:messageContainer/rdfs:member", pmap));
        propertyPaths.add(PathParser.parse(
                        "won:connections/rdfs:member/won:sourceAtom/won:messageContainer/rdfs:member/msg:previousMessage",
                        pmap));
        return linkedDataSource.getDataForResourceWithPropertyPath(atomURI, atomURI, propertyPaths, maxRequests, depth);
    }

    public static Dataset getConversationAndAtomsDataset(URI connectionURI, LinkedDataSource linkedDataSource) {
        assert linkedDataSource != null : "linkedDataSource must not be null";
        int depth = 5;
        int maxRequests = 1000;
        List<Path> propertyPaths = new ArrayList<>();
        PrefixMapping pmap = new PrefixMappingImpl();
        pmap.withDefaultMappings(PrefixMapping.Standard);
        pmap.setNsPrefix("won", WON.getURI());
        pmap.setNsPrefix("msg", WONMSG.getURI());
        propertyPaths.add(PathParser.parse("won:messageContainer", pmap));
        propertyPaths.add(PathParser.parse("won:messageContainer/rdfs:member", pmap));
        propertyPaths.add(PathParser.parse("won:messageContainer/rdfs:member/msg:correspondingRemoteMessage", pmap));
        propertyPaths.add(PathParser.parse("won:messageContainer/rdfs:member/msg:previousMessage", pmap));
        propertyPaths.add(PathParser.parse("won:sourceAtom", pmap));
        propertyPaths.add(PathParser.parse("won:sourceAtom/won:messageContainer", pmap));
        propertyPaths.add(PathParser.parse("won:sourceAtom/won:messageContainer/rdfs:member", pmap));
        propertyPaths.add(
                        PathParser.parse("won:sourceAtom/won:messageContainer/rdfs:member/msg:previousMessage", pmap));
        propertyPaths.add(PathParser.parse("won:targetAtom", pmap));
        propertyPaths.add(PathParser.parse("won:targetAtom/won:messageContainer", pmap));
        propertyPaths.add(PathParser.parse("won:targetAtom/won:messageContainer/rdfs:member", pmap));
        propertyPaths.add(
                        PathParser.parse("won:targetAtom/won:messageContainer/rdfs:member/msg:previousMessage", pmap));
        propertyPaths.add(PathParser.parse("won:targetConnection", pmap));
        propertyPaths.add(PathParser.parse("won:targetConnection/won:messageContainer", pmap));
        propertyPaths.add(PathParser.parse("won:targetConnection/won:messageContainer/rdfs:member", pmap));
        propertyPaths.add(PathParser.parse(
                        "won:targetConnection/won:messageContainer/rdfs:member/msg:correspondingRemoteMessage", pmap));
        propertyPaths.add(PathParser.parse("won:targetConnection/won:messageContainer/rdfs:member/msg:previousMessage",
                        pmap));
        URI requesterWebId = WonLinkedDataUtils.getAtomURIforConnectionURI(connectionURI, linkedDataSource);
        return linkedDataSource.getDataForResourceWithPropertyPath(connectionURI, requesterWebId, propertyPaths,
                        maxRequests, depth);
    }

    public static Dataset getConversationDataset(String connectionURI, LinkedDataSource linkedDataSource) {
        return getConversationDataset(URI.create(connectionURI), linkedDataSource);
    }

    public static Dataset getConversationDataset(URI connectionURI, LinkedDataSource linkedDataSource) {
        assert linkedDataSource != null : "linkedDataSource must not be null";
        int depth = 5; // depth 3 from connection gives us the messages in the conversation
        int maxRequests = 1000;
        List<Path> propertyPaths = new ArrayList<>();
        PrefixMapping pmap = new PrefixMappingImpl();
        pmap.withDefaultMappings(PrefixMapping.Standard);
        pmap.setNsPrefix("won", WON.getURI());
        pmap.setNsPrefix("msg", WONMSG.getURI());
        propertyPaths.add(PathParser.parse("won:messageContainer", pmap));
        propertyPaths.add(PathParser.parse("won:messageContainer/rdfs:member", pmap));
        propertyPaths.add(PathParser.parse("won:targetConnection", pmap));
        propertyPaths.add(PathParser.parse("won:targetConnection/won:messageContainer", pmap));
        propertyPaths.add(PathParser.parse("won:targetConnection/won:messageContainer/rdfs:member", pmap));
        URI requesterWebId = WonLinkedDataUtils.getAtomURIforConnectionURI(connectionURI, linkedDataSource);
        return linkedDataSource.getDataForResourceWithPropertyPath(connectionURI, requesterWebId, propertyPaths,
                        maxRequests, depth);
    }

    public static Dataset getDataForResource(final URI connectionURI, final LinkedDataSource linkedDataSource) {
        assert linkedDataSource != null : "linkedDataSource must not be null";
        assert connectionURI != null : "connection URI must not be null";
        Dataset dataset = null;
        logger.debug("loading model for connection {}", connectionURI);
        dataset = linkedDataSource.getDataForResource(connectionURI);
        if (dataset == null) {
            throw new IllegalStateException("failed to load model for Connection " + connectionURI);
        }
        return dataset;
    }

    public static Iterator<Dataset> getModelForURIs(final Iterator<URI> uriIterator,
                    final LinkedDataSource linkedDataSource) {
        assert linkedDataSource != null : "linkedDataSource must not be null";
        return new ModelFetchingIterator(uriIterator, linkedDataSource);
    }

    public static URI getWonNodeURIForAtomOrConnectionURI(final URI resourceURI, LinkedDataSource linkedDataSource) {
        assert linkedDataSource != null : "linkedDataSource must not be null";
        logger.debug("fetching WON node URI for resource {} with linked data source {}", resourceURI, linkedDataSource);
        return getWonNodeURIForAtomOrConnection(resourceURI, linkedDataSource.getDataForResource(resourceURI));
    }

    public static URI getWonNodeURIForAtomOrConnection(final URI resURI, final Model resourceModel) {
        assert resourceModel != null : "model must not be null";
        // we didnt't get the queue name. Check if the model contains a triple <baseuri>
        // won:wonNode
        // <wonNode> and get the information from there.
        logger.debug("getting WON node URI from model");
        Resource baseResource = resourceModel.getResource(resURI.toString());
        logger.debug("resourceModel: {}", RdfUtils.toString(resourceModel));
        StmtIterator wonNodeStatementIterator = baseResource.listProperties(WON.wonNode);
        if (!wonNodeStatementIterator.hasNext()) {
            // no won:wonNode triple found. we can't do anything.
            logger.debug("base resource {} has no won:wonNode property", baseResource);
            return null;
        }
        Statement stmt = wonNodeStatementIterator.nextStatement();
        RDFNode wonNodeNode = stmt.getObject();
        if (!wonNodeNode.isResource()) {
            logger.debug("won:wonNode property of base resource {} is not a resource", baseResource);
            return null;
        }
        URI wonNodeUri = URI.create(wonNodeNode.asResource().getURI().toString());
        logger.debug("obtained WON node URI: {}", wonNodeUri);
        if (wonNodeStatementIterator.hasNext()) {
            logger.warn("multiple WON node URIs found for resource {}, using first one: {} ", baseResource, wonNodeUri);
        }
        return wonNodeUri;
    }

    public static URI getWonNodeURIForAtomOrConnection(final URI resourceURI, final Dataset resourceDataset) {
        return RdfUtils.findFirst(resourceDataset, new RdfUtils.ModelVisitor<URI>() {
            @Override
            public URI visit(final Model model) {
                return getWonNodeURIForAtomOrConnection(resourceURI, model);
            }
        });
    }

    /**
     * For the specified atom or connection URI, the model is fetched, the WON node
     * URI found there is also de-referenced, and the specified property path is
     * evaluated in that graph, starting at the WON node URI.
     *
     * @param resourceURI
     * @param propertyPath
     * @param linkedDataSource
     * @return
     */
    public static Node getWonNodePropertyForAtomOrConnectionURI(URI resourceURI, Path propertyPath,
                    LinkedDataSource linkedDataSource) {
        assert linkedDataSource != null : "linkedDataSource must not be null";
        URI wonNodeUri = WonLinkedDataUtils.getWonNodeURIForAtomOrConnectionURI(resourceURI, linkedDataSource);
        Dataset nodeDataset = linkedDataSource.getDataForResource(wonNodeUri);
        return RdfUtils.getNodeForPropertyPath(nodeDataset, wonNodeUri, propertyPath);
    }

    /**
     * For the specified URI, the model is fetched and the specified property path
     * is evaluated in that graph, starting at the specified URI.
     *
     * @param resourceURI
     * @param propertyPath
     * @param linkedDataSource
     * @return
     */
    public static Node getPropertyForURI(URI resourceURI, Path propertyPath, LinkedDataSource linkedDataSource) {
        assert linkedDataSource != null : "linkedDataSource must not be null";
        Dataset dataset = linkedDataSource.getDataForResource(resourceURI);
        return RdfUtils.getNodeForPropertyPath(dataset, resourceURI, propertyPath);
    }

    public static Optional<URI> getDefaultSocket(URI atomURI, boolean returnAnyIfNoDefaultFound,
                    LinkedDataSource linkedDataSource) {
        return WonRdfUtils.SocketUtils.getDefaultSocket(getDataForResource(atomURI, linkedDataSource), atomURI,
                        returnAnyIfNoDefaultFound);
    }

    public static Collection<URI> getSocketsOfType(URI atomURI, URI socketTypeURI, LinkedDataSource linkedDataSource) {
        return WonRdfUtils.SocketUtils.getSocketsOfType(getDataForResource(atomURI, linkedDataSource), atomURI,
                        socketTypeURI);
    }

    /**
     * Executes {@link WonRdfUtils.SocketUtils.getCompatibleSocketsForAtoms} after
     * crawling the required data.
     * 
     * @param linkedDataSource
     * @param firstAtom
     * @param secondAtom
     * @return set of pairs, the first member belonging to the firstAtom, the second
     * to the secondAtom
     */
    public static Set<Pair<URI>> getCompatibleSocketsForAtoms(LinkedDataSource linkedDataSource, URI firstAtom,
                    URI secondAtom) {
        Dataset dataset = loadDataForAtomWithSocketDefinitions(linkedDataSource, firstAtom);
        RdfUtils.addDatasetToDataset(dataset, loadDataForAtomWithSocketDefinitions(linkedDataSource, secondAtom));
        return WonRdfUtils.SocketUtils.getCompatibleSocketsForAtoms(dataset, firstAtom, secondAtom);
    }

    public static Set<Pair<URI>> getIncompatibleSocketsForAtoms(LinkedDataSource linkedDataSource, URI firstAtom,
                    URI secondAtom) {
        Dataset dataset = loadDataForAtomWithSocketDefinitions(linkedDataSource, firstAtom);
        RdfUtils.addDatasetToDataset(dataset, loadDataForAtomWithSocketDefinitions(linkedDataSource, secondAtom));
        return WonRdfUtils.SocketUtils.getIncompatibleSocketsForAtoms(dataset, firstAtom, secondAtom);
    }

    public static boolean isCompatibleSockets(LinkedDataSource linkedDataSource, URI firstSocket,
                    URI secondSocket) {
        Optional<URI> firstAtom = getAtomOfSocket(firstSocket, linkedDataSource);
        Optional<URI> secondAtom = getAtomOfSocket(secondSocket, linkedDataSource);
        if (!firstAtom.isPresent()) {
            throw new IllegalStateException("Could not determine atom of socket " + firstSocket);
        }
        if (!secondAtom.isPresent()) {
            throw new IllegalStateException("Could not determine atom of socket " + secondSocket);
        }
        Dataset dataset = loadDataForAtomWithSocketDefinitions(linkedDataSource, firstAtom.get());
        RdfUtils.addDatasetToDataset(dataset, loadDataForAtomWithSocketDefinitions(linkedDataSource, secondAtom.get()));
        return WonRdfUtils.SocketUtils.isSocketsCompatible(dataset, firstSocket, secondSocket);
    }

    public static Dataset loadDataForAtomWithSocketDefinitions(LinkedDataSource linkedDataSource, URI atomURI) {
        Dataset dataset = linkedDataSource.getDataForResource(atomURI);
        Set<URI> sockets = WonRdfUtils.SocketUtils.getSocketsOfAtom(dataset, atomURI);
        sockets.forEach(socket -> {
            RdfUtils.addDatasetToDataset(dataset, loadDataForSocket(linkedDataSource, socket));
        });
        return dataset;
    }

    public static Dataset loadDataForSocket(LinkedDataSource linkedDataSource, URI socket) {
        Dataset dataset = linkedDataSource.getDataForResource(socket);
        // load all data for configurations
        List<URI> configURIs = RdfUtils.getObjectsOfProperty(dataset, socket,
                        URI.create(WON.socketDefinition.getURI()),
                        node -> node.isURIResource() ? URI.create(node.asResource().getURI()) : null);
        if (configURIs.size() > 1) {
            throw new IllegalArgumentException("More than one socket definition found for socket " + socket);
        }
        if (configURIs.size() == 0) {
            throw new IllegalArgumentException("No socket definition found for socket " + socket);
        }
        configURIs.stream().forEach(configURI -> {
            Dataset ds = linkedDataSource.getDataForResource(configURI);
            RdfUtils.addDatasetToDataset(dataset, ds);
        });
        return dataset;
    }

    public static Optional<SocketDefinition> getSocketDefinition(LinkedDataSource linkedDataSource, URI socket) {
        Dataset dataset = linkedDataSource.getDataForResource(socket);
        // load all data for configurations
        List<URI> configURIs = RdfUtils.getObjectsOfProperty(dataset, socket,
                        URI.create(WON.socketDefinition.getURI()),
                        node -> node.isURIResource() ? URI.create(node.asResource().getURI()) : null);
        if (configURIs.size() > 1) {
            throw new IllegalArgumentException("More than one socket definition found for socket " + socket);
        }
        if (configURIs.size() == 0) {
            throw new IllegalArgumentException("No socket definition found for socket " + socket);
        }
        configURIs.stream().forEach(configURI -> {
            Dataset ds = linkedDataSource.getDataForResource(configURI);
            RdfUtils.addDatasetToDataset(dataset, ds);
        });
        URI socketDefinitionURI = configURIs.stream().findFirst().get();
        SocketDefinitionImpl socketDef = new SocketDefinitionImpl(socket);
        // if a socket definition is referenced via won:socketDefinition, it has to be
        // the subject of a triple
        boolean isSocketDefFound = RdfUtils.findFirst(dataset, model -> {
            if (model.listStatements(new SimpleSelector(model.createResource(socketDefinitionURI.toString()), null,
                            (RDFNode) null)).hasNext()) {
                return socket;
            }
            return null;
        }) != null;
        if (!isSocketDefFound) {
            throw new IllegalArgumentException("Could not find data for socket definition " + socketDefinitionURI
                            + " of socket " + socket);
        }
        socketDef.setSocketDefinitionURI(socketDefinitionURI);
        WonRdfUtils.SocketUtils.setCompatibleSocketDefinitions(socketDef, dataset, socket);
        WonRdfUtils.SocketUtils.setAutoOpen(socketDef, dataset, socket);
        WonRdfUtils.SocketUtils.setSocketCapacity(socketDef, dataset, socket);
        WonRdfUtils.SocketUtils.setDerivationProperties(socketDef, dataset, socket);
        WonRdfUtils.SocketUtils.setInverseDerivationProperties(socketDef, dataset, socket);
        return Optional.of(socketDef);
    }

    public static Optional<URI> getTypeOfSocket(URI socketURI, LinkedDataSource linkedDataSource) {
        return WonRdfUtils.SocketUtils.getTypeOfSocket(getDataForResource(socketURI, linkedDataSource), socketURI);
    }

    public static Optional<URI> getAtomOfSocket(URI socketURI, LinkedDataSource linkedDataSource) {
        return WonRdfUtils.SocketUtils.getAtomOfSocket(getDataForResource(socketURI, linkedDataSource), socketURI);
    }

    /**
     * Crawls all connections of the specified atom without messages.
     */
    public static Dataset getConnectionNetwork(URI atomURI, LinkedDataSource linkedDataSource) {
        assert linkedDataSource != null : "linkedDataSource must not be null";
        int depth = 5;
        int maxRequests = 1000;
        List<Path> propertyPaths = new ArrayList<>();
        PrefixMapping pmap = new PrefixMappingImpl();
        pmap.withDefaultMappings(PrefixMapping.Standard);
        pmap.setNsPrefix("won", WON.getURI());
        pmap.setNsPrefix("msg", WONMSG.getURI());
        propertyPaths.add(PathParser.parse("won:connections", pmap));
        propertyPaths.add(PathParser.parse("won:connections/rdfs:member", pmap));
        return linkedDataSource.getDataForResourceWithPropertyPath(atomURI, atomURI, propertyPaths, maxRequests, depth);
    }

    /**
     * Iterator implementation that fetches linked data lazily for the specified
     * iterator of URIs.
     */
    private static class ModelFetchingIterator implements Iterator<Dataset> {
        private Iterator<URI> uriIterator = null;
        private LinkedDataSource linkedDataSource = null;

        private ModelFetchingIterator(final Iterator<URI> uriIterator, final LinkedDataSource linkedDataSource) {
            this.uriIterator = uriIterator;
            this.linkedDataSource = linkedDataSource;
        }

        @Override
        public Dataset next() {
            URI uri = uriIterator.next();
            return linkedDataSource.getDataForResource(uri);
        }

        @Override
        public boolean hasNext() {
            return uriIterator.hasNext();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("this iterator cannot remove");
        }
    }
}
