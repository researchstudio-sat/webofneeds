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
import com.hp.hpl.jena.rdf.model.Model;
import won.protocol.exception.IllegalNeedContentException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.model.Match;

import java.net.URI;
import java.util.Collection;

/**
 * User: fkleedorfer
 * Date: 05.11.12
 */
public interface NeedManagementService
{
  /**
   * Creates a new need with the specified content, ownerURI and active state.
   *
   * @param ownerURI
   * @param content
   * @param activate
   * @return the URI of the newly created need
   */
  public URI createNeed(final URI ownerURI, Model content, final boolean activate) throws IllegalNeedContentException;

  /**
   * Activates the need object.
   *
   * @param needURI
   * @throws won.protocol.exception.NoSuchNeedException if needURI does not refer to an existing need
   */
  public void activate(URI needURI) throws NoSuchNeedException;

  /**
   * Deactivates the need object, closing all its established connections.
   *
   * @param needURI
   * @throws NoSuchNeedException if needURI does not refer to an existing need
   */
  public void deactivate(URI needURI) throws NoSuchNeedException;
}
