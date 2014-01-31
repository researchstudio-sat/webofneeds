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

package won.bot.framework.core.base;

import won.bot.framework.component.needproducer.NeedProducer;
import won.bot.framework.component.nodeurisource.NodeURISource;
import won.protocol.owner.OwnerProtocolNeedServiceClientSide;

/**
 * Base class for bots containing basic services.
 */
public abstract class BasicServiceBot extends BaseBot
{
  private NodeURISource nodeURISource;
  private NeedProducer needProducer;
  private OwnerProtocolNeedServiceClientSide ownerService;

  protected NodeURISource getNodeURISource()
  {
    return nodeURISource;
  }

  public void setNodeURISource(final NodeURISource nodeURISource)
  {
    this.nodeURISource = nodeURISource;
  }

  protected OwnerProtocolNeedServiceClientSide getOwnerService()
  {
    return ownerService;
  }

  public void setOwnerService(final OwnerProtocolNeedServiceClientSide ownerService)
  {
    this.ownerService = ownerService;
  }

  protected NeedProducer getNeedProducer()
  {
    return needProducer;
  }

  public void setNeedProducer(final NeedProducer needProducer)
  {
    this.needProducer = needProducer;
  }

}
