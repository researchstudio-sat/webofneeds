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

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.expr.nodevalue.NodeValueBoolean;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.tdb.TDB;
import org.apache.jena.tdb.TDBFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import won.protocol.rest.DatasetResponseWithStatusCodeAndHeaders;
import won.protocol.rest.LinkedDataRestClient;
import won.protocol.util.RdfUtils;

import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LinkedDataSource implementation that delegates fetching linked data resources to the
 * provided LinedDataRestClient.
 */
public class LinkedDataSourceBase implements LinkedDataSource
{
  private final Logger logger = LoggerFactory.getLogger(LinkedDataSourceBase.class);
  protected LinkedDataRestClient linkedDataRestClient;

    /**
     * extract the previous link (in case won node had more data than could be sent or was requested) from http response headers
     * @param datasetWithHeaders
     * @return the previous link to more data on the node or null if the link header does not exist
     */
  public String getPreviousLinkFromDatasetWithHeaders(DatasetResponseWithStatusCodeAndHeaders datasetWithHeaders) {

      String prevLink = null;
      List<String> links = datasetWithHeaders.getResponseHeaders().get("Link");
      if (links != null) {
          for (String link : links) {
              Pattern pattern = Pattern.compile("<(.+)>; rel=\"?prev\"?");
              Matcher matcher = pattern.matcher(link);
              if (matcher.find()) {
                  prevLink = matcher.group(1);
                  return prevLink;
              }
          }
      }

     return prevLink;
  }

    /**
     * get a dataset with headers. this methods can be used in combination with getPreviousLinkFromDatasetWithHeaders
     * in case links to previous pages of data from the won node should be checked too
     *
     * @param resource uri of the resource to request
     * @return dataset including http response headers
     */
  public DatasetResponseWithStatusCodeAndHeaders getDatasetWithHeadersForResource(URI resource, HttpHeaders httpHeaders) {

      assert resource != null : "resource must not be null";
      logger.debug("fetching linked data for URI {}", resource);
      return linkedDataRestClient.readResourceDataWithHeaders(resource, httpHeaders);
  }

  @Override
  public Dataset getDataForResource(URI resource){

    assert resource != null : "resource must not be null";

    logger.debug("fetching linked data for URI {}", resource);
    Dataset dataset = DatasetFactory.createGeneral();
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
    Dataset dataset = DatasetFactory.createGeneral();
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
        crawledURIs.add(currentURI);
        requests++;
        logger.debug("current Request: "+requests);
        if (requests >= maxRequest) break OUTER;

      }
      newlyDiscoveredURIs.addAll(getURIsToCrawl(dataset, crawledURIs, properties));
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
        crawledURIs.add(currentURI);
        requests++;
        logger.debug("current Request: "+requests);
        if (requests >= maxRequest) break OUTER;

      }
      newlyDiscoveredURIs.addAll(getURIsToCrawl(dataset, crawledURIs, properties));
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
        crawledURIs.add(currentURI);
        requests++;
        logger.debug("current Request: "+requests);
        if (requests >= maxRequest) break OUTER;

      }
      newlyDiscoveredURIs.addAll(getURIsToCrawlWithPropertyPath(resultDataset, resourceURI, crawledURIs, properties));
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
        crawledURIs.add(currentURI);
        requests++;
        logger.debug("current Request: "+requests);
        if (requests >= maxRequest) break OUTER;
      }
      newlyDiscoveredURIs.addAll(getURIsToCrawlWithPropertyPath(resultDataset, resourceURI, crawledURIs, properties));
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
    return DatasetFactory.wrap(dsg);
  }
}
