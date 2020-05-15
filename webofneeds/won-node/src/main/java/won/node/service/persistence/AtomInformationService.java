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
package won.node.service.persistence;

import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;

import org.apache.jena.rdf.model.Model;
import org.springframework.data.domain.Slice;

import won.protocol.exception.NoSuchAtomException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.message.WonMessageType;
import won.protocol.model.*;

/**
 * Service for obtaining information about atoms and connections in the system
 * in RDF.
 */
public interface AtomInformationService {
    /**
     * Retrieves a list of all atoms on the atomserver.
     *
     * @return a collection of all atom URIs.
     */
    Collection<URI> listAtomURIs();

    /**
     * Retrieves a list of all atoms with the given atomState on the atomserver.
     * 
     * @param atomState State that an atom needs to have to be included.
     * @return a collection of all atom URIs.
     */
    Collection<URI> listAtomURIs(AtomState atomState);

    /**
     * Retrieves a page of the list of atoms on the atomserver that have a given
     * state with number of atom uris per page preference.
     *
     * @param page the page number
     * @param preferredSize preferred number of members per page, null {@literal =>}
     * use default
     * @param atomState Active/Inactive, null {@literal =>} all states
     * @return a collection of all atom URIs.
     */
    Slice<URI> listPagedAtomURIs(int page, Integer preferredSize, AtomState atomState);

    /**
     * Retrieves list of atoms on the atomserver that where created earlier than the
     * given atom that have a given state with number of atom uris per page
     * preference.
     *
     * @param atom
     * @param preferredSize preferred number of members per page, null {@literal =>}
     * use default
     * @param atomState Active/Inactive, null {@literal =>} all states
     * @return a collection of all atom URIs.
     */
    Slice<URI> listPagedAtomURIsBefore(URI atom, Integer preferredSize, AtomState atomState);

    /**
     * Retrieves list of atoms on the atomserver that where created later than the
     * given atom that have a given state with number of atom uris per page
     * preference.
     *
     * @param atom
     * @param preferredSize preferred number of members per page, null {@literal =>}
     * use default
     * @param atomState Active/Inactive, null {@literal =>} all states
     * @return a collection of all atom URIs.
     */
    Slice<URI> listPagedAtomURIsAfter(URI atom, Integer preferredSize, AtomState atomState);

    /**
     * retrieves atoms that have been modified after a certain date
     *
     * @param modifiedAfter modification date of the atoms to retrieve
     * @param atomState filterBy
     * @return collection of modified atoms
     */
    Collection<URI> listAtomURIsModifiedAfter(Date modifiedAfter, AtomState atomState);

    /**
     * retrieves atoms that have been modified after a certain date
     *
     * @param createdAfter modification date of the atoms to retrieve
     * @param atomState filterBy
     * @return collection of modified atoms
     */
    Collection<URI> listAtomURIsCreatedAfter(Date createdAfter, AtomState atomState);

    /**
     * Retrieves all connection URIs (regardless of state).
     *
     * @return a collection of connection URIs.
     */
    Collection<URI> listConnectionURIs();

    /**
     * Retrieves all connections (regardless of state).
     *
     * @return a collection of connections.
     */
    Collection<Connection> listConnections();

    /**
     * Retrieves the connection identified by the specified sockets.
     *
     * @return a collection of connections.
     */
    Optional<Connection> getConnection(URI socket, URI targetSocket);

    /**
     * Retrieves all connections that were modified (by adding events) after a
     * certain date
     *
     * @param modifiedAfter modification date
     * @return
     */
    Collection<Connection> listModifiedConnectionsAfter(Date modifiedAfter);

    /**
     * Retrieves slice of the connection URIs list for a given page number
     *
     * @param page the page number
     * @param preferredSize preferred number of members per page or null; null
     * {@literal =>} use default
     * @param timeSpot time at which we want the list state to be fixed, if null -
     * current state
     * @return a slice connection URIs.
     */
    Slice<URI> listConnectionURIs(int page, Integer preferredSize, Date timeSpot);

    /**
     * Retrieves slice of the connection URIs list for a given page number
     *
     * @param page the page number
     * @param preferredSize preferred number of members per page or null; null
     * {@literal =>} use default
     * @param timeSpot time at which we want the list state to be fixed, if null -
     * current state
     * @return a slice connection URIs.
     */
    Slice<Connection> listConnections(int page, Integer preferredSize, Date timeSpot);

    /**
     * Retrieves slice of the connections that precede the given connection URI from
     * the point of view of their latest events.
     *
     * @param resumeConnURI the returned slice connections precede (in time of their
     * latest events) this connection uri
     * @param preferredPageSize preferred number of members per page or null; null
     * {@literal =>} use default
     * @param timeSpot time at which we want the list state to be fixed, cannot be
     * null
     * @return a slice of connection URIs.
     */
    Slice<Connection> listConnectionsBefore(final URI resumeConnURI, final Integer preferredPageSize,
                    final Date timeSpot);

    /**
     * Retrieves slice of the connections that follows the given connection URI from
     * the point of view of their latest events.
     *
     * @param resumeConnURI the returned slice connections follow (in time of their
     * latest events) this connection uri
     * @param preferredPageSize preferred number of members per page or null; null
     * {@literal =>} use default
     * @param timeSpot time at which we want the list state to be fixed, cannot be
     * null
     * @return a slice of connection URIs.
     */
    Slice<Connection> listConnectionsAfter(final URI resumeConnURI, final Integer preferredPageSize,
                    final Date timeSpot);

    /**
     * Retrieves all connection URIs (regardless of state) for the specified local
     * atom URI.
     *
     * @param atomURI the URI of the atom
     * @return a collection of connection URIs.
     * @throws won.protocol.exception.NoSuchAtomException if atomURI is not a known
     * atom URI
     */
    Collection<URI> listConnectionURIs(URI atomURI) throws NoSuchAtomException;

    /**
     * Retrieves all connections (regardless of state) for the specified local atom
     * URI.
     *
     * @param atomURI the URI of the atom
     * @param filterByConnectionState if not null, only return connections with the
     * given connectionState
     * @return a collection of connections.
     * @throws won.protocol.exception.NoSuchAtomException if atomURI is not a known
     * atom URI
     */
    Collection<Connection> listConnections(URI atomURI, ConnectionState filterByConnectionState)
                    throws NoSuchAtomException;

    /**
     * Retrieves slice of the list of connection URIs for the specified local atom
     * URI.
     *
     * @param atomURI the URI of the atom
     * @param page the page number
     * @param preferredSize preferred number of members per page or null; null
     * {@literal =>} use default
     * @param messageType event type that should be used for defining connection
     * latest activity; null {@literal =>} all event types
     * @param timeSpot time at which we want the list state to be fixed, if null -
     * current state
     * @return a collection of connection URIs.
     */
    Slice<URI> listConnectionURIs(URI atomURI, int page, Integer preferredSize, WonMessageType messageType,
                    Date timeSpot);

    /**
     * Retrieves slice of the list of connections for the specified local atom URI.
     *
     * @param atomURI the URI of the atom
     * @param page the page number
     * @param preferredSize preferred number of members per page or null; null
     * {@literal =>} use default
     * @param messageType event type that should be used for defining connection
     * latest activity; null {@literal =>} all event types
     * @param timeSpot time at which we want the list state to be fixed, if null -
     * current state
     * @param filterByConnectionState if not null, only return connections with the
     * given connectionState
     * @return a collection of connections.
     */
    Slice<Connection> listConnections(URI atomURI, int page, Integer preferredSize, WonMessageType messageType,
                    Date timeSpot, ConnectionState filterByConnectionState);

    /**
     * Retrieves slice of the connections for the specified local atom URI that
     * precede the given connection URI from the point of view of their latest
     * events.
     *
     * @param atomURI
     * @param resumeConnURI the returned slice connections precede (in time of their
     * latest events) this connection uri
     * @param preferredPageSize preferred number of members per page or null; null
     * {@literal =>} use default
     * @param messageType event type that should be used for defining connection
     * latest activity; null {@literal =>} all event types
     * @param timeSpot time at which we want the list state to be fixed, cannot be
     * null
     * @param filterByConnectionState if not null, only return connections with the
     * given connectionState
     * @return a slice of connection URIs.
     */
    Slice listConnectionsBefore(URI atomURI, URI resumeConnURI, Integer preferredPageSize,
                    WonMessageType messageType, Date timeSpot, ConnectionState filterByConnectionState);

    /**
     * Retrieves slice of the connections that follows the given connection URI from
     * the point of view of their latest events.
     *
     * @param atomURI
     * @param resumeConnURI the returned slice connections follow (in time of their
     * latest events) this connection uri
     * @param preferredPageSize preferred number of members per page or null; null
     * {@literal =>} use default
     * @param messageType event type that should be used for defining connection
     * latest activity; null {@literal =>} all event types
     * @param timeSpot time at which we want the list state to be fixed, cannot be
     * null
     * @param filterByConnectionState if not null, only return connections with the
     * given connectionState
     * @return a slice of connection URIs.
     */
    Slice listConnectionsAfter(URI atomURI, URI resumeConnURI, Integer preferredPageSize,
                    WonMessageType messageType, Date timeSpot, ConnectionState filterByConnectionState);

    /**
     * Read general information about the atom.
     *
     * @param atomURI
     * @return
     * @throws NoSuchAtomException
     */
    Atom readAtom(URI atomURI) throws NoSuchAtomException;

    /**
     * read atom data including if atom version number is equal to etag
     *
     * @param atomURI describes the atom to lookup
     * @param etag describes the version of the data to look up
     * @return atom data with corresponding etag version number or null if no atom
     * with the specified URI or the version number of the etag is found
     * @throws NoSuchAtomException
     */
    DataWithEtag<Atom> readAtom(URI atomURI, String etag) throws NoSuchAtomException;

    /**
     * Retrieves the public description of the atom as an RDF graph.
     *
     * @param atomURI
     * @return
     * @throws NoSuchAtomException
     */
    Model readAtomContent(URI atomURI) throws NoSuchAtomException;

    /**
     * Read general information about the connection.
     *
     * @param connectionURI
     * @return
     */
    Connection readConnection(URI connectionURI) throws NoSuchConnectionException;

    /**
     * read connection data including etag if connection version number is equal to
     * etag
     *
     * @param connectionURI describes the connection to lookup
     * @param Etag describes the version of the data to look up
     * @return connection data with corresponding etag version number or null if no
     * atom with the specified URI or the version number of the etag is found
     */
    DataWithEtag<Connection> readConnection(URI connectionURI, String Etag);

    /**
     * Retrieves the public description of the connection as an RDF graph.
     *
     * @param connectionURI
     * @return
     */
    Model readConnectionContent(URI connectionURI) throws NoSuchConnectionException;

    Slice<MessageEvent> listConnectionEvents(URI connectionUri, int page, Integer preferredPageSize,
                    WonMessageType messageType);

    Slice<MessageEvent> listConnectionEventsBefore(URI connectionUri, URI msgURI,
                    Integer preferredPageSize, WonMessageType msgType);

    Slice<MessageEvent> listConnectionEventsAfter(URI connectionUri, URI msgURI,
                    Integer preferredPageSize, WonMessageType msgType);

    class PagedResource<T, E> {
        private T content;
        private E resumeBefore = null;
        private E resumeAfter = null;

        public PagedResource(final T content) {
            this.content = content;
        }

        public PagedResource(final T content, final E resumeBefore, final E resumeAfter) {
            this.content = content;
            this.resumeBefore = resumeBefore;
            this.resumeAfter = resumeAfter;
        }

        public T getContent() {
            return content;
        }

        public boolean hasNext() {
            return resumeAfter != null;
        }

        public E getResumeAfter() {
            return this.resumeAfter;
        }

        public boolean hasPrevious() {
            return resumeBefore != null;
        }

        public E getResumeBefore() {
            return resumeBefore;
        }
    }
}
