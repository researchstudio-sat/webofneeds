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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.component.needproducer.impl.GroupNeedProducer;
import won.bot.framework.events.event.GroupFacetCreatedEvent;
import won.bot.framework.events.event.NeedCreatedEvent;
import won.bot.framework.events.event.WorkDoneEvent;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;

import java.net.URI;
import java.util.List;

/**
 * User: fkleedorfer
 * Date: 02.02.14
 */
public class EventBotActions
{
  private static final Logger logger = LoggerFactory.getLogger(EventBotActions.class);

  public static abstract class Action implements Runnable {
    private EventListenerContext eventListenerContext;

    private Action(){}

    protected Action(final EventListenerContext eventListenerContext)
    {
      this.eventListenerContext = eventListenerContext;
    }

    @Override
    public final void run()
    {
      try {
        doRun();
      } catch (Exception e) {
        logger.warn("could not run action {}", getClass().getName(), e);
      }
    }

    protected final EventListenerContext getEventListenerContext()
    {
      return eventListenerContext;
    }

    protected abstract void doRun() throws Exception;


  }

  /**
   * Action telling the framework that the bot's work  is done.
   */
  public static class SignalWorkDoneAction extends Action {

    public SignalWorkDoneAction(final EventListenerContext eventListenerContext)
    {
      super(eventListenerContext);
    }

    @Override
    protected void doRun() throws Exception
    {
      logger.debug("signaling that the bot's work is done");
      getEventListenerContext().workIsDone();
      getEventListenerContext().getEventBus().publish(new WorkDoneEvent());
    }
  }

  /**
   * Action connecting two needs on the specified facets. The need's URIs are obtained from
   * the bot context. The first two URIs found there are used.
   */
  public static class ConnectTwoNeedsAction extends Action {
    private URI remoteFacet;
    private URI localFacet;

    public ConnectTwoNeedsAction(final EventListenerContext eventListenerContext, final URI remoteFacet, final URI localFacet)
    {
      super(eventListenerContext);
      this.remoteFacet = remoteFacet;
      this.localFacet = localFacet;
    }

    @Override
    public void doRun()
    {
      List<URI> needs = getEventListenerContext().getBotContext().listNeedUris();
      try {
        getEventListenerContext().getOwnerService().connect(needs.get(0), needs.get(1), WonRdfUtils.FacetUtils.createModelForConnect(localFacet, remoteFacet));
      } catch (Exception e) {
        logger.warn("could not connect {} and {}", new Object[]{needs.get(0), needs.get(1)}, e);
      }
    }
  }
  public static class ConnectTwoNeedsWithGroupAction extends Action {
      private URI remoteFacet;
      private URI localFacet;
      public ConnectTwoNeedsWithGroupAction (final EventListenerContext eventListenerContext, final URI remoteFacet, final URI localFacet)
      {

          super(eventListenerContext);
          this.remoteFacet = remoteFacet;
          this.localFacet = localFacet;
      }

      @Override
      protected void doRun() throws Exception {
          List<URI> needs = getEventListenerContext().getBotContext().listNeedUris();
          List<URI> groups = getEventListenerContext().getBotContext().listGroupUris();
          for (int i = 0; i< needs.size();i++){
              try{
                  //TODO: duplicate code. see ConnectTwoNeedsAction
                  getEventListenerContext().getOwnerService().connect(needs.get(i),groups.get(0),WonRdfUtils.FacetUtils.createModelForConnect(localFacet,remoteFacet));
              } catch (Exception e) {
                  logger.warn("could not connect {} and {}", new Object[]{needs.get(i), groups.get(0)}, e);
              }

          }

      }
  }

  public static class CreateGroupNeedAction extends Action{
     public CreateGroupNeedAction(final EventListenerContext eventListenerContext){
         super(eventListenerContext);
     }

     @Override
      protected  void doRun() throws Exception{
         if (getEventListenerContext().getNeedProducer().isExhausted()){
             logger.info("group need bot's need procucer is exhausted.");
             return;
         }
         final Model groupModel = getEventListenerContext().getNeedProducer().create(GroupNeedProducer.class);
         final URI wonNodeUri = getEventListenerContext().getNodeURISource().getNodeURI();
         final ListenableFuture<URI> futureNeedUri = getEventListenerContext().getOwnerService().createNeed(URI.create("we://dont.need.this/anymore"),groupModel,true,wonNodeUri);

         futureNeedUri.addListener(new Runnable()
         {
             @Override
             public void run()
             {
                 if (futureNeedUri.isDone()){
                     try {
                         URI uri = futureNeedUri.get();
                         logger.info("group creation finished, new group URI is: {}", uri);
                         getEventListenerContext().getBotContext().rememberGroupUri(uri);
                         getEventListenerContext().getEventBus().publish(new GroupFacetCreatedEvent(uri, wonNodeUri, groupModel));
                     } catch (Exception e){
                         logger.warn("create group facet failed", e);
                     }
                 }

             }
         }, getEventListenerContext().getExecutor());
     }
  }

  public static class CreateNeedAction extends Action {
    public CreateNeedAction(final EventListenerContext eventListenerContext)
    {
      super(eventListenerContext);
    }

    @Override
    protected void doRun() throws Exception
    {
        if (getEventListenerContext().getNeedProducer().isExhausted()){
            logger.info("bot's need procucer is exhausted.");
            return;
        }
      final Model needModel = getEventListenerContext().getNeedProducer().create();

      final URI wonNodeUri = getEventListenerContext().getNodeURISource().getNodeURI();
      logger.info("creating need on won node {} with content {} ", wonNodeUri, StringUtils.abbreviate(RdfUtils.toString(needModel), 150));
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
              logger.info("need creation finished, new need URI is: {}", uri);
              getEventListenerContext().getBotContext().rememberNeedUri(uri);
              getEventListenerContext().getEventBus().publish(new NeedCreatedEvent(uri, wonNodeUri, needModel));
            } catch (Exception e){
              logger.warn("createNeed failed", e);
            }
          }
        }
      }, getEventListenerContext().getExecutor());
    }
  }
}
