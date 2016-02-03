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

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.cryptography.rdfsign.WonKeysReaderWriter;
import won.cryptography.service.CryptographyService;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.model.Connection;
import won.protocol.model.MessageEventPlaceholder;
import won.protocol.model.Need;
import won.protocol.repository.MessageEventRepository;
import won.protocol.repository.rdfstorage.RDFStorageService;
import won.protocol.service.LinkedDataService;
import won.protocol.service.NeedInformationService;
import won.protocol.util.ConnectionModelMapper;
import won.protocol.util.DefaultPrefixUtils;
import won.protocol.util.NeedModelMapper;
import won.protocol.vocabulary.LDP;
import won.protocol.vocabulary.WON;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;

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

  @Deprecated
  public Dataset listNeedURIsOld(final int pageNum)
  {
    Model model = ModelFactory.createDefaultModel();
    setNsPrefixes(model);
    Resource needListPageResource = null;
    Collection<URI> uris = null;
    if (pageNum >= 0) {
      NeedInformationService.Page page = needInformationService.listNeedURIs(pageNum);
      needListPageResource = createPage(model, this.needResourceURIPrefix+"/", pageNum, page);
      uris = page.getContent();
    } else {
      uris = needInformationService.listNeedURIs();
      needListPageResource = model.createResource(this.needResourceURIPrefix+"/");
    }

    for (URI needURI : uris) {
      model.add(model.createStatement(needListPageResource, RDFS.member, model.createResource(needURI.toString())));
    }
    Dataset ret = newDatasetWithNamedModel(createDataGraphUri(needListPageResource), model);
    addBaseUriAndDefaultPrefixes(ret);
    return ret;
  }

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
    Dataset ret = newDatasetWithNamedModel(createDataGraphUri(needListPageResource), model);
    addBaseUriAndDefaultPrefixes(ret);
    return ret;
  }


  public NeedInformationService.PagedResource<Dataset> listNeedURIs(final int pageNum)
  {
    Model model = ModelFactory.createDefaultModel();
    setNsPrefixes(model);
    Resource needListPageResource = null;
    Collection<URI> uris = null;
    int infoServicePageNum = pageNum - 1;
    NeedInformationService.Page page = needInformationService.listNeedURIs(infoServicePageNum);
    needListPageResource = model.createResource(this.needResourceURIPrefix + "/");
    uris = page.getContent();
    for (URI needURI : uris) {
      model.add(model.createStatement(needListPageResource, RDFS.member, model.createResource(needURI.toString())));
    }
    Dataset dataset = newDatasetWithNamedModel(createDataGraphUri(needListPageResource), model);
    addBaseUriAndDefaultPrefixes(dataset);
    NeedInformationService.PagedResource<Dataset> containerPage = new NeedInformationService.PagedResource(dataset, page.hasNext());
    return containerPage;
  }

  private String createDataGraphUri(Resource needListPageResource) {
    URI uri = URI.create(needListPageResource.getURI());
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

  public Dataset listConnectionURIs(final int pageNum)
  {
    Collection<URI> uris = null;
    Model model = ModelFactory.createDefaultModel();
    setNsPrefixes(model);
    Resource connections = null;
    if (pageNum >= 0) {
      NeedInformationService.Page page = needInformationService.listConnectionURIs(pageNum);
      connections = createPage(model, this.connectionResourceURIPrefix+"/", pageNum, page);
      uris = page.getContent();
    } else {
      connections = model.createResource(this.connectionResourceURIPrefix+"/",LDP.CONTAINER);
      uris = needInformationService.listConnectionURIs();
    }
    for (URI connectionURI : uris) {
      model.add(model.createStatement(connections, RDFS.member, model.createResource(connectionURI.toString())));

    }
    return addBaseUriAndDefaultPrefixes(newDatasetWithNamedModel(createDataGraphUri(connections), model));
  }

  public Dataset getNeedDataset(final URI needUri) throws NoSuchNeedException {
    Need need = needInformationService.readNeed(needUri);

    // load the dataset from storage
    Dataset dataset = rdfStorage.loadDataset(need.getNeedURI());
    Model metaModel = needModelMapper.toModel(need);

    Resource needResource = metaModel.getResource(needUri.toString());

    // add connections
    Resource connectionsContainer = metaModel.createResource(need.getNeedURI().toString() + "/connections/");
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

    public Dataset getNodeDataset()
    {
      Model model = ModelFactory.createDefaultModel();
      setNsPrefixes(model);
      Resource showNodePageResource = null;
      showNodePageResource = model.createResource(this.resourceURIPrefix);
      addNeedList(model, showNodePageResource);
      addProtocolEndpoints(model, showNodePageResource);
      Dataset ret = newDatasetWithNamedModel(createDataGraphUri(showNodePageResource), model);
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

  public Dataset getConnectionDataset(final URI connectionUri, boolean includeEventData) throws NoSuchConnectionException
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

    if (includeEventData) {
      //create event container and attach it to the member
      Resource eventContainer = model.createResource(connection.getConnectionURI().toString()+"/events", WON.EVENT_CONTAINER);
      connectionResource.addProperty(WON.HAS_EVENT_CONTAINER, eventContainer);
      connectionResource.addProperty(WON.HAS_REMOTE_NEED, model.createResource(connection.getRemoteNeedURI().toString()));
      addAdditionalData(model, connection.getConnectionURI(), connectionResource);

      // add the events with the new format (only the URI, no content)
      List<MessageEventPlaceholder> connectionEvents = messageEventRepository.findByParentURI(connectionUri);
      for (MessageEventPlaceholder event : connectionEvents) {
        model.add(model.createStatement(eventContainer,
                                        RDFS.member,
                                        model.getResource(event.getMessageURI().toString())));
      }
    }

    return addBaseUriAndDefaultPrefixes(newDatasetWithNamedModel(createDataGraphUri(connectionResource), model));
  }



  public Dataset getDatasetForUri(URI datasetUri) {
    Dataset result = rdfStorage.loadDataset(datasetUri);
    if (result == null) return null;
    DefaultPrefixUtils.setDefaultPrefixes(result.getDefaultModel());
    addBaseUriAndDefaultPrefixes(result);
    return result;
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

  public Dataset listConnectionURIs(final int pageNum, final URI needURI) throws NoSuchNeedException
  {
    Collection<URI> uris = null;
    Model model = ModelFactory.createDefaultModel();
    setNsPrefixes(model);
    //model.setNsPrefix("", needURI.toString());

    Resource connections = null;
    if (pageNum >= 0) {
      NeedInformationService.Page<URI> page = needInformationService.listConnectionURIs(needURI, pageNum);
      connections = createPage(model, needURI.toString() + "/connections/", pageNum, page);
      uris = page.getContent();
    } else {
      connections = model.createResource(needURI.toString() + "/connections/");
      uris = needInformationService.listConnectionURIs(needURI);
    }
    for (URI connURI : uris)
      model.add(model.createStatement(connections, RDFS.member, model.createResource(connURI.toString())));
    return addBaseUriAndDefaultPrefixes(newDatasetWithNamedModel(createDataGraphUri(connections), model));
  }

  private String addPageQueryString(String uri, int page)
  {
    //TODO: simple implementation for adding page number to uri - breaks as soon as other query strings are present!
    return uri + "?page=" + page;
  }

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
