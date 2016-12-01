/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package won.protocol.service;

import com.hp.hpl.jena.rdf.model.Model;
import org.springframework.data.domain.Slice;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.message.WonMessageType;
import won.protocol.model.Connection;
import won.protocol.model.DataWithEtag;
import won.protocol.model.Need;
import won.protocol.model.NeedState;

import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Service for obtaining information about needs and connections in the system in RDF.
 */
public interface NeedInformationService {

    /**
     * Retrieves a list of all needs on the needserver.
     *
     * @return a collection of all need URIs.
     */
    public Collection<URI> listNeedURIs();

  /**
   * Retrieves a page of the list of needs on the needserver that have a given state
   * with number of need uris per page preference.
   *
   * @param page the page number
   * @param preferredSize preferred number of members per page, null => use default
   * @param needState Active/Inactive, null => all states
   * @return a collection of all need URIs.
   */
    public Slice<URI> listNeedURIs(int page, Integer preferredSize, NeedState needState);


  /**
   * Retrieves list of needs on the needserver that where created earlier than the given need
   * that have a given state with number of need uris per page preference.
   *
   * @param need
   * @param preferredSize preferred number of members per page, null => use default
   * @param needState Active/Inactive, null => all states
   * @return a collection of all need URIs.
   */
    public Slice<URI> listNeedURIsBefore(URI need, Integer preferredSize, NeedState needState);


  /**
   * Retrieves list of needs on the needserver that where created later than the given need
   * that have a given state with number of need uris per page preference.
   *
   * @param need
   * @param preferredSize preferred number of members per page, null => use default
   * @param needState Active/Inactive, null => all states
   * @return a collection of all need URIs.
   */
    public Slice<URI> listNeedURIsAfter(URI need, Integer preferredSize, NeedState needState);


    /**
     * Retrieves all connection URIs (regardless of state).
     *
     * @return a collection of connection URIs.
     */
    public Collection<URI> listConnectionURIs();

    /**
     * Retrieves slice of the connection URIs list for a given page number
     *
     * @param page the page number
     * @param preferredSize preferred number of members per page or null; null => use default
     * @param timeSpot time at which we want the list state to be fixed, if null - current state
     *
     * @return a slice connection URIs.
     */
    public Slice<URI> listConnectionURIs(int page, Integer preferredSize, Date timeSpot);

    /**
     * Retrieves slice of the connection URIs that precede the given connection URI from the point of view of their
     * latest events.
     *
     * @param resumeConnURI the returned slice connections precede (in time of their latest events) this connection uri
     * @param preferredPageSize preferred number of members per page or null; null => use default
     * @param timeSpot time at which we want the list state to be fixed, cannot be null
     *
     * @return a slice of connection URIs.
     */
    public Slice<URI> listConnectionURIsBefore(
      final URI resumeConnURI, final Integer preferredPageSize, final Date timeSpot);

    /**
     * Retrieves slice of the connection URIs that follows the given connection URI from the point of view of their
     * latest events.
     *
     * @param resumeConnURI the returned slice connections follow (in time of their latest events) this connection uri
     * @param preferredPageSize preferred number of members per page or null; null => use default
     * @param timeSpot time at which we want the list state to be fixed, cannot be null
     *
     * @return a slice of connection URIs.
     */
    public Slice<URI> listConnectionURIsAfter(
      final URI resumeConnURI, final Integer preferredPageSize, final Date timeSpot);


    /**
     * Retrieves all connection URIs (regardless of state) for the specified local need URI.
     *
     * @param needURI the URI of the need
     * @return a collection of connection URIs.
     * @throws won.protocol.exception.NoSuchNeedException
     *          if needURI is not a known need URI
     */
    public Collection<URI> listConnectionURIs(URI needURI) throws NoSuchNeedException;

    /**
     * Retrieves slice of the list of connection URIs for the specified local need URI.
     *
     * @param needURI the URI of the need
     * @param page the page number
     * @param preferredSize preferred number of members per page or null; null => use default
     * @param messageType event type that should be used for defining connection latest activity; null => all event
     *                    types
     * @param timeSpot time at which we want the list state to be fixed, if null - current state
     * @return a collection of connection URIs.
     * @throws won.protocol.exception.NoSuchNeedException
     *          if needURI is not a known need URI
     */
    public Slice<URI> listConnectionURIs(
      URI needURI, int page, Integer preferredSize, WonMessageType messageType, Date timeSpot);

    /**
     * Retrieves slice of the connection URIs  for the specified local need URI that precede the given connection URI
     * from the point of view of their latest events.
     *
     * @param resumeConnURI the returned slice connections precede (in time of their latest events) this connection uri
     * @param preferredPageSize preferred number of members per page or null; null => use default
     * @param messageType event type that should be used for defining connection latest activity; null => all event
     *                    types
     * @param timeSpot time at which we want the list state to be fixed, cannot be null
     *
     * @return a slice of connection URIs.
     */
    public Slice listConnectionURIsBefore(
      URI needURI, URI resumeConnURI, Integer preferredPageSize, WonMessageType messageType, Date timeSpot);

    /**
     * Retrieves slice of the connection URIs that follows the given connection URI from the point of view of their
     * latest events.
     *
     * @param resumeConnURI the returned slice connections follow (in time of their latest events) this connection uri
     * @param preferredPageSize preferred number of members per page or null; null => use default
     * @param messageType event type that should be used for defining connection latest activity; null => all event
     *                    types
     * @param timeSpot time at which we want the list state to be fixed, cannot be null
     *
     * @return a slice of connection URIs.
     */
    public Slice  listConnectionURIsAfter(
      URI needURI, URI resumeConnURI, Integer preferredPageSize, WonMessageType messageType, Date timeSpot);

    /**
     * Read general information about the need.
     *
     *
     *
     * @param needURI
     * @return
     * @throws NoSuchNeedException
     */
    public Need readNeed(URI needURI) throws NoSuchNeedException;

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
    public DataWithEtag<Connection> readConnection(URI connectionURI, String Etag);

    /**
     * Retrieves the public description of the connection as an RDF graph.
     *
     * @param connectionURI
     * @return
     * @throws NoSuchNeedException
     */
    public Model readConnectionContent(URI connectionURI) throws NoSuchConnectionException;

  /**
   * Retrieves list of event uris of the specified connection.
   *
   * @param connectionUri
   * @return a collection of all event URIs.
   */
    public List<URI> listConnectionEventURIs(URI connectionUri);


  /**
   * Retrieves list of event uris of the specified connection that have a given message type and that are on the
   * given page, taking into account number of uris per page preference.
   *
   * @param connectionUri
   * @param page
   * @param preferredPageSize preferred number of members per page, null => use default
   * @param messageType null => all types
   * @return a collection of all event URIs.
   */
    public Slice<URI> listConnectionEventURIs(
      URI connectionUri, int page, Integer preferredPageSize, WonMessageType messageType);


  /**
   * Retrieves list of event uris of the specified connection that where created earlier than the given
   * event (specified by event message uri) and  that have a given message type, with number of uris per page
   * preference.
   *
   * @param connectionUri
   * @param msgURI before which the messages should be returned (created before this message msgURI)
   * @param preferredPageSize preferred number of members per page, null => use default
   * @param msgType null => all types
   * @return a collection of all event URIs.
   */
    public Slice<URI> listConnectionEventURIsBefore(
      URI connectionUri, URI msgURI, Integer preferredPageSize, WonMessageType msgType);


  /**
   * Retrieves list of event uris of the specified connection that where created later than the given
   * event (specified by event message uri) and  that have a given message type, with number of uris per page
   * preference.
   *
   * @param connectionUri
   * @param msgURI after which the messages should be returned (created after this message msgURI)
   * @param preferredPageSize preferred number of members per page, null => use default
   * @param msgType null => all types
   * @return a collection of all event URIs.
   */
    public Slice<URI> listConnectionEventURIsAfter(
    URI connectionUri, URI msgURI, Integer preferredPageSize, WonMessageType msgType);


  @Deprecated
  public static class Page<T>{
    private Collection<T> content;
    private boolean hasNext;
    private T resumeBefore = null;
    private T resumeAfter = null;

    public Page(final Collection<T> content, final T resumeBefore, final T resumeAfter) {
      this.content = content;
      this.resumeBefore = resumeBefore;
      this.resumeAfter = resumeAfter;
    }

    public Collection<T> getContent() {
      return content;
    }

    public boolean hasNext() {
      return resumeAfter != null;
    }

    public T getResumeAfter() {
      return this.resumeAfter;
    }

    public boolean hasPrevious() {
      return resumeBefore != null;
    }

    public T getResumeBefore() {
      return resumeBefore;
    }
  }


  public static class PagedResource<T,E>{
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
