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

package won.server.protocol;

import won.server.need.Match;

import java.net.URI;
import java.util.Collection;

/**
 *  Interface defining the external methods for the owner protocol on the need side, i.e. those methods
 *  that are directly or indirectly exposed via Web Service toward the end user application.
 *
 *  This interface contains the methods required for client-pull operations only. No pull operations are declared here.
 *
 *  This interface is need-oriented: all methods relate to one need. No bulk operations are declared here.
 *
 *  TODO: missing: how will the owner get the chat-messages? However this is solved, it should be declared in this interface.
 *
 */
public interface OwnerProtocolExternalFacade
{
  /**
   * Creates a new need with the specified content.
   * TODO replace String with the type used to hold the need content
   * @param content
   * @return the URI of the newly created need
   */
  public URI createNeed(String content);

  /**
   * Returns a representation of the need object with the specified URI.
   * TODO: replace String with the type used to hold the content.
   * @param needURI
   * @return a representation of the need.
   */
  public String readNeed(URI needURI);

  /**
   * Replace the content of the need with the specified URI with the specified content.
   * TODO replace String with the type used to hold the need content
   * @param content
   * @return the URI of the newly created need
   */
  public void updateNeed(URI needURI, String content);

  /**
   * Delete the need with the specified URI.
   * @param needURI the URI of the need to delete
   */
  public void deleteNeed(URI needURI);

  /**
   * Returns all matches for the specified need.
   * @param needURI the URI of the need to get the matches for
   * @return a collection of matches
   */
  public Collection<Match> getMatches(URI needURI);

  /**
   * Initiates a connection from the need identified by localNeedURI to the need identified by remoteNeedURI.
   * The former must be controlled by the local needserver, the latter may reside anywhere.
   * @param localNeedURI the local need to connect from
   * @param remoteNeedURI the remote need to connect to
   * @return an URI identifying the transaction
   */
  public URI connect(URI localNeedURI, URI remoteNeedURI);

  /**
   * Aborts the transaction identified by the specified URI, indicating failure.
   * @param transactionURI the URI that uniquely identifies the transaction
   */
  public void abort(URI transactionURI);

  /**
   * Closes the transaction identified by the specified URI, indicating success.
   * @param transactionURI the URI that uniquely identifies the transaction
   */
  public void close(URI transactionURI);

}
