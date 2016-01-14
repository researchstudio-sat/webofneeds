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

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.expr.nodevalue.NodeValueBoolean;
import com.hp.hpl.jena.sparql.path.Path;
import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.tdb.TDBFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.rest.LinkedDataRestClient;
import won.protocol.util.RdfUtils;

import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * LinkedDataSource implementation that delegates fetching linked data resources to the
 * provided LinedDataRestClient.
 */
public class LinkedDataSourceBase implements LinkedDataSource
{
  private final Logger logger = LoggerFactory.getLogger(getClass());
  protected LinkedDataRestClient linkedDataRestClient;


  @Override
  public Dataset getDataForResource(URI resource){

    assert resource != null : "resource must not be null";

    logger.debug("fetching linked data for URI {}", resource);
    Dataset dataset = DatasetFactory.createMem();
    try {
      dataset = linkedDataRestClient.readResourceData(resource);
    } catch (Exception e){
        logger.debug(String.format("Couldn't fetch resource %s",resource),e);
    }
    return dataset;
  }

  @Override
  public Dataset getDataForResource(final URI resource, final URI requesterWebID) {
    assert (resource != null && requesterWebID != null) : "resource and requester must not be null";

    logger.debug("fetching linked data for URI {} requester {}", resource, requesterWebID);
    Dataset dataset = DatasetFactory.createMem();
    try {
      dataset = linkedDataRestClient.readResourceData(resource, requesterWebID);
    } catch (Exception e){
      logger.debug(String.format("Couldn't fetch resource %s",resource),e);
    }
    return dataset;
  }

  @Override
  public Dataset getDataForResource(final URI resourceURI, List<URI> properties,
    int maxRequest, int maxDepth) {
    Set<URI> crawledURIs = new HashSet<URI>();
    Set<URI> newlyDiscoveredURIs = new HashSet<URI>();
    Set<URI> urisToCrawl = null;
    newlyDiscoveredURIs.add(resourceURI);
    int depth = 0;
    int requests = 0;

    Dataset dataset = makeDataset();
    OUTER: while (newlyDiscoveredURIs.size() > 0 && depth < maxDepth && requests < maxRequest){
      urisToCrawl = newlyDiscoveredURIs;
      newlyDiscoveredURIs = new HashSet<URI>();
      for (URI currentURI: urisToCrawl) {
        //add all models from urisToCrawl
        Dataset currentModel =  getDataForResource(currentURI);
        RdfUtils.addDatasetToDataset(dataset, currentModel);
        newlyDiscoveredURIs.addAll(getURIsToCrawl(currentModel, crawledURIs, properties));
        crawledURIs.add(currentURI);
        requests++;
        logger.debug("current Request: "+requests);
        if (requests >= maxRequest) break OUTER;

      }
      depth++;
      logger.debug("current Depth: "+depth);
    }
    return dataset;
  }

  @Override
  public Dataset getDataForResource(final URI resourceURI, final URI requesterWebID, final List<URI> properties, final int maxRequest, final int maxDepth) {
    Set<URI> crawledURIs = new HashSet<URI>();
    Set<URI> newlyDiscoveredURIs = new HashSet<URI>();
    Set<URI> urisToCrawl = null;
    newlyDiscoveredURIs.add(resourceURI);
    int depth = 0;
    int requests = 0;

    Dataset dataset = makeDataset();
    OUTER: while (newlyDiscoveredURIs.size() > 0 && depth < maxDepth && requests < maxRequest){
      urisToCrawl = newlyDiscoveredURIs;
      newlyDiscoveredURIs = new HashSet<URI>();
      for (URI currentURI: urisToCrawl) {
        //add all models from urisToCrawl
        Dataset currentModel =  getDataForResource(currentURI, requesterWebID);
        RdfUtils.addDatasetToDataset(dataset, currentModel);
        newlyDiscoveredURIs.addAll(getURIsToCrawl(currentModel, crawledURIs, properties));
        crawledURIs.add(currentURI);
        requests++;
        logger.debug("current Request: "+requests);
        if (requests >= maxRequest) break OUTER;

      }
      depth++;
      logger.debug("current Depth: "+depth);
    }
    return dataset;
  }

  @Override
  public Dataset getDataForResourceWithPropertyPath(final URI resourceURI, final List<Path> properties,
    final int maxRequest, final int maxDepth, final boolean moveAllTriplesInDefaultGraph) {

    Set<URI> crawledURIs = new HashSet<URI>();
    Set<URI> newlyDiscoveredURIs = new HashSet<URI>();
    Set<URI> urisToCrawl = null;
    newlyDiscoveredURIs.add(resourceURI);
    int depth = 0;
    int requests = 0;

    Dataset resultDataset = makeDataset();

    OUTER: while (newlyDiscoveredURIs.size() > 0 && depth < maxDepth && requests < maxRequest){
      urisToCrawl = newlyDiscoveredURIs;
      newlyDiscoveredURIs = new HashSet<URI>();
      for (URI currentURI: urisToCrawl) {
        //add all models from urisToCrawl

        Dataset currentDataset =  getDataForResource(currentURI);
        //logger.debug("current dataset: {} "+RdfUtils.toString(currentModel));
        if (moveAllTriplesInDefaultGraph){
          RdfUtils.copyDatasetTriplesToModel(currentDataset, resultDataset.getDefaultModel());
        } else {
          RdfUtils.addDatasetToDataset(resultDataset, currentDataset);
        }
        newlyDiscoveredURIs.addAll(getURIsToCrawlWithPropertyPath(resultDataset, resourceURI, crawledURIs, properties));
        crawledURIs.add(currentURI);
        requests++;
        logger.debug("current Request: "+requests);
        if (requests >= maxRequest) break OUTER;

      }
      depth++;
      logger.debug("current Depth: "+depth);
    }
    return resultDataset;

  }

  @Override
  public Dataset getDataForResourceWithPropertyPath(final URI resourceURI, final URI requesterWebID, final List<Path> properties, final int maxRequest, final int maxDepth, final boolean moveAllTriplesInDefaultGraph) {
    Set<URI> crawledURIs = new HashSet<URI>();
    Set<URI> newlyDiscoveredURIs = new HashSet<URI>();
    Set<URI> urisToCrawl = null;
    newlyDiscoveredURIs.add(resourceURI);
    int depth = 0;
    int requests = 0;

    Dataset resultDataset = makeDataset();

    OUTER: while (newlyDiscoveredURIs.size() > 0 && depth < maxDepth && requests < maxRequest){
      urisToCrawl = newlyDiscoveredURIs;
      newlyDiscoveredURIs = new HashSet<URI>();
      for (URI currentURI: urisToCrawl) {
        //add all models from urisToCrawl

        Dataset currentDataset =  getDataForResource(currentURI, requesterWebID);
        //logger.debug("current dataset: {} "+RdfUtils.toString(currentModel));
        if (moveAllTriplesInDefaultGraph){
          RdfUtils.copyDatasetTriplesToModel(currentDataset, resultDataset.getDefaultModel());
        } else {
          RdfUtils.addDatasetToDataset(resultDataset, currentDataset);
        }
        newlyDiscoveredURIs.addAll(getURIsToCrawlWithPropertyPath(resultDataset, resourceURI, crawledURIs, properties));
        crawledURIs.add(currentURI);
        requests++;
        logger.debug("current Request: "+requests);
        if (requests >= maxRequest) break OUTER;

      }
      depth++;
      logger.debug("current Depth: "+depth);
    }
    return resultDataset;
  }

  /**
   * For the specified resourceURI, evaluates the specified property paths and adds the identified
   * resources to the returned set if they are not contained in the specified exclude set.
   * @param dataset
   * @param resourceURI
   * @param excludedUris
   * @param properties
   * @return
   */
  private Set<URI> getURIsToCrawlWithPropertyPath(Dataset dataset, URI resourceURI, Set<URI> excludedUris,
                                                  List<Path> properties){
    Set<URI> toCrawl = new HashSet<URI>();
    for (int i = 0; i<properties.size();i++){
      Iterator<URI> newURIs = RdfUtils.getURIsForPropertyPathByQuery(dataset,
        resourceURI,
        properties.get(i));
      while (newURIs.hasNext()){
        URI newUri = newURIs.next();
        if (!excludedUris.contains(newUri)) {
          toCrawl.add(newUri);
        }
      }
    }
    return toCrawl;
  }

  /**
   * For the specified properties, finds their objects and adds the identified
   * resources to the returned set if they are not contained in the specified exclude set.
   * @param dataset
     @param excludedUris
   * @param properties
   * @return
   */
  private Set<URI> getURIsToCrawl(Dataset dataset, Set<URI> excludedUris, final List<URI> properties) {
    Set<URI> toCrawl = new HashSet<>();
    for (int i = 0; i<properties.size();i++){
      final URI property = properties.get(i);
      NodeIterator objectIterator = RdfUtils.visitFlattenedToNodeIterator(dataset, new RdfUtils.ModelVisitor<NodeIterator>()
      {
        @Override
        public NodeIterator visit(final Model model) {
          final Property p = model.createProperty(property.toString());
          return model.listObjectsOfProperty(p);
        }
      });
      for (;objectIterator.hasNext();){
        RDFNode objectNode = objectIterator.next();

        if (objectNode.isURIResource()) {
          URI discoveredUri = URI.create(objectNode.asResource().getURI());
          if (!excludedUris.contains(discoveredUri)){
            toCrawl.add(discoveredUri);
          }
        }
      }

    }
    return toCrawl;
  }


  public void setLinkedDataRestClient(final LinkedDataRestClient linkedDataRestClient) {
    this.linkedDataRestClient = linkedDataRestClient;
  }

  public static Dataset makeDataset() {
    DatasetGraph dsg = TDBFactory.createDatasetGraph();
    dsg.getContext().set(TDB.symUnionDefaultGraph, new NodeValueBoolean(true));
    return DatasetFactory.create(dsg);
  }
}
