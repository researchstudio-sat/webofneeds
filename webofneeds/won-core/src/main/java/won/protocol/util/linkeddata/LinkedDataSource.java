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
import com.hp.hpl.jena.sparql.path.Path;

import java.net.URI;
import java.util.List;

/**
 * Interface for fetching linked data as a jena Model.
 */
public interface LinkedDataSource
{
    /**
     *
      * @param resourceURI
     * @return empty Dataset if not a valid URI or not found
     */
  public Dataset getDataForResource(URI resourceURI);

  public Dataset getDataForResource(final URI resourceURI, List<URI> properties,
    int maxRequest, int maxDepth);

  public Dataset getDataForResourceWithPropertyPath(final URI resourceURI, final List<Path> properties, int maxRequest,
    int maxDepth, final boolean moveAllTriplesInDefaultGraph);
}
