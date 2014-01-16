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

package won.bot.needconsumer.impl;

import com.hp.hpl.jena.rdf.model.Model;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.needconsumer.NeedConsumer;
import won.bot.nodeurisource.NodeURISource;
import won.protocol.owner.OwnerProtocolNeedServiceClientSide;
import won.protocol.util.RdfUtils;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * NeedConsumer implementation that uses a NodeURISource to obtain a won node URI and post the need that
 * is passed for consumption.
 * Can be configured to use each nodeURI only once. In this case, it will be marked as exhausted as soon as
 * a nodeURI re-occurs.
 */
public class OwnerEmulatingNeedConsumer implements NeedConsumer
{
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private OwnerProtocolNeedServiceClientSide ownerService;
  private NodeURISource nodeURISource;
  private boolean consumeOncePerNode = false;
  private Set<URI> consumedNodes = new HashSet<URI>();
  private boolean exhausted = false;

  @Override
  public void consume(final Model need)
  {
    URI nodeURI = nodeURISource.getNodeURI();
    if (!shouldConsume(need, nodeURI)) {
      logger.debug("suppressing creation of need on node {} with content {}", nodeURI, StringUtils.abbreviate(RdfUtils.toString(need),50));
      return;
    }
    if (logger.isDebugEnabled()){
      logger.debug("creating need on won node {} with content {} ", nodeURI, StringUtils.abbreviate(RdfUtils.toString(need),50));
    }
    try {
      Future<URI> needURI = ownerService.createNeed(URI.create("not:needed.any/more"), need, true, nodeURI);
    } catch (Exception e) {
      logger.info("could not create need", e);
    }
  }

  private boolean shouldConsume(final Model need, final URI nodeURI)
  {
    if (consumeOncePerNode){
      if (this.consumedNodes.contains(nodeURI)){
        this.exhausted = true;
        return false;
      }
      this.consumedNodes.add(nodeURI);
    }
    return true;
  }

  @Override
  public boolean isExhausted()
  {
    return exhausted;
  }

  public void setConsumeOncePerNode(final boolean consumeOncePerNode)
  {
    this.consumeOncePerNode = consumeOncePerNode;
  }

  public void setOwnerService(final OwnerProtocolNeedServiceClientSide ownerService)
  {
    this.ownerService = ownerService;
  }

  public void setNodeURISource(final NodeURISource nodeURISource)
  {
    this.nodeURISource = nodeURISource;
  }
}
