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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.apache.activemq.blob.FTPBlobUploadStrategy;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WON;
import won.protocol.vocabulary.WONMSG;

/**
 * Utilitiy functions for common linked data lookups.
 */
public class WonLinkedDataUtils
{
  private static final Logger logger = LoggerFactory.getLogger(WonLinkedDataUtils.class);

  public static URI getConnectionStateforConnectionURI(URI connectionURI, LinkedDataSource linkedDataSource) {
	    assert linkedDataSource != null : "linkedDataSource must not be null";
	    Dataset dataset = getDataForResource(connectionURI, linkedDataSource);
	    Path propertyPath = PathParser.parse("<" + WON.HAS_CONNECTION_STATE+ ">", PrefixMapping.Standard);
	    return RdfUtils.getURIPropertyForPropertyPath(dataset, connectionURI, propertyPath);
	}
  
  public static URI getNeedURIforConnectionURI(URI connectionURI, LinkedDataSource linkedDataSource) {
	    assert linkedDataSource != null : "linkedDataSource must not be null";
	    Dataset dataset = getDataForResource(connectionURI, linkedDataSource);
	    Path propertyPath = PathParser.parse("<" + WON.BELONGS_TO_NEED + ">", PrefixMapping.Standard);
	    return RdfUtils.getURIPropertyForPropertyPath(dataset, connectionURI, propertyPath);
	  }
  
  public static URI getRemoteConnectionURIforConnectionURI(URI connectionURI, LinkedDataSource linkedDataSource) {
    assert linkedDataSource != null : "linkedDataSource must not be null";
    Dataset dataset = getDataForResource(connectionURI, linkedDataSource);
    Path propertyPath = PathParser.parse("<" + WON.HAS_REMOTE_CONNECTION + ">", PrefixMapping.Standard);
    return RdfUtils.getURIPropertyForPropertyPath(dataset, connectionURI, propertyPath);
  }

  public static URI getRemoteNeedURIforConnectionURI(URI connectionURI, LinkedDataSource linkedDataSource) {
    assert linkedDataSource != null : "linkedDataSource must not be null";
    Dataset dataset = getDataForResource(connectionURI, linkedDataSource);
    Path propertyPath = PathParser.parse("<" + WON.HAS_REMOTE_NEED + ">", PrefixMapping.Standard);
    return RdfUtils.getURIPropertyForPropertyPath(dataset, connectionURI, propertyPath);
  }
  
  public static URI getEventContainerURIforConnectionURI(URI connectionURI, LinkedDataSource linkedDataSource) {
	    assert linkedDataSource != null : "linkedDataSource must not be null";
	    Dataset dataset = getDataForResource(connectionURI, linkedDataSource);
	    Path propertyPath = PathParser.parse("<" + WON.HAS_EVENT_CONTAINER+ ">", PrefixMapping.Standard);
	    return RdfUtils.getURIPropertyForPropertyPath(dataset, connectionURI, propertyPath);
  }
  
  public static URI getEventContainerURIforNeedURI(URI needURI, LinkedDataSource linkedDataSource) {
	    assert linkedDataSource != null : "linkedDataSource must not be null";
	    Dataset dataset = getDataForResource(needURI, linkedDataSource);
	    Path propertyPath = PathParser.parse("<" + WON.HAS_EVENT_CONTAINER+ ">", PrefixMapping.Standard);
	    return RdfUtils.getURIPropertyForPropertyPath(dataset, needURI, propertyPath);
}

  public static Dataset getConversationAndNeedsDataset(String connectionURI, LinkedDataSource linkedDataSource) {
      return getConversationAndNeedsDataset(URI.create(connectionURI), linkedDataSource);
  }

    public static Dataset getFullNeedDataset(URI needURI, LinkedDataSource linkedDataSource) {
        assert linkedDataSource != null : "linkedDataSource must not be null";
        int depth = 7;
        int maxRequests = 1000;
        List<Path> propertyPaths = new ArrayList<>();
        PrefixMapping pmap = new PrefixMappingImpl();
        pmap.withDefaultMappings(PrefixMapping.Standard);
        pmap.setNsPrefix("won", WON.getURI());
        pmap.setNsPrefix("msg", WONMSG.getURI());
        propertyPaths.add(PathParser.parse("won:hasConnections", pmap));
        propertyPaths.add(PathParser.parse("won:hasConnections/rdfs:member", pmap));
        propertyPaths.add(PathParser.parse("won:hasConnections/rdfs:member/won:hasEventContainer", pmap));
        propertyPaths.add(PathParser.parse("won:hasConnections/rdfs:member/won:hasEventContainer/rdfs:member", pmap));
        propertyPaths.add(PathParser.parse("won:hasConnections/rdfs:member/won:hasEventContainer/rdfs:member/msg:hasCorrespondingRemoteMessage", pmap));
        propertyPaths.add(PathParser.parse("won:hasConnections/rdfs:member/won:hasEventContainer/rdfs:member/msg:hasPreviousMessage", pmap));
        propertyPaths.add(PathParser.parse("won:hasConnections/rdfs:member/won:belongsToNeed", pmap));
        propertyPaths.add(PathParser.parse("won:hasConnections/rdfs:member/won:belongsToNeed/won:hasEventContainer", pmap));
        propertyPaths.add(PathParser.parse("won:hasConnections/rdfs:member/won:belongsToNeed/won:hasEventContainer/rdfs:member", pmap));
        propertyPaths.add(PathParser.parse("won:hasConnections/rdfs:member/won:belongsToNeed/won:hasEventContainer/rdfs:member/msg:hasPreviousMessage", pmap));

        return linkedDataSource.getDataForResourceWithPropertyPath(needURI, needURI, propertyPaths, maxRequests, depth);
    }

  public static Dataset getConversationAndNeedsDataset(URI connectionURI, LinkedDataSource linkedDataSource) {
      assert linkedDataSource != null : "linkedDataSource must not be null";
      int depth = 5; 
      int maxRequests = 1000;
      List<Path> propertyPaths = new ArrayList<>();
      PrefixMapping pmap = new PrefixMappingImpl();
      pmap.withDefaultMappings(PrefixMapping.Standard);
      pmap.setNsPrefix("won", WON.getURI());
      pmap.setNsPrefix("msg", WONMSG.getURI());
      propertyPaths.add(PathParser.parse("won:hasEventContainer", pmap));
      propertyPaths.add(PathParser.parse("won:hasEventContainer/rdfs:member", pmap));
      propertyPaths.add(PathParser.parse("won:hasEventContainer/rdfs:member/msg:hasCorrespondingRemoteMessage", pmap));
      propertyPaths.add(PathParser.parse("won:hasEventContainer/rdfs:member/msg:hasPreviousMessage", pmap));
      propertyPaths.add(PathParser.parse("won:belongsToNeed", pmap));
      propertyPaths.add(PathParser.parse("won:belongsToNeed/won:hasEventContainer", pmap));
      propertyPaths.add(PathParser.parse("won:belongsToNeed/won:hasEventContainer/rdfs:member", pmap));
      propertyPaths.add(PathParser.parse("won:belongsToNeed/won:hasEventContainer/rdfs:member/msg:hasPreviousMessage", pmap));
      propertyPaths.add(PathParser.parse("won:hasRemoteNeed", pmap));
      propertyPaths.add(PathParser.parse("won:hasRemoteNeed/won:hasEventContainer", pmap));
      propertyPaths.add(PathParser.parse("won:hasRemoteNeed/won:hasEventContainer/rdfs:member", pmap));
      propertyPaths.add(PathParser.parse("won:hasRemoteNeed/won:hasEventContainer/rdfs:member/msg:hasPreviousMessage", pmap));
      propertyPaths.add(PathParser.parse("won:hasRemoteConnection", pmap));
      propertyPaths.add(PathParser.parse("won:hasRemoteConnection/won:hasEventContainer", pmap));
      propertyPaths.add(PathParser.parse("won:hasRemoteConnection/won:hasEventContainer/rdfs:member", pmap));
      propertyPaths.add(PathParser.parse("won:hasRemoteConnection/won:hasEventContainer/rdfs:member/msg:hasCorrespondingRemoteMessage", pmap));
      propertyPaths.add(PathParser.parse("won:hasRemoteConnection/won:hasEventContainer/rdfs:member/msg:hasPreviousMessage", pmap));
      URI requesterWebId = WonLinkedDataUtils.getNeedURIforConnectionURI(connectionURI, linkedDataSource);

      return linkedDataSource.getDataForResourceWithPropertyPath(connectionURI, requesterWebId, propertyPaths, maxRequests, depth);
  }

    public static Dataset getConversationDataset(String connectionURI, LinkedDataSource linkedDataSource) {
        return getConversationDataset(URI.create(connectionURI), linkedDataSource);
    }

    public static Dataset getConversationDataset(URI connectionURI, LinkedDataSource linkedDataSource) {
        assert linkedDataSource != null : "linkedDataSource must not be null";
        int depth = 5; // depth 3 from connection gives us the messages in the conversation
        int maxRequests = 1000;
        List<Path> propertyPaths = new ArrayList<>();
        PrefixMapping pmap = new PrefixMappingImpl();
        pmap.withDefaultMappings(PrefixMapping.Standard);
        pmap.setNsPrefix("won", WON.getURI());
        pmap.setNsPrefix("msg", WONMSG.getURI());
        propertyPaths.add(PathParser.parse("won:hasEventContainer", pmap));
        propertyPaths.add(PathParser.parse("won:hasEventContainer/rdfs:member", pmap));
        propertyPaths.add(PathParser.parse("won:hasRemoteConnection", pmap));
        propertyPaths.add(PathParser.parse("won:hasRemoteConnection/won:hasEventContainer", pmap));
        propertyPaths.add(PathParser.parse("won:hasRemoteConnection/won:hasEventContainer/rdfs:member", pmap));
        URI requesterWebId = WonLinkedDataUtils.getNeedURIforConnectionURI(connectionURI, linkedDataSource);

        return linkedDataSource.getDataForResourceWithPropertyPath(connectionURI, requesterWebId, propertyPaths, maxRequests, depth);
    }

    
  public static Dataset getDataForResource(final URI connectionURI, final LinkedDataSource linkedDataSource) {
    assert linkedDataSource != null : "linkedDataSource must not be null";
    assert connectionURI != null : "connection URI must not be null";
    Dataset dataset = null;
    logger.debug("loading model for connection {}", connectionURI);
    dataset = linkedDataSource.getDataForResource(connectionURI);
    if (dataset == null) {
      throw new IllegalStateException("failed to load model for Connection " + connectionURI);
    }
    return dataset;
  }

  public static Iterator<Dataset> getModelForURIs(final Iterator<URI> uriIterator,
    final LinkedDataSource linkedDataSource) {
    assert linkedDataSource != null : "linkedDataSource must not be null";
    return new ModelFetchingIterator(uriIterator, linkedDataSource);
  }

  public static URI getWonNodeURIForNeedOrConnectionURI(final URI resourceURI, LinkedDataSource linkedDataSource) {
    assert linkedDataSource != null : "linkedDataSource must not be null";
    logger.debug("fetching WON node URI for resource {} with linked data source {}", resourceURI, linkedDataSource);
    return getWonNodeURIForNeedOrConnection(resourceURI, linkedDataSource.getDataForResource(resourceURI));
  }

  public static URI getWonNodeURIForNeedOrConnection(final URI resURI, final Model resourceModel) {
    assert resourceModel != null : "model must not be null";
    //we didnt't get the queue name. Check if the model contains a triple <baseuri> won:hasWonNode
    // <wonNode> and get the information from there.
    logger.debug("getting WON node URI from model");
    Resource baseResource = resourceModel.getResource(resURI.toString());
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

  public static URI getWonNodeURIForNeedOrConnection(final URI resourceURI, final Dataset resourceDataset) {
    return RdfUtils.findFirst(resourceDataset, new RdfUtils.ModelVisitor<URI>(){
      @Override
      public URI visit(final Model model) {
        return getWonNodeURIForNeedOrConnection(resourceURI, model);
      }
    });
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
    Dataset nodeDataset = linkedDataSource.getDataForResource(wonNodeUri);
    return RdfUtils.getNodeForPropertyPath(
      nodeDataset,
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
    Dataset dataset = linkedDataSource.getDataForResource(resourceURI);
    return RdfUtils.getNodeForPropertyPath(
      dataset,
      resourceURI,
      propertyPath
    );
  }

  
  public static Optional<URI> getDefaultFacet(URI needURI, boolean returnAnyIfNoDefaultFound, LinkedDataSource linkedDataSource) {
      return WonRdfUtils.FacetUtils.getDefaultFacet(getDataForResource(needURI, linkedDataSource), needURI, returnAnyIfNoDefaultFound);
  }

  public static Collection<URI> getFacetsOfType(URI needURI, URI facetTypeURI, LinkedDataSource linkedDataSource) {
      return WonRdfUtils.FacetUtils.getFacetsOfType(getDataForResource(needURI, linkedDataSource), needURI, facetTypeURI);
  }

  public static Optional<URI> getTypeOfFacet(URI facetURI, LinkedDataSource linkedDataSource) {
      return WonRdfUtils.FacetUtils.getTypeOfFacet(getDataForResource(facetURI, linkedDataSource), facetURI);
  }

  /**
   * Crawls all connections of the specified need without messages. 
   * 
   */
  public static Dataset getConnectionNetwork(URI needURI, LinkedDataSource linkedDataSource) {
      assert linkedDataSource != null : "linkedDataSource must not be null";
      int depth = 5; 
      int maxRequests = 1000;
      List<Path> propertyPaths = new ArrayList<>();
      PrefixMapping pmap = new PrefixMappingImpl();
      pmap.withDefaultMappings(PrefixMapping.Standard);
      pmap.setNsPrefix("won", WON.getURI());
      pmap.setNsPrefix("msg", WONMSG.getURI());
      propertyPaths.add(PathParser.parse("won:hasConnections", pmap));
      propertyPaths.add(PathParser.parse("won:hasConnections/rdfs:member", pmap));
      return linkedDataSource.getDataForResourceWithPropertyPath(needURI, needURI, propertyPaths, maxRequests, depth);
  }
  
  /**
   * Iterator implementation that fetches linked data lazily for the specified iterator of URIs.
   */
  private static class ModelFetchingIterator implements Iterator<Dataset> {
    private Iterator<URI> uriIterator = null;
    private LinkedDataSource linkedDataSource = null;

    private ModelFetchingIterator(final Iterator<URI> uriIterator, final LinkedDataSource linkedDataSource) {
      this.uriIterator = uriIterator;
      this.linkedDataSource = linkedDataSource;
    }

    @Override
    public Dataset next() {
      URI uri = uriIterator.next();
      return linkedDataSource.getDataForResource(uri);
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
