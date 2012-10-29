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

import java.net.URI;
import java.util.Collection;

/**
 * User: fkleedorfer
 * Date: 23.10.12
 */
public interface NeedContainerService
{
  /**
   * Creates a new need with the specified content.
   * TODO replace String with the type used to hold the need content
   *
   * @param content
   * @return the URI of the newly created need
   */
  public URI createNeed(String content);

  /**
   * Retrieves a list of all needs on the needserver.
   * @return a collection of all need URIs.
   */
  public Collection<URI> listNeedURIs();

}
