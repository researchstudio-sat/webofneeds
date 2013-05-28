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
import won.protocol.exception.*;

import java.net.URI;

/**
 * Interface defining methods for connection manipulation. These are the same from the owner side as from the node side.
 */
public interface ConnectionCommunicationService
{

  /**
   * Opens a connection identified by connectionURI. A rdf graph can be sent along with the request.
   *
   * @param connectionURI the URI of the connection
   * @param content a rdf graph describing properties of the event
   * @throws NoSuchConnectionException if connectionURI does not refer to an existing connection
   * @throws IllegalMessageForConnectionStateException if the message is not allowed in the current state of the connection
   */
  public void open(URI connectionURI, Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

  /**
   * Closes the connection identified by the specified URI.
   *
   * @param connectionURI the URI of the connection
   * @param content a rdf graph describing properties of the event
   * @throws NoSuchConnectionException if connectionURI does not refer to an existing connection
   * @throws IllegalMessageForConnectionStateException if the message is not allowed in the current state of the connection
   */
  public void close(URI connectionURI, Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

  /**
   * Sends a chat message via the local connection identified by the specified connectionURI
   * to the remote partner.
   *
   * @param connectionURI the local connection
   * @param message       the chat message
   * @throws NoSuchConnectionException if connectionURI does not refer to an existing connection
   * @throws IllegalMessageForConnectionStateException if the message is not allowed in the current state of the connection
   */
  public void textMessage(URI connectionURI, String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException;

}
