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

package won.protocol.matcher;

import won.protocol.exception.NoSuchNeedException;

import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 31.10.12
 */
public interface NodeFromMatcherReceiver
{
  /**
   * Notifies the need of the fact that it attains the specified match score with otherNeed. Originator
   * identifies the entity making the call. Normally, originator is a matching service.
   *
   * @param needURI the URI of the need
   * @param otherNeed URI of the other need (may be on the local needserver)
   * @param score      match score between 0.0 (bad) and 1.0 (good). Implementations treat lower values as 0.0 and higher values as 1.0.
   * @param originator an URI identifying the calling entity
   * @throws NoSuchNeedException if needURI is unknown
   */
  public void hint(URI needURI, URI otherNeed, double score, URI originator) throws NoSuchNeedException;

}
