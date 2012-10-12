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

/**
 *  Interface defining the external methods for the matcher protocol, i.e. those methods
 *  that are directly or indirectly exposed via Web Service.
 *
 */
public interface MatcherProtocolExternalFacade
{

  /**
   * Notifies the localNeed of the fact that it attains the specified match score with remoteNeed. Originator
   * identifies the entity making the call. Normally, originator is a matchign service.
   *
   * @param localNeed  URI of the need that's managed in the local needserver
   * @param remoteNeed URI of the other need (may be on the local needserver)
   * @param score      match score between 0.0 (bad) and 1.0 (good). Implementations treat lower values as 0.0 and higher values as 1.0.
   * @param originator an URI identifying the calling entity
   */
  public void hint(URI localNeed, URI remoteNeed, double score, URI originator);

}
