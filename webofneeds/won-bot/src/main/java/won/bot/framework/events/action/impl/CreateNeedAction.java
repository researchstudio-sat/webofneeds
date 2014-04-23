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

package won.bot.framework.events.action.impl;

import com.google.common.util.concurrent.ListenableFuture;
import com.hp.hpl.jena.rdf.model.Model;
import org.apache.commons.lang3.StringUtils;
import won.bot.framework.events.event.Event;
import won.bot.framework.events.action.BaseEventBotAction;
import won.bot.framework.events.action.EventBotActionUtils;
import won.bot.framework.events.event.impl.NeedCreatedEvent;
import won.bot.framework.events.EventListenerContext;
import won.bot.framework.events.event.impl.NeedProducerExhaustedEvent;
import won.protocol.model.FacetType;
import won.protocol.util.RdfUtils;

import java.net.URI;

/**
* User: fkleedorfer
* Date: 28.03.14
*/
public class CreateNeedAction extends BaseEventBotAction
{
  private String uriListName;
  public CreateNeedAction(final EventListenerContext eventListenerContext)
  {
    this(eventListenerContext, null);
  }

    public CreateNeedAction(EventListenerContext eventListenerContext, String uriListName) {
        super(eventListenerContext);
        this.uriListName = uriListName;
    }

    @Override
  protected void doRun(Event event) throws Exception
  {
      if (getEventListenerContext().getNeedProducer().isExhausted()){
          logger.debug("bot's need procucer is exhausted.");
          getEventListenerContext().getEventBus().publish(new NeedProducerExhaustedEvent());
          return;
      }
    final Model needModel = getEventListenerContext().getNeedProducer().create();

    final URI wonNodeUri = getEventListenerContext().getNodeURISource().getNodeURI();
    logger.debug("creating need on won node {} with content {} ", wonNodeUri, StringUtils.abbreviate(RdfUtils.toString(needModel), 150));
    final ListenableFuture<URI> futureNeedUri = getEventListenerContext().getOwnerService().createNeed(URI.create("we://dont.need.this/anymore"), needModel, true, wonNodeUri);
    //add a listener that adds the need URI to the botContext
    futureNeedUri.addListener(new Runnable()
    {
      @Override
      public void run()
      {
        if (futureNeedUri.isDone()){
          try {
            URI uri = futureNeedUri.get();
            logger.debug("need creation finished, new need URI is: {}", uri);
              EventBotActionUtils.rememberInListIfNamePresent(getEventListenerContext(), uri, uriListName);
              getEventListenerContext().getEventBus().publish(new NeedCreatedEvent(uri, wonNodeUri, needModel, FacetType.OwnerFacet));
          } catch (Exception e){
            logger.warn("createNeed failed", e);
          }
        }
      }
    }, getEventListenerContext().getExecutor());
  }


}
