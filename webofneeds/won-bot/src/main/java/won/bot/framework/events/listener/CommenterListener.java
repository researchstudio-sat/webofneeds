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
import won.bot.framework.events.event.ConnectFromOtherNeedEvent;
import won.bot.framework.events.event.OpenFromOtherNeedEvent;
import won.protocol.model.ConnectionState;
import won.protocol.model.FacetType;

import java.net.URI;

/**
 * User: fkleedorfer
 * Date: 30.01.14
 */
public class CommenterListener extends BaseEventListener
{

  public CommenterListener(final EventListenerContext context)
  {
    super(context);
  }

  @Override
  public void doOnEvent(final Event event) throws Exception
  {
      if (getEventListenerContext().getNeedProducer().isExhausted()){
          logger.info("comment need bot's need producer is exhausted");
          return;
      }
      final Model commentModel = getEventListenerContext().getNeedProducer().create(CommentNeedProducer.class);
      final URI wonNodeUri = getEventListenerContext().getNodeURISource().getNodeURI();
      final ListenableFuture<URI> futureNeedUri = getEventListenerContext().getOwnerService().createNeed(URI.create("we://dont.need.this/anymore"),commentModel,true,wonNodeUri);
      futureNeedUri.addListener(new Runnable()
      {
          @Override
          public void run()
          {
              if (futureNeedUri.isDone()){
                  try {
                      URI uri = futureNeedUri.get();
                      logger.info("comment creation finished, new comment URI is: {}", uri);
                      getEventListenerContext().getBotContext().rememberNeedUriWithName(uri, FacetType.CommentFacet.name() );
                      getEventListenerContext().getEventBus().publish(new CommentFacetCreatedEvent(uri, wonNodeUri, commentModel));
                  } catch (Exception e){
                      logger.warn("create comment facet failed", e);
                  }
              }

          }
      }, getEventListenerContext().getExecutor());

  }
}
