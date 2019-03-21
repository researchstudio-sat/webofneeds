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

package won.bot.framework.eventbot.event.impl.debugbot;

import java.net.URI;

import org.apache.jena.query.Dataset;

import won.bot.framework.eventbot.event.BaseNeedSpecificEvent;
import won.protocol.model.FacetType;

/**
 *
 */
public class NeedCreatedEventForDebugHint extends BaseNeedSpecificEvent
{
  private final URI wonNodeUri;
  private final Dataset needDataset;
  private final FacetType facetType;

  public NeedCreatedEventForDebugHint(final URI needURI, final URI wonNodeUri, final Dataset needDataset, final FacetType facetType) {
    super(needURI);
    this.wonNodeUri = wonNodeUri;
    this.needDataset = needDataset;
    this.facetType = facetType;
  }

  public URI getWonNodeUri()
  {
    return wonNodeUri;
  }

  public Dataset getNeedDataset()
  {
    return needDataset;
  }


}
