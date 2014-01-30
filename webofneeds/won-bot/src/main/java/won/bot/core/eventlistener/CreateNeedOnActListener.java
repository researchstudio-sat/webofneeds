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

package won.bot.core.eventlistener;

import com.google.common.util.concurrent.ListenableFuture;
import com.hp.hpl.jena.rdf.model.Model;
import org.apache.commons.lang3.StringUtils;
import won.bot.core.event.ActEvent;
import won.bot.core.event.NeedCreatedEvent;
import won.bot.events.Event;
import won.protocol.util.RdfUtils;

import java.net.URI;

/**
 * Event listener that creates a new need each time it receives an ActEvent.
 * When the need creation finishes (the new need's URI is returned by the WON node) a NewNeedCreatedEvent is published.
 */
public class CreateNeedOnActListener extends BaseEventListener
{
  private int numberOfNeedsTried;
  private int numberOfNeedsSuccess;
  private int targetNumberOfNeeds;

  /**
   * @param context
   * @param numberOfNeeds if > 0, the listener will de-register from ActEvents after having created the specified number of needs.
   */
  public CreateNeedOnActListener(final EventListenerContext context, int numberOfNeeds)
  {
    super(context);
    this.targetNumberOfNeeds = numberOfNeeds;
  }

  @Override
  public void onEvent(final Event event) throws Exception
  {
    if (!(event instanceof ActEvent)) return;
    if (getEventListenerContext().getNeedProducer().isExhausted()){
      logger.info("bot's need procucer is exhausted. will not reschedule execution");
      getEventListenerContext().getEventBus().unsubscribe(ActEvent.class,this);
      return;
    }
    final Model needModel = getEventListenerContext().getNeedProducer().create();
    final URI wonNodeUri = getEventListenerContext().getNodeURISource().getNodeURI();
    logger.info("creating need on won node {} with content {} ", wonNodeUri, StringUtils.abbreviate(RdfUtils.toString(needModel), 150));
    final ListenableFuture<URI> futureNeedUri = getEventListenerContext().getOwnerService().createNeed(URI.create("we://dont.need.this/anymore"), needModel, true, wonNodeUri);
    synchronized (this){
      this.numberOfNeedsTried++;
    }
    //add a listener that adds the need URI to the botContext
    futureNeedUri.addListener(new Runnable()
    {
      @Override
      public void run()
      {
        if (futureNeedUri.isDone()){
          try {
            URI uri = futureNeedUri.get();
            logger.info("need creation finished, new need URI is: {}", uri);
            getEventListenerContext().getBotContext().rememberNeedUri(uri);
            getEventListenerContext().getEventBus().publish(new NeedCreatedEvent(uri, wonNodeUri, needModel));
            synchronized (this){
              numberOfNeedsSuccess++;
            }
          } catch (Exception e){
            logger.warn("createNeed failed", e);
          }
          //if we reached the targeted number of needs, de-register (note: we may accidentally create too many here)
          if (targetNumberOfNeeds > 0 && targetNumberOfNeeds >= numberOfNeedsSuccess) {
            getEventListenerContext().getEventBus().unsubscribe(ActEvent.class, CreateNeedOnActListener.this);
          }
          logger.debug("tried to create {} needs, succeeded {} times so far", numberOfNeedsTried, numberOfNeedsSuccess);
        }
      }
    }, getEventListenerContext().getExecutor());

  }


}
