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

package won.protocol.rest;

import com.hp.hpl.jena.query.Dataset;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.text.MessageFormat;

/**
 * User: fkleedorfer
 * Date: 28.11.12
 */
public class LinkedDataRestClient
{
  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Retrieves RDF for the specified resource URI.
   * Expects that the resource URI will lead to a 303 response, redirecting to the URI where RDF can be downloaded.
   * Paging is not supported.
   *
   * @param resourceURI
   * @return
   */
  public Dataset readResourceData(URI resourceURI){
    assert resourceURI != null : "resource URI must not be null";
    logger.debug("fetching linked data resource: {}", resourceURI);
    ClientConfig cc = new DefaultClientConfig();
    cc.getProperties().put(
        ClientConfig.PROPERTY_FOLLOW_REDIRECTS, true);
    cc.getClasses().add(DatasetReaderWriter.class);
    Client c = Client.create(cc);
    WebResource r = c.resource(resourceURI);
    //TODO: improve error handling
    //If a ClientHandlerException is thrown here complaining that it can't read a Model with MIME media type text/html,
    //it was probably the wrong resourceURI
    Dataset result;
    try {
       result = r.accept(RDFMediaType.APPLICATION_TRIG).get(Dataset.class);
    } catch (ClientHandlerException e) {
      throw new IllegalArgumentException(
        MessageFormat.format(
        "caught a clientHandler exception, " +
        "which may indicate that the URI that was accessed isn''t a" +
        " linked data URI, please check {0}", resourceURI), e);
    }
    if (logger.isDebugEnabled()) {
      logger.debug("fetched model with {} statements in default model for resource {}",result.getDefaultModel().size(),
        resourceURI);
    }
    return result;
  }



}
