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

package won.bot.core.impl;

import com.google.common.util.concurrent.ListenableFuture;
import com.hp.hpl.jena.rdf.model.Model;
import org.apache.camel.CamelContextAware;
import won.bot.core.base.TriggeredBot;

import java.net.URI;
import java.util.concurrent.ExecutionException;

/**
 * Bot that can be scheduled to create new needs with a Trigger.
 */
public class SchedulableNeedCreator extends TriggeredBot
{
  @Override
  public void act() throws Exception
  {
    final Model needModel = getNeedProducer().create();
    final URI wonNodeUri = getNodeURISource().getNodeURI();
    final ListenableFuture<URI> futureNeedUri = getOwnerService().createNeed(URI.create("we://dont.need.this/anymore"), needModel, true, wonNodeUri);
    //add a listener that adds the need URI to the botContext
    futureNeedUri.addListener(new Runnable()
    {
      @Override
      public void run()
      {
        if (futureNeedUri.isDone()){
          try {
            URI uri = futureNeedUri.get();
            getBotContext().rememberNeedUri(uri);
            onNewNeedCreated(uri, wonNodeUri, needModel);
          } catch (InterruptedException e) {
            logger.warn("interrupted while waiting for result of createNeed",e);
          } catch (ExecutionException e) {
            logger.warn("createNeed failed", e);
          }
        }
      }
    }, getExecutor());
  }
}
