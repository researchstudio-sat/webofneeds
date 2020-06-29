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
package won.node.service.linkeddata.generate;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.NoSuchMessageException;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import won.cryptography.rdfsign.WonKeysReaderWriter;
import won.cryptography.service.CryptographyService;
import won.node.service.nodeconfig.URIService;
import won.node.service.persistence.AtomInformationService;
import won.protocol.exception.NoSuchAtomException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.message.WonMessageType;
import won.protocol.model.Atom;
import won.protocol.model.AtomModelMapper;
import won.protocol.model.AtomState;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionModelMapper;
import won.protocol.model.ConnectionState;
import won.protocol.model.DataWithEtag;
import won.protocol.model.DatasetHolder;
import won.protocol.model.DatasetHolderAggregator;
import won.protocol.model.MessageEvent;
import won.protocol.model.unread.UnreadMessageInfo;
import won.protocol.model.unread.UnreadMessageInfoForAtom;
import won.protocol.repository.AtomRepository;
import won.protocol.repository.DatasetHolderRepository;
import won.protocol.repository.MessageContainerRepository;
import won.protocol.repository.MessageEventRepository;
import won.protocol.service.impl.UnreadInformationService;
import won.protocol.util.DefaultAtomModelWrapper;
import won.protocol.util.DefaultPrefixUtils;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.RDFG;
import won.protocol.vocabulary.WON;

/**
 * Creates rdf models from the relational database. TODO: conform to:
 * https://dvcs.w3.org/hg/ldpwg/raw-file/default/ldp-paging.html, especially for
 * sorting
 */
@Component("linkedDataService")
public class LinkedDataServiceImpl implements LinkedDataService, InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    // prefix of an atom resource
    private String atomResourceURIPrefix;
    // prefix of a connection resource
    private String connectionResourceURIPrefix;
    // prefix of a event resource
    private String messageResourceURIPrefix;
    // prefix for URIs referring to real-world things
    @Value("${uri.prefix.resource}")
    private String resourceURIPrefix;
    @Autowired
    private MessageEventRepository messageEventRepository;
    @Autowired
    private MessageContainerRepository messageContainerRepository;
    @Autowired
    private AtomRepository atomRepository;
    @Autowired
    private DatasetHolderRepository datasetHolderRepository;
    // TODO: used to access/create event URIs for connection model rendering. Could
    // be removed if events knew their URIs.
    @Autowired
    private URIService uriService;
    private AtomModelMapper atomModelMapper = new AtomModelMapper();
    private ConnectionModelMapper connectionModelMapper = new ConnectionModelMapper();
    @Autowired
    private CryptographyService cryptographyService;
    @Autowired
    private UnreadInformationService unreadInformationService;
    @Autowired
    private AtomInformationService atomInformationService;
    @Value("${uri.protocol.activemq}")
    private String activeMqEndpoint;
    @Value("${activemq.queuename.atom.incoming}")
    private String activeMqAtomProtcolQueueName;
    @Value("${activemq.queuename.owner.incoming}")
    private String activeMqOwnerProtcolQueueName;
    @Value("${activemq.queuename.matcher.incoming}")
    private String activeMqMatcherPrtotocolQueueName;
    @Value("${activemq.matcher.outgoing.topicname.atom.created}")
    private String activeMqMatcherProtocolTopicNameAtomCreated;
    @Value("${activemq.matcher.outgoing.topicname.atom.activated}")
    private String activeMqMatcherProtocolTopicNameAtomActivated;
    @Value("${activemq.matcher.outgoing.topicname.atom.deactivated}")
    private String activeMqMatcherProtocolTopicNameAtomDeactivated;
    @Value("${activemq.matcher.outgoing.topicname.atom.deleted}")
    private String activeMqMatcherProtocolTopicNameAtomDeleted;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.atomResourceURIPrefix = this.resourceURIPrefix + "/atom";
        this.connectionResourceURIPrefix = this.resourceURIPrefix + "/connection";
        this.messageResourceURIPrefix = this.resourceURIPrefix + "/msg";
        logger.info("setting prefixes: atom: {}, connection: {}, event: {}", new Object[] { this.atomResourceURIPrefix,
                        this.connectionResourceURIPrefix, this.messageResourceURIPrefix });
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public Dataset listAtomURIs() {
        return listAtomURIs(null);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public Dataset listAtomURIs(AtomState atomState) {
        return listAtomURIs(atomState, null, null);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public Dataset listAtomURIs(AtomState atomState, URI filterSocketTypeUri, URI filterAtomTypeUri) {
        Model model = ModelFactory.createDefaultModel();
        setNsPrefixes(model);
        Collection<URI> uris = atomInformationService.listAtomURIs(atomState);
        return getFilteredAtomURIListDataset(model, uris, filterSocketTypeUri, filterAtomTypeUri);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public AtomInformationService.PagedResource<Dataset, URI> listPagedAtomURIs(final int pageNum) {
        return listPagedAtomURIs(pageNum, null, null);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public AtomInformationService.PagedResource<Dataset, URI> listPagedAtomURIsBefore(final URI atom) {
        return listPagedAtomURIsBefore(atom, null, null);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public AtomInformationService.PagedResource<Dataset, URI> listPagedAtomURIsAfter(final URI atom) {
        return listPagedAtomURIsAfter(atom, null, null);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public AtomInformationService.PagedResource<Dataset, URI> listPagedAtomURIs(final int pageNum,
                    final Integer preferedSize, AtomState atomState) {
        Slice<URI> slice = atomInformationService.listPagedAtomURIs(pageNum, preferedSize, atomState);
        return toContainerPage(this.atomResourceURIPrefix + "/", slice);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public AtomInformationService.PagedResource<Dataset, URI> listPagedAtomURIsBefore(final URI atom,
                    final Integer preferedSize, AtomState atomState) {
        Slice<URI> slice = atomInformationService.listPagedAtomURIsBefore(atom, preferedSize, atomState);
        return toContainerPage(this.atomResourceURIPrefix + "/", slice);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public AtomInformationService.PagedResource<Dataset, URI> listPagedAtomURIsAfter(final URI atom,
                    final Integer preferedSize, AtomState atomState) {
        Slice<URI> slice = atomInformationService.listPagedAtomURIsAfter(atom, preferedSize, atomState);
        return toContainerPage(this.atomResourceURIPrefix + "/", slice);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public Dataset listAtomURIsModifiedAfter(Date modifiedDate, AtomState atomState, URI filterSocketTypeUri,
                    URI filterAtomTypeUri) {
        Model model = ModelFactory.createDefaultModel();
        setNsPrefixes(model);
        Collection<URI> uris = atomInformationService.listAtomURIsModifiedAfter(modifiedDate, atomState);
        return getFilteredAtomURIListDataset(model, uris, filterSocketTypeUri, filterAtomTypeUri);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public Dataset listAtomURIsCreatedAfter(Date createdDate, AtomState atomState, URI filterSocketTypeUri,
                    URI filterAtomTypeUri) {
        Model model = ModelFactory.createDefaultModel();
        setNsPrefixes(model);
        Collection<URI> uris = atomInformationService.listAtomURIsCreatedAfter(createdDate, atomState);
        return getFilteredAtomURIListDataset(model, uris, filterSocketTypeUri, filterAtomTypeUri);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public DataWithEtag<Dataset> getAtomDataset(final URI atomUri, String etag) {
        Instant start = logger.isDebugEnabled() ? Instant.now() : null;
        DataWithEtag<Atom> atomDataWithEtag;
        try {
            atomDataWithEtag = atomInformationService.readAtom(atomUri, etag);
        } catch (NoSuchAtomException e) {
            if (logger.isDebugEnabled() && start != null) {
                Instant finish = Instant.now();
                logger.debug("getAtomDataset({}) took {}ms", atomUri, Duration.between(start, finish).toMillis());
            }
            return DataWithEtag.dataNotFound();
        }
        if (atomDataWithEtag.isNotFound()) {
            if (logger.isDebugEnabled() && start != null) {
                Instant finish = Instant.now();
                logger.debug("getAtomDataset({}) took {}ms", atomUri, Duration.between(start, finish).toMillis());
            }
            return DataWithEtag.dataNotFound();
        }
        if (!atomDataWithEtag.isChanged()) {
            if (logger.isDebugEnabled() && start != null) {
                Instant finish = Instant.now();
                logger.debug("getAtomDataset({}) took {}ms", atomUri, Duration.between(start, finish).toMillis());
            }
            return DataWithEtag.dataNotChanged(atomDataWithEtag);
        }
        Atom atom = atomDataWithEtag.getData();
        String newEtag = atomDataWithEtag.getEtag();
        // load the dataset from storage
        boolean isDeleted = (atom.getState() == AtomState.DELETED);
        Dataset dataset = isDeleted ? DatasetFactory.createGeneral() : atom.getDatatsetHolder().getDataset();
        Model metaModel = atomModelMapper.toModel(atom);
        Resource atomResource = metaModel.getResource(atomUri.toString());
        String atomMetaInformationURI = uriService.createAtomSysInfoGraphURI(atomUri).toString();
        Resource atomMetaInformationResource = metaModel.getResource(atomMetaInformationURI);
        // link atomMetaInformationURI to atom via rdfg:subGraphOf
        atomMetaInformationResource.addProperty(RDFG.SUBGRAPH_OF, atomResource);
        // add connections
        Resource connectionsContainer = metaModel.createResource(atom.getAtomURI().toString() + "/c");
        metaModel.add(metaModel.createStatement(atomResource, WON.connections, connectionsContainer));
        // add atom event container
        Resource atomMessageContainer = metaModel.createResource(atom.getAtomURI().toString() + "#msg",
                        WON.MessageContainer);
        metaModel.add(metaModel.createStatement(atomResource, WON.messageContainer, atomMessageContainer));
        // add atom event URIs
        Collection<MessageEvent> messageEvents = messageEventRepository.findByParentURI(atomUri);
        for (MessageEvent messageEvent : messageEvents) {
            metaModel.add(metaModel.createStatement(atomMessageContainer, RDFS.member,
                            metaModel.getResource(messageEvent.getMessageURI().toString())));
        }
        // add WON node link
        atomResource.addProperty(WON.wonNode, metaModel.createResource(this.resourceURIPrefix));
        // link all atom graphs taken from the create message to atom uri:
        Iterator<String> namesIt = dataset.listNames();
        while (namesIt.hasNext()) {
            String name = namesIt.next();
            Resource atomGraphResource = metaModel.getResource(name);
            atomResource.addProperty(WON.contentGraph, atomGraphResource);
        }
        // add meta model to dataset
        dataset.addNamedModel(atomMetaInformationURI, metaModel);
        addBaseUriAndDefaultPrefixes(dataset);
        if (logger.isDebugEnabled() && start != null) {
            Instant finish = Instant.now();
            logger.debug("getAtomDataset({}) took {}ms", atomUri, Duration.between(start, finish).toMillis());
        }
        return new DataWithEtag<>(dataset, newEtag, etag, isDeleted);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public Dataset getAtomDataset(final URI atomUri, boolean deep, Integer deepLayerSize)
                    throws NoSuchAtomException, NoSuchConnectionException, NoSuchMessageException {
        Dataset dataset = getAtomDataset(atomUri, null).getData();
        if (deep) {
            Atom atom = atomInformationService.readAtom(atomUri);
            if (atom.getState() == AtomState.ACTIVE) {
                // only add deep data if atom is active
                Slice<URI> slice = atomInformationService.listConnectionURIs(atomUri, 1, deepLayerSize, null, null);
                AtomInformationService.PagedResource<Dataset, URI> connectionsResource = toContainerPage(
                                this.uriService.createConnectionsURIForAtom(atomUri).toString(), slice);
                addDeepConnectionData(connectionsResource.getContent(), slice.getContent());
                RdfUtils.addDatasetToDataset(dataset, connectionsResource.getContent());
                for (URI connectionUri : slice.getContent()) {
                    AtomInformationService.PagedResource<Dataset, URI> eventsResource = listConnectionEventURIs(
                                    connectionUri, 1, deepLayerSize, null, true);
                    RdfUtils.addDatasetToDataset(dataset, eventsResource.getContent());
                }
            }
        }
        return dataset;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public Model getUnreadInformationForAtom(URI atomURI, Collection<URI> lastSeenMessageURIs) {
        UnreadMessageInfoForAtom unreadInfo = this.unreadInformationService.getUnreadInformation(atomURI,
                        lastSeenMessageURIs);
        Model ret = ModelFactory.createDefaultModel();
        Resource atomRes = ret.createResource(atomURI.toString());
        addUnreadInfoWithProperty(ret, atomRes, WON.unreadSuggested,
                        unreadInfo.getUnreadInfoByConnectionState().get(ConnectionState.SUGGESTED));
        addUnreadInfoWithProperty(ret, atomRes, WON.unreadConnected,
                        unreadInfo.getUnreadInfoByConnectionState().get(ConnectionState.CONNECTED));
        addUnreadInfoWithProperty(ret, atomRes, WON.unreadRequestSent,
                        unreadInfo.getUnreadInfoByConnectionState().get(ConnectionState.REQUEST_SENT));
        addUnreadInfoWithProperty(ret, atomRes, WON.unreadRequestReceived,
                        unreadInfo.getUnreadInfoByConnectionState().get(ConnectionState.REQUEST_RECEIVED));
        addUnreadInfoWithProperty(ret, atomRes, WON.unreadClosed,
                        unreadInfo.getUnreadInfoByConnectionState().get(ConnectionState.CLOSED));
        unreadInfo.getUnreadMessageInfoForConnections().forEach(info -> {
            Resource connRes = ret.createResource(info.getConnectionURI().toString());
            addUnreadInfoWithProperty(ret, connRes, null, info.getUnreadInformation());
        });
        return ret;
    }

    private void addUnreadInfoWithProperty(Model ret, Resource subject, Property property, UnreadMessageInfo info) {
        if (info != null) {
            if (property != null) {
                // if we are given a property, make a blank node and connect it to the subject
                // using the property
                Resource node = ret.createResource();
                subject.addProperty(property, node);
                subject = node;
            }
            subject.addLiteral(WON.unreadCount, info.getCount());
            subject.addLiteral(WON.unreadOldestTimestamp, info.getOldestTimestamp().getTime());
            subject.addLiteral(WON.unreadNewestTimestamp, info.getNewestTimestamp().getTime());
        }
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public Dataset getNodeDataset() {
        Model model = ModelFactory.createDefaultModel();
        setNsPrefixes(model);
        Resource showNodePageResource = model.createResource(this.resourceURIPrefix);
        addAtomList(model, showNodePageResource);
        addProtocolEndpoints(model, showNodePageResource);
        Dataset ret = newDatasetWithNamedModel(createDataGraphUriFromResource(showNodePageResource), model);
        addBaseUriAndDefaultPrefixes(ret);
        addPublicKey(model, showNodePageResource);
        return ret;
    }

    private void addAtomList(Model model, Resource res) {
        res.addProperty(WON.atomList, model.createResource(this.atomResourceURIPrefix));
    }

    private void addPublicKey(Model model, Resource res) {
        WonKeysReaderWriter keyWriter = new WonKeysReaderWriter();
        try {
            keyWriter.writeToModel(model, res, cryptographyService.getPublicKey(res.getURI()));
        } catch (Exception e) {
            logger.warn("No public key could be added to RDF for " + res.getURI());
        }
    }

    // TODO: protocol endpoint specification in RDF model atoms refactoring!
    private void addProtocolEndpoints(Model model, Resource res) {
        Resource blankNodeActiveMq = model.createResource();
        res.addProperty(WON.supportsWonProtocolImpl, blankNodeActiveMq);
        blankNodeActiveMq.addProperty(RDF.type, WON.WonOverActiveMq)
                        .addProperty(WON.brokerUri, model.createResource(this.activeMqEndpoint))
                        .addProperty(WON.ownerQueue, this.activeMqOwnerProtcolQueueName, XSDDatatype.XSDstring)
                        .addProperty(WON.nodeQueue, this.activeMqAtomProtcolQueueName, XSDDatatype.XSDstring)
                        .addProperty(WON.matcherQueue, this.activeMqMatcherPrtotocolQueueName, XSDDatatype.XSDstring)
                        .addProperty(WON.atomActivatedTopic, this.activeMqMatcherProtocolTopicNameAtomActivated,
                                        XSDDatatype.XSDstring)
                        .addProperty(WON.atomDeactivatedTopic, this.activeMqMatcherProtocolTopicNameAtomDeactivated,
                                        XSDDatatype.XSDstring)
                        .addProperty(WON.atomDeletedTopic, this.activeMqMatcherProtocolTopicNameAtomDeleted,
                                        XSDDatatype.XSDstring)
                        .addProperty(WON.atomCreatedTopic, this.activeMqMatcherProtocolTopicNameAtomCreated,
                                        XSDDatatype.XSDstring);
        Resource blankNodeUriSpec = model.createResource();
        res.addProperty(WON.uriPrefixSpecification, blankNodeUriSpec);
        blankNodeUriSpec.addProperty(WON.atomUriPrefix, model.createLiteral(this.atomResourceURIPrefix));
        blankNodeUriSpec.addProperty(WON.connectionUriPrefix, model.createLiteral(this.connectionResourceURIPrefix));
        blankNodeUriSpec.addProperty(WON.messageUriPrefix, model.createLiteral(this.messageResourceURIPrefix));
    }

    /**
     * ETag-aware method for obtaining connection data. Currently does not take into
     * account new events, only changes to the connection itself.
     *
     * @param connectionUri
     * @param includeMessageContainer
     * @param includeMessageContainer
     * @param etag
     * @return
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public DataWithEtag<Dataset> getConnectionDataset(final URI connectionUri, final boolean includeMessageContainer,
                    final String etag) {
        DataWithEtag<Connection> data = atomInformationService.readConnection(connectionUri, etag);
        if (data.isNotFound()) {
            return DataWithEtag.dataNotFound();
        }
        if (!data.isChanged()) {
            return DataWithEtag.dataNotChanged(data);
        }
        Connection connection = data.getData();
        if (connection == null) {
            return DataWithEtag.dataNotFound();
        }
        String newEtag = data.getEtag();
        // load the model from storage
        Model model = connectionModelMapper.toModel(connection);
        Dataset connectionDataset = connection.getDatasetHolder() == null ? null
                        : connection.getDatasetHolder().getDataset();
        if (connectionDataset == null) {
            connectionDataset = DatasetFactory.createGeneral();
        } else {
            connectionDataset = RdfUtils.cloneDataset(connectionDataset);
        }
        setNsPrefixes(model);
        // model.setNsPrefix("", connection.getConnectionURI().toString());
        // create connection member
        Resource connectionResource = model.getResource(connection.getConnectionURI().toString());
        // add WON node link
        connectionResource.addProperty(WON.wonNode, model.createResource(this.resourceURIPrefix));
        if (includeMessageContainer) {
            // create event container and attach it to the member
            Resource messageContainer = model.createResource(connection.getConnectionURI().toString() + "/msg");
            connectionResource.addProperty(WON.messageContainer, messageContainer);
            messageContainer.addProperty(RDF.type, WON.MessageContainer);
            DatasetHolder datasetHolder = connection.getDatasetHolder();
            if (datasetHolder != null) {
                addAdditionalData(model, datasetHolder.getDataset().getDefaultModel(), connectionResource);
            }
        }
        connectionDataset.addNamedModel(createDataGraphUriFromResource(connectionResource), model);
        connectionDataset = addBaseUriAndDefaultPrefixes(connectionDataset);
        return new DataWithEtag<>(connectionDataset, newEtag, etag);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public Dataset listConnection(URI socketUri, URI targetSocketUri, boolean deep) throws NoSuchConnectionException {
        Optional<Connection> con = atomInformationService.getConnection(socketUri, targetSocketUri);
        Dataset data = makeConnectionContainer(this.connectionResourceURIPrefix + "/", Arrays.asList(con.get()));
        if (deep) {
            addDeepConnectionData(data, Arrays.asList(con.get().getConnectionURI()));
        }
        return data;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public AtomInformationService.PagedResource<Dataset, Connection> listConnections(final boolean deep)
                    throws NoSuchConnectionException {
        List<Connection> connections = new ArrayList<>(atomInformationService.listConnections());
        AtomInformationService.PagedResource<Dataset, Connection> connectionsContainerPage = toConnectionsContainerPage(
                        this.connectionResourceURIPrefix + "/", new SliceImpl<>(connections));
        if (deep) {
            List<URI> uris = connections.stream().map(Connection::getConnectionURI).collect(Collectors.toList());
            addDeepConnectionData(connectionsContainerPage.getContent(), uris);
        }
        return connectionsContainerPage;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public AtomInformationService.PagedResource<Dataset, Connection> listModifiedConnectionsAfter(Date modifiedAfter,
                    boolean deep) throws NoSuchConnectionException {
        List<Connection> connections = new ArrayList<>(
                        atomInformationService.listModifiedConnectionsAfter(modifiedAfter));
        AtomInformationService.PagedResource<Dataset, Connection> connectionsContainerPage = toConnectionsContainerPage(
                        this.connectionResourceURIPrefix + "/", new SliceImpl<>(connections));
        if (deep) {
            List<URI> uris = connections.stream().map(Connection::getConnectionURI).collect(Collectors.toList());
            addDeepConnectionData(connectionsContainerPage.getContent(), uris);
        }
        return connectionsContainerPage;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public AtomInformationService.PagedResource<Dataset, Connection> listConnections(final int page,
                    final Integer preferredSize, Date timeSpot, final boolean deep) throws NoSuchConnectionException {
        Slice<Connection> slice = atomInformationService.listConnections(page, preferredSize, timeSpot);
        AtomInformationService.PagedResource<Dataset, Connection> connectionsContainerPage = toConnectionsContainerPage(
                        this.connectionResourceURIPrefix + "/", slice);
        if (deep) {
            List<URI> uris = slice.getContent().stream().map(Connection::getConnectionURI).collect(Collectors.toList());
            addDeepConnectionData(connectionsContainerPage.getContent(), uris);
        }
        return connectionsContainerPage;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public AtomInformationService.PagedResource<Dataset, Connection> listConnectionsBefore(URI beforeConnURI,
                    final Integer preferredSize, Date timeSpot, boolean deep) throws NoSuchConnectionException {
        Slice<Connection> slice = atomInformationService.listConnectionsBefore(beforeConnURI, preferredSize, timeSpot);
        AtomInformationService.PagedResource<Dataset, Connection> connectionsContainerPage = toConnectionsContainerPage(
                        this.connectionResourceURIPrefix + "/", slice);
        if (deep) {
            List<URI> uris = slice.getContent().stream().map(Connection::getConnectionURI).collect(Collectors.toList());
            addDeepConnectionData(connectionsContainerPage.getContent(), uris);
        }
        return connectionsContainerPage;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public AtomInformationService.PagedResource<Dataset, Connection> listConnectionsAfter(URI afterConnURI,
                    final Integer preferredSize, Date timeSpot, boolean deep) throws NoSuchConnectionException {
        Slice<Connection> slice = atomInformationService.listConnectionsAfter(afterConnURI, preferredSize, timeSpot);
        AtomInformationService.PagedResource<Dataset, Connection> connectionsContainerPage = toConnectionsContainerPage(
                        this.connectionResourceURIPrefix + "/", slice);
        if (deep) {
            List<URI> uris = slice.getContent().stream().map(Connection::getConnectionURI).collect(Collectors.toList());
            addDeepConnectionData(connectionsContainerPage.getContent(), uris);
        }
        return connectionsContainerPage;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public AtomInformationService.PagedResource<Dataset, Connection> listConnections(final URI atomURI, boolean deep,
                    boolean addMetadata, ConnectionState filterByConnectionState)
                    throws NoSuchAtomException, NoSuchConnectionException {
        List<Connection> connections = new ArrayList<>(
                        atomInformationService.listConnections(atomURI, filterByConnectionState));
        URI connectionsUri = this.uriService.createConnectionsURIForAtom(atomURI);
        AtomInformationService.PagedResource<Dataset, Connection> connectionsContainerPage = toConnectionsContainerPage(
                        connectionsUri.toString(), new SliceImpl<>(connections));
        if (deep) {
            List<URI> uris = connections.stream().map(Connection::getConnectionURI).collect(Collectors.toList());
            addDeepConnectionData(connectionsContainerPage.getContent(), uris);
        }
        if (addMetadata) {
            addConnectionMetadata(connectionsContainerPage.getContent(), atomURI, connectionsUri);
        }
        return connectionsContainerPage;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public AtomInformationService.PagedResource<Dataset, Connection> listConnections(final int page, final URI atomURI,
                    final Integer preferredSize, final WonMessageType messageType, final Date timeSpot, boolean deep,
                    boolean addMetadata, ConnectionState filterByConnectionState)
                    throws NoSuchAtomException, NoSuchConnectionException {
        Slice<Connection> slice = atomInformationService.listConnections(atomURI, page, preferredSize, messageType,
                        timeSpot, filterByConnectionState);
        URI connectionsUri = this.uriService.createConnectionsURIForAtom(atomURI);
        AtomInformationService.PagedResource<Dataset, Connection> connectionsContainerPage = toConnectionsContainerPage(
                        connectionsUri.toString(), slice);
        if (deep) {
            List<URI> uris = slice.getContent().stream().map(Connection::getConnectionURI).collect(Collectors.toList());
            addDeepConnectionData(connectionsContainerPage.getContent(), uris);
        }
        if (addMetadata) {
            addConnectionMetadata(connectionsContainerPage.getContent(), atomURI, connectionsUri);
        }
        return connectionsContainerPage;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public AtomInformationService.PagedResource<Dataset, Connection> listConnectionsBefore(final URI atomURI,
                    URI beforeEventURI, final Integer preferredSize, final WonMessageType messageType,
                    final Date timeSpot, boolean deep, boolean addMetadata, ConnectionState filterByConnectionState)
                    throws NoSuchAtomException, NoSuchConnectionException {
        Slice<Connection> slice = atomInformationService.listConnectionsBefore(atomURI, beforeEventURI, preferredSize,
                        messageType, timeSpot, filterByConnectionState);
        URI connectionsUri = this.uriService.createConnectionsURIForAtom(atomURI);
        AtomInformationService.PagedResource<Dataset, Connection> connectionsContainerPage = toConnectionsContainerPage(
                        connectionsUri.toString(), slice);
        if (deep) {
            List<URI> uris = slice.getContent().stream().map(Connection::getConnectionURI).collect(Collectors.toList());
            addDeepConnectionData(connectionsContainerPage.getContent(), uris);
        }
        if (addMetadata) {
            addConnectionMetadata(connectionsContainerPage.getContent(), atomURI, connectionsUri);
        }
        return connectionsContainerPage;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public AtomInformationService.PagedResource<Dataset, Connection> listConnectionsAfter(final URI atomURI,
                    URI resumeConnURI, final Integer preferredSize, final WonMessageType messageType,
                    final Date timeSpot, boolean deep, boolean addMetadata, ConnectionState filterByConnectionState)
                    throws NoSuchAtomException, NoSuchConnectionException {
        Slice<Connection> slice = atomInformationService.listConnectionsAfter(atomURI, resumeConnURI, preferredSize,
                        messageType, timeSpot, filterByConnectionState);
        URI connectionsUri = this.uriService.createConnectionsURIForAtom(atomURI);
        AtomInformationService.PagedResource<Dataset, Connection> connectionsContainerPage = toConnectionsContainerPage(
                        connectionsUri.toString(), slice);
        if (deep) {
            List<URI> uris = slice.getContent().stream().map(Connection::getConnectionURI).collect(Collectors.toList());
            addDeepConnectionData(connectionsContainerPage.getContent(), uris);
        }
        if (addMetadata) {
            addConnectionMetadata(connectionsContainerPage.getContent(), atomURI, connectionsUri);
        }
        return connectionsContainerPage;
    }

    private void addConnectionMetadata(final Dataset content, URI atomURI, URI containerURI) {
        Model model = content.getNamedModel(createDataGraphUriFromUri(containerURI));
        List<Object[]> connectionCountsPerState = atomRepository.getCountsPerConnectionState(atomURI);
        Resource containerResource = model.getResource(containerURI.toString());
        for (Object[] countForState : connectionCountsPerState) {
            ConnectionState stateName = (ConnectionState) countForState[0];
            Long count = (Long) countForState[1];
            Property countProperty = getRdfPropertyForState(stateName);
            if (countProperty == null) {
                logger.warn("did not recognize connection state " + stateName);
                continue;
            }
            containerResource.addProperty(countProperty, Integer.toString(count.intValue()), XSDDatatype.XSDint);
        }
    }

    private Property getRdfPropertyForState(ConnectionState state) {
        switch (state) {
            case SUGGESTED:
                return WON.suggestedCount;
            case REQUEST_RECEIVED:
                return WON.requestReceivedCount;
            case REQUEST_SENT:
                return WON.requestSentCount;
            case CONNECTED:
                return WON.connectedCount;
            case CLOSED:
                return WON.closedCount;
            case DELETED:
                return WON.deletedCount;
        }
        return null;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public Dataset listConnectionEventURIs(final URI connectionUri, boolean deep) throws NoSuchConnectionException {
        Model model = ModelFactory.createDefaultModel();
        setNsPrefixes(model);
        Connection connection = atomInformationService.readConnection(connectionUri);
        Resource messageContainer = model.createResource(connection.getConnectionURI().toString() + "/msg",
                        WON.MessageContainer);
        // add the events with the new format (only the URI, no content)
        List<MessageEvent> connectionEvents = messageEventRepository.findByParentURI(connectionUri);
        Dataset eventsContainerDataset = newDatasetWithNamedModel(createDataGraphUriFromResource(messageContainer),
                        model);
        addBaseUriAndDefaultPrefixes(eventsContainerDataset);
        for (MessageEvent event : connectionEvents) {
            model.add(model.createStatement(messageContainer, RDFS.member,
                            model.getResource(event.getMessageURI().toString())));
            if (deep) {
                Dataset eventDataset = event.getDatasetHolder().getDataset();
                RdfUtils.addDatasetToDataset(eventsContainerDataset, eventDataset);
            }
        }
        return eventsContainerDataset;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public AtomInformationService.PagedResource<Dataset, URI> listConnectionEventURIs(final URI connectionUri,
                    final int pageNum, Integer preferedSize, WonMessageType msgType, boolean deep)
                    throws NoSuchConnectionException {
        Slice<MessageEvent> slice = atomInformationService.listConnectionEvents(connectionUri, pageNum,
                        preferedSize, msgType);
        return eventsToContainerPage(this.uriService.createMessagesURIForConnection(connectionUri).toString(), slice,
                        deep);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public AtomInformationService.PagedResource<Dataset, URI> listConnectionEventURIsAfter(final URI connectionUri,
                    final URI msgURI, Integer preferedSize, WonMessageType msgType, boolean deep)
                    throws NoSuchConnectionException {
        Slice<MessageEvent> slice = atomInformationService.listConnectionEventsAfter(connectionUri, msgURI,
                        preferedSize, msgType);
        return eventsToContainerPage(this.uriService.createMessagesURIForConnection(connectionUri).toString(), slice,
                        deep);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public AtomInformationService.PagedResource<Dataset, URI> listConnectionEventURIsBefore(final URI connectionUri,
                    final URI msgURI, Integer preferedSize, WonMessageType msgType, boolean deep)
                    throws NoSuchConnectionException {
        Slice<MessageEvent> slice = atomInformationService.listConnectionEventsBefore(connectionUri, msgURI,
                        preferedSize, msgType);
        return eventsToContainerPage(this.uriService.createMessagesURIForConnection(connectionUri).toString(), slice,
                        deep);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public DataWithEtag<Dataset> getDatasetForUri(URI datasetUri, String etag) {
        Integer version = etag == null ? -1 : Integer.valueOf(etag);
        Optional<DatasetHolder> datasetHolder = datasetHolderRepository.findOneByUri(datasetUri);
        if (!datasetHolder.isPresent()) {
            return DataWithEtag.dataNotFound();
        }
        if (version == datasetHolder.get().getVersion()) {
            return DataWithEtag.dataNotChanged(etag);
        }
        Dataset dataset = datasetHolder.get().getDataset();
        DefaultPrefixUtils.setDefaultPrefixes(dataset.getDefaultModel());
        addBaseUriAndDefaultPrefixes(dataset);
        return new DataWithEtag<>(dataset, Integer.toString(datasetHolder.get().getVersion()), etag);
    }

    private String createDataGraphUriFromResource(Resource atomListPageResource) {
        URI uri = URI.create(atomListPageResource.getURI());
        return createDataGraphUriFromUri(uri);
    }

    private String createDataGraphUriFromUri(final URI uri) {
        try {
            URI ret = new URI(uri.getScheme(), uri.getHost(), uri.getPath(), uri.getQuery(), "data");
            return ret.toString();
        } catch (URISyntaxException e) {
            return uri.toString() + "#data";
        }
    }

    private Dataset newDatasetWithNamedModel(String graphUri, Model model) {
        Dataset dataset = DatasetFactory.createGeneral();
        dataset.addNamedModel(graphUri, model);
        return dataset;
    }

    private void addAdditionalData(final Model targetModel, Model fromModel, final Resource targetResource) {
        if (fromModel != null && fromModel.size() > 0) {
            Resource additionalData = fromModel.createResource();
            // TODO: check if the statement below is now necessary
            // RdfUtils.replaceBaseResource(additionalDataModel, additionalData);
            targetModel.add(targetModel.createStatement(targetResource, WON.additionalData, additionalData));
            targetModel.add(fromModel);
        }
    }

    private void setNsPrefixes(final Model model) {
        DefaultPrefixUtils.setDefaultPrefixes(model);
    }

    /**
     * Adds the specified URI as the default prefix for each model in the dataset
     * and return the dataset.
     *
     * @param dataset
     * @return
     */
    private Dataset addBaseUriAndDefaultPrefixes(Dataset dataset) {
        setNsPrefixes(dataset.getDefaultModel());
        addPrefixForSpecialResources(dataset, "local", this.resourceURIPrefix);
        addPrefixForSpecialResources(dataset, "atom", this.atomResourceURIPrefix);
        addPrefixForSpecialResources(dataset, "event", this.messageResourceURIPrefix);
        addPrefixForSpecialResources(dataset, "conn", this.connectionResourceURIPrefix);
        return dataset;
    }

    private void addPrefixForSpecialResources(Dataset dataset, String prefix, String uri) {
        if (uri == null)
            return; // ignore if no uri specified
        // the prefix (prefix of all local URIs must end with a slash or a hash,
        // otherwise,
        // it will never be used by RDF serializations. Force that.
        if (!uri.endsWith("/") && !uri.endsWith("#")) {
            uri += "/";
        }
        dataset.getDefaultModel().getGraph().getPrefixMapping().setNsPrefix(prefix, uri);
    }

    private AtomInformationService.PagedResource<Dataset, URI> toContainerPage(String containerUri, Slice<URI> slice) {
        List<URI> uris = slice.getContent();
        URI resumeBefore = null;
        URI resumeAfter = null;
        // very confusing but correct
        // our collections are sorted newest first
        // when we use resumeafter, it means we want the next page
        // when we use resumebefore, it means we want the previous page
        // so, for resumeafter, we want the oldest item, for resumebefore the newest
        // the following works ONLY for a container whose order is newest-first (such as
        // atoms)
        if (slice.getSort() != null && !uris.isEmpty()) {
            Iterator<Sort.Order> sortOrders = slice.getSort().iterator();
            if (sortOrders.hasNext()) {
                Sort.Order sortOrder = sortOrders.next();
                if (sortOrder.getDirection() == Sort.Direction.DESC) {
                    resumeBefore = uris.get(0);
                    resumeAfter = uris.get(uris.size() - 1);
                } else {
                    resumeBefore = uris.get(uris.size() - 1);
                    resumeAfter = uris.get(0);
                }
            }
        }
        Model model = ModelFactory.createDefaultModel();
        setNsPrefixes(model);
        Resource atomListPageResource = model.createResource(containerUri);
        for (URI atomURI : uris) {
            model.add(model.createStatement(atomListPageResource, RDFS.member,
                            model.createResource(atomURI.toString())));
        }
        Dataset dataset = newDatasetWithNamedModel(createDataGraphUriFromResource(atomListPageResource), model);
        addBaseUriAndDefaultPrefixes(dataset);
        return new AtomInformationService.PagedResource(dataset, slice.hasPrevious() ? resumeBefore : null,
                        slice.hasNext() ? resumeAfter : null);
    }

    private AtomInformationService.PagedResource<Dataset, Connection> toConnectionsContainerPage(String containerUri,
                    Slice<Connection> slice) {
        List<Connection> connections = slice.getContent();
        Connection resumeBefore = null;
        Connection resumeAfter = null;
        if (slice.getSort() != null && !connections.isEmpty()) {
            Iterator<Sort.Order> sortOrders = slice.getSort().iterator();
            if (sortOrders.hasNext()) {
                Sort.Order sortOrder = sortOrders.next();
                if (sortOrder.getDirection() == Sort.Direction.ASC) {
                    resumeBefore = connections.get(0);
                    resumeAfter = connections.get(connections.size() - 1);
                } else {
                    resumeBefore = connections.get(connections.size() - 1);
                    resumeAfter = connections.get(0);
                }
            }
        }
        Dataset dataset = makeConnectionContainer(containerUri, connections);
        addBaseUriAndDefaultPrefixes(dataset);
        return new AtomInformationService.PagedResource(dataset, slice.hasPrevious() ? resumeBefore : null,
                        slice.hasNext() ? resumeAfter : null);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public Dataset makeConnectionContainer(String containerUri, List<Connection> connections) {
        Model model = ModelFactory.createDefaultModel();
        setNsPrefixes(model);
        Resource atomListPageResource = model.createResource(containerUri);
        for (Connection conn : connections) {
            model.add(model.createStatement(atomListPageResource, RDFS.member,
                            model.createResource(conn.getConnectionURI().toString())));
            model.add(connectionModelMapper.toModel(conn));
        }
        Dataset dataset = newDatasetWithNamedModel(createDataGraphUriFromResource(atomListPageResource), model);
        return dataset;
    }

    /**
     * Returns a container resource with the messageUris of all MessageEvent objects
     * in the slice. If deep == true, the event datasets are added, too.
     *
     * @param containerUri
     * @param slice
     * @param deep
     * @return
     */
    private AtomInformationService.PagedResource<Dataset, URI> eventsToContainerPage(String containerUri,
                    Slice<MessageEvent> slice, boolean deep) {
        List<MessageEvent> events = slice.getContent();
        URI resumeBefore = null;
        URI resumeAfter = null;
        if (slice.getSort() != null && !events.isEmpty()) {
            Iterator<Sort.Order> sortOrders = slice.getSort().iterator();
            if (sortOrders.hasNext()) {
                Sort.Order sortOrder = sortOrders.next();
                if (sortOrder.getDirection() == Sort.Direction.ASC) {
                    resumeAfter = events.get(0).getMessageURI();
                    resumeBefore = events.get(events.size() - 1).getMessageURI();
                } else {
                    resumeAfter = events.get(events.size() - 1).getMessageURI();
                    resumeBefore = events.get(0).getMessageURI();
                }
            }
        }
        Model model = ModelFactory.createDefaultModel();
        setNsPrefixes(model);
        Resource atomListPageResource = model.createResource(containerUri);
        DatasetHolderAggregator aggregator = new DatasetHolderAggregator();
        for (MessageEvent event : events) {
            model.add(model.createStatement(atomListPageResource, RDFS.member,
                            model.createResource(event.getMessageURI().toString())));
            if (deep) {
                aggregator.appendDataset(event.getDatasetHolder());
            }
        }
        Dataset dataset = aggregator.aggregate();
        dataset.addNamedModel(createDataGraphUriFromResource(atomListPageResource), model);
        addBaseUriAndDefaultPrefixes(dataset);
        return new AtomInformationService.PagedResource(dataset, slice.hasPrevious() ? resumeBefore : null,
                        slice.hasNext() ? resumeAfter : null);
    }

    private void addDeepConnectionData(Dataset dataset, List<URI> connectionURIs) {
        // add the connection model for each connection URI
        for (URI connectionURI : connectionURIs) {
            DataWithEtag<Dataset> connectionDataset = getConnectionDataset(connectionURI, true, null);
            RdfUtils.addDatasetToDataset(dataset, connectionDataset.getData());
        }
    }

    public void setAtomResourceURIPrefix(final String atomResourceURIPrefix) {
        this.atomResourceURIPrefix = atomResourceURIPrefix;
    }

    public void setConnectionResourceURIPrefix(final String connectionResourceURIPrefix) {
        this.connectionResourceURIPrefix = connectionResourceURIPrefix;
    }

    public void setEventResourceURIPrefix(final String eventResourceURIPrefix) {
        this.messageResourceURIPrefix = eventResourceURIPrefix;
    }

    public void setResourceURIPrefix(final String resourceURIPrefix) {
        this.resourceURIPrefix = resourceURIPrefix;
    }

    public void setAtomInformationService(final AtomInformationService atomInformationService) {
        this.atomInformationService = atomInformationService;
    }

    public void setUriService(URIService uriService) {
        this.uriService = uriService;
    }

    public void setActiveMqOwnerProtcolQueueName(String activeMqOwnerProtcolQueueName) {
        this.activeMqOwnerProtcolQueueName = activeMqOwnerProtcolQueueName;
    }

    public void setActiveMqAtomProtcolQueueName(String activeMqAtomProtcolQueueName) {
        this.activeMqAtomProtcolQueueName = activeMqAtomProtcolQueueName;
    }

    public void setActiveMqMatcherPrtotocolQueueName(String activeMqMatcherPrtotocolQueueName) {
        this.activeMqMatcherPrtotocolQueueName = activeMqMatcherPrtotocolQueueName;
    }

    public void setActiveMqEndpoint(String activeMqEndpoint) {
        this.activeMqEndpoint = activeMqEndpoint;
    }

    public void setActiveMqMatcherProtocolTopicNameAtomCreated(
                    final String activeMqMatcherProtocolTopicNameAtomCreated) {
        this.activeMqMatcherProtocolTopicNameAtomCreated = activeMqMatcherProtocolTopicNameAtomCreated;
    }

    public void setActiveMqMatcherProtocolTopicNameAtomActivated(
                    final String activeMqMatcherProtocolTopicNameAtomActivated) {
        this.activeMqMatcherProtocolTopicNameAtomActivated = activeMqMatcherProtocolTopicNameAtomActivated;
    }

    public void setActiveMqMatcherProtocolTopicNameAtomDeactivated(
                    final String activeMqMatcherProtocolTopicNameAtomDeactivated) {
        this.activeMqMatcherProtocolTopicNameAtomDeactivated = activeMqMatcherProtocolTopicNameAtomDeactivated;
    }

    public void setActiveMqMatcherProtocolTopicNameAtomDeleted(
                    final String activeMqMatcherProtocolTopicNameAtomDeleted) {
        this.activeMqMatcherProtocolTopicNameAtomDeleted = activeMqMatcherProtocolTopicNameAtomDeleted;
    }

    private Dataset getFilteredAtomURIListDataset(Model model, Collection<URI> uris, URI filterSocketTypeUri,
                    URI filterAtomTypeUri) {
        Resource atomListPageResource = model.createResource(this.atomResourceURIPrefix + "/");
        Instant start = logger.isDebugEnabled() ? Instant.now() : null;
        if (filterSocketTypeUri == null && filterAtomTypeUri == null) {
            uris.forEach(atomURI -> model.add(model.createStatement(atomListPageResource, RDFS.member,
                            model.createResource(atomURI.toString()))));
        } else {
            uris.forEach(atomURI -> {
                Dataset atomDataset = getAtomDatasetForFilter(atomURI);
                DefaultAtomModelWrapper atomModelWrapper = new DefaultAtomModelWrapper(atomDataset);
                if ((filterSocketTypeUri == null ||
                                atomModelWrapper.getSocketTypeUriMap().containsValue(filterSocketTypeUri)) &&
                                (filterAtomTypeUri == null ||
                                                atomModelWrapper.getContentTypes().contains(filterAtomTypeUri))) {
                    model.add(model.createStatement(atomListPageResource, RDFS.member,
                                    model.createResource(atomURI.toString())));
                }
            });
            // Parallel Approach 1:
            // The parallel Approach leads to LazyInitializationException due to the lazy
            // fetch of the Atom, and thus cant be used
            // for now, TODO: FIGURE OUT A DIFFERENT APPROACH TO MAKE PARALLEL PROCESSING
            // POSSIBLE OR TO ENHANCE THE PERFORMANCE
            //
            // uris.parallelStream().filter(atomURI -> {
            // Dataset atomDataset = getAtomDatasetForFilter(atomURI);
            // DefaultAtomModelWrapper atomModelWrapper = new
            // DefaultAtomModelWrapper(atomDataset);
            // return (filterSocketTypeUri == null
            // || atomModelWrapper.getSocketTypeUriMap().containsValue(filterSocketTypeUri))
            // && (filterAtomTypeUri == null
            // || atomModelWrapper.getContentTypes().contains(filterAtomTypeUri));
            // }).collect(Collectors.toList()).forEach(atomURI ->
            // model.add(model.createStatement(atomListPageResource,
            // RDFS.member, model.createResource(atomURI.toString()))));
        }
        Dataset ret = newDatasetWithNamedModel(createDataGraphUriFromResource(atomListPageResource), model);
        addBaseUriAndDefaultPrefixes(ret);
        if (logger.isDebugEnabled() && start != null) {
            Instant finish = Instant.now();
            logger.debug("getFilteredAtomURIListDataset for {} Uris took {}ms", uris.size(),
                            Duration.between(start, finish).toMillis());
        }
        return ret;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED, readOnly = true)
    public Dataset getAtomDatasetForFilter(final URI atomUri) {
        Instant start = logger.isDebugEnabled() ? Instant.now() : null;
        DataWithEtag<Atom> atomDataWithEtag;
        try {
            atomDataWithEtag = atomInformationService.readAtom(atomUri, null);
        } catch (NoSuchAtomException e) {
            if (logger.isDebugEnabled() && start != null) {
                Instant finish = Instant.now();
                logger.debug("getAtomDatasetForfilter({}) took {}ms", atomUri,
                                Duration.between(start, finish).toMillis());
            }
            return null;
        }
        if (atomDataWithEtag.isNotFound()) {
            if (logger.isDebugEnabled() && start != null) {
                Instant finish = Instant.now();
                logger.debug("getAtomDatasetForfilter({}) took {}ms", atomUri,
                                Duration.between(start, finish).toMillis());
            }
            return null;
        }
        Atom atom = atomDataWithEtag.getData();
        // load the dataset from storage
        boolean isDeleted = (atom.getState() == AtomState.DELETED);
        Dataset dataset = isDeleted ? DatasetFactory.createGeneral() : atom.getDatatsetHolder().getDataset();
        Model metaModel = atomModelMapper.toModel(atom);
        Resource atomResource = metaModel.getResource(atomUri.toString());
        String atomMetaInformationURI = uriService.createAtomSysInfoGraphURI(atomUri).toString();
        Resource atomMetaInformationResource = metaModel.getResource(atomMetaInformationURI);
        // link atomMetaInformationURI to atom via rdfg:subGraphOf
        atomMetaInformationResource.addProperty(RDFG.SUBGRAPH_OF, atomResource);
        // add WON node link
        atomResource.addProperty(WON.wonNode, metaModel.createResource(this.resourceURIPrefix));
        // link all atom graphs taken from the create message to atom uri:
        Iterator<String> namesIt = dataset.listNames();
        while (namesIt.hasNext()) {
            String name = namesIt.next();
            Resource atomGraphResource = metaModel.getResource(name);
            atomResource.addProperty(WON.contentGraph, atomGraphResource);
        }
        // add meta model to dataset
        dataset.addNamedModel(atomMetaInformationURI, metaModel);
        addBaseUriAndDefaultPrefixes(dataset);
        if (logger.isDebugEnabled() && start != null) {
            Instant finish = Instant.now();
            logger.debug("getAtomDatasetForfilter({}) took {}ms", atomUri, Duration.between(start, finish).toMillis());
        }
        return dataset;
    }
}
