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
package won.protocol.service;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.springframework.context.NoSuchMessageException;
import won.protocol.exception.NoSuchAtomException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.message.WonMessageType;
import won.protocol.model.AtomState;
import won.protocol.model.Connection;
import won.protocol.model.DataWithEtag;

import java.net.URI;
import java.util.Collection;
import java.util.Date;

/**
 * User: fkleedorfer Date: 26.11.12
 */
public interface LinkedDataService {
    /**
     * Returns a model containing all atom URIs.
     *
     * @return
     */
    public Dataset listAtomURIs();

    /**
     * Returns a model containing all atom URIs with the given atomState.
     *
     * @param atomState State that an atom needs to have to be included.
     * @return
     */
    public Dataset listAtomURIs(AtomState atomState);

    /**
     * Returns a model containing all atom URIs. If page >= 0, paging is used and
     * the respective page is returned.
     *
     * @param page
     * @return
     */
    public AtomInformationService.PagedResource<Dataset, URI> listAtomURIs(final int page);

    /**
     * Returns a model containing all atom URIs that are in the specified state. If
     * page >= 0, paging is used and the respective page is returned.
     *
     * @param page
     * @param preferedSize preferred number of atom uris per page (null means use
     * default)
     * @param atomState
     * @return
     */
    public AtomInformationService.PagedResource<Dataset, URI> listAtomURIs(final int page, final Integer preferedSize,
                    AtomState atomState);

    /**
     * Return all atom URIs that where created before the provided atom
     *
     * @param atom
     * @return
     */
    public AtomInformationService.PagedResource<Dataset, URI> listAtomURIsBefore(final URI atom);

    /**
     * Return all atom URIs that where created after the provided atom
     *
     * @param atom
     * @return
     */
    public AtomInformationService.PagedResource<Dataset, URI> listAtomURIsAfter(final URI atom);

    /**
     * Return all atom URIs that where created after the provided atom and that are
     * in the specified state
     *
     * @param atom
     * @param preferedSize preferred number of atom uris per page (null means use
     * default)
     * @param atomState
     * @return
     */
    public AtomInformationService.PagedResource<Dataset, URI> listAtomURIsBefore(final URI atom,
                    final Integer preferedSize, AtomState atomState);

    /**
     * Return all atom URIs that where created after the provided atom and that are
     * in the specified state
     *
     * @param atom
     * @param preferedSize preferred number of atom uris per page (null means use
     * default)
     * @param atomState
     * @return
     */
    public AtomInformationService.PagedResource<Dataset, URI> listAtomURIsAfter(final URI atom,
                    final Integer preferedSize, AtomState atomState);

    /**
     * Returns container dataset containing all atoms that have been modified after
     * a certain date
     *
     * @param modifiedDate modification date of atoms
     * @param atomState filter by atomState
     * @return
     */
    public Dataset listModifiedAtomURIsAfter(Date modifiedDate, AtomState atomState);

    /**
     * Returns container dataset containing all connections. If deep is true, the
     * resource data of those connection uris is also part of the returned resource.
     *
     * @param deep
     * @return
     * @throws NoSuchConnectionException only in case deep is set to true and
     * connection data for a member connection uri cannot be retrieved.
     */
    public AtomInformationService.PagedResource<Dataset, Connection> listConnections(final boolean deep)
                    throws NoSuchConnectionException;

    /**
     * Returns container dataset containing all connection URIs that where modified
     * after a certain date.
     *
     * @param modifiedAfter modification date
     * @param deep If deep is true, the resource data of those connection uris is
     * also part of the returned resource.
     * @return
     * @throws NoSuchConnectionException
     */
    public AtomInformationService.PagedResource<Dataset, Connection> listModifiedConnectionsAfter(Date modifiedAfter,
                    boolean deep) throws NoSuchConnectionException;

    /**
     * Returns a resource containing connections at given page. If deep is true, the
     * resource data of those connection uris is also part of the returned resource.
     * If page >0, paging is used and the respective page is returned.
     *
     * @param page
     * @param preferredSize preferred number of connection uris per page (null means
     * use default)
     * @param deep
     * @return
     * @throws NoSuchAtomException
     * @throws NoSuchConnectionException
     */
    public AtomInformationService.PagedResource<Dataset, Connection> listConnections(final int page,
                    final Integer preferredSize, Date timeSpot, final boolean deep) throws NoSuchConnectionException;

    /**
     * Returns a resource containing connection uris that precede (by time of their
     * latest event activities) the given connection as of state that was at the
     * specified time.
     *
     * @param beforeConnURI a connection the preceding connections of which we are
     * interested in
     * @param preferredSize preferred number of connection uris per page (null means
     * use default)
     * @param timeSpot date and time that specifies the connections and events state
     * of interest
     * @param deep if true, the resource data of those connection uris is also part
     * of the resource
     * @return
     * @throws NoSuchAtomException
     * @throws NoSuchConnectionException only in case deep is set to true and
     * connection data for a member connection uri cannot be retrieved.
     */
    public AtomInformationService.PagedResource<Dataset, Connection> listConnectionsBefore(URI beforeConnURI,
                    final Integer preferredSize, Date timeSpot, final boolean deep) throws NoSuchConnectionException;

    /**
     * Returns a resource containing connections that follow (by time of their
     * latest event activities) the given connection as of state that was at the
     * specified time.
     *
     * @param afterConnURI a connection the following connections of which we are
     * interested in
     * @param preferredSize preferred number of connection uris per page (null means
     * use default)
     * @param timeSpot date and time that specifies the connections and events state
     * of interest
     * @param deep if true, the resource data of those connection uris is also part
     * of the resource
     * @return
     * @throws NoSuchAtomException
     * @throws NoSuchConnectionException only in case deep is set to true and
     * connection data for a member connection uri cannot be retrieved.
     */
    public AtomInformationService.PagedResource<Dataset, Connection> listConnectionsAfter(URI afterConnURI,
                    final Integer preferredSize, Date timeSpot, final boolean deep) throws NoSuchConnectionException;

    /**
     * Returns a model containing all connection uris belonging to the specified
     * atom.
     *
     * @param atomURI
     * @param addMetadata - if true, a metadata graph is added to the dataset
     * containing counts by connection state
     * @param deep - if true, connection data is added (not only connection URIs)
     * @return
     * @throws NoSuchAtomException
     */
    public AtomInformationService.PagedResource<Dataset, Connection> listConnections(final URI atomURI, boolean deep,
                    final boolean addMetadata) throws NoSuchAtomException, NoSuchConnectionException;

    /**
     * Returns paged resource containing all connections belonging to the specified
     * atom.
     *
     * @param page number
     * @param preferredSize preferred number of connection uris per page (null means
     * use default)
     * @param atomURI local atom the connections of which are retrieved
     * @param messageType the event type that should be used for defining connection
     * latest activity; null => all event types
     * @param timeSpot time at which we want the list state to be fixed
     * @param deep if true, the resource data of those connection uris is also part
     * of the resource
     * @param addMetadata - if true, a metadata graph is added to the dataset
     * containing counts by connection state
     * @return
     * @throws NoSuchAtomException when specified atom is not found
     * @throws NoSuchConnectionException only in case deep is set to true and
     * connection data for a member connection uri cannot be retrieved.
     */
    public AtomInformationService.PagedResource<Dataset, Connection> listConnections(int page, URI atomURI,
                    Integer preferredSize, WonMessageType messageType, Date timeSpot, boolean deep, boolean addMetadata)
                    throws NoSuchAtomException, NoSuchConnectionException;

    /**
     * Returns paged resource containing all connections belonging to the specified
     * atom that precede the given connection URI from the point of view of their
     * latest events.
     *
     * @param atomURI local atom the connections of which are retrieved
     * @param resumeConnURI the returned slice connections precede (in time of their
     * latest events) this connection uri
     * @param preferredSize preferred number of connection uris per page (null means
     * use default)
     * @param messageType the event type that should be used for defining connection
     * latest activity; null => all event types
     * @param timeSpot time at which we want the list state to be fixed
     * @param deep if true, the resource data of those connection uris is also part
     * of the resource
     * @param addMetadata - if true, a metadata graph is added to the dataset
     * containing counts by connection state
     * @return
     * @throws NoSuchAtomException when specified atom is not found
     * @throws NoSuchConnectionException only in case deep is set to true and
     * connection data for a member connection uri cannot be retrieved.
     */
    public AtomInformationService.PagedResource<Dataset, Connection> listConnectionsBefore(URI atomURI,
                    URI resumeConnURI, Integer preferredSize, WonMessageType messageType, Date timeSpot, boolean deep,
                    boolean addMetadata) throws NoSuchAtomException, NoSuchConnectionException;

    /**
     * Returns paged resource containing all connections belonging to the specified
     * atom that follows the given connection URI from the point of view of their
     * latest events.
     *
     * @param atomURI local atom the connections of which are retrieved
     * @param resumeConnURI the returned slice connections follow (in time of their
     * latest events) this connection uri
     * @param preferredSize preferred number of connection uris per page (null means
     * use default)
     * @param messageType the event type that should be used for defining connection
     * latest activity; null => all event types
     * @param timeSpot time at which we want the list state to be fixed
     * @param deep if true, the resource data of those connection uris is also part
     * of the resource
     * @param addMetadata - if true, a metadata graph is added to the dataset
     * containing counts by connection state
     * @return
     * @throws NoSuchAtomException when specified atom is not found
     * @throws NoSuchConnectionException only in case deep is set to true and
     * connection data for a member connection uri cannot be retrieved.
     */
    public AtomInformationService.PagedResource<Dataset, Connection> listConnectionsAfter(URI atomURI,
                    URI resumeConnURI, Integer preferredSize, WonMessageType messageType, Date timeSpot, boolean deep,
                    boolean addMetadata) throws NoSuchAtomException, NoSuchConnectionException;

    /**
     * Returns a dataset describing the atom, if the etag indicates that it has
     * changed.
     *
     * @param atomUri
     * @param etag
     * @return dataset with etag describing the atom or null if not found
     */
    public DataWithEtag<Dataset> getAtomDataset(final URI atomUri, String etag);

    /**
     * Returns a dataset describing the atom with the specified URI. If the atom is
     * in state ACTIVE, 'deep' data is added if requested.
     *
     * @param atomUri
     * @param deep - include atom's connections datasets and each connection's
     * events' datasets
     * @param deepLayerSize - number of connections and events to include in the
     * deep atom dataset
     * @return
     * @throws NoSuchAtomException
     */
    public Dataset getAtomDataset(final URI atomUri, final boolean deep, final Integer deepLayerSize)
                    throws NoSuchAtomException, NoSuchConnectionException, NoSuchMessageException;

    /**
     * Returns a dataset describing the connection, if the etag indicates that it
     * has changed.
     *
     * @param connectionUri
     * @param includeMessageContainer
     * @param etag
     * @return
     * @throws NoSuchConnectionException
     */
    DataWithEtag<Dataset> getConnectionDataset(URI connectionUri, boolean includeMessageContainer, String etag);

    /**
     * Returns a dataset containing all event uris belonging to the specified
     * connection.
     *
     * @param connectionUri
     * @param deep - include events dataset
     * @return
     * @throws NoSuchConnectionException
     */
    public Dataset listConnectionEventURIs(final URI connectionUri, final boolean deep)
                    throws NoSuchConnectionException;

    /**
     * Returns paged resource containing all event uris belonging to the specified
     * connection. If deep is true, the event dataset is added to the result.
     *
     * @param connectionUri connection parent of the events
     * @param pageNum number of the page to be returned
     * @param preferedSize preferred number of uris per page, null means use default
     * @param messageType message type, null means all types
     * @param deep
     * @return
     * @throws NoSuchConnectionException
     */
    public AtomInformationService.PagedResource<Dataset, URI> listConnectionEventURIs(final URI connectionUri,
                    final int pageNum, Integer preferedSize, WonMessageType messageType, boolean deep)
                    throws NoSuchConnectionException;

    /**
     * Returns a dataset containing all event uris belonging to the specified
     * connection that were created after the specified event uri. If deep is true,
     * the event dataset is added to the result.
     *
     * @param connectionUri connection parent of the events
     * @param msgURI message to follow (in message creation time)
     * @param preferedSize preferred number of uris per page, null means use default
     * @param msgType message type, null means all types
     * @param deep
     * @return
     * @throws NoSuchConnectionException
     */
    public AtomInformationService.PagedResource<Dataset, URI> listConnectionEventURIsAfter(final URI connectionUri,
                    final URI msgURI, Integer preferedSize, WonMessageType msgType, boolean deep)
                    throws NoSuchConnectionException;

    /**
     * Returns a dataset containing all event uris belonging to the specified
     * connection that were created before the specified event uri. If deep is true,
     * the event dataset is added to the result.
     *
     * @param connectionUri connection parent of the events
     * @param msgURI message to precede (in message creation time)
     * @param preferedSize preferred number of uris per page, null means use default
     * @param msgType message type, null means all types
     * @param deep
     * @return
     * @throws NoSuchConnectionException
     */
    public AtomInformationService.PagedResource<Dataset, URI> listConnectionEventURIsBefore(final URI connectionUri,
                    final URI msgURI, Integer preferedSize, WonMessageType msgType, boolean deep)
                    throws NoSuchConnectionException;

    public Dataset getNodeDataset();

    /**
     * returns a dataset of the (message) event with the specified URI, with a value
     * that can be used for an etag. If the current etag is the same as the
     * specified one, the dataset is null, indicating no change.
     *
     * @param eventURI
     */
    public DataWithEtag<Dataset> getDatasetForUri(final URI eventURI, String etag);

    /**
     * Returns a model specifying the number of messages and their latest and
     * earliest timestamp found per connection after the specified
     * lastSeenMessageURI, or the total message count and respective timestamps for
     * a connection for which no lastSeenMessageURI is specified.
     *
     * @param atomURI
     * @param lastSeenMessageURIs
     * @return
     */
    Model getUnreadInformationForAtom(URI atomURI, Collection<URI> lastSeenMessageURIs);
}
