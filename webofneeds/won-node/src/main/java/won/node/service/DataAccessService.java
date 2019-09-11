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
package won.node.service;

import org.apache.jena.rdf.model.Resource;
import won.protocol.exception.*;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEventType;
import won.protocol.model.ConnectionState;
import won.protocol.model.Socket;

import java.net.URI;
import java.util.Optional;

/**
 * User: fkleedorfer Date: 18.04.14
 */
public interface DataAccessService {
    public Optional<Socket> getDefaultSocket(URI atomUri) throws NoSuchAtomException;

    /**
     * Get the specified socket or the default socket, or any of the sockets, or
     * throw a runtime exception.
     * 
     * @param atomURI
     * @param socketUri
     * @return
     * @throws NoSuchAtomException
     * @throws IllegalArgumentException
     */
    public Socket getSocket(URI atomURI, Optional<URI> socketUri) throws IllegalArgumentException, NoSuchAtomException;

    public Connection createConnection(final URI connectionURI, final URI atomURI, final URI otherAtomURI,
                    final URI otherConnectionURI, final URI socketURI, final URI socketTypeURI,
                    final URI targetSocketURI, final ConnectionState connectionState,
                    final ConnectionEventType connectionEventType)
                    throws NoSuchAtomException, IllegalMessageForAtomStateException, ConnectionAlreadyExistsException;

    Connection nextConnectionState(URI connectionURI, ConnectionEventType connectionEventType)
                    throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

    Connection nextConnectionState(Connection con, ConnectionEventType connectionEventType)
                    throws IllegalMessageForConnectionStateException;

    /**
     * Adds feedback, represented by the subgraph reachable from feedback, to the
     * RDF description of the item identified by forResource
     * 
     * @param connection
     * @param feedback
     * @return true if feedback could be added false otherwise
     */
    boolean addFeedback(Connection connection, Resource feedback);

    void updateTargetConnectionURI(Connection con, URI targetConnectionURI);
}
