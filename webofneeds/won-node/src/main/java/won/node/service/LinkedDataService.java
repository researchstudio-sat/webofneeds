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

package won.node.service;

import com.hp.hpl.jena.rdf.model.Model;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;

import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 26.11.12
 */
public interface LinkedDataService
{
  /**
   * Returns a model containing all need URIs.
   * If page >= 0, paging is used and the respective page is returned.
   * @param page
   * @return
   */
  public Model listNeedURIs(final int page);

  /**
   * Returns a model containing all connection URIs.
   * If page >= 0, paging is used and the respective page is returned.
   * @param page
   * @return
   */
  public Model listConnectionURIs(final int page);


  /**
   * Returns a model describing the need with the specified URI.
   * @param needUri
   * @return
   * @throws NoSuchNeedException
   */
  public Model getNeedModel(final URI needUri) throws NoSuchNeedException;

  /**
   * Returns a model describing the connection with the specified URI.
   * @param connectionUri
   * @return
   * @throws NoSuchConnectionException
   */
  public Model getConnectionModel(final URI connectionUri) throws NoSuchConnectionException;


  /**
   * Returns a model containing all connection uris belonging to the specified need.
   * If page >=0, paging is used and the respective page is returned.
   * @param page
   * @param needURI
   * @return
   * @throws NoSuchNeedException
   */
  public Model listConnectionURIs(final int page, final URI needURI) throws NoSuchNeedException;
}
