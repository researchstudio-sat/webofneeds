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

package won.bot.framework.bot.context;

import won.bot.framework.bot.BotContext;
import won.protocol.model.FacetType;

import java.net.URI;
import java.util.*;

/**
 * Straightforward BotContext implementation using a List and a Map.
 */
public class InMemoryBotContext implements BotContext
{
  private List<URI> needUris = new ArrayList<URI>();
  private Map<String,URI> namedNeedUris = new HashMap<String, URI>();
  private Map<URI,FacetType> needUriTypeMap = new HashMap<>();

  @Override
  public List<URI> listNeedUris()
  {
      List<URI> needList = new ArrayList<URI>();
      needList.addAll(needUriTypeMap.keySet());
      return needList;
    //return needUris;
  }

  @Override
  public List<URI> listNeedUrisOfType(FacetType type) {
    List<URI> uriListOfType = new ArrayList<URI>();
    if (!needUriTypeMap.isEmpty())
    {
        Iterator it =  needUriTypeMap.entrySet().iterator();
        for(Map.Entry<URI, FacetType> entry :needUriTypeMap.entrySet()){
            if (entry.getValue().equals(type))
                uriListOfType.add(entry.getKey());
        }
    }
    return uriListOfType;
  }


    @Override
  public boolean isNeedKnown(final URI needURI)
  {
    if (needUris.contains(needURI))
        return needUris.contains(needURI);
    else if (needUriTypeMap.containsKey(needURI))
        return needUriTypeMap.containsKey(needURI);
    else
        return false;
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
  public void rememberNeedUriWithType(final URI uri, final FacetType type){
      needUriTypeMap.put(uri, type);
  }

    @Override
    public void forgetNeedUri(URI uri) {
        needUris.remove(uri);
        needUriTypeMap.remove(uri);
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
