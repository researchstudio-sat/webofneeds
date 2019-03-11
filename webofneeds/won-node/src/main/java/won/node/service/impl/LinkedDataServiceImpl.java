/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package won.node.service.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

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
import won.protocol.exception.NoSuchNeedException;
import won.protocol.message.WonMessageType;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionModelMapper;
import won.protocol.model.ConnectionState;
import won.protocol.model.DataWithEtag;
import won.protocol.model.DatasetHolder;
import won.protocol.model.DatasetHolderAggregator;
import won.protocol.model.MessageEventPlaceholder;
import won.protocol.model.Need;
import won.protocol.model.NeedModelMapper;
import won.protocol.model.NeedState;
import won.protocol.model.unread.UnreadMessageInfo;
import won.protocol.model.unread.UnreadMessageInfoForNeed;
import won.protocol.repository.DatasetHolderRepository;
import won.protocol.repository.MessageEventRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.service.LinkedDataService;
import won.protocol.service.NeedInformationService;
import won.protocol.service.impl.UnreadInformationService;
import won.protocol.util.DefaultPrefixUtils;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.LDP;
import won.protocol.vocabulary.RDFG;
import won.protocol.vocabulary.WON;

/**
 * Creates rdf models from the relational database.
 * TODO: conform to: https://dvcs.w3.org/hg/ldpwg/raw-file/default/ldp-paging.html, especially for sorting
 */
public class LinkedDataServiceImpl implements LinkedDataService
{
  final Logger logger = LoggerFactory.getLogger(getClass());

  //prefix of a need resource
  private String needResourceURIPrefix;
  //prefix of a connection resource
  private String connectionResourceURIPrefix;
  //prefix of a event resource
  private String eventResourceURIPrefix;
  //prefix for URISs of RDF data
  private String dataURIPrefix;
  //prefix for URIs referring to real-world things
  private String resourceURIPrefix;
  //prefix for human readable pages
  private String pageURIPrefix;


  @Autowired
  private MessageEventRepository messageEventRepository;
  @Autowired
  private NeedRepository needRepository;
  @Autowired
  private DatasetHolderRepository datasetHolderRepository;

  //TODO: used to access/create event URIs for connection model rendering. Could be removed if events knew their URIs.
  private URIService uriService;
  @Autowired
  private NeedModelMapper needModelMapper;
  @Autowired
  private ConnectionModelMapper connectionModelMapper;

  @Autowired
  private CryptographyService cryptographyService;
  
  @Autowired 
  private UnreadInformationService unreadInformationService;
  

  private String needProtocolEndpoint;
  private String matcherProtocolEndpoint;
  private String ownerProtocolEndpoint;

  private NeedInformationService needInformationService;


  private String activeMqEndpoint;
  private String activeMqNeedProtcolQueueName;
  private String activeMqOwnerProtcolQueueName;
  private String activeMqMatcherPrtotocolQueueName;
  private String activeMqMatcherProtocolTopicNameNeedCreated;
  private String activeMqMatcherProtocolTopicNameNeedActivated;
  private String activeMqMatcherProtocolTopicNameNeedDeactivated;
  private String activeMqMatcherProtocolTopicNameNeedDeleted;


  @Transactional
  public Dataset listNeedURIs()
  {
    Model model = ModelFactory.createDefaultModel();
    setNsPrefixes(model);
    Resource needListPageResource = null;
    Collection<URI> uris = null;
    uris = needInformationService.listNeedURIs();
    needListPageResource = model.createResource(this.needResourceURIPrefix+"/");

    for (URI needURI : uris) {
      model.add(model.createStatement(needListPageResource, RDFS.member, model.createResource(needURI.toString())));
    }
    Dataset ret = newDatasetWithNamedModel(createDataGraphUriFromResource(needListPageResource), model);
    addBaseUriAndDefaultPrefixes(ret);
    return ret;
  }

  @Transactional
  public NeedInformationService.PagedResource<Dataset,URI> listNeedURIs(final int pageNum)
  {
    return listNeedURIs(pageNum, null, null);
  }

  @Transactional
  public NeedInformationService.PagedResource<Dataset,URI> listNeedURIsBefore(final URI need)
  {
    return listNeedURIsBefore(need, null, null);
  }

  @Transactional
  public NeedInformationService.PagedResource<Dataset,URI> listNeedURIsAfter(final URI need)
  {
    return listNeedURIsAfter(need, null, null);
  }

  @Transactional
  public NeedInformationService.PagedResource<Dataset,URI> listNeedURIs(final int pageNum, final Integer preferedSize,
                                                                    NeedState needState) {
    Slice<URI> slice = needInformationService.listNeedURIs(pageNum, preferedSize, needState);
    return toContainerPage(this.needResourceURIPrefix + "/", slice);

  }

  @Transactional
  public NeedInformationService.PagedResource<Dataset,URI> listNeedURIsBefore(
    final URI need, final Integer preferedSize, NeedState needState) {

    Slice<URI> slice = needInformationService.listNeedURIsBefore(need, preferedSize, needState);
    return toContainerPage(this.needResourceURIPrefix + "/", slice);

  }

  @Transactional
  public NeedInformationService.PagedResource<Dataset, URI> listNeedURIsAfter(
    final URI need, final Integer preferedSize, NeedState needState) {

    Slice<URI> slice = needInformationService.listNeedURIsAfter(need, preferedSize, needState);
    return toContainerPage(this.needResourceURIPrefix + "/", slice);

  }

  @Transactional
  public Dataset listModifiedNeedURIsAfter(Date modifiedDate) {

      Model model = ModelFactory.createDefaultModel();
      setNsPrefixes(model);
      Resource needListPageResource = null;
      Collection<URI> uris = null;
      uris = needInformationService.listModifiedNeedURIsAfter(modifiedDate);
      needListPageResource = model.createResource(this.needResourceURIPrefix+"/");

      for (URI needURI : uris) {
          model.add(model.createStatement(needListPageResource, RDFS.member, model.createResource(needURI.toString())));
      }
      Dataset ret = newDatasetWithNamedModel(createDataGraphUriFromResource(needListPageResource), model);
      addBaseUriAndDefaultPrefixes(ret);
      return ret;
  }

  @Transactional
  public DataWithEtag<Dataset> getNeedDataset(final URI needUri, String etag) {

      DataWithEtag<Need> data = null;
      try {
          data = needInformationService.readNeed(needUri, etag);
      } catch (NoSuchNeedException e) {
          return DataWithEtag.dataNotFound();
      }
      if (data.isNotFound()) {
          return DataWithEtag.dataNotFound();
      }
      if (!data.isChanged()){
          return DataWithEtag.dataNotChanged(data);
      }
      Need need = data.getData();
      String newEtag = data.getEtag();

    // load the dataset from storage
    boolean isDeleted = !!(need.getState() == NeedState.DELETED);
    Dataset dataset = isDeleted? DatasetFactory.createGeneral() : need.getDatatsetHolder().getDataset();
    Model metaModel = needModelMapper.toModel(need);

    Resource needResource = metaModel.getResource(needUri.toString());
    String needMetaInformationURI = uriService.createNeedSysInfoGraphURI(needUri).toString();
    Resource needMetaInformationResource = metaModel.getResource(needMetaInformationURI);
    
    //link needMetaInformationURI to need via rdfg:subGraphOf
    needMetaInformationResource.addProperty(RDFG.SUBGRAPH_OF, needResource);

    // add connections
    Resource connectionsContainer = metaModel.createResource(need.getNeedURI().toString() + "/connections");
    metaModel.add(metaModel.createStatement(needResource, WON.HAS_CONNECTIONS, connectionsContainer));

    // add need event container
    Resource needEventContainer = metaModel.createResource(need.getNeedURI().toString()+"#events", WON.EVENT_CONTAINER);
    metaModel.add(metaModel.createStatement(needResource, WON.HAS_EVENT_CONTAINER, needEventContainer));

    // add need event URIs
    Collection<MessageEventPlaceholder> messageEvents = need.getEventContainer().getEvents();
    for (MessageEventPlaceholder messageEvent : messageEvents) {
    metaModel.add(metaModel.createStatement(needEventContainer,
                                            RDFS.member,
                                            metaModel.getResource(messageEvent.getMessageURI().toString())));
    }

    // add WON node link
    needResource.addProperty(WON.HAS_WON_NODE, metaModel.createResource(this.resourceURIPrefix));

    // link all need graphs taken from the create message to need uri:
    Iterator<String> namesIt = dataset.listNames();
    while(namesIt.hasNext()) {
    	String name = namesIt.next();
    	Resource needGraphResource = metaModel.getResource(name);
    	needResource.addProperty(WON.HAS_CONTENT_GRAPH, needGraphResource);
    }
    
    // add meta model to dataset
    dataset.addNamedModel(needMetaInformationURI, metaModel);
    addBaseUriAndDefaultPrefixes(dataset);

    return new DataWithEtag<>(dataset ,newEtag, etag, isDeleted);
  }



  @Override
  @Transactional
  public Dataset getNeedDataset(final URI needUri, boolean deep, Integer deepLayerSize
                                ) throws NoSuchNeedException, NoSuchConnectionException, NoSuchMessageException {
    Dataset dataset = getNeedDataset(needUri, null).getData();
    if (deep) {
      Need need = needInformationService.readNeed(needUri);
      if (need.getState() == NeedState.ACTIVE) {
        //only add deep data if need is active
        Slice<URI> slice = needInformationService.listConnectionURIs(needUri, 1, deepLayerSize, null, null);
        NeedInformationService.PagedResource<Dataset, URI> connectionsResource = toContainerPage(
                this.uriService.createConnectionsURIForNeed(needUri).toString(), slice);
        addDeepConnectionData(connectionsResource.getContent(), slice.getContent());
        RdfUtils.addDatasetToDataset(dataset, connectionsResource.getContent());
        for (URI connectionUri : slice.getContent()) {
          NeedInformationService.PagedResource<Dataset, URI> eventsResource = listConnectionEventURIs(
                  connectionUri, 1, deepLayerSize, null, true);
          RdfUtils.addDatasetToDataset(dataset, eventsResource.getContent());
        }
      }
    }
    return dataset;
  }

  @Override
@Transactional
  public Model getUnreadInformationForNeed(URI needURI, Collection<URI> lastSeenMessageURIs) {
		UnreadMessageInfoForNeed unreadInfo = this.unreadInformationService.getUnreadInformation(needURI, lastSeenMessageURIs);
		Model ret = ModelFactory.createDefaultModel();
		Resource needRes = ret.createResource(needURI.toString());
		addUnreadInfoWithProperty(ret, needRes, WON.HAS_UNREAD_SUGGESTED, unreadInfo.getUnreadInfoByConnectionState().get(ConnectionState.SUGGESTED));
		addUnreadInfoWithProperty(ret, needRes, WON.HAS_UNREAD_CONNECTED, unreadInfo.getUnreadInfoByConnectionState().get(ConnectionState.CONNECTED));
		addUnreadInfoWithProperty(ret, needRes, WON.HAS_UNREAD_REQUEST_SENT, unreadInfo.getUnreadInfoByConnectionState().get(ConnectionState.REQUEST_SENT));
		addUnreadInfoWithProperty(ret, needRes, WON.HAS_UNREAD_REQUEST_RECEIVED, unreadInfo.getUnreadInfoByConnectionState().get(ConnectionState.REQUEST_RECEIVED));
		addUnreadInfoWithProperty(ret, needRes, WON.HAS_UNREAD_CLOSED, unreadInfo.getUnreadInfoByConnectionState().get(ConnectionState.CLOSED));
		unreadInfo.getUnreadMessageInfoForConnections().forEach(info -> {
			Resource connRes = ret.createResource(info.getConnectionURI().toString());
			addUnreadInfoWithProperty(ret, connRes, null, info.getUnreadInformation());
		});
		return ret;
  }

	private void addUnreadInfoWithProperty(Model ret, Resource subject, Property property, UnreadMessageInfo info) {
		if (info != null) {
			if (property != null) {
				//if we are given a property, make a blank node and connect it to the subject using the property
				Resource node = ret.createResource();
				subject.addProperty(property, node);
				subject = node;
			} 
			subject.addLiteral(WON.HAS_UNREAD_COUNT, info.getCount());
			subject.addLiteral(WON.HAS_UNREAD_OLDEST_TIMESTAMP, info.getOldestTimestamp().getTime());
			subject.addLiteral(WON.HAS_UNREAD_NEWEST_TIMESTAMP, info.getNewestTimestamp().getTime());
		}
	}

  @Transactional
  public Dataset getNodeDataset()
    {
      Model model = ModelFactory.createDefaultModel();
      setNsPrefixes(model);
      Resource showNodePageResource = null;
      showNodePageResource = model.createResource(this.resourceURIPrefix);
      addNeedList(model, showNodePageResource);
      addProtocolEndpoints(model, showNodePageResource);
      Dataset ret = newDatasetWithNamedModel(createDataGraphUriFromResource(showNodePageResource), model);
      addBaseUriAndDefaultPrefixes(ret);
      addPublicKey(model, showNodePageResource);
      return ret;
    }

  private void addNeedList(Model model, Resource res) {
    res.addProperty(WON.HAS_NEED_LIST, model.createResource(this.needResourceURIPrefix));
  }

  private void addPublicKey(Model model, Resource res) {
    WonKeysReaderWriter keyWriter = new WonKeysReaderWriter();
    try {
      keyWriter.writeToModel(model, res, cryptographyService.getPublicKey(res.getURI()));
    } catch (Exception e) {
      logger.warn("No public key could be added to RDF for " + res.getURI());
    }
  }

  //TODO: protocol endpoint specification in RDF model needs refactoring!
  private void addProtocolEndpoints(Model model, Resource res)
  {
      Resource blankNodeActiveMq = model.createResource();
      res.addProperty(WON.SUPPORTS_WON_PROTOCOL_IMPL, blankNodeActiveMq);
      blankNodeActiveMq
              .addProperty(RDF.type, WON.WON_OVER_ACTIVE_MQ)
              .addProperty(WON.HAS_BROKER_URI, model.createResource(this.activeMqEndpoint))
              .addProperty(WON.HAS_ACTIVEMQ_OWNER_PROTOCOL_QUEUE_NAME,this.activeMqOwnerProtcolQueueName,XSDDatatype.XSDstring)
              .addProperty(WON.HAS_ACTIVEMQ_NEED_PROTOCOL_QUEUE_NAME,this.activeMqNeedProtcolQueueName,XSDDatatype.XSDstring)
              .addProperty(WON.HAS_ACTIVEMQ_MATCHER_PROTOCOL_QUEUE_NAME,this.activeMqMatcherPrtotocolQueueName,XSDDatatype.XSDstring)
              .addProperty(WON.HAS_ACTIVEMQ_MATCHER_PROTOCOL_OUT_NEED_ACTIVATED_TOPIC_NAME,
                           this.activeMqMatcherProtocolTopicNameNeedActivated,XSDDatatype.XSDstring)
              .addProperty(WON.HAS_ACTIVEMQ_MATCHER_PROTOCOL_OUT_NEED_DEACTIVATED_TOPIC_NAME,this.activeMqMatcherProtocolTopicNameNeedDeactivated,XSDDatatype.XSDstring)
              .addProperty(WON.HAS_ACTIVEMQ_MATCHER_PROTOCOL_OUT_NEED_DELETED_TOPIC_NAME,this.activeMqMatcherProtocolTopicNameNeedDeleted,XSDDatatype.XSDstring)
              .addProperty(WON.HAS_ACTIVEMQ_MATCHER_PROTOCOL_OUT_NEED_CREATED_TOPIC_NAME,this.activeMqMatcherProtocolTopicNameNeedCreated,XSDDatatype.XSDstring)
      ;

      Resource blankNodeUriSpec = model.createResource();
      res.addProperty(WON.HAS_URI_PATTERN_SPECIFICATION, blankNodeUriSpec);
      blankNodeUriSpec.addProperty(WON.HAS_NEED_URI_PREFIX,
                                   model.createLiteral(this.needResourceURIPrefix));
      blankNodeUriSpec.addProperty(WON.HAS_CONNECTION_URI_PREFIX,
                                   model.createLiteral(this.connectionResourceURIPrefix));
      blankNodeUriSpec.addProperty(WON.HAS_EVENT_URI_PREFIX, model.createLiteral(this.eventResourceURIPrefix));
  }

  /***
   * ETag-aware method for obtaining connection data. Currently does not take into account new events, only changes
   * to the connection itself.
   * @param connectionUri
   * @param includeEventContainer
   * @param includeLatestEvent
   * @param etag
   * @return
   */
  @Override
  @Transactional
  public DataWithEtag<Dataset> getConnectionDataset(final URI connectionUri, final boolean includeEventContainer, final String etag)

  {
    DataWithEtag<Connection> data = null;
    Connection connection = null;
    data = needInformationService.readConnection(connectionUri, etag);
    if (data.isNotFound()) {
      return DataWithEtag.dataNotFound();
    }
    if (!data.isChanged()){
      return DataWithEtag.dataNotChanged(data);
    }
    connection = data.getData();
    if(connection == null) {
      return DataWithEtag.dataNotFound();
    }
    String newEtag = data.getEtag();

    // load the model from storage
    Model model = connectionModelMapper.toModel(connection);
    Model additionalData = connection.getDatasetHolder() == null ? null : connection.getDatasetHolder().getDataset().getDefaultModel();
    setNsPrefixes(model);
    if (additionalData != null) {
      model.add(additionalData);
    }
    //model.setNsPrefix("", connection.getConnectionURI().toString());

    //create connection member
    Resource connectionResource = model.getResource(connection.getConnectionURI().toString());

    // add WON node link
    connectionResource.addProperty(WON.HAS_WON_NODE, model.createResource(this.resourceURIPrefix));
    if (includeEventContainer) {
      //create event container and attach it to the member
      Resource eventContainer = model.createResource(connection.getConnectionURI().toString()+"/events");
      connectionResource.addProperty(WON.HAS_EVENT_CONTAINER, eventContainer);
      eventContainer.addProperty(RDF.type, WON.EVENT_CONTAINER);
      DatasetHolder datasetHolder = connection.getDatasetHolder();
      if (datasetHolder != null) {
        addAdditionalData(model, datasetHolder.getDataset().getDefaultModel(), connectionResource);
      }
    }

    Dataset connectionDataset = addBaseUriAndDefaultPrefixes(newDatasetWithNamedModel(createDataGraphUriFromResource
                                                                       (connectionResource), model));
    return new DataWithEtag<>(connectionDataset,newEtag, etag);
  }


  @Override
  @Transactional
  public Dataset listConnectionURIs(final boolean deep) throws NoSuchConnectionException {
    List<URI> uris = new ArrayList<URI>(needInformationService.listConnectionURIs());
    NeedInformationService.PagedResource<Dataset, URI> containerPage = toContainerPage(
      this.connectionResourceURIPrefix + "/", new SliceImpl<URI>(uris));
    if (deep) {
      addDeepConnectionData(containerPage.getContent(), uris);
    }

    return containerPage.getContent();
  }

  @Override
  @Transactional
  public Dataset listModifiedConnectionURIsAfter(Date modifiedAfter, boolean deep) throws NoSuchConnectionException {
      List<URI> uris = new ArrayList<URI>(needInformationService.listModifiedConnectionURIsAfter(modifiedAfter));
      NeedInformationService.PagedResource<Dataset, URI> containerPage = toContainerPage(
              this.connectionResourceURIPrefix + "/", new SliceImpl<URI>(uris));
      if (deep) {
          addDeepConnectionData(containerPage.getContent(), uris);
      }

      return containerPage.getContent();
  }

  @Override
  @Transactional
  public NeedInformationService.PagedResource<Dataset, URI> listConnectionURIs(final int page, final Integer
    preferredSize, Date timeSpot, final boolean deep) throws NoSuchConnectionException {
    Slice<URI> slice = needInformationService.listConnectionURIs(page, preferredSize, timeSpot);
    NeedInformationService.PagedResource<Dataset, URI> containerPage = toContainerPage(
      this.connectionResourceURIPrefix + "/", slice);
    if (deep) {
      addDeepConnectionData(containerPage.getContent(), slice.getContent());
    }
    return containerPage;
  }

  @Override
  @Transactional
  public NeedInformationService.PagedResource<Dataset, URI> listConnectionURIsBefore(
    URI beforeConnURI, final Integer preferredSize, Date timeSpot, boolean deep) throws NoSuchConnectionException {
    Slice<URI> slice = needInformationService.listConnectionURIsBefore(beforeConnURI, preferredSize, timeSpot);
    NeedInformationService.PagedResource<Dataset, URI> containerPage = toContainerPage(this.connectionResourceURIPrefix + "/", slice);
    if (deep) {
      addDeepConnectionData(containerPage.getContent(), slice.getContent());
    }
    return containerPage;
  }

  @Override
  @Transactional
  public NeedInformationService.PagedResource<Dataset, URI> listConnectionURIsAfter(
    URI afterConnURI, final Integer preferredSize, Date timeSpot, boolean deep) throws NoSuchConnectionException {
    Slice<URI> slice = needInformationService.listConnectionURIsAfter(afterConnURI, preferredSize, timeSpot);
    NeedInformationService.PagedResource<Dataset, URI> containerPage = toContainerPage(
      this.connectionResourceURIPrefix + "/", slice);
    if (deep) {
      addDeepConnectionData(containerPage.getContent(), slice.getContent());
    }
    return containerPage;
  }

    @Override
    @Transactional
    public Dataset listConnectionURIs(final URI needURI, boolean deep, boolean addMetadata)
    throws NoSuchNeedException, NoSuchConnectionException {
    List<URI> uris = new ArrayList<URI>(needInformationService.listConnectionURIs(needURI));
    URI connectionsUri = this.uriService.createConnectionsURIForNeed(needURI);
    NeedInformationService.PagedResource<Dataset, URI> containerPage = toContainerPage(connectionsUri.toString(), new
      SliceImpl<URI>(uris));
    if (deep) {
      addDeepConnectionData(containerPage.getContent(), uris);
    }
    if (addMetadata){
      addConnectionMetadata(containerPage.getContent(), needURI, connectionsUri);
    }
    return containerPage.getContent();
  }

  @Override
  @Transactional
  public NeedInformationService.PagedResource<Dataset, URI> listConnectionURIs(
    final int page, final URI needURI, final Integer preferredSize, final WonMessageType messageType, final Date
    timeSpot, boolean deep, boolean addMetadata)
    throws NoSuchNeedException, NoSuchConnectionException {
    Slice<URI> slice = needInformationService.listConnectionURIs(needURI, page, preferredSize, messageType, timeSpot);
    URI connectionsUri = this.uriService.createConnectionsURIForNeed(needURI);
    NeedInformationService.PagedResource<Dataset, URI> containerPage = toContainerPage(connectionsUri.toString(), slice);
    if (deep) {
      addDeepConnectionData(containerPage.getContent(), slice.getContent());
    }
    if (addMetadata){
      addConnectionMetadata(containerPage.getContent(), needURI, connectionsUri);
    }
    return containerPage;
  }

  @Override
  @Transactional
  public NeedInformationService.PagedResource<Dataset, URI> listConnectionURIsBefore(final URI needURI, URI
    beforeEventURI, final Integer preferredSize, final WonMessageType messageType, final Date timeSpot, boolean deep,
                                                                                     boolean addMetadata)
    throws NoSuchNeedException, NoSuchConnectionException {
    Slice<URI> slice = needInformationService.listConnectionURIsBefore(
      needURI, beforeEventURI, preferredSize, messageType, timeSpot);
    URI connectionsUri = this.uriService.createConnectionsURIForNeed(needURI);
    NeedInformationService.PagedResource<Dataset, URI> containerPage = toContainerPage(connectionsUri.toString(), slice);
    if (deep) {
      addDeepConnectionData(containerPage.getContent(), slice.getContent());
    }
    if (addMetadata){
      addConnectionMetadata(containerPage.getContent(), needURI, connectionsUri);
    }
    return containerPage;
  }

  @Override
  @Transactional
  public NeedInformationService.PagedResource<Dataset, URI> listConnectionURIsAfter(final URI needURI, URI
    resumeConnURI, final Integer preferredSize, final WonMessageType messageType, final Date timeSpot, boolean deep,
                                                                                    boolean addMetadata)
    throws NoSuchNeedException, NoSuchConnectionException {
    Slice<URI> slice = needInformationService.listConnectionURIsAfter(
      needURI, resumeConnURI, preferredSize, messageType, timeSpot);
    URI connectionsUri = this.uriService.createConnectionsURIForNeed(needURI);
    NeedInformationService.PagedResource<Dataset, URI> containerPage = toContainerPage(
      connectionsUri.toString(), slice);
    if (deep) {
      addDeepConnectionData(containerPage.getContent(), slice.getContent());
    }
    if (addMetadata){
      addConnectionMetadata(containerPage.getContent(), needURI, connectionsUri);
    }
    return containerPage;
  }

  private void addConnectionMetadata(final Dataset content, URI needURI, URI containerURI) {
    Model model = content.getNamedModel(createDataGraphUriFromUri(containerURI) );
    List<Object[]> connectionCountsPerState =  needRepository.getCountsPerConnectionState(needURI);
    Resource containerResource = model.getResource(containerURI.toString());
    for (Object[] countForState : connectionCountsPerState){
      ConnectionState stateName = (ConnectionState) countForState[0];
      Long count = (Long) countForState[1];
      Property countProperty = getRdfPropertyForState(stateName);
      if (countProperty == null) {
        logger.warn("did not recognize connection state " + stateName);
        continue;
      }
      containerResource.addProperty(countProperty, Integer.toString(count.intValue()), XSDDatatype.XSDint);
    }

    List<Object[]> connectionUrisWithState = needRepository.getConnectionUrisAndState(needURI);
    for (Object[] connUriWithState : connectionUrisWithState) {
        ConnectionState stateName = (ConnectionState) connUriWithState[0];
        URI connectionUri = (URI) connUriWithState[1];
        Property stateUrisProperty = getRdfPropertyForStateUris(stateName);
        if (stateUrisProperty == null) {
            logger.warn("did not recognize connection state " + stateName);
            continue;
        }
        containerResource.addProperty(stateUrisProperty, connectionUri.toString(), XSDDatatype.XSDID);
    }
  }

  private Property getRdfPropertyForState(ConnectionState state) {
    switch (state){
      case SUGGESTED: return WON.HAS_SUGGESTED_COUNT;
      case REQUEST_RECEIVED: return WON.HAS_REQUEST_RECEIVED_COUNT;
      case REQUEST_SENT: return WON.HAS_REQUEST_SENT_COUNT;
      case CONNECTED: return WON.HAS_CONNECTED_COUNT;
      case CLOSED: return WON.HAS_CLOSED_COUNT;
      case DELETED: return WON.HAS_DELETED_COUNT;
    }
    return null;
  }

  private Property getRdfPropertyForStateUris(ConnectionState state) {
    switch (state){
      case SUGGESTED: return WON.SUGGESTED;
      case REQUEST_RECEIVED: return WON.REQUEST_RECEIVED;
      case REQUEST_SENT: return WON.REQUEST_SENT;
      case CONNECTED: return WON.CONNECTED;
      case CLOSED: return WON.CLOSED;
      case DELETED: return WON.DELETED;
    }
    return null;
  }


  @Override
  @Transactional
  public Dataset listConnectionEventURIs(final URI connectionUri) throws
    NoSuchConnectionException
  {
    return  listConnectionEventURIs(connectionUri, false);
  }

  @Override
  @Transactional
  public Dataset listConnectionEventURIs(final URI connectionUri, boolean deep) throws
    NoSuchConnectionException
  {

    Model model = ModelFactory.createDefaultModel();
    setNsPrefixes(model);

    Connection connection = needInformationService.readConnection(connectionUri);
    Resource eventContainer = model.createResource(connection.getConnectionURI().toString() + "/events",
                                                   WON.EVENT_CONTAINER);
    // add the events with the new format (only the URI, no content)
    List<MessageEventPlaceholder> connectionEvents = messageEventRepository.findByParentURI(connectionUri);
    Dataset eventsContainerDataset = newDatasetWithNamedModel(createDataGraphUriFromResource(eventContainer), model);
    addBaseUriAndDefaultPrefixes(eventsContainerDataset);
    for (MessageEventPlaceholder event : connectionEvents) {
      model.add(model.createStatement(eventContainer,
                                      RDFS.member,
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
  public NeedInformationService.PagedResource<Dataset,URI> listConnectionEventURIs(final URI connectionUri, final int
    pageNum, Integer preferedSize, WonMessageType msgType, boolean deep) throws
    NoSuchConnectionException
  {
    Slice<MessageEventPlaceholder> slice = needInformationService.listConnectionEvents(connectionUri, pageNum,
                                                                      preferedSize, msgType);
    NeedInformationService.PagedResource<Dataset,URI> containerPage = eventsToContainerPage(
      this.uriService.createEventsURIForConnection(connectionUri).toString(), slice, deep);
    return containerPage;
  }



  @Override
  @Transactional
  public NeedInformationService.PagedResource<Dataset,URI> listConnectionEventURIsAfter(
    final URI connectionUri, final URI msgURI, Integer preferedSize, WonMessageType msgType, boolean deep) throws
    NoSuchConnectionException
  {
    Slice<MessageEventPlaceholder> slice = needInformationService.listConnectionEventsAfter(
      connectionUri, msgURI, preferedSize, msgType);
    NeedInformationService.PagedResource<Dataset,URI> containerPage = eventsToContainerPage(this.uriService
                                                                                        .createEventsURIForConnection
                                                                                          (connectionUri).toString(),
                                                                                      slice, deep);
    return containerPage;
  }

  @Override
  @Transactional
  public NeedInformationService.PagedResource<Dataset,URI> listConnectionEventURIsBefore(
    final URI connectionUri, final URI msgURI, Integer preferedSize, WonMessageType msgType, boolean deep) throws
    NoSuchConnectionException
  {

    Slice<MessageEventPlaceholder> slice = needInformationService.listConnectionEventsBefore(
      connectionUri, msgURI, preferedSize, msgType);
    NeedInformationService.PagedResource<Dataset,URI> containerPage = eventsToContainerPage(this.uriService.createEventsURIForConnection
      (connectionUri).toString(), slice, deep);
    return containerPage;
  }

  private Dataset setDefaults(Dataset dataset) {
    if (dataset == null) return null;
    DefaultPrefixUtils.setDefaultPrefixes(dataset.getDefaultModel());
    addBaseUriAndDefaultPrefixes(dataset);
    return dataset;
  }

  @Override
  @Transactional
  public DataWithEtag<Dataset> getDatasetForUri(URI datasetUri, String etag) {
    Integer version = etag == null ? -1: Integer.valueOf(etag);
    DatasetHolder datasetHolder = datasetHolderRepository.findOneByUri(datasetUri);
    if (datasetHolder == null) {
      return DataWithEtag.dataNotFound();
    }
    if (version.intValue() == datasetHolder.getVersion()){
      return DataWithEtag.dataNotChanged(etag);
    }
    Dataset dataset = datasetHolder.getDataset();
    DefaultPrefixUtils.setDefaultPrefixes(dataset.getDefaultModel());
    addBaseUriAndDefaultPrefixes(dataset);
    return new DataWithEtag<Dataset>(dataset, Integer.toString(datasetHolder.getVersion()), etag);
  }


  private String createDataGraphUriFromResource(Resource needListPageResource) {
    URI uri = URI.create(needListPageResource.getURI());
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
      //TODO: check if the statement below is now necessary
      //RdfUtils.replaceBaseResource(additionalDataModel, additionalData);
      targetModel.add(targetModel.createStatement(targetResource, WON.HAS_ADDITIONAL_DATA, additionalData));
      targetModel.add(fromModel);
    }
  }



  private String addPageQueryString(String uri, int page)
  {
    //TODO: simple implementation for adding page number to uri - breaks as soon as other query strings are present!
    return uri + "?page=" + page;
  }

  /**
   * @deprecated  the method returns the paged resource description according to Linked Data Platform Draft 2013
   * https://www.w3.org/TR/2013/WD-ldp-20130730/. As of state Feb 2016 (https://www.w3.org/TR/2015/REC-ldp-20150226/)
   * the paged resource should not contain any paging information as part of resource description, this information
   * is conveyed vie HEADERs. Therefore, this method should no longer be used.
   */
  @Deprecated
  private Resource createPage(final Model model, final String containerURI, final int pageNum, NeedInformationService.Page page)
  {
    String containerPageURI = addPageQueryString(containerURI, pageNum);
    Resource containerPageResource = model.createResource(containerPageURI);
    Resource containerResource = model.createResource(containerURI);
    model.add(model.createStatement(containerPageResource, RDF.type, LDP.PAGE));
    model.add(model.createStatement(containerPageResource, LDP.PAGE_OF, containerResource));
    model.add(model.createStatement(containerPageResource, RDF.type, LDP.CONTAINER));
    Resource containerNextPageResource = null;
    //assume last page if we didn't fetch pageSize uris
    if (page.hasNext()) {
      containerNextPageResource = model.createResource(addPageQueryString(containerURI, pageNum + 1));
      model.add(model.createStatement(containerPageResource, LDP.NEXT_PAGE, containerNextPageResource));
    }

    return containerPageResource;
  }

  private void setNsPrefixes(final Model model)
  {
    DefaultPrefixUtils.setDefaultPrefixes(model);
  }

  /**
   * Adds the specified URI as the default prefix for each model in the dataset and
   * return the dataset.
   * @param dataset
   * @return
   */
  private Dataset addBaseUriAndDefaultPrefixes(Dataset dataset){
    setNsPrefixes(dataset.getDefaultModel());
    addPrefixForSpecialResources(dataset, "local", this.resourceURIPrefix);
    addPrefixForSpecialResources(dataset, "need", this.needResourceURIPrefix);
    addPrefixForSpecialResources(dataset, "event", this.eventResourceURIPrefix);
    addPrefixForSpecialResources(dataset, "conn", this.connectionResourceURIPrefix);
    return dataset;
  }

  private void addPrefixForSpecialResources(Dataset dataset, String prefix, String uri) {
    if (uri == null) return; //ignore if no uri specified
    //the prefix (prefix of all local URIs must end with a slash or a hash, otherwise,
    //it will never be used by RDF serializations. Force that.
    if (!uri.endsWith("/") && !uri.endsWith("#")) {
      uri += "/";
    }
    dataset.getDefaultModel().getGraph().getPrefixMapping().setNsPrefix(prefix, uri);
  }

  private NeedInformationService.PagedResource<Dataset,URI> toContainerPage(String containerUri, Slice<URI>
  slice) {

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
  Resource needListPageResource = null;

  needListPageResource = model.createResource(containerUri);

  for (URI needURI : uris) {
    model.add(model.createStatement(needListPageResource, RDFS.member, model.createResource(needURI.toString())));
  }
  Dataset dataset = newDatasetWithNamedModel(createDataGraphUriFromResource(needListPageResource), model);
  addBaseUriAndDefaultPrefixes(dataset);
  NeedInformationService.PagedResource<Dataset,URI> containerPage = new NeedInformationService.PagedResource
    (dataset, resumeBefore, resumeAfter);
  return containerPage;
}

  /**
   * Returns a container resource with the messageUris of all MessageEventPlaceholder objects in the slice.
   * If deep == true, the event datasets are added, too.
   * @param containerUri
   * @param slice
   * @param deep
   * @return
   */
  private NeedInformationService.PagedResource<Dataset,URI> eventsToContainerPage(String containerUri,
                                                                             Slice<MessageEventPlaceholder>
    slice, boolean deep) {

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
    Resource needListPageResource = null;

    needListPageResource = model.createResource(containerUri);

    DatasetHolderAggregator aggregator = new DatasetHolderAggregator();
    for (MessageEventPlaceholder event : events) {
      model.add(model.createStatement(needListPageResource, RDFS.member, model.createResource(event.getMessageURI().toString())));
      if (deep) {
        aggregator.appendDataset(event.getDatasetHolder());
      }
    }
    Dataset dataset = aggregator.aggregate();
    dataset.addNamedModel(createDataGraphUriFromResource(needListPageResource), model);
    addBaseUriAndDefaultPrefixes(dataset);
    NeedInformationService.PagedResource<Dataset,URI> containerPage = new NeedInformationService.PagedResource
      (dataset, resumeBefore, resumeAfter);
    return containerPage;
  }

  public void addDeepConnectionData(Dataset dataset, List<URI> connectionURIs) throws NoSuchConnectionException {
    //add the connection model for each connection URI
    for (URI connectionURI : connectionURIs) {
      DataWithEtag<Dataset> connectionDataset =
        getConnectionDataset(connectionURI, true, null);
      RdfUtils.addDatasetToDataset(dataset, connectionDataset.getData());
    }
  }

  public void setNeedResourceURIPrefix(final String needResourceURIPrefix)
  {
    this.needResourceURIPrefix = needResourceURIPrefix;
  }

  public void setConnectionResourceURIPrefix(final String connectionResourceURIPrefix)
  {
    this.connectionResourceURIPrefix = connectionResourceURIPrefix;
  }

  public void setEventResourceURIPrefix(final String eventResourceURIPrefix) {
    this.eventResourceURIPrefix = eventResourceURIPrefix;
  }

  public void setDataURIPrefix(final String dataURIPrefix)
  {
    this.dataURIPrefix = dataURIPrefix;
  }

  public void setResourceURIPrefix(final String resourceURIPrefix)
  {
    this.resourceURIPrefix = resourceURIPrefix;
  }

  public void setNeedInformationService(final NeedInformationService needInformationService)
  {
    this.needInformationService = needInformationService;
  }

  public void setNeedProtocolEndpoint(final String needProtocolEndpoint)
  {
    this.needProtocolEndpoint = needProtocolEndpoint;
  }

  public void setMatcherProtocolEndpoint(final String matcherProtocolEndpoint)
  {
    this.matcherProtocolEndpoint = matcherProtocolEndpoint;
  }

  public void setOwnerProtocolEndpoint(final String ownerProtocolEndpoint)
  {
    this.ownerProtocolEndpoint = ownerProtocolEndpoint;
  }

  public void setPageURIPrefix(final String pageURIPrefix)
  {
    this.pageURIPrefix = pageURIPrefix;
  }

  public void setUriService(URIService uriService) {
    this.uriService = uriService;
  }

  public void setActiveMqOwnerProtcolQueueName(String activeMqOwnerProtcolQueueName) {
      this.activeMqOwnerProtcolQueueName = activeMqOwnerProtcolQueueName;
  }

  public void setActiveMqNeedProtcolQueueName(String activeMqNeedProtcolQueueName) {
      this.activeMqNeedProtcolQueueName = activeMqNeedProtcolQueueName;
  }
  public void setActiveMqMatcherPrtotocolQueueName(String activeMqMatcherPrtotocolQueueName) {
      this.activeMqMatcherPrtotocolQueueName = activeMqMatcherPrtotocolQueueName;
  }
  public void setActiveMqEndpoint(String activeMqEndpoint) {
      this.activeMqEndpoint = activeMqEndpoint;
  }

  public void setActiveMqMatcherProtocolTopicNameNeedCreated(final String activeMqMatcherProtocolTopicNameNeedCreated) {
    this.activeMqMatcherProtocolTopicNameNeedCreated = activeMqMatcherProtocolTopicNameNeedCreated;
  }

  public void setActiveMqMatcherProtocolTopicNameNeedActivated(final String activeMqMatcherProtocolTopicNameNeedActivated) {
    this.activeMqMatcherProtocolTopicNameNeedActivated = activeMqMatcherProtocolTopicNameNeedActivated;
  }

  public void setActiveMqMatcherProtocolTopicNameNeedDeactivated(final String activeMqMatcherProtocolTopicNameNeedDeactivated) {
    this.activeMqMatcherProtocolTopicNameNeedDeactivated = activeMqMatcherProtocolTopicNameNeedDeactivated;
  }
  
  public void setActiveMqMatcherProtocolTopicNameNeedDeleted(final String activeMqMatcherProtocolTopicNameNeedDeleted) {
      this.activeMqMatcherProtocolTopicNameNeedDeleted = activeMqMatcherProtocolTopicNameNeedDeleted;
    }
}
