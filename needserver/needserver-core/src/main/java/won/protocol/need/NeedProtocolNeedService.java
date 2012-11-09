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

package won.protocol.need;

import won.protocol.exception.*;
import won.protocol.service.ConnectionCommunicationService;

import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 31.10.12
 */
public interface NeedProtocolNeedService extends ConnectionCommunicationService
{

  /**
   * Requests a connection from the need otherNeedURI. The other need refers to the
   * connection using the specified otherConnectionURI. A short message can be sent along with the
   * request.

   *
   * @param need the URI of the need
   * @param otherNeedURI
   * @param otherConnectionURI
   * @param message
   *
   */
  public URI connectionRequested(URI need, URI otherNeedURI, URI otherConnectionURI, String message) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException;

}
