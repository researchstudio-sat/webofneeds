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

import com.hp.hpl.jena.rdf.model.*;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 28.11.12
 */
public class LinkedDataRestClient
{


  /**
   * Retrieves RDF for the specified resource URI.
   * Expects that the resource URI will lead to a 303 response, redirecting to the URI where RDF can be downloaded.
   * Paging is not supported.
   *
   * @param resourceURI
   * @return
   */
  public Model readResourceData(URI resourceURI){
    ClientConfig cc = new DefaultClientConfig();
    cc.getProperties().put(
        ClientConfig.PROPERTY_FOLLOW_REDIRECTS, true);
    cc.getClasses().add(ModelReaderWriter.class);
    Client c = Client.create(cc);
    WebResource r = c.resource(resourceURI);
    //TODO: improve error handling
    //If a ClientHandlerException is thrown here complaining that it can't read a Model with MIME media type text/html,
    //it was probably the wrong resourceURI
    return r.accept(RDFMediaType.APPLICATION_RDF_XML).get(Model.class);
  }

  public URI getURIPropertyForResource(final URI resourceURI, Property property)
  {
    Model rdfModel = readResourceData(resourceURI);
    StmtIterator stmts = rdfModel.listStatements(
        new SimpleSelector(rdfModel.createResource(resourceURI.toString()), property, (RDFNode) null));
    //assume only one endpoint
    if (!stmts.hasNext()) return null;
    Statement stmt = stmts.next();
    return URI.create(stmt.getObject().toString());
  }

}
