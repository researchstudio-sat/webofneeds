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

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.sparql.path.Path;
import com.hp.hpl.jena.sparql.path.eval.PathEval;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.text.MessageFormat;
import java.util.Iterator;

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
  public Model readResourceData(URI resourceURI){
    assert resourceURI != null : "resource URI must not be null";
    logger.debug("fetching linked data resource: {}", resourceURI);
    ClientConfig cc = new DefaultClientConfig();
    cc.getProperties().put(
        ClientConfig.PROPERTY_FOLLOW_REDIRECTS, true);
    cc.getClasses().add(ModelReaderWriter.class);
    Client c = Client.create(cc);
    WebResource r = c.resource(resourceURI);
    //TODO: improve error handling
    //If a ClientHandlerException is thrown here complaining that it can't read a Model with MIME media type text/html,
    //it was probably the wrong resourceURI
    Model result;
    try {
       result = r.accept(RDFMediaType.APPLICATION_RDF_XML).get(Model.class);
    } catch (ClientHandlerException e) {
      throw new IllegalArgumentException(MessageFormat.format("caught a clientHandler exception, " +
        "which may indicate that the URI that was accessed isn't a" +
        " linked data URI, please check {0}", resourceURI), e);
    }
    if (logger.isDebugEnabled()) {
      logger.debug("fetched model with {} statements for resource {}",result.size(), resourceURI);
    }
    return result;
  }

    /**
     * Looks for the triple [resourceURI, property, X] in the model obtained by
     * dereferencing the specified resourceURI and returns X as a URI.
     * If multiple triples are found, only the object of the first one is returned.
     * @param resourceURI
     * @param property
     * @return null if the model is empty or the property does not exist
     * @throws  IllegalArgumentException if the node found by the path is not a URI
     */
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

    /**
     * Looks for the triple [resourceURI, property, X] in the model obtained by
     * dereferencing the specified resourceURI and returns X as a string.
     * If multiple triples are found, only the object of the first one is returned.
     * @param resourceURI
     * @param property
     * @return null if the model is empty or the property does not exist
     */
    public String getStringPropertyForResource(final URI resourceURI, Property property)
    {
        Model rdfModel =  readResourceData(resourceURI);
        StmtIterator stmts = rdfModel.listStatements(
                new SimpleSelector(rdfModel.createResource(resourceURI.toString()), property, (RDFNode) null));
        //assume only one endpoint
        if (!stmts.hasNext()) return null;
        Statement stmt = stmts.next();
        return stmt.getString();
    }

    /**
     * Evaluates the path on the model obtained by dereferencing the specified resourceURI.
     * If the path resolves to multiple resources, only the first one is returned.
     * <br />
     * <br />
     * Note: For more information on property paths, see http://jena.sourceforge.net/ARQ/property_paths.html
     * <br />
     * To create a Path object for the path "rdf:type/rdfs:subClassOf*":
     * <pre>
     * Path path = PathParser.parse("rdf:type/rdfs:subClassOf*", PrefixMapping.Standard) ;
     * </pre>
     * @param resourceURI
     * @param propertyPath
     * @return null if the model is empty or the path does not resolve to a node
     * @throws  IllegalArgumentException if the node found by the path is not a URI
     */
    public URI getURIPropertyForPropertyPath(final URI resourceURI, Path propertyPath)
    {
       Node result = getNodeForPropertyPath(resourceURI, propertyPath);
        return URI.create(result.getURI());
    }

    /**
     * Evaluates the path on the model obtained by dereferencing the specified resourceURI.
     * If the path resolves to multiple resources, only the first one is returned.
     * <br />
     * <br />
     * Note: For more information on property paths, see http://jena.sourceforge.net/ARQ/property_paths.html
     * <br />
     * To create a Path object for the path "rdf:type/rdfs:subClassOf*":
     * <pre>
     * Path path = PathParser.parse("rdf:type/rdfs:subClassOf*", PrefixMapping.Standard) ;
     * </pre>
     * @param resourceURI
     * @param propertyPath
     * @return null if the model is empty or the path does not resolve to a node
     */
    public String getStringPropertyForPropertyPath(final URI resourceURI, Path propertyPath)
    {
        Node result = getNodeForPropertyPath(resourceURI, propertyPath);
        return result.getLiteralLexicalForm();
    }


    private Node getNodeForPropertyPath(URI resourceURI, Path propertyPath) {
        Model rdfModel = readResourceData(resourceURI);
        Iterator<Node> result =  PathEval.eval(rdfModel.getGraph(), rdfModel.getResource(resourceURI.toString()).asNode(), propertyPath);
        if (!result.hasNext()) return null;
        return result.next();
    }
}
