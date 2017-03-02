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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import won.cryptography.rdfsign.WonKeysReaderWriter;
import won.cryptography.service.CryptographyService;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.message.WonMessageType;
import won.protocol.model.*;
import won.protocol.repository.MessageEventRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.repository.rdfstorage.RDFStorageService;
import won.protocol.service.LinkedDataService;
import won.protocol.service.NeedInformationService;
import won.protocol.util.ConnectionModelMapper;
import won.protocol.util.DefaultPrefixUtils;
import won.protocol.util.NeedModelMapper;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.LDP;
import won.protocol.vocabulary.WON;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

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
  private RDFStorageService rdfStorage;
  @Autowired
  private MessageEventRepository messageEventRepository;
  @Autowired
  private NeedRepository needRepository;

  //TODO: used to access/create event URIs for connection model rendering. Could be removed if events knew their URIs.
  private URIService uriService;
  @Autowired
  private NeedModelMapper needModelMapper;
  @Autowired
  private ConnectionModelMapper connectionModelMapper;

  @Autowired
  private CryptographyService cryptographyService;

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


  public NeedInformationService.PagedResource<Dataset,URI> listNeedURIs(final int pageNum)
  {
    return listNeedURIs(pageNum, null, null);
  }

  public NeedInformationService.PagedResource<Dataset,URI> listNeedURIsBefore(final URI need)
  {
    return listNeedURIsBefore(need, null, null);
  }

  public NeedInformationService.PagedResource<Dataset,URI> listNeedURIsAfter(final URI need)
  {
    return listNeedURIsAfter(need, null, null);
  }

  public NeedInformationService.PagedResource<Dataset,URI> listNeedURIs(final int pageNum, final Integer preferedSize,
                                                                    NeedState needState) {
    Slice<URI> slice = needInformationService.listNeedURIs(pageNum, preferedSize, needState);
    return toContainerPage(this.needResourceURIPrefix + "/", slice);

  }
  public NeedInformationService.PagedResource<Dataset,URI> listNeedURIsBefore(
    final URI need, final Integer preferedSize, NeedState needState) {

    Slice<URI> slice = needInformationService.listNeedURIsBefore(need, preferedSize, needState);
    return toContainerPage(this.needResourceURIPrefix + "/", slice);

  }

  public NeedInformationService.PagedResource<Dataset, URI> listNeedURIsAfter(
    final URI need, final Integer preferedSize, NeedState needState) {

    Slice<URI> slice = needInformationService.listNeedURIsAfter(need, preferedSize, needState);
    return toContainerPage(this.needResourceURIPrefix + "/", slice);

  }

  public Dataset getNeedDataset(final URI needUri) throws NoSuchNeedException {
  Need need = needInformationService.readNeed(needUri);

  // load the dataset from storage
  Dataset dataset = rdfStorage.loadDataset(need.getNeedURI());
  Model metaModel = needModelMapper.toModel(need);

  Resource needResource = metaModel.getResource(needUri.toString());

  // add connections
  Resource connectionsContainer = metaModel.createResource(need.getNeedURI().toString() + "/connections");
  metaModel.add(metaModel.createStatement(needResource, WON.HAS_CONNECTIONS, connectionsContainer));

  // add need event container
  Resource needEventContainer = metaModel.createResource(need.getNeedURI().toString()+"#events", WON.EVENT_CONTAINER);
  metaModel.add(metaModel.createStatement(needResource, WON.HAS_EVENT_CONTAINER, needEventContainer));

  // add need event URIs
  List<MessageEventPlaceholder> messageEvents = messageEventRepository.findByParentURI(needUri);
  for (MessageEventPlaceholder messageEvent : messageEvents) {
    metaModel.add(metaModel.createStatement(needEventContainer,
                                            RDFS.member,
                                            metaModel.getResource(messageEvent.getMessageURI().toString())));
  }

  // add WON node link
  needResource.addProperty(WON.HAS_WON_NODE, metaModel.createResource(this.resourceURIPrefix));

  // add meta model to dataset
  String needMetaInformationURI = uriService.createNeedSysInfoGraphURI(needUri).toString();
  dataset.addNamedModel(needMetaInformationURI, metaModel);
  addBaseUriAndDefaultPrefixes(dataset);
  return dataset;
}

  @Override
  public Dataset getNeedDataset(final URI needUri, boolean deep, Integer deepLayerSize
                                ) throws NoSuchNeedException, NoSuchConnectionException, NoSuchMessageException {
    Dataset dataset = getNeedDataset(needUri);
    if (deep) {
      Slice<URI> slice = needInformationService.listConnectionURIs(needUri, 1, deepLayerSize, null, null);
      NeedInformationService.PagedResource<Dataset, URI> connectionsResource = toContainerPage(
        this.uriService.createConnectionsURIForNeed(needUri).toString(), slice);
      addDeepConnectionData(connectionsResource.getContent(), slice.getContent());
      RdfUtils.addDatasetToDataset(dataset, connectionsResource.getContent());
      for (URI connectionUri : slice.getContent()) {
        NeedInformationService.PagedResource<Dataset,URI> eventsResource = listConnectionEventURIs(
          connectionUri, 1, deepLayerSize, null, true);
        RdfUtils.addDatasetToDataset(dataset, eventsResource.getContent());
      }
    }
    return dataset;
  }

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

  @Override
  public Dataset getConnectionDataset(final URI connectionUri, final boolean includeEventContainer, final boolean
    includeLatestEvent) throws
    NoSuchConnectionException
  {
    Connection connection = needInformationService.readConnection(connectionUri);

    // load the model from storage
    Model model = connectionModelMapper.toModel(connection);
    Model additionalData = rdfStorage.loadModel(connection.getConnectionURI());
    setNsPrefixes(model);
    if (additionalData != null) {
      model.add(additionalData);
    }
    //model.setNsPrefix("", connection.getConnectionURI().toString());

    //create connection member
    Resource connectionResource = model.getResource(connection.getConnectionURI().toString());

    // add WON node link
    connectionResource.addProperty(WON.HAS_WON_NODE, model.createResource(this.resourceURIPrefix));
    Dataset eventDataset = null;
    if (includeEventContainer) {
      //create event container and attach it to the member
      Resource eventContainer = model.createResource(connection.getConnectionURI().toString()+"/events");
      connectionResource.addProperty(WON.HAS_EVENT_CONTAINER, eventContainer);
      eventContainer.addProperty(RDF.type, WON.EVENT_CONTAINER);
      if (includeLatestEvent) {
        //we add the latest event in the connection
        Slice<URI> latestEvents =
          messageEventRepository.getMessageURIsByParentURI(connectionUri, new PageRequest(0, 1, new Sort(Sort
                                                                                                           .Direction.DESC, "creationDate")));
        if (latestEvents.hasContent()) {
          URI eventURI = latestEvents.getContent().get(0);
          //add the event's dataset
          eventDataset = getDatasetForUri(eventURI);
          //connect the event to its container
          eventContainer.addProperty(RDFS.member, model.getResource(eventURI.toString()));
        }
      }
      addAdditionalData(model, connection.getConnectionURI(), connectionResource);
    }

    Dataset connectionDataset = addBaseUriAndDefaultPrefixes(newDatasetWithNamedModel(createDataGraphUriFromResource
                                                                       (connectionResource), model));
    RdfUtils.addDatasetToDataset(connectionDataset, eventDataset);
    return connectionDataset;
  }


  @Override
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
  }

  private Property getRdfPropertyForState(ConnectionState state) {
    switch (state){
      case SUGGESTED: return WON.HAS_SUGGESTED_COUNT;
      case REQUEST_RECEIVED: return WON.HAS_REQUEST_RECEIVED_COUNT;
      case REQUEST_SENT: return WON.HAS_REQUEST_SENT_COUNT;
      case CONNECTED: return WON.HAS_CONNECTED_COUNT;
      case CLOSED: return WON.HAS_CLOSED_COUNT;
    }
    return null;
  }


  @Override
  public Dataset listConnectionEventURIs(final URI connectionUri) throws
    NoSuchConnectionException
  {
    return  listConnectionEventURIs(connectionUri, false);
  }

  @Override
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
        Dataset eventDataset = getDatasetForUri(event.getMessageURI());
        RdfUtils.addDatasetToDataset(eventsContainerDataset, eventDataset);
      }
    }

    return eventsContainerDataset;
  }

  @Override
  public NeedInformationService.PagedResource<Dataset,URI> listConnectionEventURIs(final URI connectionUri, final int
    pageNum, Integer preferedSize, WonMessageType msgType, boolean deep) throws
    NoSuchConnectionException
  {
    Slice<URI> slice = needInformationService.listConnectionEventURIs(connectionUri, pageNum,
                                                                      preferedSize, msgType);
    NeedInformationService.PagedResource<Dataset,URI> containerPage = toContainerPage(
      this.uriService.createEventsURIForConnection(connectionUri).toString(), slice);
    if (deep) addEventData(slice, containerPage);
    return containerPage;
  }



  @Override
  public NeedInformationService.PagedResource<Dataset,URI> listConnectionEventURIsAfter(
    final URI connectionUri, final URI msgURI, Integer preferedSize, WonMessageType msgType, boolean deep) throws
    NoSuchConnectionException
  {
    Slice<URI> slice = needInformationService.listConnectionEventURIsAfter(
      connectionUri, msgURI, preferedSize, msgType);
    NeedInformationService.PagedResource<Dataset,URI> containerPage = toContainerPage(this.uriService.createEventsURIForConnection(connectionUri).toString(), slice);
    if (deep) addEventData(slice, containerPage);
    return containerPage;
  }

  @Override
  public NeedInformationService.PagedResource<Dataset,URI> listConnectionEventURIsBefore(
    final URI connectionUri, final URI msgURI, Integer preferedSize, WonMessageType msgType, boolean deep) throws
    NoSuchConnectionException
  {

    Slice<URI> slice = needInformationService.listConnectionEventURIsBefore(
      connectionUri, msgURI, preferedSize, msgType);
    NeedInformationService.PagedResource<Dataset,URI> containerPage = toContainerPage(this.uriService.createEventsURIForConnection(connectionUri).toString(), slice);
    if (deep) addEventData(slice, containerPage);
    return containerPage;
  }

  @Override
  public Dataset getDatasetForUri(URI datasetUri) {
    Dataset result = rdfStorage.loadDataset(datasetUri);
    if (result == null) return null;
    DefaultPrefixUtils.setDefaultPrefixes(result.getDefaultModel());
    addBaseUriAndDefaultPrefixes(result);
    return result;
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
    Dataset dataset = DatasetFactory.createMem();
    dataset.addNamedModel(graphUri, model);
    return dataset;
  }

  private void addAdditionalData(final Model model, URI resourceToLoad, final Resource targetResource) {
    Model additionalDataModel = rdfStorage.loadModel(resourceToLoad);
    if (additionalDataModel != null && additionalDataModel.size() > 0) {
      Resource additionalData = additionalDataModel.createResource();
      //TODO: check if the statement below is now necessary
      //RdfUtils.replaceBaseResource(additionalDataModel, additionalData);
      model.add(model.createStatement(targetResource, WON.HAS_ADDITIONAL_DATA, additionalData));
      model.add(additionalDataModel);
    }
  }

  public void addEventData(final Slice<URI> slice, final NeedInformationService.PagedResource<Dataset, URI> containerPage) {
    for (URI eventUri : slice.getContent()) {
      Dataset eventDataset = getDatasetForUri(eventUri);
      RdfUtils.addDatasetToDataset(containerPage.getContent(), eventDataset);
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

  public void addDeepConnectionData(Dataset dataset, List<URI> connectionURIs) throws NoSuchConnectionException {
    //add the connection model for each connection URI
    for (URI connectionURI : connectionURIs) {
      Dataset connectionDataset =
        getConnectionDataset(connectionURI, true, true);
      RdfUtils.addDatasetToDataset(dataset, connectionDataset);
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

    public void setRdfStorage(RDFStorageService rdfStorage)
  {
    this.rdfStorage = rdfStorage;
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
}
