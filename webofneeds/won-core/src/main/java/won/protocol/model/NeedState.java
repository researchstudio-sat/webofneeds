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

package won.protocol.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.vocabulary.WON;

import java.net.URI;

/**
 * User: fsalcher
 * Date: 10.10.12
 * Time: 14:13
 */
public enum NeedState
{
  INACTIVE("Inactive"),
  ACTIVE("Active");

  private static final Logger logger = LoggerFactory.getLogger(NeedState.class);
  private String name;

  private NeedState(String name)
  {
    this.name = name;
  }

  public URI getURI()
  {
    return URI.create(WON.BASE_URI + name);
  }

  /**
   * Tries to match the given string against all enum values.
   *
   * @param fragment string to match
   * @return matched enum, null otherwise
   */
  public static NeedState parseString(final String fragment)
  {
    for(NeedState state : values())
      if(state.name.equals(fragment))
        return state;

    logger.warn("No enum could be matched for: {}", fragment);
    return null;
  }

  /**
   * Tries to match the given URI against all enum values.
   *
   * @param uri URI to match
   * @return matched enum, null otherwise
   */
  public static NeedState fromURI(final URI uri)
  {
    for (NeedState state : values())
      if (state.getURI().equals(uri))
        return state;
    return null;
  }
}
