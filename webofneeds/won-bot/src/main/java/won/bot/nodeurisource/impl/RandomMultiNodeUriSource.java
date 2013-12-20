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

package won.bot.nodeurisource.impl;

import won.bot.nodeurisource.NodeURISource;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * User: fkleedorfer
 * Date: 19.12.13
 */
public class RandomMultiNodeUriSource implements NodeURISource
{
  private List<URI> nodeURIs = null;
  private long seed = System.currentTimeMillis();
  private Random random = new Random(seed);


  @Override
  public URI getWonNodeURI()
  {
    if (this.nodeURIs == null || this.nodeURIs.isEmpty()) return null;
    return this.nodeURIs.get(this.random.nextInt(this.nodeURIs.size()));
  }

  public void setNodeURIs(final Collection<URI> nodeURIs)
  {
    if (nodeURIs == null){
      this.nodeURIs = new ArrayList<URI>();
    } else {
      this.nodeURIs = new ArrayList<URI>(nodeURIs.size());
      this.nodeURIs.addAll(nodeURIs);
    }
  }

  public void setSeed(final long seed)
  {
    this.seed = seed;
    this.random = new Random(this.seed);
  }


}
