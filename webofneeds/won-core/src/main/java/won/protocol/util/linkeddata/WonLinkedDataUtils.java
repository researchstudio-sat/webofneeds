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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
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
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageUtils;
import won.protocol.model.AtomState;
import won.protocol.model.Connection;
import won.protocol.model.SocketDefinition;
import won.protocol.model.SocketDefinitionImpl;
import won.protocol.rest.DatasetResponseWithStatusCodeAndHeaders;
import won.protocol.rest.LinkedDataFetchingException;
import won.protocol.service.WonNodeInfo;
import won.protocol.util.RdfUtils;
import won.protocol.util.RdfUtils.Pair;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.uriresolver.WonRelativeUriHelper;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utilitiy functions for common linked data lookups.
 */
public class WonLinkedDataUtils {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Ehcache linkedDataObjectCache;
    private static final Integer DEFAULT_FETCH_ATOM_URIS_PAGE_SIZE = 200;
    private static final Pattern LDP_PREV_LINK_PATTERN = Pattern.compile("<([^>]*)>; rel=\"prev\"");
    private static final Pattern LDP_NEXT_LINK_PATTERN = Pattern.compile("<([^>]*)>; rel=\"next\"");
    static {
        CacheManager manager = CacheManager.getInstance();
        linkedDataObjectCache = new Cache("linkedDataObjectCache", 1000, false, false, 3600, 3600);
        manager.addCache(linkedDataObjectCache);
    }

    /**
     * Use
     * {@link WonLinkedDataUtils#getConnectionStateforConnectionURIUsingAtomAsWebId(URI, LinkedDataSource)}
     * or
     * {@link WonLinkedDataUtils#getConnectionStateforConnectionURI(URI, URI, LinkedDataSource)}
     * instead.
     * 
     * @param connectionURI
     * @param linkedDataSource
     * @return
     */
    @Deprecated
    public static URI getConnectionStateforConnectionURI(URI connectionURI, LinkedDataSource linkedDataSource) {
        return getConnectionStateforConnectionURI(connectionURI,
                        WonRelativeUriHelper.stripConnectionSuffix(connectionURI), linkedDataSource);
    }

    public static URI getConnectionStateforConnectionURIUsingAtomAsWebId(URI connectionURI,
                    LinkedDataSource linkedDataSource) {
        return getConnectionStateforConnectionURI(connectionURI,
                        WonRelativeUriHelper.stripConnectionSuffix(connectionURI), linkedDataSource);
    }

    public static URI getConnectionStateforConnectionURI(URI connectionURI, URI webId,
                    LinkedDataSource linkedDataSource) {
        Objects.requireNonNull(linkedDataSource);
        Objects.requireNonNull(connectionURI);
        Dataset dataset = linkedDataSource.getDataForResource(connectionURI, webId);
        Path propertyPath = PathParser.parse("<" + WON.connectionState + ">", PrefixMapping.Standard);
        return RdfUtils.getURIPropertyForPropertyPath(dataset, connectionURI, propertyPath);
    }

    public static Optional<URI> getConnectionURIForIncomingMessage(WonMessage wonMessage,
                    LinkedDataSource linkedDataSource) {
        try {
            if (wonMessage.getMessageTypeRequired().isSocketHintMessage()) {
                return WonLinkedDataUtils.getConnectionURIForSocketAndTargetSocket(
                                wonMessage.getRecipientSocketURIRequired(),
                                wonMessage.getHintTargetSocketURIRequired(), linkedDataSource,
                                WonMessageUtils.getRecipientAtomURIRequired(wonMessage));
            } else if (wonMessage.getMessageTypeRequired().isAtomHintMessage()) {
                return Optional.empty();
            }
            if (wonMessage.isMessageWithBothResponses()) {
                // message with both responses is an incoming message from another atom.
                // our node's response (the remote response, in this delivery chain) has the
                // connection URI
                return Optional.of(wonMessage.getRemoteResponse().get().getConnectionURIRequired());
            } else if (wonMessage.isMessageWithResponse()) {
                // message with onlny one response is our node's response plus the echo
                // our node's response (the response in this delivery chain) has the connection
                // URI
                if (wonMessage.getHeadMessage().get().getMessageTypeRequired().isConnectionSpecificMessage()) {
                    return Optional.of(wonMessage.getResponse().get().getConnectionURIRequired());
                } else {
                    return Optional.empty();
                }
            } else if (wonMessage.isRemoteResponse()) {
                // only a remote response. Our connection URI isn't there at all
                // here, we fetch it from the node by asking for the connection for the two
                // sockets
                // - we could also use some kind of local storage for that.
                return WonLinkedDataUtils.getConnectionURIForSocketAndTargetSocket(
                                wonMessage.getRecipientSocketURIRequired(),
                                wonMessage.getSenderSocketURIRequired(), linkedDataSource,
                                WonMessageUtils.getRecipientAtomURIRequired(wonMessage));
            }
        } catch (Exception e) {
            logger.debug("Error fetching connection for incoming message",
                            wonMessage.getSenderSocketURI(), wonMessage.getRecipientSocketURI(), e);
        }
        return Optional.empty();
    }

    public static Optional<URI> getConnectionURIForOutgoingMessage(WonMessage wonMessage,
                    LinkedDataSource linkedDataSource) {
        if (wonMessage.getMessageTypeRequired().isConnectionSpecificMessage()) {
            try {
                return WonLinkedDataUtils.getConnectionURIForSocketAndTargetSocket(
                                wonMessage.getSenderSocketURIRequired(),
                                wonMessage.getRecipientSocketURIRequired(), linkedDataSource,
                                WonMessageUtils.getSenderAtomURIRequired(wonMessage));
            } catch (Exception e) {
                logger.debug("Error fetching conection for outgoing message via socket {} and target socket {}",
                                wonMessage.getSenderSocketURI(), wonMessage.getRecipientSocketURI(), e);
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public static Optional<Connection> getConnectionForIncomingMessage(WonMessage wonMessage,
                    LinkedDataSource linkedDataSource) {
        Optional<URI> connectionURI = getConnectionURIForIncomingMessage(wonMessage, linkedDataSource);
        return connectionURI.map(uri -> WonRdfUtils.ConnectionUtils
                        .getConnection(linkedDataSource.getDataForResource(uri,
                                        WonMessageUtils.getOwnAtomFromIncomingMessage(wonMessage).get()), uri));
    }

    public static Optional<Connection> getConnectionForOutgoingMessage(WonMessage wonMessage,
                    LinkedDataSource linkedDataSource) {
        Optional<URI> connectionURI = getConnectionURIForOutgoingMessage(wonMessage, linkedDataSource);
        return connectionURI.map(uri -> WonRdfUtils.ConnectionUtils
                        .getConnection(linkedDataSource
                                        .getDataForResource(uri,
                                                        WonMessageUtils.getOwnAtomFromOutgoingMessage(wonMessage)
                                                                        .get()),
                                        uri));
    }

    public static Optional<Connection> getConnectionForConnectionURI(URI connectionURI,
                    LinkedDataSource linkedDataSource) {
        return Optional.ofNullable(WonRdfUtils.ConnectionUtils
                        .getConnection(linkedDataSource.getDataForPublicResource(connectionURI), connectionURI));
    }

    /**
     * Use
     * {@link won.protocol.util.linkeddata.uriresolver.WonRelativeUriHelper#stripConnectionSuffix(URI)}
     * instead.
     */
    @Deprecated
    public static URI getAtomURIforConnectionURI(URI connectionURI, LinkedDataSource linkedDataSource) {
        assert linkedDataSource != null : "linkedDataSource must not be null";
        Dataset dataset = getDataForPublicResource(connectionURI, linkedDataSource);
        Path propertyPath = PathParser.parse("<" + WON.sourceAtom + ">", PrefixMapping.Standard);
        return RdfUtils.getURIPropertyForPropertyPath(dataset, connectionURI, propertyPath);
    }

    public static URI getTargetConnectionURIforConnectionURI(URI connectionURI, LinkedDataSource linkedDataSource) {
        return getTargetConnectionURIforConnectionURI(connectionURI,
                        WonRelativeUriHelper.stripConnectionSuffix(connectionURI), linkedDataSource);
    }

    public static URI getTargetConnectionURIforConnectionURI(URI connectionURI, URI webId,
                    LinkedDataSource linkedDataSource) {
        Objects.requireNonNull(connectionURI);
        Objects.requireNonNull(linkedDataSource);
        Dataset dataset = linkedDataSource.getDataForResource(connectionURI, webId);
        Path propertyPath = PathParser.parse("<" + WON.targetConnection + ">", PrefixMapping.Standard);
        return RdfUtils.getURIPropertyForPropertyPath(dataset, connectionURI, propertyPath);
    }

    /**
     * Use
     * {@link WonLinkedDataUtils#getTargetAtomURIforConnectionURIUsingAtomAsWebId(URI, LinkedDataSource)}
     * or
     * {@link WonLinkedDataUtils#getTargetAtomURIforConnectionURI(URI, URI, LinkedDataSource)}
     * instead.
     */
    @Deprecated
    public static URI getTargetAtomURIforConnectionURI(URI connectionURI, LinkedDataSource linkedDataSource) {
        return getTargetAtomURIforConnectionURI(connectionURI,
                        WonRelativeUriHelper.stripConnectionSuffix(connectionURI), linkedDataSource);
    }

    public static URI getTargetAtomURIforConnectionURIUsingAtomAsWebId(URI connectionURI,
                    LinkedDataSource linkedDataSource) {
        return getTargetAtomURIforConnectionURI(connectionURI,
                        WonRelativeUriHelper.stripConnectionSuffix(connectionURI), linkedDataSource);
    }

    public static URI getTargetAtomURIforConnectionURI(URI connectionURI, URI webId,
                    LinkedDataSource linkedDataSource) {
        Objects.requireNonNull(connectionURI);
        Objects.requireNonNull(linkedDataSource);
        Dataset dataset = linkedDataSource.getDataForResource(connectionURI, webId);
        Path propertyPath = PathParser.parse("<" + WON.targetAtom + ">", PrefixMapping.Standard);
        return RdfUtils.getURIPropertyForPropertyPath(dataset, connectionURI, propertyPath);
    }

    /**
     * Use
     * {@link won.protocol.util.linkeddata.uriresolver.WonRelativeUriHelper#createMessageContainerURIForConnection(URI)}
     * instead.
     */
    @Deprecated
    public static URI getMessageContainerURIforConnectionURI(URI connectionURI, LinkedDataSource linkedDataSource) {
        assert linkedDataSource != null : "linkedDataSource must not be null";
        Dataset dataset = getDataForPublicResource(connectionURI, linkedDataSource);
        Path propertyPath = PathParser.parse("<" + WON.messageContainer + ">", PrefixMapping.Standard);
        return RdfUtils.getURIPropertyForPropertyPath(dataset, connectionURI, propertyPath);
    }

    /**
     * Use
     * {@link WonLinkedDataUtils#getSocketURIForConnectionURIUsingAtomAsWebId(URI, LinkedDataSource)}
     * or
     * {@link WonLinkedDataUtils#getSocketURIForConnectionURI(URI, URI, LinkedDataSource)}
     * instead.
     */
    @Deprecated
    public static URI getSocketURIForConnectionURI(URI connectionURI, LinkedDataSource linkedDataSource) {
        return getSocketURIForConnectionURI(connectionURI, WonRelativeUriHelper.stripConnectionSuffix(connectionURI),
                        linkedDataSource);
    }

    public static URI getSocketURIForConnectionURIUsingAtomAsWebId(URI connectionURI,
                    LinkedDataSource linkedDataSource) {
        return getSocketURIForConnectionURI(connectionURI, WonRelativeUriHelper.stripConnectionSuffix(connectionURI),
                        linkedDataSource);
    }

    public static URI getSocketURIForConnectionURI(URI connectionURI, URI webId, LinkedDataSource linkedDataSource) {
        Objects.requireNonNull(linkedDataSource);
        Objects.requireNonNull(connectionURI);
        Dataset dataset = linkedDataSource.getDataForResource(connectionURI, webId);
        return WonRdfUtils.ConnectionUtils.getSocketURIFromConnection(dataset, connectionURI);
    }

    /**
     * Use
     * {@link WonLinkedDataUtils#getTargetSocketURIForConnectionURIUsingAtomAsWebId(URI, LinkedDataSource)}
     * or
     * {@link WonLinkedDataUtils#getTargetSocketURIForConnectionURI(URI, URI, LinkedDataSource)}
     * instead.
     */
    @Deprecated
    public static URI getTargetSocketURIForConnectionURI(URI connectionURI, LinkedDataSource linkedDataSource) {
        return getTargetSocketURIForConnectionURI(connectionURI,
                        WonRelativeUriHelper.stripConnectionSuffix(connectionURI), linkedDataSource);
    }

    public static URI getTargetSocketURIForConnectionURIUsingAtomAsWebId(URI connectionURI,
                    LinkedDataSource linkedDataSource) {
        return getTargetSocketURIForConnectionURI(connectionURI,
                        WonRelativeUriHelper.stripConnectionSuffix(connectionURI), linkedDataSource);
    }

    public static URI getTargetSocketURIForConnectionURI(URI connectionURI, URI webId,
                    LinkedDataSource linkedDataSource) {
        Objects.requireNonNull(linkedDataSource);
        Objects.requireNonNull(connectionURI);
        Dataset dataset = linkedDataSource.getDataForResource(connectionURI, webId);
        return WonRdfUtils.ConnectionUtils.getTargetSocketURIFromConnection(dataset, connectionURI);
    }

    /**
     * Use
     * {@link won.protocol.util.linkeddata.uriresolver.WonRelativeUriHelper#createMessageContainerURIForAtom(URI)}
     * instead.
     */
    @Deprecated
    public static URI getMessageContainerURIforAtomURI(URI atomURI, LinkedDataSource linkedDataSource) {
        assert linkedDataSource != null : "linkedDataSource must not be null";
        Dataset dataset = getDataForPublicResource(atomURI, linkedDataSource);
        Path propertyPath = PathParser.parse("<" + WON.messageContainer + ">", PrefixMapping.Standard);
        return RdfUtils.getURIPropertyForPropertyPath(dataset, atomURI, propertyPath);
    }

    @Deprecated
    public static Dataset getConversationAndAtomsDataset(String connectionURI, LinkedDataSource linkedDataSource) {
        return getConversationAndAtomsDataset(URI.create(connectionURI), linkedDataSource);
    }

    public static List<URI> getNodeAtomUris(URI nodeURI, LinkedDataSource linkedDataSource) {
        return getNodeAtomUrisPage(nodeURI, null, null, null, null, null, null, null, linkedDataSource)
                        .getContent();
    }

    public static List<URI> getNodeAtomUris(URI nodeURI, ZonedDateTime modifiedAfter, ZonedDateTime createdAfter,
                    AtomState atomState, URI filterBySocketTypeUri, URI filterByAtomTypeUri,
                    LinkedDataSource linkedDataSource) {
        return getNodeAtomUrisPage(nodeURI, modifiedAfter, createdAfter, atomState, filterBySocketTypeUri,
                        filterByAtomTypeUri, null, null, linkedDataSource).getContent();
    }

    /**
     * Fetch <code>pageSize</code> atoms with specified restrictions, starting at
     * page <code>page</code>.
     *
     * @param nodeURI
     * @param modifiedAfter
     * @param createdAfter
     * @param atomState
     * @param filterBySocketTypeUri
     * @param filterByAtomTypeUri
     * @param linkedDataSource
     * @param page the page index, 1-based
     * @param pageSize
     * @return
     */
    public static LDPContainerPage<List<URI>> getNodeAtomUrisPage(URI nodeURI, ZonedDateTime modifiedAfter,
                    ZonedDateTime createdAfter,
                    AtomState atomState, URI filterBySocketTypeUri, URI filterByAtomTypeUri,
                    Integer page, Integer pageSize, LinkedDataSource linkedDataSource) {
        Dataset nodeDataset = getDataForPublicResource(nodeURI, linkedDataSource);
        WonNodeInfo wonNodeInfo = WonRdfUtils.WonNodeUtils.getWonNodeInfo(nodeURI, nodeDataset);
        URI atomListUri = URI.create(wonNodeInfo.getAtomListURI());
        Map<String, String> params = extractQueryParams(atomListUri.getQuery());
        HttpHeaders headers = new HttpHeaders();
        addOptionalQueryParam(params, "state", atomState);
        addOptionalQueryParam(params, "createdafter", createdAfter);
        addOptionalQueryParam(params, "modifiedafter", modifiedAfter);
        addOptionalQueryParam(params, "filterByAtomTypeUri", filterByAtomTypeUri);
        addOptionalQueryParam(params, "filterBySocketTypeUri", filterBySocketTypeUri);
        if (pageSize != null || page != null) {
            if (page <= 0) {
                throw new IllegalArgumentException("The page index must be a positive integer");
            }
            // if we have a page, use a default page size
            // if we only have a page size, use page 1 (first page in LDP Paging)
            if (page == null) {
                page = 1;
            }
            if (pageSize == null) {
                pageSize = DEFAULT_FETCH_ATOM_URIS_PAGE_SIZE;
            }
            headers.set("Prefer", String.format("return=representation; max-member-count=\"%d\"", pageSize));
        }
        if (page != null) {
            addOptionalQueryParam(params, "p", page.toString());
        }
        try {
            atomListUri = new URI(atomListUri.getScheme(), atomListUri.getAuthority(), atomListUri.getPath(),
                            toQueryString(params),
                            atomListUri.getFragment());
        } catch (URISyntaxException e) {
            logger.warn("Could not append parameters to nodeURI, proceeding request without parameters");
        }
        DatasetResponseWithStatusCodeAndHeaders result = getDataForResourceWithHeaders(atomListUri, headers,
                        linkedDataSource);
        List<URI> uris = RdfUtils.visitFlattenedToList(result.getDataset(), model -> {
            StmtIterator it = model.listStatements(null, RDFS.member, (RDFNode) null);
            List<URI> ret = new ArrayList<>();
            while (it.hasNext()) {
                ret.add(URI.create(it.next().getObject().toString()));
            }
            return ret;
        });
        return new LDPContainerPage<List<URI>>(uris, result.getResponseHeaders());
    }

    /**
     * Fetch <code>pageSize</code> atoms with specified restrictions, starting after
     * the specified atom <code>resumeAfter</code>.
     *
     * @param nodeURI
     * @param modifiedAfter
     * @param createdAfter
     * @param atomState
     * @param filterBySocketTypeUri
     * @param filterByAtomTypeUri
     * @param resumeAfter the next link URI as returned by the container. Must start
     * with the node URI.
     * @param pageSize
     * @param linkedDataSource
     * @return
     */
    public static LDPContainerPage<List<URI>> getNodeAtomUrisPageAfter(URI nodeURI, ZonedDateTime modifiedAfter,
                    ZonedDateTime createdAfter,
                    AtomState atomState, URI filterBySocketTypeUri, URI filterByAtomTypeUri,
                    URI resumeAfter, int pageSize, LinkedDataSource linkedDataSource) {
        URI atomListUri = null;
        if (resumeAfter == null) {
            Dataset nodeDataset = getDataForPublicResource(nodeURI, linkedDataSource);
            WonNodeInfo wonNodeInfo = WonRdfUtils.WonNodeUtils.getWonNodeInfo(nodeURI, nodeDataset);
            atomListUri = URI.create(wonNodeInfo.getAtomListURI());
        } else {
            if (!resumeAfter.toString().startsWith(nodeURI.toString())) {
                throw new IllegalArgumentException(String.format(
                                "URI for resumeAfter is provided as '%s'but it does not start with node URI '%s'",
                                resumeAfter, nodeURI));
            }
            atomListUri = resumeAfter;
        }
        Map<String, String> params = extractQueryParams(atomListUri.getQuery());
        HttpHeaders headers = new HttpHeaders();
        addOptionalQueryParam(params, "state", atomState);
        addOptionalQueryParam(params, "createdafter", createdAfter);
        addOptionalQueryParam(params, "modifiedafter", modifiedAfter);
        addOptionalQueryParam(params, "filterByAtomTypeUri", filterByAtomTypeUri);
        addOptionalQueryParam(params, "filterBySocketTypeUri", filterBySocketTypeUri);
        if (pageSize <= 0) {
            throw new IllegalArgumentException("The pageSize must be a positive integer");
        }
        headers.set("Prefer", String.format("return=representation; max-member-count=\"%d\"", pageSize));
        try {
            atomListUri = new URI(atomListUri.getScheme(), atomListUri.getAuthority(), atomListUri.getPath(),
                            toQueryString(params),
                            atomListUri.getFragment());
        } catch (URISyntaxException e) {
            logger.warn("Could not append parameters to nodeURI, proceeding request without parameters");
        }
        DatasetResponseWithStatusCodeAndHeaders result = getDataForResourceWithHeaders(atomListUri, headers,
                        linkedDataSource);
        List<URI> uris = RdfUtils.visitFlattenedToList(result.getDataset(), model -> {
            StmtIterator it = model.listStatements(null, RDFS.member, (RDFNode) null);
            List<URI> ret = new ArrayList<>();
            while (it.hasNext()) {
                ret.add(URI.create(it.next().getObject().toString()));
            }
            return ret;
        });
        return new LDPContainerPage<List<URI>>(uris, result.getResponseHeaders());
    }

    private static Map<String, String> extractQueryParams(String query) {
        Map<String, String> result = new HashMap<>();
        if (StringUtils.isEmpty(query)) {
            return result;
        }
        String[] keyValuePairs = query.split("&");
        for (int i = 0; i < keyValuePairs.length; i++) {
            String[] keyValue = keyValuePairs[i].split("=");
            result.put(keyValue[0], keyValue[1]);
        }
        return result;
    }

    private static void addOptionalQueryParam(Map<String, String> params, String paramName, Object paramValue) {
        if (paramValue != null
                        && !StringUtils.isEmpty(paramValue.toString())
                        && !StringUtils.isEmpty(paramName)) {
            // TODO: avoid duplicate query params
            params.put(paramName, paramValue.toString());
        }
    }

    private static String toQueryString(Map<String, String> params) {
        return params.entrySet().stream()
                        .map((e) -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining("&"));
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

    /**
     * Use
     * {@link WonLinkedDataUtils#getConversationAndAtomsDatasetUsingAtomUriAsWebId(URI, LinkedDataSource)}
     * or
     * {@link WonLinkedDataUtils#getConversationAndAtomsDataset(URI, URI, LinkedDataSource)}
     * instead.
     */
    @Deprecated
    public static Dataset getConversationAndAtomsDataset(URI connectionURI, LinkedDataSource linkedDataSource) {
        return getConversationAndAtomsDataset(connectionURI, linkedDataSource);
    }

    public static Dataset getConversationAndAtomsDatasetUsingAtomUriAsWebId(URI connectionURI,
                    LinkedDataSource linkedDataSource) {
        return getConversationAndAtomsDataset(connectionURI, WonRelativeUriHelper.stripConnectionSuffix(connectionURI),
                        linkedDataSource);
    }

    public static Dataset getConversationAndAtomsDataset(URI connectionURI, URI requesterWebId,
                    LinkedDataSource linkedDataSource) {
        Objects.requireNonNull(linkedDataSource);
        Objects.requireNonNull(connectionURI);
        int depth = 5;
        int maxRequests = 1000;
        List<Path> propertyPaths = new ArrayList<>();
        PrefixMapping pmap = new PrefixMappingImpl();
        pmap.withDefaultMappings(PrefixMapping.Standard);
        pmap.setNsPrefix("won", WON.getURI());
        pmap.setNsPrefix("msg", WONMSG.getURI());
        propertyPaths.add(PathParser.parse("won:messageContainer", pmap));
        propertyPaths.add(PathParser.parse("won:messageContainer/rdfs:member", pmap));
        propertyPaths.add(PathParser.parse("won:messageContainer/rdfs:member/msg:previousMessage", pmap));
        return linkedDataSource.getDataForResourceWithPropertyPath(connectionURI, requesterWebId, propertyPaths,
                        maxRequests, depth);
    }

    public static Optional<WonNodeInfo> findWonNode(URI someURI, Optional<URI> requesterWebID,
                    LinkedDataSource linkedDataSource) {
        assert linkedDataSource != null : "linkedDataSource must not be null";
        int depth = 5;
        int maxRequests = 1000;
        List<Path> propertyPaths = new ArrayList<>();
        PrefixMapping pmap = new PrefixMappingImpl();
        pmap.withDefaultMappings(PrefixMapping.Standard);
        pmap.setNsPrefix("won", WON.getURI());
        pmap.setNsPrefix("msg", WONMSG.getURI());
        pmap.setNsPrefix("rdfs", RDFS.getURI());
        propertyPaths.add(PathParser.parse("^rdfs:member / ^won:messageContainer / ^won:wonNode", pmap));
        propertyPaths.add(PathParser.parse("^won:messageContainer / ^won:wonNode", pmap));
        propertyPaths.add(PathParser.parse("^won:wonNode", pmap));
        propertyPaths.add(PathParser.parse("rdfs:member / ^won:wonNode", pmap));
        Dataset ds = linkedDataSource.getDataForResourceWithPropertyPath(someURI, requesterWebID, propertyPaths,
                        maxRequests, depth);
        WonNodeInfo info = WonRdfUtils.WonNodeUtils.getWonNodeInfo(ds);
        return Optional.ofNullable(info);
    }

    /**
     * Use {@link WonLinkedDataUtils#getConversationDataset(URI, LinkedDataSource)}
     * instead.
     */
    @Deprecated
    public static Dataset getConversationDataset(String connectionURI, LinkedDataSource linkedDataSource) {
        return getConversationDataset(URI.create(connectionURI), linkedDataSource);
    }

    /**
     * Use
     * {@link WonLinkedDataUtils#getConversationDataset(URI, URI, LinkedDataSource)}
     * or
     * {@link WonLinkedDataUtils#getConversationDatasetUsingAtomAsWebId(URI, LinkedDataSource)}.
     */
    @Deprecated
    public static Dataset getConversationDataset(URI connectionURI, LinkedDataSource linkedDataSource) {
        return getConversationDataset(connectionURI, WonRelativeUriHelper.stripConnectionSuffix(connectionURI),
                        linkedDataSource);
    }

    public static Dataset getConversationDatasetUsingAtomAsWebId(URI connectionURI, LinkedDataSource linkedDataSource) {
        return getConversationDataset(connectionURI, WonRelativeUriHelper.stripConnectionSuffix(connectionURI),
                        linkedDataSource);
    }

    public static Dataset getConversationDataset(URI connectionURI, URI requesterWebId,
                    LinkedDataSource linkedDataSource) {
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
        return linkedDataSource.getDataForResourceWithPropertyPath(connectionURI, requesterWebId, propertyPaths,
                        maxRequests, depth);
    }

    public static Dataset getDataForPublicResource(final URI resourceURI, final LinkedDataSource linkedDataSource) {
        Objects.requireNonNull(resourceURI);
        logger.debug("loading model for resource {}", resourceURI);
        Dataset dataset = linkedDataSource.getDataForPublicResource(resourceURI);
        if (dataset == null) {
            throw new IllegalStateException("failed to load model for resource " + resourceURI);
        }
        return dataset;
    }

    public static DatasetResponseWithStatusCodeAndHeaders getDataForResourceWithHeaders(final URI resourceURI,
                    HttpHeaders headers,
                    final LinkedDataSource linkedDataSource) {
        Objects.requireNonNull(resourceURI);
        logger.debug("loading model for resource {}", resourceURI);
        DatasetResponseWithStatusCodeAndHeaders response = linkedDataSource
                        .getDatasetWithHeadersForResource(resourceURI, headers);
        Dataset dataset = response.getDataset();
        if (dataset == null) {
            throw new IllegalStateException("failed to load model for resource " + resourceURI);
        }
        return response;
    }

    public static Iterator<Dataset> getModelForURIs(final Iterator<URI> uriIterator,
                    final LinkedDataSource linkedDataSource) {
        assert linkedDataSource != null : "linkedDataSource must not be null";
        return new ModelFetchingIterator(uriIterator, linkedDataSource);
    }

    public static Optional<WonNodeInfo> getWonNodeInfo(URI wonNodeUri, LinkedDataSource linkedDataSource) {
        Dataset nodeDataset = getDataForPublicResource(wonNodeUri, linkedDataSource);
        return Optional.ofNullable(WonRdfUtils.WonNodeUtils.getWonNodeInfo(wonNodeUri, nodeDataset));
    }

    /**
     * Use
     * {@link won.protocol.util.linkeddata.uriresolver.WonRelativeUriHelper#stripAtomSuffix(URI)}
     * instead.
     */
    @Deprecated
    public static URI getWonNodeURIForAtomOrConnectionURI(final URI resourceURI, LinkedDataSource linkedDataSource) {
        assert linkedDataSource != null : "linkedDataSource must not be null";
        logger.debug("fetching WON node URI for resource {} with linked data source {}", resourceURI, linkedDataSource);
        return getWonNodeURIForAtomOrConnection(resourceURI, linkedDataSource.getDataForPublicResource(resourceURI));
    }

    /**
     * Use
     * {@link won.protocol.util.linkeddata.uriresolver.WonRelativeUriHelper#stripAtomSuffix(URI)}
     * instead.
     */
    @Deprecated
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
        URI wonNodeUri = URI.create(wonNodeNode.asResource().getURI());
        logger.debug("obtained WON node URI: {}", wonNodeUri);
        if (wonNodeStatementIterator.hasNext()) {
            logger.warn("multiple WON node URIs found for resource {}, using first one: {} ", baseResource, wonNodeUri);
        }
        return wonNodeUri;
    }

    /**
     * Use
     * {@link won.protocol.util.linkeddata.uriresolver.WonRelativeUriHelper#stripAtomSuffix(URI)}
     * instead.
     */
    @Deprecated
    public static URI getWonNodeURIForAtomOrConnection(final URI resourceURI, final Dataset resourceDataset) {
        return RdfUtils.findFirst(resourceDataset, model -> getWonNodeURIForAtomOrConnection(resourceURI, model));
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
        URI wonNodeUri = WonRelativeUriHelper.stripAtomSuffix(resourceURI);
        Dataset nodeDataset = linkedDataSource.getDataForPublicResource(wonNodeUri);
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
        Dataset dataset = linkedDataSource.getDataForPublicResource(resourceURI);
        return RdfUtils.getNodeForPropertyPath(dataset, resourceURI, propertyPath);
    }

    /**
     * Deprecated as it assumes public readabiliby. Use
     * {@link WonLinkedDataUtils#getSocketsOfType(URI, URI, URI, LinkedDataSource)}
     * instead.
     * 
     * @param atomURI
     * @param socketTypeURI
     * @param linkedDataSource
     * @return
     */
    @Deprecated
    public static Collection<URI> getSocketsOfType(URI atomURI, URI socketTypeURI, LinkedDataSource linkedDataSource) {
        return WonRdfUtils.SocketUtils.getSocketsOfType(getDataForPublicResource(atomURI, linkedDataSource), atomURI,
                        socketTypeURI);
    }

    public static Collection<URI> getSocketsOfType(URI atomURI, URI socketTypeURI, URI webId,
                    LinkedDataSource linkedDataSource) {
        return WonRdfUtils.SocketUtils.getSocketsOfType(linkedDataSource.getDataForResource(atomURI, webId), atomURI,
                        socketTypeURI);
    }

    /**
     * Executes {@link WonRdfUtils.SocketUtils#getCompatibleSocketsForAtoms} after
     * crawling the required data. Deprecated as it assumes public readbility of
     * atoms. Use
     * {@link WonLinkedDataUtils#getCompatibleSocketsForAtoms(LinkedDataSource, URI, URI, URI)}
     * instead.
     *
     * @param linkedDataSource
     * @param firstAtom
     * @param secondAtom
     * @return set of pairs, the first member belonging to the firstAtom, the second
     * to the secondAtom
     */
    @Deprecated
    public static Set<Pair<URI>> getCompatibleSocketsForAtoms(LinkedDataSource linkedDataSource, URI firstAtom,
                    URI secondAtom) {
        Dataset dataset = loadDataForAtomWithSocketDefinitions(linkedDataSource, firstAtom);
        RdfUtils.addDatasetToDataset(dataset, loadDataForAtomWithSocketDefinitions(linkedDataSource, secondAtom));
        return WonRdfUtils.SocketUtils.getCompatibleSocketsForAtoms(dataset, firstAtom, secondAtom);
    }

    public static Set<Pair<URI>> getCompatibleSocketsForAtoms(LinkedDataSource linkedDataSource, URI firstAtom,
                    URI secondAtom, URI webId) {
        Dataset dataset = loadDataForAtomWithSocketDefinitions(linkedDataSource, firstAtom, webId);
        RdfUtils.addDatasetToDataset(dataset,
                        loadDataForAtomWithSocketDefinitions(linkedDataSource, secondAtom, webId));
        return WonRdfUtils.SocketUtils.getCompatibleSocketsForAtoms(dataset, firstAtom, secondAtom);
    }

    /**
     * Deprecated as it assumes public readability. Use
     * {@link WonLinkedDataUtils#getIncompatibleSocketsForAtoms(LinkedDataSource, URI, URI, URI)}
     * instead.
     */
    @Deprecated
    public static Set<Pair<URI>> getIncompatibleSocketsForAtoms(LinkedDataSource linkedDataSource, URI firstAtom,
                    URI secondAtom) {
        Dataset dataset = loadDataForAtomWithSocketDefinitions(linkedDataSource, firstAtom);
        RdfUtils.addDatasetToDataset(dataset, loadDataForAtomWithSocketDefinitions(linkedDataSource, secondAtom));
        return WonRdfUtils.SocketUtils.getIncompatibleSocketsForAtoms(dataset, firstAtom, secondAtom);
    }

    public static Set<Pair<URI>> getIncompatibleSocketsForAtoms(LinkedDataSource linkedDataSource, URI firstAtom,
                    URI secondAtom, URI webId) {
        Dataset dataset = loadDataForAtomWithSocketDefinitions(linkedDataSource, firstAtom, webId);
        RdfUtils.addDatasetToDataset(dataset,
                        loadDataForAtomWithSocketDefinitions(linkedDataSource, secondAtom, webId));
        return WonRdfUtils.SocketUtils.getIncompatibleSocketsForAtoms(dataset, firstAtom, secondAtom);
    }

    public static boolean isCompatibleSockets(LinkedDataSource linkedDataSource, URI firstSocket,
                    URI secondSocket) {
        URI firstAtom = WonRelativeUriHelper.stripFragment(firstSocket);
        URI secondAtom = WonRelativeUriHelper.stripFragment(secondSocket);
        Dataset dataset = loadDataForAtomWithSocketDefinitions(linkedDataSource, firstAtom);
        RdfUtils.addDatasetToDataset(dataset, loadDataForAtomWithSocketDefinitions(linkedDataSource, secondAtom));
        return WonRdfUtils.SocketUtils.isSocketsCompatible(dataset, firstSocket, secondSocket);
    }

    /**
     * Assumes we can use the atom URI as requestor's webId.
     * 
     * @param linkedDataSource
     * @param atomURI
     * @return
     */
    public static Dataset loadDataForAtomWithSocketDefinitions(LinkedDataSource linkedDataSource, URI atomURI) {
        return loadDataForAtomWithSocketDefinitions(linkedDataSource, atomURI, atomURI);
    }

    public static Dataset loadDataForAtomWithSocketDefinitions(LinkedDataSource linkedDataSource, URI atomURI,
                    URI webId) {
        Dataset dataset = linkedDataSource.getDataForResource(atomURI, webId);
        Set<URI> sockets = WonRdfUtils.SocketUtils.getSocketsOfAtom(dataset, atomURI);
        sockets.forEach(socket -> RdfUtils.addDatasetToDataset(dataset,
                        loadDataForSocket(linkedDataSource, socket, webId)));
        return dataset;
    }

    /**
     * Use
     * {@link WonLinkedDataUtils#loadDataForSocketUsingAtomAsWebId(LinkedDataSource, URI)}
     * or {@link WonLinkedDataUtils#loadDataForSocket(LinkedDataSource, URI, URI)}
     * instead
     * 
     * @param linkedDataSource
     * @param socket
     * @return
     */
    @Deprecated
    public static Dataset loadDataForSocket(LinkedDataSource linkedDataSource, URI socket) {
        return loadDataForSocket(linkedDataSource, socket, WonRelativeUriHelper.stripFragment(socket));
    }

    public static Dataset loadDataForSocketUsingAtomAsWebId(LinkedDataSource linkedDataSource, URI socket) {
        return loadDataForSocket(linkedDataSource, socket, WonRelativeUriHelper.stripFragment(socket));
    }

    public static Dataset loadDataForSocket(LinkedDataSource linkedDataSource, URI socket, URI webId) {
        Dataset dataset = linkedDataSource.getDataForResource(socket, webId);
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
            Dataset ds = linkedDataSource.getDataForResource(configURI, webId);
            RdfUtils.addDatasetToDataset(dataset, ds);
        });
        return dataset;
    }

    /**
     * Use
     * {@link WonLinkedDataUtils#getSocketDefinitionOfSocketUsingAtomAsWebid(LinkedDataSource, URI)}
     * or
     * {@link WonLinkedDataUtils#getSocketDefinitionOfSocket(LinkedDataSource, URI, URI)}
     * instead.
     * 
     * @param linkedDataSource
     * @param socket
     * @return
     */
    @Deprecated
    public static Optional<SocketDefinition> getSocketDefinitionOfSocket(LinkedDataSource linkedDataSource,
                    URI socket) {
        return getSocketDefinitionOfSocket(linkedDataSource, socket, WonRelativeUriHelper.stripFragment(socket));
    }

    public static Optional<SocketDefinition> getSocketDefinitionOfSocketUsingAtomAsWebid(
                    LinkedDataSource linkedDataSource,
                    URI socket) {
        return getSocketDefinitionOfSocket(linkedDataSource, socket, WonRelativeUriHelper.stripFragment(socket));
    }

    public static Optional<SocketDefinition> getSocketDefinitionOfSocket(LinkedDataSource linkedDataSource,
                    URI socket, URI webId) {
        Dataset dataset = linkedDataSource.getDataForResource(socket, webId);
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
            Dataset ds = linkedDataSource.getDataForPublicResource(configURI);
            RdfUtils.addDatasetToDataset(dataset, ds);
        });
        URI socketDefinitionURI = configURIs.stream().findFirst().get();
        return getSocketDefinition(linkedDataSource, socketDefinitionURI, webId);
    }

    public static Optional<SocketDefinition> getSocketDefinition(LinkedDataSource linkedDataSource,
                    URI socketDefinitionURI, URI webId) {
        SocketDefinitionImpl socketDef = getSocketDefinitionFromCache(socketDefinitionURI);
        if (socketDef != null) {
            return Optional.of(socketDef);
        }
        Dataset dataset = linkedDataSource.getDataForResource(socketDefinitionURI, webId);
        socketDef = new SocketDefinitionImpl(socketDefinitionURI);
        // if a socket definition is referenced via won:socketDefinition, it has to be
        // the subject of a triple
        boolean isSocketDefFound = RdfUtils.findFirst(dataset, model -> {
            if (model.listStatements(new SimpleSelector(model.createResource(socketDefinitionURI.toString()), null,
                            (RDFNode) null)).hasNext()) {
                return socketDefinitionURI;
            }
            return null;
        }) != null;
        if (!isSocketDefFound) {
            throw new IllegalArgumentException("Could not find data for socket definition " + socketDefinitionURI);
        }
        socketDef.setSocketDefinitionURI(socketDefinitionURI);
        WonRdfUtils.SocketUtils.setCompatibleSocketDefinitions(socketDef, dataset, socketDefinitionURI);
        WonRdfUtils.SocketUtils.setAutoOpen(socketDef, dataset, socketDefinitionURI);
        WonRdfUtils.SocketUtils.setSocketCapacity(socketDef, dataset, socketDefinitionURI);
        WonRdfUtils.SocketUtils.setDerivationProperties(socketDef, dataset, socketDefinitionURI);
        WonRdfUtils.SocketUtils.setInverseDerivationProperties(socketDef, dataset, socketDefinitionURI);
        linkedDataObjectCache.put(new Element(socketDefinitionURI, socketDef));
        return Optional.of(socketDef);
    }

    private static SocketDefinitionImpl getSocketDefinitionFromCache(URI socketDefinitionURI) {
        Element e = linkedDataObjectCache.get(socketDefinitionURI);
        if (e == null) {
            return null;
        }
        return (SocketDefinitionImpl) e.getObjectValue();
    }

    public static Optional<URI> getTypeOfSocket(URI socketURI, URI webId, LinkedDataSource linkedDataSource) {
        return WonRdfUtils.SocketUtils.getTypeOfSocket(linkedDataSource.getDataForResource(socketURI, webId),
                        socketURI);
    }

    /**
     * Use
     * {@link WonLinkedDataUtils#getTypeOfSocketUsingAtomAsWebId(URI, LinkedDataSource)}
     * or {@link WonLinkedDataUtils#getTypeOfSocket(URI, URI, LinkedDataSource)}
     * instead.
     *
     * @param socketURI
     * @param linkedDataSource
     * @return
     */
    @Deprecated
    public static Optional<URI> getTypeOfSocket(URI socketURI, LinkedDataSource linkedDataSource) {
        return WonRdfUtils.SocketUtils.getTypeOfSocket(
                        linkedDataSource.getDataForResource(socketURI, WonRelativeUriHelper.stripFragment(socketURI)),
                        socketURI);
    }

    public static Optional<URI> getTypeOfSocketUsingAtomAsWebId(URI socketURI, LinkedDataSource linkedDataSource) {
        return WonRdfUtils.SocketUtils.getTypeOfSocket(
                        linkedDataSource.getDataForResource(socketURI, WonRelativeUriHelper.stripFragment(socketURI)),
                        socketURI);
    }

    /**
     * Use
     * {@link won.protocol.util.linkeddata.uriresolver.WonRelativeUriHelper#stripFragment(URI)}
     * instead.
     */
    @Deprecated
    public static Optional<URI> getAtomOfSocket(URI socketURI, LinkedDataSource linkedDataSource) {
        return WonRdfUtils.SocketUtils.getAtomOfSocket(getDataForPublicResource(socketURI, linkedDataSource),
                        socketURI);
    }

    /**
     * Crawls all connections of the specified atom without messages.
     */
    public static Dataset getConnectionNetwork(URI atomURI, LinkedDataSource linkedDataSource) {
        return getConnectionNetwork(atomURI, atomURI, linkedDataSource);
    }

    public static Dataset getConnectionNetwork(URI atomURI, URI webId, LinkedDataSource linkedDataSource) {
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
        return linkedDataSource.getDataForResourceWithPropertyPath(atomURI, webId, propertyPaths, maxRequests, depth);
    }

    public static Optional<URI> getConnectionURIForSocketAndTargetSocket(URI socket,
                    URI targetSocket, LinkedDataSource linkedDataSource, URI requesterWebId) {
        Dataset ds = linkedDataSource.getDataForResource(socket, requesterWebId);
        Optional<URI> atomUri = WonRdfUtils.SocketUtils.getAtomOfSocket(ds, socket);
        if (!atomUri.isPresent()) {
            return Optional.empty();
        }
        Optional<URI> connectionContainer = WonRdfUtils.AtomUtils.getConnectionContainerOfAtom(ds, atomUri.get());
        if (!connectionContainer.isPresent()) {
            return Optional.empty();
        }
        Optional<Dataset> connConnData;
        try {
            connConnData = Optional.ofNullable(linkedDataSource.getDataForResource(
                            URI.create(connectionContainer.get().toString()
                                            + "?socket=" + URLEncoder.encode(socket.toString(), "UTF-8")
                                            + "&targetSocket="
                                            + URLEncoder.encode(targetSocket.toString(), "UTF-8")),
                            requesterWebId));
        } catch (UnsupportedEncodingException e) {
            throw new LinkedDataFetchingException(connectionContainer.get(),
                            "Error building request for connection by socket " + socket.toString()
                                            + " and targetSocket " + targetSocket.toString());
        }
        if (!connConnData.isPresent()) {
            return Optional.empty();
        }
        Iterator<URI> it = WonRdfUtils.AtomUtils.getConnections(connConnData.get(), connectionContainer.get());
        return it.hasNext() ? Optional.of(it.next()) : Optional.empty();
    }

    /**
     * Extracts the prev page link from a list of HTTP 'Link' headers returned by an
     * LDP container.
     *
     * @see {@link "https://www.w3.org/TR/ldp-paging/#ldpp-ex-paging-other-links"}
     * @param linkHeaders
     * @return
     */
    public static Optional<URI> extractLDPPrevPageLinkFromLinkHeaders(List<String> linkHeaders) {
        for (String header : linkHeaders) {
            Matcher m = LDP_PREV_LINK_PATTERN.matcher(header);
            if (m.find()) {
                String link = null;
                try {
                    link = m.group(1);
                    return Optional.of(new URI(link));
                } catch (URISyntaxException e) {
                    logger.info("Error parsing ldp prev link: {} ", link, e);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Extracts the next page link from a list of HTTP 'Link' headers returned by an
     * LDP container.
     *
     * @see {@link "https://www.w3.org/TR/ldp-paging/#ldpp-ex-paging-other-links"}
     * @param linkHeaders
     * @return
     */
    public static Optional<URI> extractLDPNextPageLinkFromLinkHeaders(List<String> linkHeaders) {
        for (String header : linkHeaders) {
            Matcher m = LDP_NEXT_LINK_PATTERN.matcher(header);
            if (m.find()) {
                String link = null;
                try {
                    link = m.group(1);
                    return Optional.of(new URI(link));
                } catch (URISyntaxException e) {
                    logger.info("Error parsing ldp next link: {} ", link, e);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Iterator implementation that fetches linked data lazily for the specified
     * iterator of URIs.
     */
    private static class ModelFetchingIterator implements Iterator<Dataset> {
        private Iterator<URI> uriIterator;
        private LinkedDataSource linkedDataSource;

        private ModelFetchingIterator(final Iterator<URI> uriIterator, final LinkedDataSource linkedDataSource) {
            this.uriIterator = uriIterator;
            this.linkedDataSource = linkedDataSource;
        }

        @Override
        public Dataset next() {
            URI uri = uriIterator.next();
            return linkedDataSource.getDataForPublicResource(uri);
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
