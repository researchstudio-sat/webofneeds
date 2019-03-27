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

import java.net.URI;
import java.util.Collection;
import java.util.Date;

import org.apache.jena.rdf.model.Model;
import org.springframework.data.domain.Slice;

import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.message.WonMessageType;
import won.protocol.model.Connection;
import won.protocol.model.DataWithEtag;
import won.protocol.model.MessageEventPlaceholder;
import won.protocol.model.Need;
import won.protocol.model.NeedState;

/**
 * Service for obtaining information about needs and connections in the system
 * in RDF.
 */
public interface NeedInformationService {
    /**
     * Retrieves a list of all needs on the needserver.
     *
     * @return a collection of all need URIs.
     */
    public Collection<URI> listNeedURIs();

    /**
     * Retrieves a page of the list of needs on the needserver that have a given
     * state with number of need uris per page preference.
     *
     * @param page the page number
     * @param preferredSize preferred number of members per page, null => use
     * default
     * @param needState Active/Inactive, null => all states
     * @return a collection of all need URIs.
     */
    public Slice<URI> listNeedURIs(int page, Integer preferredSize, NeedState needState);

    /**
     * Retrieves list of needs on the needserver that where created earlier than the
     * given need that have a given state with number of need uris per page
     * preference.
     *
     * @param need
     * @param preferredSize preferred number of members per page, null => use
     * default
     * @param needState Active/Inactive, null => all states
     * @return a collection of all need URIs.
     */
    public Slice<URI> listNeedURIsBefore(URI need, Integer preferredSize, NeedState needState);

    /**
     * Retrieves list of needs on the needserver that where created later than the
     * given need that have a given state with number of need uris per page
     * preference.
     *
     * @param need
     * @param preferredSize preferred number of members per page, null => use
     * default
     * @param needState Active/Inactive, null => all states
     * @return a collection of all need URIs.
     */
    public Slice<URI> listNeedURIsAfter(URI need, Integer preferredSize, NeedState needState);

    /**
     * retrieves needs that have been modified after a certain date
     * 
     * @param modifiedAfter modification date of the needs to retrieve
     * @return collection of modified needs
     */
    public Collection<URI> listModifiedNeedURIsAfter(Date modifiedAfter);

    /**
     * Retrieves all connection URIs (regardless of state).
     *
     * @return a collection of connection URIs.
     */
    public Collection<URI> listConnectionURIs();

    /**
     * Retrieves all connections (regardless of state).
     *
     * @return a collection of connections.
     */
    public Collection<Connection> listConnections();

    /**
     * Retrieves all connections that were modified (by adding events) after a
     * certain date
     *
     * @param modifiedAfter modification date
     * @return
     */
    public Collection<Connection> listModifiedConnectionsAfter(Date modifiedAfter);

    /**
     * Retrieves slice of the connection URIs list for a given page number
     *
     * @param page the page number
     * @param preferredSize preferred number of members per page or null; null =>
     * use default
     * @param timeSpot time at which we want the list state to be fixed, if null -
     * current state
     * @return a slice connection URIs.
     */
    public Slice<URI> listConnectionURIs(int page, Integer preferredSize, Date timeSpot);

    /**
     * Retrieves slice of the connection URIs list for a given page number
     *
     * @param page the page number
     * @param preferredSize preferred number of members per page or null; null =>
     * use default
     * @param timeSpot time at which we want the list state to be fixed, if null -
     * current state
     * @return a slice connection URIs.
     */
    public Slice<Connection> listConnections(int page, Integer preferredSize, Date timeSpot);

    /**
     * Retrieves slice of the connections that precede the given connection URI from
     * the point of view of their latest events.
     *
     * @param resumeConnURI the returned slice connections precede (in time of their
     * latest events) this connection uri
     * @param preferredPageSize preferred number of members per page or null; null
     * => use default
     * @param timeSpot time at which we want the list state to be fixed, cannot be
     * null
     * @return a slice of connection URIs.
     */
    public Slice<Connection> listConnectionsBefore(final URI resumeConnURI, final Integer preferredPageSize,
                    final Date timeSpot);

    /**
     * Retrieves slice of the connections that follows the given connection URI from
     * the point of view of their latest events.
     *
     * @param resumeConnURI the returned slice connections follow (in time of their
     * latest events) this connection uri
     * @param preferredPageSize preferred number of members per page or null; null
     * => use default
     * @param timeSpot time at which we want the list state to be fixed, cannot be
     * null
     * @return a slice of connection URIs.
     */
    public Slice<Connection> listConnectionsAfter(final URI resumeConnURI, final Integer preferredPageSize,
                    final Date timeSpot);

    /**
     * Retrieves all connection URIs (regardless of state) for the specified local
     * need URI.
     *
     * @param needURI the URI of the need
     * @return a collection of connection URIs.
     * @throws won.protocol.exception.NoSuchNeedException if needURI is not a known
     * need URI
     */
    public Collection<URI> listConnectionURIs(URI needURI) throws NoSuchNeedException;

    /**
     * Retrieves all connections (regardless of state) for the specified local need
     * URI.
     *
     * @param needURI the URI of the need
     * @return a collection of connections.
     * @throws won.protocol.exception.NoSuchNeedException if needURI is not a known
     * need URI
     */
    public Collection<Connection> listConnections(URI needURI) throws NoSuchNeedException;

    /**
     * Retrieves slice of the list of connection URIs for the specified local need
     * URI.
     *
     * @param needURI the URI of the need
     * @param page the page number
     * @param preferredSize preferred number of members per page or null; null =>
     * use default
     * @param messageType event type that should be used for defining connection
     * latest activity; null => all event types
     * @param timeSpot time at which we want the list state to be fixed, if null -
     * current state
     * @return a collection of connection URIs.
     * @throws won.protocol.exception.NoSuchNeedException if needURI is not a known
     * need URI
     */
    public Slice<URI> listConnectionURIs(URI needURI, int page, Integer preferredSize, WonMessageType messageType,
                    Date timeSpot);

    /**
     * Retrieves slice of the list of connections for the specified local need URI.
     *
     * @param needURI the URI of the need
     * @param page the page number
     * @param preferredSize preferred number of members per page or null; null =>
     * use default
     * @param messageType event type that should be used for defining connection
     * latest activity; null => all event types
     * @param timeSpot time at which we want the list state to be fixed, if null -
     * current state
     * @return a collection of connections.
     * @throws won.protocol.exception.NoSuchNeedException if needURI is not a known
     * need URI
     */
    public Slice<Connection> listConnections(URI needURI, int page, Integer preferredSize, WonMessageType messageType,
                    Date timeSpot);

    /**
     * Retrieves slice of the connections for the specified local need URI that
     * precede the given connection URI from the point of view of their latest
     * events.
     *
     * @param resumeConnURI the returned slice connections precede (in time of their
     * latest events) this connection uri
     * @param preferredPageSize preferred number of members per page or null; null
     * => use default
     * @param messageType event type that should be used for defining connection
     * latest activity; null => all event types
     * @param timeSpot time at which we want the list state to be fixed, cannot be
     * null
     * @return a slice of connection URIs.
     */
    public Slice listConnectionsBefore(URI needURI, URI resumeConnURI, Integer preferredPageSize,
                    WonMessageType messageType, Date timeSpot);

    /**
     * Retrieves slice of the connections that follows the given connection URI from
     * the point of view of their latest events.
     *
     * @param resumeConnURI the returned slice connections follow (in time of their
     * latest events) this connection uri
     * @param preferredPageSize preferred number of members per page or null; null
     * => use default
     * @param messageType event type that should be used for defining connection
     * latest activity; null => all event types
     * @param timeSpot time at which we want the list state to be fixed, cannot be
     * null
     * @return a slice of connection URIs.
     */
    public Slice listConnectionsAfter(URI needURI, URI resumeConnURI, Integer preferredPageSize,
                    WonMessageType messageType, Date timeSpot);

    /**
     * Read general information about the need.
     *
     * @param needURI
     * @return
     * @throws NoSuchNeedException
     */
    public Need readNeed(URI needURI) throws NoSuchNeedException;

    /**
     * read need data including if need version number is equal to etag
     *
     * @param needURI describes the need to lookup
     * @param etag describes the version of the data to look up
     * @return need data with corresponding etag version number or null if no need
     * with the specified URI or the version number of the etag is found
     * @throws NoSuchNeedException
     */
    public DataWithEtag<Need> readNeed(URI needURI, String etag) throws NoSuchNeedException;

    /**
     * Retrieves the public description of the need as an RDF graph.
     *
     * @param needURI
     * @return
     * @throws NoSuchNeedException
     */
    public Model readNeedContent(URI needURI) throws NoSuchNeedException;

    /**
     * Read general information about the connection.
     *
     * @param connectionURI
     * @return
     * @throws NoSuchNeedException
     */
    public Connection readConnection(URI connectionURI) throws NoSuchConnectionException;

    /**
     * read connection data including etag if connection version number is equal to
     * etag
     *
     * @param connectionURI describes the connection to lookup
     * @param Etag describes the version of the data to look up
     * @return connection data with corresponding etag version number or null if no
     * need with the specified URI or the version number of the etag is found
     */
    public DataWithEtag<Connection> readConnection(URI connectionURI, String Etag);

    /**
     * Retrieves the public description of the connection as an RDF graph.
     *
     * @param connectionURI
     * @return
     * @throws NoSuchNeedException
     */
    public Model readConnectionContent(URI connectionURI) throws NoSuchConnectionException;

    public Slice<MessageEventPlaceholder> listConnectionEvents(URI connectionUri, int page, Integer preferredPageSize,
                    WonMessageType messageType);

    public Slice<MessageEventPlaceholder> listConnectionEventsBefore(URI connectionUri, URI msgURI,
                    Integer preferredPageSize, WonMessageType msgType);

    public Slice<MessageEventPlaceholder> listConnectionEventsAfter(URI connectionUri, URI msgURI,
                    Integer preferredPageSize, WonMessageType msgType);

    public static class PagedResource<T, E> {
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
