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

import com.hp.hpl.jena.graph.Graph;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.model.Connection;
import won.protocol.model.Need;

import java.net.URI;
import java.util.Collection;

/**
 * User: fkleedorfer
 * Date: 05.11.12
 */
public interface NeedInformationService
{
  /**
   * Retrieves a list of all needs on the needserver.
   * @return a collection of all need URIs.
   */
  public Collection<URI> listNeedURIs();


  /**
   * Retrieves all connection URIs (regardless of state) for the specified local need URI.
   * @param needURI the URI of the need
   * @throws won.protocol.exception.NoSuchNeedException if needURI is not a known need URI
   * @return a collection of connection URIs.
   */
  public Collection<URI> listConnectionURIs(URI needURI) throws NoSuchNeedException;

  /**
   * Read general information about the need.
   * @param needURI
   * @return
   * @throws NoSuchNeedException
   */
  public Need readNeed(URI needURI) throws NoSuchNeedException;

  /**
   * Retrieves the public description of the need as an RDF graph.
   * TODO: this is a simulation of the linked data interface, which we'll develop later
   * @param needURI
   * @return
   * @throws NoSuchNeedException
   */
  public Graph readNeedContent(URI needURI) throws NoSuchNeedException;

  /**
   * Read general information about the connection.
   *
   * @param connectionURI
   * @return
   * @throws NoSuchNeedException
   */
  public Connection readConnection(URI connectionURI) throws NoSuchConnectionException;


  /**
   * Retrieves the public description of the connection as an RDF graph.
   *
   * @param connectionURI
   * @return
   * @throws NoSuchNeedException
   */
  public Graph readConnectionContent(URI connectionURI) throws NoSuchConnectionException;

}
