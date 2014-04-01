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

package won.bot.framework.events.listener.action;

import won.bot.framework.events.listener.EventListenerContext;
import won.protocol.util.WonRdfUtils;

import java.net.URI;
import java.util.Date;
import java.util.List;

/**
   * EventBotAction connecting two needs on the specified facets. The need's URIs are obtained from
   * the bot context. The first two URIs found there are used.
   */
public class ConnectFromListToListAction extends EventBotAction
{
      private String fromListName;
      private String toListName;
      private URI fromFacet;
      private URI toFacet;
      private long millisBetweenCalls;

      public ConnectFromListToListAction(EventListenerContext eventListenerContext, String fromListName, String toListName, URI fromFacet, URI toFacet, final long millisBetweenCalls) {
          super(eventListenerContext);
          this.fromListName = fromListName;
          this.toListName = toListName;
          this.fromFacet = fromFacet;
          this.toFacet = toFacet;
          this.millisBetweenCalls = millisBetweenCalls;
      }

      @Override
      public void doRun()
      {

          List<URI> fromNeeds = getEventListenerContext().getBotContext().getNamedNeedUriList(fromListName);
          List<URI> toNeeds = getEventListenerContext().getBotContext().getNamedNeedUriList(toListName);
          logger.debug("connecting {} needs from list {} to {} needs from list {}", new Object[]{fromNeeds.size(),fromListName, toNeeds.size(), toListName});
          long start = System.currentTimeMillis();
          long count = 0;
          if (fromListName.equals(toListName)){
              //only one connection per pair if from-list is to-list
              for (int i = 0; i < fromNeeds.size();i++){
                  URI fromUri = fromNeeds.get(i);
                  for (int j = i +1; j < fromNeeds.size(); j++){
                      URI toUri = fromNeeds.get(j);
                      try {
                        count ++;
                        performConnect(fromUri, toUri, new Date(start + count * millisBetweenCalls));
                      } catch (Exception e) {
                          logger.warn("could not connect {} and {}", new Object[]{fromUri, toUri}, e);
                      }
                  }
              }
          } else {
              for (URI fromUri: fromNeeds){
                  for (URI toUri:toNeeds) {
                      try{
                        count ++;
                        performConnect(fromUri, toUri, new Date(start + count * millisBetweenCalls));
                      } catch (Exception e) {
                          logger.warn("could not connect {} and {}", new Object[]{fromUri, toUri}, e);
                      }
                  }
              }
          }
      }

  private void performConnect(final URI fromUri, final URI toUri, final Date when) throws Exception
  {
    logger.info("scheduling connection message for date {}",when);
    getEventListenerContext().getTaskScheduler().schedule(new Runnable()
    {
      public void run()
      {
        try {
          logger.info("connecting needs {} and {}",fromUri,toUri);
          getEventListenerContext().getOwnerService().connect(fromUri,toUri, WonRdfUtils.FacetUtils.createFacetModelForHintOrConnect(fromFacet, toFacet));
        } catch (Exception e) {
          logger.info("could not connect {} and {}", fromUri, toUri);
          logger.info("caught exception", e);
        }
      }
    }, when);
  }
}
