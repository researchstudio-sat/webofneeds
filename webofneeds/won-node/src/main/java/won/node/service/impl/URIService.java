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

import org.springframework.beans.factory.annotation.Autowired;
import won.cryptography.service.RandomNumberService;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEvent;
import won.protocol.model.Need;
import won.protocol.util.WonRdfUtils;

import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 06.11.12
 */
public class URIService
{

  @Autowired
  private RandomNumberService randomNumberService;

  //prefix of any URI
  private String generalURIPrefix;
  //prefix of a need resource
  private String needResourceURIPrefix;
  //prefix of a connection resource
  private String connectionResourceURIPrefix;
  //prefix of a messageEvent resource
  private String messageEventResourceURIInfix;
  //need meta information suffix
  private String needMetaInformationURISuffix;
  //prefix for URISs of RDF data
  private String dataURIPrefix;
  //prefix for URIs referring to real-world things
  private String resourceURIPrefix;
  //prefix for human readable pages
  private String pageURIPrefix;


  /**
   * Transforms the specified URI, which may be a resource URI or a page URI, to a data URI.
   * If the specified URI doesn't start with the right prefix, it's returned unchanged.
   * @param pageOrResourceURI
   * @return
   */
  public URI toDataURIIfPossible(URI pageOrResourceURI){
    String fromURI = resolveAgainstGeneralURIPrefix(pageOrResourceURI);
    if (fromURI.startsWith(this.pageURIPrefix)){
      return URI.create(fromURI.replaceFirst(this.pageURIPrefix, this.dataURIPrefix));
    }
    if (fromURI.startsWith(this.resourceURIPrefix)){
      return URI.create(fromURI.replaceFirst(this.resourceURIPrefix, this.dataURIPrefix));
    }
    return pageOrResourceURI;
  }

  /**
   * Transforms the specified URI, which may be a resource URI or a page URI, to a page URI.
   * If the specified URI doesn't start with the right prefix, it's returned unchanged.
   * @param dataOrResourceURI
   * @return
   */
  public URI toPageURIIfPossible(URI dataOrResourceURI){
    String fromURI = resolveAgainstGeneralURIPrefix(dataOrResourceURI);
    if (fromURI.startsWith(this.dataURIPrefix)) {
      return URI.create(fromURI.replaceFirst(this.dataURIPrefix, this.pageURIPrefix));
    }
    if (fromURI.startsWith(this.resourceURIPrefix)){
      return URI.create(fromURI.replaceFirst(this.resourceURIPrefix, this.pageURIPrefix));
    }
    return dataOrResourceURI;
  }



  /**
   * Transforms the specified URI, which may be a resource URI or a page URI, to a page URI.
   * If the specified URI doesn't start with the right prefix, it's returned unchanged.
   * @param pageOrDataURI
   * @return
   */
  public URI toResourceURIIfPossible(URI pageOrDataURI){
    String fromURI = resolveAgainstGeneralURIPrefix(pageOrDataURI);
    if (fromURI.startsWith(this.dataURIPrefix)) {
      return URI.create(fromURI.replaceFirst(this.dataURIPrefix, this.resourceURIPrefix));
    }
    if (fromURI.startsWith(this.pageURIPrefix)){
      return URI.create(fromURI.replaceFirst(this.pageURIPrefix, this.resourceURIPrefix));
    }
    return pageOrDataURI;
  }

  private String resolveAgainstGeneralURIPrefix(final URI uri)
  {
    if (uri.isAbsolute()) return uri.toString();
    return URI.create(generalURIPrefix).resolve(uri).toString();
  }

  public URI createNeedURIForId(String id) {
    return URI.create(needResourceURIPrefix.toString() + "/" + id);
  }

  public URI createConnectionURIForId(String id) {
    return URI.create(connectionResourceURIPrefix.toString() + "/"   + id);
  }

  public URI createNeedURI(Need need)
  {
    return URI.create(needResourceURIPrefix.toString() + "/" + need.getId());
  }

  public URI createConnectionURI(Connection con)
  {
    return URI.create(connectionResourceURIPrefix.toString() + "/" + con.getId());
  }

  public void setNeedResourceURIPrefix(final String needResourceURIPrefix)
  {
    this.needResourceURIPrefix = needResourceURIPrefix;
  }

  public void setConnectionResourceURIPrefix(final String connectionResourceURIPrefix)
  {
    this.connectionResourceURIPrefix = connectionResourceURIPrefix;
  }

  public void setMessageEventResourceURIInfix(final String messageEventResourceURIInfix) {
    this.messageEventResourceURIInfix = messageEventResourceURIInfix;
  }

  public void setNeedMetaInformationURISuffix(final String needMetaInformationURISuffix) {
    this.needMetaInformationURISuffix = needMetaInformationURISuffix;
  }

  public void setDataURIPrefix(final String dataURIPrefix)
  {
    this.dataURIPrefix = dataURIPrefix;
  }

  public void setResourceURIPrefix(final String resourceURIPrefix)
  {
    this.resourceURIPrefix = resourceURIPrefix;
  }

  public void setPageURIPrefix(final String pageURIPrefix)
  {
    this.pageURIPrefix = pageURIPrefix;
  }

  public void setGeneralURIPrefix(final String generalURIPrefix)
  {
    this.generalURIPrefix = generalURIPrefix;
  }

  public URI createEventURI(final Connection con, final ConnectionEvent event)
  {
    return URI.create(con.getConnectionURI()+"/event/"+event.getId());
  }

  public URI createEventURI(final URI connectionURI, final String eventId)
  {
    return URI.create(connectionURI.toString()+"/event/"+eventId);
  }

  public URI createMessageEventURI(final URI parentURI) {
    // ToDo (FS): take length from configuration and choose good length value (maybe change value to bytes)
    return URI.create(parentURI.toString() + messageEventResourceURIInfix + "/" + randomNumberService
      .generateRandomString(9));
  }

  public URI createNeedMetaInformationURI(final URI needURI) {
    return URI.create(needURI.toString() + needMetaInformationURISuffix + WonRdfUtils.NAMED_GRAPH_SUFFIX);
  }

  /**
   * Assumes the specified uri to be of the form [connectionURI]/event/[long event id].
   * @param eventURI
   * @return
   */
  public Long getEventIdFromEventURI(final URI eventURI)
  {
    String path = eventURI.getPath();
    return new Long(path.substring(path.lastIndexOf("/")+1,path.length()));
  }



  public String getGeneralURIPrefix(){
      return this.generalURIPrefix;
  }
}
