/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.bot.context.impl;

import won.bot.context.BotContext;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Straightforward BotContext implementation using a List and a Map.
 */
public class InMemoryBotContext implements BotContext
{
  private List<URI> needUris = new ArrayList<URI>();
  private Map<String,URI> namedNeedUris = new HashMap<String, URI>();

  @Override
  public List<URI> listNeedUris()
  {
    return needUris;
  }

  @Override
  public boolean isNeedKnown(final URI needURI)
  {
    return needUris.contains(needURI);
  }

  /**
   * Caution, this call is expensive as it uses List.contains(uri).
   * @param uri
   * @param name
   */
  @Override
  public void rememberNeedUriWithName(final URI uri, final String name)
  {
    if (!needUris.contains(uri)){
      needUris.add(uri);
    }
    namedNeedUris.put(name, uri);
  }

  /**
   * @param uri
   */
  @Override
  public void rememberNeedUri(final URI uri)
  {
    needUris.add(uri);
  }


  @Override
  public URI getNeedByName(final String name)
  {
    return namedNeedUris.get(name);
  }

  @Override
  public List<String> listNeedUriNames()
  {
    List ret = new ArrayList<URI>(namedNeedUris.size());
    ret.addAll(namedNeedUris.keySet());
    return ret;
  }
}
