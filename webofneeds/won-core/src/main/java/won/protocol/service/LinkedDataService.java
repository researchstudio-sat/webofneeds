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

import com.hp.hpl.jena.query.Dataset;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.message.WonMessageType;
import won.protocol.model.NeedState;

import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 26.11.12
 */
public interface LinkedDataService
{

  /**
   * Returns a model containing all need URIs.
   * @return
   */
  public Dataset listNeedURIs();

  /**
   * Returns a model containing all need URIs.
   * If page >= 0, paging is used and the respective page is returned.
   * @param page
   * @return
   */
  public NeedInformationService.PagedResource<Dataset,URI> listNeedURIs(final int page);


  /**
   * Returns a model containing all need URIs that are in the specified state.
   * If page >= 0, paging is used and the respective page is returned.
   * @param page
   * @param preferedSize preferred number of need uris per page (null means use default)
   * @param needState
   * @return
   */
  public NeedInformationService.PagedResource<Dataset,URI> listNeedURIs(
    final int page, final Integer preferedSize, NeedState needState);

  /**
   * Return all need URIs that where created before the provided need
   * @param need
   * @return
   */
  public NeedInformationService.PagedResource<Dataset,URI> listNeedURIsBefore(final URI need);

  /**
   * Return all need URIs that where created after the provided need
   * @param need
   * @return
   */
  public NeedInformationService.PagedResource<Dataset,URI> listNeedURIsAfter(final URI need);



  /**
   * Return all need URIs that where created after the provided need and that are in the specified state
   * @param need
   * @param preferedSize preferred number of need uris per page (null means use default)
   * @param needState
   * @return
   */
  public NeedInformationService.PagedResource<Dataset,URI> listNeedURIsBefore(
    final URI need, final Integer preferedSize, NeedState needState);

  /**
   * Return all need URIs that where created after the provided need and that are in the specified state
   * @param need
   * @param preferedSize preferred number of need uris per page (null means use default)
   * @param needState
   * @return
   */
  public NeedInformationService.PagedResource<Dataset,URI> listNeedURIsAfter(
    final URI need, final Integer preferedSize, NeedState needState);


  /**
   * Returns a model containing all connection URIs.
   * If page >= 0, paging is used and the respective page is returned.
   * @param page
   * @return
   */
  public Dataset listConnectionURIs(final int page);

  /**
   * Returns a dataset describing the need with the specified URI.
   * @param needUri
   * @return
   * @throws NoSuchNeedException
   */
  public Dataset getNeedDataset(final URI needUri) throws NoSuchNeedException;

  /**
   * Returns a model describing the connection with the specified URI.
   * @param connectionUri
   * @return
   * @throws NoSuchConnectionException
   */
  public Dataset getConnectionDataset(final URI connectionUri, boolean includeEventData) throws NoSuchConnectionException;

  /**
   * Returns a dataset containing all event uris belonging to the specified connection.
   * @param connectionUri
   * @return
   * @throws NoSuchConnectionException
   */
  public Dataset listConnectionEventURIs(final URI connectionUri) throws NoSuchConnectionException;

  /**
   * Returns a dataset containing all event uris belonging to the specified connection.
   * If page >= 0, paging is used and the respective page is returned.
   * @param connectionUri
   * @param pageNum
   * @return
   * @throws NoSuchConnectionException
   */
  public NeedInformationService.PagedResource<Dataset,URI> listConnectionEventURIs(
    final URI connectionUri, final int pageNum) throws NoSuchConnectionException;

  /**
   * Returns a dataset containing all event uris belonging to the specified connection and are of the specified type.
   * If page >= 0, paging is used and the respective page is returned. Paging is used, the preferred page size can be
   * specified.
   * @param connectionUri
   * @param pageNum
   * @param preferedSize preferred number of uris per page, null means use default
   * @param messageType message type, null means all types
   * @return
   * @throws NoSuchConnectionException
   */
  public NeedInformationService.PagedResource<Dataset,URI> listConnectionEventURIs(
    final URI connectionUri, final int pageNum, Integer preferedSize, WonMessageType messageType) throws
    NoSuchConnectionException;

  /**
   * Returns a dataset containing all event uris belonging to the specified connection that were created after the
   * specified event uri. Paging is used, the preferred page size can be specified
   * @param connectionUri
   * @param preferedSize preferred number of uris per page, null means use default
   * @param msgType message type, null means all types
   * @return
   * @throws NoSuchConnectionException
   */
  public NeedInformationService.PagedResource<Dataset,URI> listConnectionEventURIsAfter(
    final URI connectionUri, final URI msgURI, Integer preferedSize, WonMessageType msgType) throws
    NoSuchConnectionException;

  /**
   * Returns a dataset containing all event uris belonging to the specified connection that were created before the
   * specified event uri. Paging is used, the preferred page size can be specified
   * @param connectionUri
   * @param preferedSize preferred number of uris per page, null means use default
   * @param msgType message type, null means all types
   * @return
   * @throws NoSuchConnectionException
   */
  public NeedInformationService.PagedResource<Dataset,URI> listConnectionEventURIsBefore(
    final URI connectionUri, final URI msgURI, Integer preferedSize, WonMessageType msgType) throws
    NoSuchConnectionException;

  public Dataset getNodeDataset();

  /**
   * Returns a model containing all connection uris belonging to the specified need.
   * If page >=0, paging is used and the respective page is returned.
   * @param page
   * @param needURI
   * @return
   * @throws NoSuchNeedException
   */
  public Dataset listConnectionURIs(final int page, final URI needURI) throws NoSuchNeedException;

  /**
   * returns a dataset of the (message) event with the specified URI
   * @param eventURI
   */
  public Dataset getDatasetForUri(final URI eventURI);

}
