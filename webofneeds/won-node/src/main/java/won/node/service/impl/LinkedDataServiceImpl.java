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
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.node.rdfstorage.RDFStorageService;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEvent;
import won.protocol.model.Need;
import won.protocol.service.LinkedDataService;
import won.protocol.service.NeedInformationService;
import won.protocol.util.*;
import won.protocol.vocabulary.LDP;
import won.protocol.vocabulary.WON;

import java.net.URI;
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
  //prefix for URISs of RDF data
  private String dataURIPrefix;
  //prefix for URIs referring to real-world things
  private String resourceURIPrefix;
  //prefix for human readable pages
  private String pageURIPrefix;

  private int pageSize = 0;

  @Autowired
  private RDFStorageService rdfStorage;

  //TODO: used to access/create event URIs for connection model rendering. Could be removed if events knew their URIs.
  private URIService uriService;
  @Autowired
  private NeedModelMapper needModelMapper;
  @Autowired
  private ConnectionModelMapper connectionModelMapper;

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

    public Model listNeedURIs(final int page)
  {
    Collection<URI> uris = null;
    if (page >= 0) {
      uris = needInformationService.listNeedURIs(page);
    } else {
      uris = needInformationService.listNeedURIs();
    }
    Model model = ModelFactory.createDefaultModel();
    setNsPrefixes(model);
    Resource needListPageResource = null;
    if (page >= 0) {
      needListPageResource = createPage(model, this.needResourceURIPrefix, page, uris.size());
    } else {
      needListPageResource = model.createResource(this.needResourceURIPrefix);
    }
    for (URI needURI : uris) {
      model.add(model.createStatement(needListPageResource, RDFS.member, model.createResource(needURI.toString())));
    }
    return model;
  }

  public Model listConnectionURIs(final int page)
  {
    Collection<URI> uris = null;
    if (page >= 0) {
      uris = needInformationService.listConnectionURIs(page);
    } else {
      uris = needInformationService.listConnectionURIs();
    }
    Model model = ModelFactory.createDefaultModel();
    setNsPrefixes(model);
    Resource connections = null;
    if (page >= 0) {
      connections = createPage(model, this.connectionResourceURIPrefix, page, uris.size());
    } else {
      connections = model.createResource(this.connectionResourceURIPrefix);
    }
    for (URI connectionURI : uris) {
      model.add(model.createStatement(connections, RDFS.member, model.createResource(connectionURI.toString())));
    }
    return model;
  }

  public Model getNeedModel(final URI needUri) throws NoSuchNeedException
  {
    Need need = needInformationService.readNeed(needUri);

    // load the model from storage
    Model model = rdfStorage.loadContent(need.getNeedURI());
    setNsPrefixes(model);

    Model needModel = needModelMapper.toModel(need);
    //merge needModel and model
    model.add(needModel.listStatements());
    model.setNsPrefix("",need.getNeedURI().toString());

    Resource needResource = model.getResource(needUri.toString());

    // add connections
    Resource connectionsContainer = model.createResource(need.getNeedURI().toString() + "/connections/", LDP.CONTAINER);
    model.add(model.createStatement(needResource, WON.HAS_CONNECTIONS, connectionsContainer));
    // add WON node link
    needResource.addProperty(WON.HAS_WON_NODE, model.createResource(this.resourceURIPrefix));

    return model;
  }
    public Model getNodeModel()
    {
      Model model = ModelFactory.createDefaultModel();
      setNsPrefixes(model);
      Resource showNodePageResource = null;
      showNodePageResource = model.createResource(this.resourceURIPrefix);
      addProtocolEndpoints(model, showNodePageResource);
      return model;
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
      Resource blankNodeSoapWs = model.createResource();
      res.addProperty(WON.SUPPORTS_WON_PROTOCOL_IMPL, blankNodeSoapWs);
      blankNodeSoapWs
              .addProperty(RDF.type, WON.WON_OVER_SOAP_WS)
              .addProperty(WON.HAS_MATCHER_PROTOCOL_ENDPOINT,model.createResource(this.matcherProtocolEndpoint))
              .addProperty(WON.HAS_NEED_PROTOCOL_ENDPOINT,model.createResource(this.needProtocolEndpoint))
              .addProperty(WON.HAS_OWNER_PROTOCOL_ENDPOINT,model.createResource(this.ownerProtocolEndpoint));

  }

  public Model getConnectionModel(final URI connectionUri, boolean includeEventData) throws NoSuchConnectionException
  {
    Connection connection = needInformationService.readConnection(connectionUri);

    // load the model from storage
    Model model = rdfStorage.loadContent(connection.getConnectionURI());
    setNsPrefixes(model);
    Model mappedModel = connectionModelMapper.toModel(connection);
    model.add(mappedModel);
    model.setNsPrefix("",connection.getConnectionURI().toString());

    //create connection member
    Resource connectionResource = model.getResource(connection.getConnectionURI().toString());

    // add WON node link
    connectionResource.addProperty(WON.HAS_WON_NODE, model.createResource(this.resourceURIPrefix));

    if (includeEventData) {
      //create event container and attach it to the member
      List<ConnectionEvent> events = needInformationService.readEvents(connectionUri);
      Resource eventContainer = model.createResource(WON.EVENT_CONTAINER);
      connectionResource.addProperty(WON.HAS_EVENT_CONTAINER, eventContainer);
      connectionResource.addProperty(WON.HAS_REMOTE_NEED, model.createResource(connection.getRemoteNeedURI().toString()));
      addAdditionalData(model, connection.getConnectionURI(), connectionResource);
  
      //add event members and attach them
      for (ConnectionEvent e : events) {
        Resource eventMember = model.createResource(this.uriService.createEventURI(connection,e).toString(),WON.toResource(e.getType()));
        if (e.getOriginatorUri() != null)
          eventMember.addProperty(WON.HAS_ORIGINATOR, model.createResource(e.getOriginatorUri().toString()));
  
        if (e.getCreationDate() != null)
          eventMember.addProperty(WON.HAS_TIME_STAMP, DateTimeUtils.toLiteral(e.getCreationDate(), model));
  
        addAdditionalData(model, this.uriService.createEventURI(connection,e), eventMember);
        model.add(model.createStatement(eventContainer, RDFS.member, eventMember));
      }
    }

    return model;
  }

  /**
   * Returns the rdf model for the event.
   * @param eventURI
   * @return null if no event is found.
   * @throws NoSuchConnectionException
   */
  @Override
  public Model getEventModel(final URI eventURI)
  {
    ConnectionEvent event = needInformationService.readEvent(eventURI);
    if (event == null) return null;
    Model model = ModelFactory.createDefaultModel();
    setNsPrefixes(model);
    model.setNsPrefix("",eventURI.toString());

    Resource eventMember = model.createResource(eventURI.toString(),WON.toResource(event.getType()));
    if (event.getOriginatorUri() != null) {
      eventMember.addProperty(WON.HAS_ORIGINATOR, model.createResource(event.getOriginatorUri().toString()));
    }

    if (event.getCreationDate() != null) {
      eventMember.addProperty(WON.HAS_TIME_STAMP, DateTimeUtils.toLiteral(event.getCreationDate(), model));
    }

    addAdditionalData(model, eventURI, eventMember);
    return model;
  }

  private void addAdditionalData(final Model model, URI resourceToLoad, final Resource targetResource) {
    Model additionalDataModel = rdfStorage.loadContent(resourceToLoad);
    if (additionalDataModel != null && additionalDataModel.size() > 0) {
      Resource additionalData = additionalDataModel.createResource();
      //TODO: check if the statement below is now necessary
      //RdfUtils.replaceBaseResource(additionalDataModel, additionalData);
      model.add(model.createStatement(targetResource, WON.HAS_ADDITIONAL_DATA, additionalData));
      model.add(additionalDataModel);
    }
  }

  public Model listConnectionURIs(final int page, final URI needURI) throws NoSuchNeedException
  {
    Collection<URI> uris = null;
    if (page >= 0)
      uris = needInformationService.listConnectionURIs(needURI, page);
    else
      uris = needInformationService.listConnectionURIs(needURI);

    Model model = ModelFactory.createDefaultModel();
    setNsPrefixes(model);
    model.setNsPrefix("", needURI.toString());

    Resource connections = null;
    if (page >= 0)
      connections = createPage(model, needURI.toString() + "/connections/", page, uris.size());
    else
      connections = model.createResource(needURI.toString() + "/connections/");

    for (URI connURI : uris)
      model.add(model.createStatement(connections, RDFS.member, model.createResource(connURI.toString())));

    return model;
  }

  private String addPageQueryString(String uri, int page)
  {
    //TODO: simple implementation for adding page number to uri - breaks as soon as other query strings are present!
    return uri + "?page=" + page;
  }

  private Resource createPage(final Model model, final String containerURI, final int page, final int numberOfMembers)
  {
    String containerPageURI = addPageQueryString(containerURI, page);
    Resource containerPageResource = model.createResource(containerPageURI);
    Resource containerResource = model.createResource(containerURI);
    model.add(model.createStatement(containerPageResource, RDF.type, LDP.PAGE));
    model.add(model.createStatement(containerPageResource, LDP.PAGE_OF, containerResource));
    model.add(model.createStatement(containerPageResource, RDF.type, LDP.CONTAINER));
    Resource containerNextPageResource = null;
    //assume last page if we didn't fetch pageSize uris
    if (numberOfMembers < pageSize) {
      containerNextPageResource = RDF.nil;
    } else {
      containerNextPageResource = model.createResource(addPageQueryString(containerURI, page + 1));
    }
    model.add(model.createStatement(containerPageResource, LDP.NEXT_PAGE, containerNextPageResource));
    return containerPageResource;
  }

  private void setNsPrefixes(final Model model)
  {
    DefaultPrefixUtils.setDefaultPrefixes(model);
  }

  public void setNeedResourceURIPrefix(final String needResourceURIPrefix)
  {
    this.needResourceURIPrefix = needResourceURIPrefix;
  }

  public void setConnectionResourceURIPrefix(final String connectionResourceURIPrefix)
  {
    this.connectionResourceURIPrefix = connectionResourceURIPrefix;
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

  public int getPageSize()
  {
    return pageSize;
  }

  public void setPageSize(final int pageSize)
  {
    this.pageSize = pageSize;
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
