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
package won.node.service.impl;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.NoSuchMessageException;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import won.cryptography.rdfsign.WonKeysReaderWriter;
import won.cryptography.service.CryptographyService;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchAtomException;
import won.protocol.message.WonMessageType;
import won.protocol.model.*;
import won.protocol.model.unread.UnreadMessageInfo;
import won.protocol.model.unread.UnreadMessageInfoForAtom;
import won.protocol.repository.DatasetHolderRepository;
import won.protocol.repository.MessageEventRepository;
import won.protocol.repository.AtomRepository;
import won.protocol.service.LinkedDataService;
import won.protocol.service.AtomInformationService;
import won.protocol.service.impl.UnreadInformationService;
import won.protocol.util.DefaultPrefixUtils;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.LDP;
import won.protocol.vocabulary.RDFG;
import won.protocol.vocabulary.WON;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Creates rdf models from the relational database. TODO: conform to:
 * https://dvcs.w3.org/hg/ldpwg/raw-file/default/ldp-paging.html, especially for
 * sorting
 */
public class LinkedDataServiceImpl implements LinkedDataService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    // prefix of an atom resource
    private String atomResourceURIPrefix;
    // prefix of a connection resource
    private String connectionResourceURIPrefix;
    // prefix of a event resource
    private String eventResourceURIPrefix;
    // prefix for URISs of RDF data
    private String dataURIPrefix;
    // prefix for URIs referring to real-world things
    private String resourceURIPrefix;
    // prefix for human readable pages
    private String pageURIPrefix;
    @Autowired
    private MessageEventRepository messageEventRepository;
    @Autowired
    private AtomRepository atomRepository;
    @Autowired
    private DatasetHolderRepository datasetHolderRepository;
    // TODO: used to access/create event URIs for connection model rendering. Could
    // be removed if events knew their URIs.
    private URIService uriService;
    @Autowired
    private AtomModelMapper atomModelMapper;
    @Autowired
    private ConnectionModelMapper connectionModelMapper;
    @Autowired
    private CryptographyService cryptographyService;
    @Autowired
    private UnreadInformationService unreadInformationService;
    private String atomProtocolEndpoint;
    private String matcherProtocolEndpoint;
    private String ownerProtocolEndpoint;
    private AtomInformationService atomInformationService;
    private String activeMqEndpoint;
    private String activeMqAtomProtcolQueueName;
    private String activeMqOwnerProtcolQueueName;
    private String activeMqMatcherPrtotocolQueueName;
    private String activeMqMatcherProtocolTopicNameAtomCreated;
    private String activeMqMatcherProtocolTopicNameAtomActivated;
    private String activeMqMatcherProtocolTopicNameAtomDeactivated;
    private String activeMqMatcherProtocolTopicNameAtomDeleted;

    @Transactional
    public Dataset listAtomURIs() {
        Model model = ModelFactory.createDefaultModel();
        setNsPrefixes(model);
        Collection<URI> uris = atomInformationService.listAtomURIs();
        Resource atomListPageResource = model.createResource(this.atomResourceURIPrefix + "/");
        for (URI atomURI : uris) {
            model.add(model.createStatement(atomListPageResource, RDFS.member,
                            model.createResource(atomURI.toString())));
        }
        Dataset ret = newDatasetWithNamedModel(createDataGraphUriFromResource(atomListPageResource), model);
        addBaseUriAndDefaultPrefixes(ret);
        return ret;
    }

    @Transactional
    public AtomInformationService.PagedResource<Dataset, URI> listAtomURIs(final int pageNum) {
        return listAtomURIs(pageNum, null, null);
    }

    @Transactional
    public AtomInformationService.PagedResource<Dataset, URI> listAtomURIsBefore(final URI atom) {
        return listAtomURIsBefore(atom, null, null);
    }

    @Transactional
    public AtomInformationService.PagedResource<Dataset, URI> listAtomURIsAfter(final URI atom) {
        return listAtomURIsAfter(atom, null, null);
    }

    @Transactional
    public AtomInformationService.PagedResource<Dataset, URI> listAtomURIs(final int pageNum,
                    final Integer preferedSize, AtomState atomState) {
        Slice<URI> slice = atomInformationService.listAtomURIs(pageNum, preferedSize, atomState);
        return toContainerPage(this.atomResourceURIPrefix + "/", slice);
    }

    @Transactional
    public AtomInformationService.PagedResource<Dataset, URI> listAtomURIsBefore(final URI atom,
                    final Integer preferedSize, AtomState atomState) {
        Slice<URI> slice = atomInformationService.listAtomURIsBefore(atom, preferedSize, atomState);
        return toContainerPage(this.atomResourceURIPrefix + "/", slice);
    }

    @Transactional
    public AtomInformationService.PagedResource<Dataset, URI> listAtomURIsAfter(final URI atom,
                    final Integer preferedSize, AtomState atomState) {
        Slice<URI> slice = atomInformationService.listAtomURIsAfter(atom, preferedSize, atomState);
        return toContainerPage(this.atomResourceURIPrefix + "/", slice);
    }

    @Transactional
    public Dataset listModifiedAtomURIsAfter(Date modifiedDate) {
        Model model = ModelFactory.createDefaultModel();
        setNsPrefixes(model);
        Collection<URI> uris = atomInformationService.listModifiedAtomURIsAfter(modifiedDate);
        Resource atomListPageResource = model.createResource(this.atomResourceURIPrefix + "/");
        for (URI atomURI : uris) {
            model.add(model.createStatement(atomListPageResource, RDFS.member,
                            model.createResource(atomURI.toString())));
        }
        Dataset ret = newDatasetWithNamedModel(createDataGraphUriFromResource(atomListPageResource), model);
        addBaseUriAndDefaultPrefixes(ret);
        return ret;
    }

    @Transactional
    public DataWithEtag<Dataset> getAtomDataset(final URI atomUri, String etag) {
        DataWithEtag<Atom> data;
        try {
            data = atomInformationService.readAtom(atomUri, etag);
        } catch (NoSuchAtomException e) {
            return DataWithEtag.dataNotFound();
        }
        if (data.isNotFound()) {
            return DataWithEtag.dataNotFound();
        }
        if (!data.isChanged()) {
            return DataWithEtag.dataNotChanged(data);
        }
        Atom atom = data.getData();
        String newEtag = data.getEtag();
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
        Resource connectionsContainer = metaModel.createResource(atom.getAtomURI().toString() + "/connections");
        metaModel.add(metaModel.createStatement(atomResource, WON.connections, connectionsContainer));
        // add atom event container
        Resource atomMessageContainer = metaModel.createResource(atom.getAtomURI().toString() + "#events",
                        WON.MessageContainer);
        metaModel.add(metaModel.createStatement(atomResource, WON.messageContainer, atomMessageContainer));
        // add atom event URIs
        Collection<MessageEventPlaceholder> messageEvents = atom.getMessageContainer().getEvents();
        for (MessageEventPlaceholder messageEvent : messageEvents) {
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
        return new DataWithEtag<>(dataset, newEtag, etag, isDeleted);
    }

    @Override
    @Transactional
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
    @Transactional
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

    @Transactional
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
                        .addProperty(WON.activeMQOwnerProtocolQueueName, this.activeMqOwnerProtcolQueueName,
                                        XSDDatatype.XSDstring)
                        .addProperty(WON.activeMQAtomProtocolQueueName, this.activeMqAtomProtcolQueueName,
                                        XSDDatatype.XSDstring)
                        .addProperty(WON.activeMQMatcherProtocolQueueName, this.activeMqMatcherPrtotocolQueueName,
                                        XSDDatatype.XSDstring)
                        .addProperty(WON.activeMQMatcherProtocolOutAtomActivatedTopicName,
                                        this.activeMqMatcherProtocolTopicNameAtomActivated, XSDDatatype.XSDstring)
                        .addProperty(WON.activeMQMatcherProtocolOutAtomDeactivatedTopicName,
                                        this.activeMqMatcherProtocolTopicNameAtomDeactivated, XSDDatatype.XSDstring)
                        .addProperty(WON.activeMQMatcherProtocolOutAtomDeletedTopicName,
                                        this.activeMqMatcherProtocolTopicNameAtomDeleted, XSDDatatype.XSDstring)
                        .addProperty(WON.activeMQMatcherProtocolOutAtomCreatedTopicName,
                                        this.activeMqMatcherProtocolTopicNameAtomCreated, XSDDatatype.XSDstring);
        Resource blankNodeUriSpec = model.createResource();
        res.addProperty(WON.uriPrefixSpecification, blankNodeUriSpec);
        blankNodeUriSpec.addProperty(WON.atomUriPrefix, model.createLiteral(this.atomResourceURIPrefix));
        blankNodeUriSpec.addProperty(WON.connectionUriPrefix, model.createLiteral(this.connectionResourceURIPrefix));
        blankNodeUriSpec.addProperty(WON.eventUriPrefix, model.createLiteral(this.eventResourceURIPrefix));
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
    @Transactional
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
        Model additionalData = connection.getDatasetHolder() == null ? null
                        : connection.getDatasetHolder().getDataset().getDefaultModel();
        setNsPrefixes(model);
        if (additionalData != null) {
            model.add(additionalData);
        }
        // model.setNsPrefix("", connection.getConnectionURI().toString());
        // create connection member
        Resource connectionResource = model.getResource(connection.getConnectionURI().toString());
        // add WON node link
        connectionResource.addProperty(WON.wonNode, model.createResource(this.resourceURIPrefix));
        if (includeMessageContainer) {
            // create event container and attach it to the member
            Resource messageContainer = model.createResource(connection.getConnectionURI().toString() + "/events");
            connectionResource.addProperty(WON.messageContainer, messageContainer);
            messageContainer.addProperty(RDF.type, WON.MessageContainer);
            DatasetHolder datasetHolder = connection.getDatasetHolder();
            if (datasetHolder != null) {
                addAdditionalData(model, datasetHolder.getDataset().getDefaultModel(), connectionResource);
            }
        }
        Dataset connectionDataset = addBaseUriAndDefaultPrefixes(
                        newDatasetWithNamedModel(createDataGraphUriFromResource(connectionResource), model));
        return new DataWithEtag<>(connectionDataset, newEtag, etag);
    }

    @Override
    @Transactional
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
    @Transactional
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
    @Transactional
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
    @Transactional
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
    @Transactional
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
    @Transactional
    public AtomInformationService.PagedResource<Dataset, Connection> listConnections(final URI atomURI, boolean deep,
                    boolean addMetadata) throws NoSuchAtomException, NoSuchConnectionException {
        List<Connection> connections = new ArrayList<>(atomInformationService.listConnections(atomURI));
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
    @Transactional
    public AtomInformationService.PagedResource<Dataset, Connection> listConnections(final int page, final URI atomURI,
                    final Integer preferredSize, final WonMessageType messageType, final Date timeSpot, boolean deep,
                    boolean addMetadata) throws NoSuchAtomException, NoSuchConnectionException {
        Slice<Connection> slice = atomInformationService.listConnections(atomURI, page, preferredSize, messageType,
                        timeSpot);
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
    @Transactional
    public AtomInformationService.PagedResource<Dataset, Connection> listConnectionsBefore(final URI atomURI,
                    URI beforeEventURI, final Integer preferredSize, final WonMessageType messageType,
                    final Date timeSpot, boolean deep, boolean addMetadata)
                    throws NoSuchAtomException, NoSuchConnectionException {
        Slice<Connection> slice = atomInformationService.listConnectionsBefore(atomURI, beforeEventURI, preferredSize,
                        messageType, timeSpot);
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
    @Transactional
    public AtomInformationService.PagedResource<Dataset, Connection> listConnectionsAfter(final URI atomURI,
                    URI resumeConnURI, final Integer preferredSize, final WonMessageType messageType,
                    final Date timeSpot, boolean deep, boolean addMetadata)
                    throws NoSuchAtomException, NoSuchConnectionException {
        Slice<Connection> slice = atomInformationService.listConnectionsAfter(atomURI, resumeConnURI, preferredSize,
                        messageType, timeSpot);
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
    @Transactional
    public Dataset listConnectionEventURIs(final URI connectionUri, boolean deep) throws NoSuchConnectionException {
        Model model = ModelFactory.createDefaultModel();
        setNsPrefixes(model);
        Connection connection = atomInformationService.readConnection(connectionUri);
        Resource messageContainer = model.createResource(connection.getConnectionURI().toString() + "/events",
                        WON.MessageContainer);
        // add the events with the new format (only the URI, no content)
        List<MessageEventPlaceholder> connectionEvents = messageEventRepository.findByParentURI(connectionUri);
        Dataset eventsContainerDataset = newDatasetWithNamedModel(createDataGraphUriFromResource(messageContainer),
                        model);
        addBaseUriAndDefaultPrefixes(eventsContainerDataset);
        for (MessageEventPlaceholder event : connectionEvents) {
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
    @Transactional
    public AtomInformationService.PagedResource<Dataset, URI> listConnectionEventURIs(final URI connectionUri,
                    final int pageNum, Integer preferedSize, WonMessageType msgType, boolean deep)
                    throws NoSuchConnectionException {
        Slice<MessageEventPlaceholder> slice = atomInformationService.listConnectionEvents(connectionUri, pageNum,
                        preferedSize, msgType);
        return eventsToContainerPage(this.uriService.createEventsURIForConnection(connectionUri).toString(), slice,
                        deep);
    }

    @Override
    @Transactional
    public AtomInformationService.PagedResource<Dataset, URI> listConnectionEventURIsAfter(final URI connectionUri,
                    final URI msgURI, Integer preferedSize, WonMessageType msgType, boolean deep)
                    throws NoSuchConnectionException {
        Slice<MessageEventPlaceholder> slice = atomInformationService.listConnectionEventsAfter(connectionUri, msgURI,
                        preferedSize, msgType);
        return eventsToContainerPage(this.uriService.createEventsURIForConnection(connectionUri).toString(), slice,
                        deep);
    }

    @Override
    @Transactional
    public AtomInformationService.PagedResource<Dataset, URI> listConnectionEventURIsBefore(final URI connectionUri,
                    final URI msgURI, Integer preferedSize, WonMessageType msgType, boolean deep)
                    throws NoSuchConnectionException {
        Slice<MessageEventPlaceholder> slice = atomInformationService.listConnectionEventsBefore(connectionUri, msgURI,
                        preferedSize, msgType);
        return eventsToContainerPage(this.uriService.createEventsURIForConnection(connectionUri).toString(), slice,
                        deep);
    }

    private Dataset setDefaults(Dataset dataset) {
        if (dataset == null)
            return null;
        DefaultPrefixUtils.setDefaultPrefixes(dataset.getDefaultModel());
        addBaseUriAndDefaultPrefixes(dataset);
        return dataset;
    }

    @Override
    @Transactional
    public DataWithEtag<Dataset> getDatasetForUri(URI datasetUri, String etag) {
        Integer version = etag == null ? -1 : Integer.valueOf(etag);
        DatasetHolder datasetHolder = datasetHolderRepository.findOneByUri(datasetUri);
        if (datasetHolder == null) {
            return DataWithEtag.dataNotFound();
        }
        if (version == datasetHolder.getVersion()) {
            return DataWithEtag.dataNotChanged(etag);
        }
        Dataset dataset = datasetHolder.getDataset();
        DefaultPrefixUtils.setDefaultPrefixes(dataset.getDefaultModel());
        addBaseUriAndDefaultPrefixes(dataset);
        return new DataWithEtag<>(dataset, Integer.toString(datasetHolder.getVersion()), etag);
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

    private String addPageQueryString(String uri, int page) {
        // TODO: simple implementation for adding page number to uri - breaks as soon as
        // other query strings are present!
        return uri + "?page=" + page;
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
        addPrefixForSpecialResources(dataset, "event", this.eventResourceURIPrefix);
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
        if (slice.getSort() != null && !uris.isEmpty()) {
            Iterator<Sort.Order> sortOrders = slice.getSort().iterator();
            if (sortOrders.hasNext()) {
                Sort.Order sortOrder = sortOrders.next();
                if (sortOrder.getDirection() == Sort.Direction.ASC) {
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
        return new AtomInformationService.PagedResource(dataset, resumeBefore, resumeAfter);
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
        Model model = ModelFactory.createDefaultModel();
        setNsPrefixes(model);
        Resource atomListPageResource = model.createResource(containerUri);
        for (Connection conn : connections) {
            model.add(model.createStatement(atomListPageResource, RDFS.member,
                            model.createResource(conn.getConnectionURI().toString())));
            model.add(connectionModelMapper.toModel(conn));
        }
        Dataset dataset = newDatasetWithNamedModel(createDataGraphUriFromResource(atomListPageResource), model);
        addBaseUriAndDefaultPrefixes(dataset);
        return new AtomInformationService.PagedResource(dataset, resumeBefore, resumeAfter);
    }

    /**
     * Returns a container resource with the messageUris of all
     * MessageEventPlaceholder objects in the slice. If deep == true, the event
     * datasets are added, too.
     *
     * @param containerUri
     * @param slice
     * @param deep
     * @return
     */
    private AtomInformationService.PagedResource<Dataset, URI> eventsToContainerPage(String containerUri,
                    Slice<MessageEventPlaceholder> slice, boolean deep) {
        List<MessageEventPlaceholder> events = slice.getContent();
        URI resumeBefore = null;
        URI resumeAfter = null;
        if (slice.getSort() != null && !events.isEmpty()) {
            Iterator<Sort.Order> sortOrders = slice.getSort().iterator();
            if (sortOrders.hasNext()) {
                Sort.Order sortOrder = sortOrders.next();
                if (sortOrder.getDirection() == Sort.Direction.ASC) {
                    resumeBefore = events.get(0).getMessageURI();
                    resumeAfter = events.get(events.size() - 1).getMessageURI();
                } else {
                    resumeBefore = events.get(events.size() - 1).getMessageURI();
                    resumeAfter = events.get(0).getMessageURI();
                }
            }
        }
        Model model = ModelFactory.createDefaultModel();
        setNsPrefixes(model);
        Resource atomListPageResource = model.createResource(containerUri);
        DatasetHolderAggregator aggregator = new DatasetHolderAggregator();
        for (MessageEventPlaceholder event : events) {
            model.add(model.createStatement(atomListPageResource, RDFS.member,
                            model.createResource(event.getMessageURI().toString())));
            if (deep) {
                aggregator.appendDataset(event.getDatasetHolder());
            }
        }
        Dataset dataset = aggregator.aggregate();
        dataset.addNamedModel(createDataGraphUriFromResource(atomListPageResource), model);
        addBaseUriAndDefaultPrefixes(dataset);
        return new AtomInformationService.PagedResource(dataset, resumeBefore, resumeAfter);
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
        this.eventResourceURIPrefix = eventResourceURIPrefix;
    }

    public void setDataURIPrefix(final String dataURIPrefix) {
        this.dataURIPrefix = dataURIPrefix;
    }

    public void setResourceURIPrefix(final String resourceURIPrefix) {
        this.resourceURIPrefix = resourceURIPrefix;
    }

    public void setAtomInformationService(final AtomInformationService atomInformationService) {
        this.atomInformationService = atomInformationService;
    }

    public void setAtomProtocolEndpoint(final String atomProtocolEndpoint) {
        this.atomProtocolEndpoint = atomProtocolEndpoint;
    }

    public void setMatcherProtocolEndpoint(final String matcherProtocolEndpoint) {
        this.matcherProtocolEndpoint = matcherProtocolEndpoint;
    }

    public void setOwnerProtocolEndpoint(final String ownerProtocolEndpoint) {
        this.ownerProtocolEndpoint = ownerProtocolEndpoint;
    }

    public void setPageURIPrefix(final String pageURIPrefix) {
        this.pageURIPrefix = pageURIPrefix;
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
}
