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

package won.bot.framework.events.listener;

import com.google.common.util.concurrent.ListenableFuture;
import com.hp.hpl.jena.rdf.model.Model;
import won.bot.framework.component.needproducer.impl.CommentNeedProducer;
import won.bot.framework.events.Event;
import won.bot.framework.events.event.CommentFacetCreatedEvent;
import won.bot.framework.events.event.FacetEvent;
import won.protocol.model.FacetType;
import won.protocol.util.WonRdfUtils;

import java.net.URI;
import java.util.List;

/**
 * User: fkleedorfer
 * Date: 30.01.14
 */
public class ConnectTwoNeedsListener extends BaseEventListener
{
  private URI localFacet;
  private URI remoteFacet;
  public ConnectTwoNeedsListener(final EventListenerContext context,final URI remoteFacet, final URI localFacet)
  {
    super(context);
    this.localFacet = localFacet;
    this.remoteFacet = remoteFacet;
  }

  @Override
  public void doOnEvent(final Event event) throws Exception
  {

      URI facet = getEventListenerContext().getBotContext().getNeedByName(FacetType.getFacetType(remoteFacet).name());
      List<URI> needs = getEventListenerContext().getBotContext().listNeedUris();
      for (int i = 0; i< needs.size();i++){
          try{
              //TODO: duplicate code. see ConnectTwoNeedsAction
              if (!needs.get(i).equals(facet)){
                  getEventListenerContext().getOwnerService().connect(needs.get(i),facet, WonRdfUtils.FacetUtils.createModelForConnect(localFacet,remoteFacet));
              }

          } catch (Exception e) {
              logger.warn("could not connect {} and {}", new Object[]{needs.get(i), facet}, e);
          }

      }
  }
}
