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

package won.node.service;

import org.apache.jena.rdf.model.Resource;
import won.node.service.impl.URIService;
import won.protocol.exception.*;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.ConnectionState;
import won.protocol.model.Facet;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * User: fkleedorfer
 * Date: 18.04.14
 */
public interface DataAccessService {
  public Optional<Facet> getDefaultFacet(URI needUri) throws NoSuchNeedException;

  /**
   * Get the specified facet or the default facet, or any of the facets, or throw a runtime exception.
   *
   * @param needURI
   * @param facetUri
   * @return
   * @throws NoSuchNeedException
   * @throws IllegalArgumentException
   */
  public Facet getFacet(URI needURI, Optional<URI> facetUri) throws IllegalArgumentException, NoSuchNeedException;

  Connection getConnection(List<Connection> connections, URI facetURI, ConnectionEventType eventType)
      throws ConnectionAlreadyExistsException;

  public Connection createConnection(final URI connectionURI, final URI needURI, final URI otherNeedURI,
      final URI otherConnectionURI, final URI facetURI, final URI facetTypeURI, final URI remoteFacetURI,
      final ConnectionState connectionState, final ConnectionEventType connectionEventType)
      throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException;

  Connection nextConnectionState(URI connectionURI, ConnectionEventType connectionEventType)
      throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

  Connection nextConnectionState(Connection con, ConnectionEventType connectionEventType)
      throws IllegalMessageForConnectionStateException;

  /**
   * Adds feedback, represented by the subgraph reachable from feedback, to the RDF description of the
   * item identified by forResource
   *
   * @param connection
   * @param feedback
   * @return true if feedback could be added false otherwise
   */
  boolean addFeedback(Connection connection, Resource feedback);

  void updateRemoteConnectionURI(Connection con, URI remoteConnectionURI);

  void setURIService(URIService URIService);

}
