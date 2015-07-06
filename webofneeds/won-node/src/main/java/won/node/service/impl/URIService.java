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

import won.protocol.model.Connection;
import won.protocol.model.Need;

import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 06.11.12
 */
public class URIService
{

  //prefix of any URI
  private String generalURIPrefix;
  //prefix of a need resource
  private String needResourceURIPrefix;
  //prefix of a connection resource
  private String connectionResourceURIPrefix;
  //prefix of an event resource
  private String eventResourceURIPrefix;
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

  public URI createEventURIForId(String id) {
    return URI.create(eventResourceURIPrefix.toString() + "/"   + id);
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

  public void setEventResourceURIPrefix(final String eventResourceURIPrefix)
  {
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

  public void setPageURIPrefix(final String pageURIPrefix)
  {
    this.pageURIPrefix = pageURIPrefix;
  }

  public void setGeneralURIPrefix(final String generalURIPrefix)
  {
    this.generalURIPrefix = generalURIPrefix;
  }

  public URI createNeedSysInfoGraphURI(final URI needURI) {
    //TODO: [SECURITY] it's possible to submit need data that clashes with this name,
    // which may lead to undefined behavior
    return URI.create(needURI.toString() + "#sysinfo");
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
