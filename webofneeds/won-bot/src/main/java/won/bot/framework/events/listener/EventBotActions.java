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
import won.bot.framework.events.event.NeedCreatedEvent;
import won.bot.framework.events.event.NeedDeactivatedEvent;
import won.bot.framework.events.event.WorkDoneEvent;
import won.protocol.model.FacetType;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;

import java.net.URI;
import java.util.Arrays;
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
        getEventListenerContext().getOwnerService().connect(needs.get(0), needs.get(1), WonRdfUtils.FacetUtils.createFacetModelForHintOrConnect(localFacet, remoteFacet));
      } catch (Exception e) {
        logger.warn("could not connect {} and {}", new Object[]{needs.get(0), needs.get(1)}, e);
      }
    }
  }

    /**
     * Action connecting two needs on the specified facets. The need's URIs are obtained from
     * the bot context. The first two URIs found there are used.
     */
    public static class ConnectFromListToListAction extends Action {
        private String fromListName;
        private String toListName;
        private URI fromFacet;
        private URI toFacet;


        public ConnectFromListToListAction(EventListenerContext eventListenerContext, String fromListName, String toListName, URI fromFacet, URI toFacet) {
            super(eventListenerContext);
            this.fromListName = fromListName;
            this.toListName = toListName;
            this.fromFacet = fromFacet;
            this.toFacet = toFacet;
        }

        @Override
        public void doRun()
        {

            List<URI> fromNeeds = getEventListenerContext().getBotContext().getNamedNeedUriList(fromListName);
            List<URI> toNeeds = getEventListenerContext().getBotContext().getNamedNeedUriList(toListName);
            logger.debug("connecting {} needs from list {} to {} needs from list {}", new Object[]{fromNeeds.size(),fromListName, toNeeds.size(), toListName});
            if (fromListName.equals(toListName)){
                //only one connection per pair if from-list is to-list
                for (int i = 0; i < fromNeeds.size();i++){
                    URI fromUri = fromNeeds.get(i);
                    for (int j = i +1; j < fromNeeds.size(); j++){
                        URI toUri = fromNeeds.get(j);
                        try {
                            logger.info("connecting needs {} and {}",fromUri,toUri);
                            getEventListenerContext().getOwnerService().connect(fromUri,toUri, WonRdfUtils.FacetUtils.createFacetModelForHintOrConnect(fromFacet, toFacet));
                        } catch (Exception e) {
                            logger.warn("could not connect {} and {}", new Object[]{fromUri, toUri}, e);
                        }
                    }
                }
            } else {
                for (URI fromUri: fromNeeds){
                    for (URI toUri:toNeeds) {
                        try{
                            logger.info("connecting needs {} and {}",fromUri,toUri);
                            getEventListenerContext().getOwnerService().connect(fromUri,toUri, WonRdfUtils.FacetUtils.createFacetModelForHintOrConnect(fromFacet, toFacet));
                        } catch (Exception e) {
                            logger.warn("could not connect {} and {}", new Object[]{fromUri, toUri}, e);
                        }
                    }
                }
            }
        }
    }

  public static class MatchNeedsAction extends Action{
      public MatchNeedsAction(final EventListenerContext eventListenerContext)
      {
         super(eventListenerContext);
      }

      @Override
      protected void doRun() throws Exception{
           List<URI> needs = getEventListenerContext().getBotContext().listNeedUris();
           URI need1 = needs.get(0);
           URI need2 = needs.get(1);
           logger.info("matching needs {} and {}",need1,need2);
           logger.info("getEventListnerContext():"+getEventListenerContext());
           logger.info("getMatcherService(): "+getEventListenerContext().getMatcherService());
           getEventListenerContext().getMatcherService().hint(need1,need2,1.0,URI.create("http://localhost:8080/matcher"),null);
      }
  }


  public static class CreateNeedAction extends Action {
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
                rememberInListIfNamePresent(getEventListenerContext(),uri,uriListName);
                getEventListenerContext().getEventBus().publish(new NeedCreatedEvent(uri, wonNodeUri, needModel,FacetType.OwnerFacet));
            } catch (Exception e){
              logger.warn("createNeed failed", e);
            }
          }
        }
      }, getEventListenerContext().getExecutor());
    }


  }

    public static class CreateNeedWithFacetsAction extends Action {
        private List<URI> facets;
        private String uriListName;

        public CreateNeedWithFacetsAction(EventListenerContext eventListenerContext, String uriListName, URI... facets) {
            super(eventListenerContext);
            this.facets = Arrays.asList(facets);
            this.uriListName = uriListName;
        }

        public CreateNeedWithFacetsAction(final EventListenerContext eventListenerContext, URI... facets)
        {
            this(eventListenerContext, null, facets);
        }

        @Override
        protected void doRun() throws Exception
        {
            if (getEventListenerContext().getNeedProducer().isExhausted()){
                logger.info("bot's need producer is exhausted.");
                return;
            }
            final Model needModel = getEventListenerContext().getNeedProducer().create();
            for (URI facetURI:facets){
                WonRdfUtils.FacetUtils.addFacet(needModel,facetURI);
            }
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
                            rememberInListIfNamePresent(getEventListenerContext(),uri,uriListName);
                            getEventListenerContext().getEventBus().publish(new NeedCreatedEvent(uri, wonNodeUri, needModel,null));
                        } catch (Exception e){
                            logger.warn("createNeed failed", e);
                        }
                    }
                }
            }, getEventListenerContext().getExecutor());
        }
    }

    public static class DeactivateAllNeedsAction extends Action {
        public DeactivateAllNeedsAction(EventListenerContext eventListenerContext) {
            super(eventListenerContext);
        }

        @Override
        protected void doRun() throws Exception {
            List<URI> toDeactivate = getEventListenerContext().getBotContext().listNeedUris();
            for (URI uri: toDeactivate){
                getEventListenerContext().getOwnerService().deactivate(uri);
                getEventListenerContext().getEventBus().publish(new NeedDeactivatedEvent(uri));
            }
        }
    }

  public static class DeactivateAllNeedsOfGroupAction extends Action {
    private String groupName;
    public DeactivateAllNeedsOfGroupAction(EventListenerContext eventListenerContext, String groupName) {
      super(eventListenerContext);
      this.groupName = groupName;
    }

    @Override
    protected void doRun() throws Exception {
      List<URI> toDeactivate = getEventListenerContext().getBotContext().getNamedNeedUriList(groupName);
      for (URI uri: toDeactivate){
        getEventListenerContext().getOwnerService().deactivate(uri);
        getEventListenerContext().getEventBus().publish(new NeedDeactivatedEvent(uri));
      }
    }
  }



    private static void rememberInListIfNamePresent(EventListenerContext ctx ,URI uri, String uriListName) {
        if (uriListName != null && uriListName.trim().length() > 0){
            ctx.getBotContext().appendToNamedNeedUriList(uri, uriListName);
        } else {
            ctx.getBotContext().rememberNeedUri(uri);
        }
    }
}
