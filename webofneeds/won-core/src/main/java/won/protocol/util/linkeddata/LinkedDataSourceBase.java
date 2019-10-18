/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.protocol.util.linkeddata;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.expr.nodevalue.NodeValueBoolean;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.tdb.TDB;
import org.apache.jena.tdb.TDBFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import won.protocol.rest.DatasetResponseWithStatusCodeAndHeaders;
import won.protocol.rest.LinkedDataFetchingException;
import won.protocol.rest.LinkedDataRestClient;
import won.protocol.util.AuthenticationThreadLocal;
import won.protocol.util.RdfUtils;
import won.protocol.util.linkeddata.uriresolver.WonMessageUriResolver;

/**
 * LinkedDataSource implementation that delegates fetching linked data resources
 * to the provided LinedDataRestClient.
 */
public abstract class LinkedDataSourceBase implements LinkedDataSource {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    protected LinkedDataRestClient linkedDataRestClient;
    @Autowired
    private ThreadPoolExecutor parallelRequestsThreadpool;
    @Autowired
    protected WonMessageUriResolver wonMessageUriResolver;

    /**
     * extract the previous link (in case won node had more data than could be sent
     * or was requested) from http response headers
     * 
     * @param datasetWithHeaders
     * @return the previous link to more data on the node or null if the link header
     * does not exist
     */
    public String getPreviousLinkFromDatasetWithHeaders(DatasetResponseWithStatusCodeAndHeaders datasetWithHeaders) {
        String prevLink;
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
        return null;
    }

    /**
     * get a dataset with headers. this methods can be used in combination with
     * getPreviousLinkFromDatasetWithHeaders in case links to previous pages of data
     * from the won node should be checked too
     *
     * @param resource uri of the resource to request
     * @return dataset including http response headers
     */
    public DatasetResponseWithStatusCodeAndHeaders getDatasetWithHeadersForResource(URI resource,
                    HttpHeaders httpHeaders) {
        assert resource != null : "resource must not be null";
        logger.debug("fetching linked data for URI {}", resource);
        return linkedDataRestClient.readResourceDataWithHeaders(resource, httpHeaders);
    }

    @Override
    public Dataset getDataForResource(URI resource) {
        if (resource == null) {
            throw new IllegalArgumentException("resource must not be null");
        }
        resource = wonMessageUriResolver.toLocalMessageURI(resource, this);
        logger.debug("fetching linked data for URI {}", resource);
        Dataset dataset = DatasetFactory.createGeneral();
        try {
            dataset = linkedDataRestClient.readResourceData(resource);
            if (logger.isDebugEnabled()) {
                logger.debug("fetched resource {}:", resource);
                RDFDataMgr.write(System.out, dataset, Lang.TRIG);
            }
        } catch (Exception e) {
            logger.debug(String.format("Couldn't fetch resource %s", resource), e);
        }
        return dataset;
    }

    @Override
    public Dataset getDataForResource(URI resource, final URI requesterWebID) {
        if (resource == null || requesterWebID == null) {
            throw new IllegalArgumentException("resource and requester must not be null");
        }
        resource = wonMessageUriResolver.toLocalMessageURI(resource, this);
        logger.debug("fetching linked data for URI {} requester {}", resource, requesterWebID);
        Dataset dataset = DatasetFactory.createGeneral();
        try {
            dataset = linkedDataRestClient.readResourceData(resource, requesterWebID);
            if (logger.isDebugEnabled()) {
                logger.debug("fetched resource {} with requesterWebId {}:", resource, requesterWebID);
                RDFDataMgr.write(System.out, dataset, Lang.TRIG);
            }
        } catch (Exception e) {
            logger.debug(String.format("Couldn't fetch resource %s", resource), e);
        }
        return dataset;
    }

    @Override
    public Dataset getDataForResource(final URI resourceURI, List<URI> properties, int maxRequest, int maxDepth) {
        return getDataForResource(resourceURI, null, properties, maxRequest, maxDepth);
    }

    @Override
    public Dataset getDataForResource(final URI resourceURI, final URI requesterWebID, final List<URI> properties,
                    final int maxRequest, final int maxDepth) {
        return getDataForResource(resourceURI, Optional.ofNullable(requesterWebID), maxRequest, maxDepth,
                        (Dataset crawledData,
                                        Set<URI> crawledUris) -> getURIsToCrawl(crawledData, crawledUris, properties));
    }

    private Set<URI> retainOnlyAllowedAmount(Set<URI> newlyDiscoveredURIs, final int maxRequest, int requests) {
        if (newlyDiscoveredURIs.size() + requests > maxRequest) {
            // only crawl as many as we are allowed to
            return new HashSet<>(new ArrayList<>(newlyDiscoveredURIs).subList(0, maxRequest - requests));
        }
        return newlyDiscoveredURIs;
    }

    @Override
    public Dataset getDataForResourceWithPropertyPath(final URI resourceURI, final List<Path> properties,
                    final int maxRequest, final int maxDepth, final boolean moveAllTriplesInDefaultGraph) {
        Dataset result = DatasetFactory.createGeneral();
        Model m = result.getDefaultModel();
        RdfUtils.toStatementStream(
                        getDataForResourceWithPropertyPath(resourceURI, Optional.empty(), properties, maxRequest,
                                        maxDepth))
                        .forEach(m::add);
        return result;
    }

    @Override
    public Dataset getDataForResourceWithPropertyPath(final URI resourceURI, final Optional<URI> requesterWebID,
                    final List<Path> properties, final int maxRequest, final int maxDepth) {
        return getDataForResource(resourceURI, requesterWebID, maxRequest, maxDepth,
                        (Dataset crawledData, Set<URI> crawledUris) -> getURIsToCrawlWithPropertyPath(crawledData,
                                        resourceURI, crawledUris, properties));
    }

    @Override
    public Dataset getDataForResourceWithPropertyPath(final URI resourceURI, final URI requesterWebID,
                    final List<Path> properties, final int maxRequest, final int maxDepth) {
        return getDataForResource(resourceURI, Optional.ofNullable(requesterWebID), maxRequest, maxDepth,
                        (Dataset crawledData, Set<URI> crawledUris) -> getURIsToCrawlWithPropertyPath(crawledData,
                                        resourceURI, crawledUris, properties));
    }

    private Dataset getDataForResource(final URI resourceURI, final Optional<URI> requesterWebID, final int maxRequest,
                    final int maxDepth, BiFunction<Dataset, Set<URI>, Set<URI>> findNextUrisFunction) {
        Set<URI> crawledURIs = new HashSet<>();
        Set<URI> newlyDiscoveredURIs = new HashSet<>();
        newlyDiscoveredURIs.add(resourceURI);
        int depth = 0;
        int requests = 0;
        final Dataset dataset = makeDataset();
        while (newlyDiscoveredURIs.size() > 0 && depth < maxDepth && requests < maxRequest) {
            final Set<URI> urisToCrawl = retainOnlyAllowedAmount(newlyDiscoveredURIs, maxRequest, requests);
            // hack: there may be a threadLocal with the authentication data we need further
            // down the call stack
            // if there is one, we need to add that to the threads we use in the following
            // parallel construct
            final Optional<Object> authenticationOpt = AuthenticationThreadLocal.hasValue()
                            ? Optional.of(AuthenticationThreadLocal.getAuthentication())
                            : Optional.empty();
            Future<Optional<Dataset>> crawledData = parallelRequestsThreadpool
                            .submit(() -> urisToCrawl.parallelStream().map(uri -> {
                                try {
                                    if (authenticationOpt.isPresent()) {
                                        // theadlocal hack mentioned above
                                        AuthenticationThreadLocal.setAuthentication(authenticationOpt.get());
                                    }
                                    return requesterWebID.isPresent() ? getDataForResource(uri, requesterWebID.get())
                                                    : getDataForResource(uri);
                                } finally {
                                    // be sure to remove the principal from the threadlocal after the call
                                    AuthenticationThreadLocal.remove();
                                }
                            }).reduce(RdfUtils::addDatasetToDataset));
            Optional<Dataset> crawledDataset;
            try {
                crawledDataset = crawledData.get();
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof LinkedDataFetchingException) {
                    throw (LinkedDataFetchingException) cause;
                }
                throw new RuntimeException("Could not retrieve data for multiple URIs", e);
            } catch (Exception e) {
                throw new RuntimeException("Could not retrieve data for multiple URIs", e);
            }
            if (crawledDataset.isPresent()) {
                // Add crawledDataset to dataset, replacing any named models contained in both.
                // We do this because
                // 1. merging does not work properly in the presence of blank nodes - they end
                // up duplicated
                // 2. we do not expect to find the same named model with different content, so
                // merging should have no visible effect at all
                RdfUtils.addDatasetToDataset(dataset, crawledDataset.get(), true);
            }
            crawledURIs.addAll(urisToCrawl);
            requests += urisToCrawl.size();
            newlyDiscoveredURIs = new HashSet<>(findNextUrisFunction.apply(dataset, crawledURIs));
            depth++;
            logger.debug("current Depth: " + depth);
        }
        return dataset;
    }

    /**
     * For the specified resourceURI, evaluates the specified property paths and
     * adds the identified resources to the returned set if they are not contained
     * in the specified exclude set.
     * 
     * @param dataset
     * @param resourceURI
     * @param excludedUris
     * @param properties
     * @return
     */
    private Set<URI> getURIsToCrawlWithPropertyPath(Dataset dataset, URI resourceURI, Set<URI> excludedUris,
                    List<Path> properties) {
        if (logger.isDebugEnabled()) {
            logger.debug("evaluating property paths on data crawled so far");
            RDFDataMgr.write(System.out, dataset, Lang.TRIG);
        }
        Set<URI> toCrawl = new HashSet<>();
        properties.stream().forEach(path -> {
            Iterator<URI> newURIs = RdfUtils.getURIsForPropertyPathByQuery(dataset, resourceURI, path);
            if (!newURIs.hasNext()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("found no uris starting at {}, using path {}", new Object[] { resourceURI, path });
                }
                return;
            }
            Set<URI> newUrisThisIteration = new HashSet<>();
            int skipped = 0;
            while (newURIs.hasNext()) {
                URI newUri = newURIs.next();
                boolean skip = excludedUris.contains(newUri);
                if (skip) {
                    skipped++;
                } else {
                    newUrisThisIteration.add(newUri);
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("found uri {} starting at {}, using path {}, will {} ",
                                    new Object[] { newUri, resourceURI, path, skip ? "skip" : "fetch" });
                }
            }
            toCrawl.addAll(newUrisThisIteration);
        });
        return toCrawl;
    }

    /**
     * For the specified properties, finds their objects and adds the identified
     * resources to the returned set if they are not contained in the specified
     * exclude set.
     * 
     * @param dataset
     * @param excludedUris
     * @param properties
     * @return
     */
    private Set<URI> getURIsToCrawl(Dataset dataset, Set<URI> excludedUris, final List<URI> properties) {
        Set<URI> toCrawl = new HashSet<>();
        for (final URI property : properties) {
            NodeIterator objectIterator = RdfUtils.visitFlattenedToNodeIterator(dataset,
                            model -> {
                                final Property p = model.createProperty(property.toString());
                                return model.listObjectsOfProperty(p);
                            });
            for (; objectIterator.hasNext();) {
                RDFNode objectNode = objectIterator.next();
                if (objectNode.isURIResource()) {
                    URI discoveredUri = URI.create(objectNode.asResource().getURI());
                    if (!excludedUris.contains(discoveredUri)) {
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

    public void setParallelRequestsThreadpool(ThreadPoolExecutor parallelRequestsThreadpool) {
        this.parallelRequestsThreadpool = parallelRequestsThreadpool;
    }

    public static Dataset makeDataset() {
        DatasetGraph dsg = TDBFactory.createDatasetGraph();
        dsg.getContext().set(TDB.symUnionDefaultGraph, new NodeValueBoolean(true));
        return DatasetFactory.wrap(dsg);
    }
}
