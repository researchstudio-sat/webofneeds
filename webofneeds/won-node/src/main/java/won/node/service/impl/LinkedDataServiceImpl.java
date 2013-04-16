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

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.shared.impl.PrefixMappingImpl;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.model.Connection;
import won.protocol.model.Match;
import won.protocol.model.Need;
import won.protocol.model.WON;
import won.protocol.service.LinkedDataService;
import won.protocol.service.NeedInformationService;
import won.protocol.util.LDP;

import java.net.URI;
import java.util.Collection;

/**
 * User: fkleedorfer
 * Date: 26.11.12
 */
public class LinkedDataServiceImpl implements LinkedDataService {
  final Logger logger = LoggerFactory.getLogger(getClass());
  public static final PrefixMapping PREFIX_MAPPING = new PrefixMappingImpl();

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

  private RDFStorageService rdfStorage;


  private String needProtocolEndpoint;
  private String matcherProtocolEndpoint;
  private String ownerProtocolEndpoint;

  private NeedInformationService needInformationService;

  static {
    PREFIX_MAPPING.setNsPrefix("won",WON.getURI());
    PREFIX_MAPPING.setNsPrefix("rdf",RDF.getURI());
    PREFIX_MAPPING.setNsPrefix("ldp",LDP.getURI());
    PREFIX_MAPPING.setNsPrefix("rdfs",RDFS.getURI());
  }

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
    if (page >=0) {
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
    if (page >=0){
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
    Model model = ModelFactory.createDefaultModel();
    setNsPrefixes(model);
    Resource connectionsContainer =  model.createResource(needUri + "/connections/");
    Resource mainNeedNode = model.createResource(needUri.toString())
        .addProperty(WON.STATE, need.getState().name())
        .addProperty(WON.HAS_CONNECTIONS,connectionsContainer)
        .addProperty(WON.NEED_PROTOCOL_ENDPOINT, model.createResource(this.needProtocolEndpoint))
        .addProperty(WON.OWNER_PROTOCOL_ENDPOINT, model.createResource(this.ownerProtocolEndpoint))
        .addProperty(WON.MATCHER_PROTOCOL_ENDPOINT, model.createResource(this.matcherProtocolEndpoint))
    ;
    Model contentModel =  rdfStorage.loadContent(need);
    model.add(contentModel);
    model.add(model.createStatement(connectionsContainer,RDF.type, LDP.CONTAINER));
    //identify the outer blank node in the content model and link it to the needURI node
    ResIterator it = contentModel.listSubjectsWithProperty(RDF.type, WON.NEED_DESCRIPTION);
    if (it.hasNext()){
        Resource mainContentNode = it.next();
        model.add(model.createStatement(mainNeedNode, WON.HAS_CONTENT, mainContentNode));
    }
    return model;
  }

  public Model getConnectionModel(final URI connectionUri) throws NoSuchConnectionException
  {
    Connection connection = needInformationService.readConnection(connectionUri);
    Model model = ModelFactory.createDefaultModel();
    setNsPrefixes(model);
    Resource r = model.createResource(connectionUri.toString());
    r.addProperty(WON.STATE, connection.getState().name());
    if(connection.getRemoteConnectionURI() != null)
        r.addProperty(WON.REMOTE_CONNECTION, model.createResource(connection.getRemoteConnectionURI().toString()));
    r.addProperty(WON.REMOTE_NEED, model.createResource(connection.getRemoteNeedURI().toString()));
    r.addProperty(WON.BELONGS_TO_NEED, model.createResource(connection.getNeedURI().toString()));
    r.addProperty(WON.NEED_PROTOCOL_ENDPOINT, model.createResource(this.needProtocolEndpoint));
    r.addProperty(WON.OWNER_PROTOCOL_ENDPOINT, model.createResource(this.ownerProtocolEndpoint));

    return model;
  }

  public Model listConnectionURIs(final int page, final URI needURI) throws NoSuchNeedException
  {
    Collection<URI> uris = null;
    if (page >= 0) {
      uris = needInformationService.listConnectionURIs(needURI, page);
    } else {
      uris = needInformationService.listConnectionURIs(needURI);
    }
    Model model = ModelFactory.createDefaultModel();
    setNsPrefixes(model);
    Resource connections = null;
    if (page >=0){
      connections = createPage(model,needURI.toString() + "/connections/",page,uris.size());
    } else {
      connections = model.createResource(needURI.toString() + "/connections/");
    }
    for (URI connURI : uris) {
      model.add(model.createStatement(connections, RDFS.member, model.createResource(connURI.toString())));
    }
    return model;
  }

    private String addPageQueryString(String uri, int page) {
    //TODO: simple implementation for adding page number to uri - breaks as soon as other query strings are present!
    return uri + "?page="+page;
  }

  private Resource createPage(final Model model, final String containerURI, final int page, final int numberOfMembers)
  {
    String containerPageURI = addPageQueryString(containerURI,page);
    Resource containerPageResource = model.createResource(containerPageURI);
    Resource containerResource = model.createResource(containerURI);
    model.add(model.createStatement(containerPageResource, RDF.type, LDP.PAGE));
    model.add(model.createStatement(containerPageResource,LDP.PAGE_OF, containerResource));
    model.add(model.createStatement(containerPageResource,RDF.type, LDP.CONTAINER));
    Resource containerNextPageResource = null;
    //assume last page if we didn't fetch pageSize uris
    if (numberOfMembers < pageSize) {
      containerNextPageResource = RDF.nil;
    } else {
      containerNextPageResource = model.createResource(addPageQueryString(containerURI, page + 1));
    }
    model.add(model.createStatement(containerPageResource,LDP.NEXT_PAGE, containerNextPageResource));
    return containerPageResource;
  }

  private void setNsPrefixes(final Model model)
  {
    model.setNsPrefixes(PREFIX_MAPPING);
    model.setNsPrefix("won-res", this.resourceURIPrefix);
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

    public void setRdfStorage(RDFStorageService rdfStorage) {
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
}
