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

package won.protocol.owner;

import com.hp.hpl.jena.rdf.model.Model;
import won.protocol.exception.ConnectionAlreadyExistsException;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.service.ConnectionCommunicationService;
import javax.jms.Message;

import java.net.URI;
import java.util.List;

/**
 * User: fkleedorfer
 * Date: 31.10.12
 */
public interface OwnerProtocolOwnerService extends ConnectionCommunicationService
{
  /**
   * Informs the owner of a hint that has been received for the need.
   * @param ownNeedURI
   * @param otherNeedURI
   * @param score
   * @param originatorURI
   * @throws NoSuchNeedException if ownNeedURI is not a known need URI
   */
  public void hint(URI ownNeedURI, URI otherNeedURI, double score, URI originatorURI, Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException;

  /**
   * Informs the owner of a connection initiated by the need identified by otherNeedURI to the
   * need identified by ownNeedURI. The connection URI ownConnectionURI has been created automatically by the
   * needserver upon being contacted and is passed here to serve as a connection handle.
   *
   *
   *
   *
   *
   *
   *
   * @param ownNeedURI
   * @param otherNeedURI
   * @param ownConnectionURI
   * @param content
   * @throws NoSuchNeedException if ownNeedURI or otherNeedURI does not denote a need
   * @throws IllegalMessageForNeedStateException
   *                             if one of the needs is inactive
   * @throws ConnectionAlreadyExistsException
   *                             if the two needs are already connected
   */
  public void connect(String ownNeedURI, String otherNeedURI, String ownConnectionURI, String content) throws NoSuchNeedException, ConnectionAlreadyExistsException, IllegalMessageForNeedStateException;

  //TODO move to another interface maybe?
}
