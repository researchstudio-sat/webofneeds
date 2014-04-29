/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.protocol.util.linkeddata;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.sparql.path.Path;
import com.hp.hpl.jena.sparql.path.PathParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.WON;

import java.net.URI;
import java.util.Iterator;

/**
 * Utilitiy functions for common linked data lookups.
 */
public class WonLinkedDataUtils
{
  private static final Logger logger = LoggerFactory.getLogger(WonLinkedDataUtils.class);

  public static URI getRemoteConnectionURIforConnectionURI(URI connectionURI, LinkedDataSource linkedDataSource) {
    assert linkedDataSource != null : "linkedDataSource must not be null";
    Model model = getModelForResource(connectionURI, linkedDataSource);
    Path propertyPath = PathParser.parse("<" + WON.HAS_REMOTE_CONNECTION + ">", PrefixMapping.Standard);
    return RdfUtils.getURIPropertyForPropertyPath(model, connectionURI, propertyPath);
  }

  public static URI getRemoteNeedURIforConnectionURI(URI connectionURI, LinkedDataSource linkedDataSource) {
    assert linkedDataSource != null : "linkedDataSource must not be null";
    Model model = getModelForResource(connectionURI, linkedDataSource);
    Path propertyPath = PathParser.parse("<" + WON.HAS_REMOTE_NEED + ">", PrefixMapping.Standard);
    return RdfUtils.getURIPropertyForPropertyPath(model, connectionURI, propertyPath);
  }

  public static Model getModelForResource(final URI connectionURI, final LinkedDataSource linkedDataSource) {
    assert linkedDataSource != null : "linkedDataSource must not be null";
    assert connectionURI != null : "connection URI must not be null";
    Model model = null;
    logger.debug("loading model for connection {}", connectionURI);
    model = linkedDataSource.getModelForResource(connectionURI);
    if (model == null) {
      throw new IllegalStateException("failed to load model for Connection " + connectionURI);
    }
    return model;
  }

  public static Iterator<Model> getModelForURIs(final Iterator<URI> uriIterator, final LinkedDataSource linkedDataSource) {
    assert linkedDataSource != null : "linkedDataSource must not be null";
    return new ModelFetchingIterator(uriIterator, linkedDataSource);
  }

  public static URI getWonNodeURIForNeedOrConnectionURI(final URI resourceURI, LinkedDataSource linkedDataSource) {
    assert linkedDataSource != null : "linkedDataSource must not be null";
    logger.debug("fetching WON node URI for resource {} with linked data source {}", resourceURI, linkedDataSource);
    return getWonNodeURIForNeedOrConnection(linkedDataSource.getModelForResource(resourceURI));
  }

  public static URI getWonNodeURIForNeedOrConnection(final Model resourceModel) {
    assert resourceModel != null : "model must not be null";
    //we didnt't get the queue name. Check if the model contains a triple <baseuri> won:hasWonNode
    // <wonNode> and get the information from there.
    logger.debug("getting WON node URI from model", resourceModel);
    Resource baseResource = RdfUtils.getBaseResource(resourceModel);
    logger.debug("resourceModel: {}",RdfUtils.toString(resourceModel));
    StmtIterator wonNodeStatementIterator = baseResource.listProperties(WON.HAS_WON_NODE);
    if (! wonNodeStatementIterator.hasNext()){
      //no won:hasWonNode triple found. we can't do anything.
      logger.debug("base resource {} has no won:hasWonNode property", baseResource);
      return null;
    }
    Statement stmt = wonNodeStatementIterator.nextStatement();
    RDFNode wonNodeNode = stmt.getObject();
    if (!wonNodeNode.isResource()) {
      logger.debug("won:hasWonNode property of base resource {} is not a resource", baseResource);
      return null;
    }
    URI wonNodeUri = URI.create(wonNodeNode.asResource().getURI().toString());
    logger.debug("obtained WON node URI: {}",wonNodeUri);
    if (wonNodeStatementIterator.hasNext()) {
      logger.warn("multiple WON node URIs found for resource {}, using first one: {} ", baseResource, wonNodeUri );
    }
    return wonNodeUri;
  }

  /**
   * For the specified need or connection URI, the model is fetched, the WON node URI found there is also
   * de-referenced, and the specified property path is evaluated in that graph, starting at the WON node URI.
   *
   * @param resourceURI
   * @param propertyPath
   * @param linkedDataSource
   * @return
   */
  public static Node getWonNodePropertyForNeedOrConnectionURI(URI resourceURI, Path propertyPath, LinkedDataSource linkedDataSource) {
    assert linkedDataSource != null : "linkedDataSource must not be null";
    URI wonNodeUri = WonLinkedDataUtils.getWonNodeURIForNeedOrConnectionURI(resourceURI, linkedDataSource);
    Model nodeModel = linkedDataSource.getModelForResource(wonNodeUri);
    return RdfUtils.getNodeForPropertyPath(
      nodeModel,
      wonNodeUri,
      propertyPath
    );
  }

  /**
   * For the specified URI, the model is fetched and the specified property path is evaluated in that graph,
   * starting at the specified URI.
   *
   * @param resourceURI
   * @param propertyPath
   * @param linkedDataSource
   * @return
   */
  public static Node getPropertyForURI(URI resourceURI, Path propertyPath, LinkedDataSource linkedDataSource) {
    assert linkedDataSource != null : "linkedDataSource must not be null";
    Model model = linkedDataSource.getModelForResource(resourceURI);
    return RdfUtils.getNodeForPropertyPath(
      model,
      resourceURI,
      propertyPath
    );
  }




  /**
   * Iterator implementation that fetches linked data lazily for the specified iterator of URIs.
   */
  private static class ModelFetchingIterator implements Iterator<Model> {
    private Iterator<URI> uriIterator = null;
    private LinkedDataSource linkedDataSource = null;

    private ModelFetchingIterator(final Iterator<URI> uriIterator, final LinkedDataSource linkedDataSource) {
      this.uriIterator = uriIterator;
      this.linkedDataSource = linkedDataSource;
    }

    @Override
    public Model next() {
      URI uri = uriIterator.next();
      return linkedDataSource.getModelForResource(uri);
    }

    @Override
    public boolean hasNext() {
      return uriIterator.hasNext();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("this iterator cannot remove");
    }
  }

}
