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

import org.apache.jena.query.Dataset;
import org.apache.jena.sparql.path.Path;
import org.springframework.http.HttpHeaders;
import won.protocol.rest.DatasetResponseWithStatusCodeAndHeaders;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * Interface for fetching linked data as a jena Model.
 */
public interface LinkedDataSource {
    /**
     * Obtains resource description of the resource identified by the given URI
     *
     * @param resourceURI
     * @return resource description as Dataset, or empty Dataset if not a valid URI
     * or not found
     */
    Dataset getDataForPublicResource(URI resourceURI);

    /**
     * Obtains resource description of the resource identified by the given URI for
     * the requester identified by the given WebID. Used in case access to the
     * resource is restricted by WebAccessControl (WebID-based access control).
     *
     * @param resourceURI URI of the resource
     * @param requesterWebID WebID of the entity requesting the resource
     * @return resource description as Dataset, or empty Dataset if not a valid URI
     * or not found or access not granted
     */
    Dataset getDataForResource(URI resourceURI, URI requesterWebID);

    Dataset getDataForPublicResource(final URI resourceURI, List<URI> properties, int maxRequest, int maxDepth);

    Dataset getDataForResource(final URI resourceURI, URI requesterWebID, List<URI> properties, int maxRequest,
                    int maxDepth);

    Dataset getDataForPublicResourceWithPropertyPath(final URI resourceURI, final List<Path> properties,
                    int maxRequest, int maxDepth, final boolean moveAllTriplesInDefaultGraph);

    Dataset getDataForResourceWithPropertyPath(final URI resourceURI, URI requesterWebID,
                    final List<Path> properties, int maxRequest, int maxDepth);

    Dataset getDataForResourceWithPropertyPath(final URI resourceURI, Optional<URI> requesterWebID,
                    final List<Path> properties, int maxRequest, int maxDepth);

    DatasetResponseWithStatusCodeAndHeaders getDatasetWithHeadersForResource(URI resource, HttpHeaders httpHeaders);
}
