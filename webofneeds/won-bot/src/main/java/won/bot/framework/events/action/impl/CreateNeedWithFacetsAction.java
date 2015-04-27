package won.bot.framework.events.action.impl;

import com.hp.hpl.jena.rdf.model.Model;
import org.apache.commons.lang3.StringUtils;
import won.bot.framework.events.EventListenerContext;
import won.bot.framework.events.action.BaseEventBotAction;
import won.bot.framework.events.action.EventBotActionUtils;
import won.bot.framework.events.event.Event;
import won.bot.framework.events.event.NeedCreationFailedEvent;
import won.bot.framework.events.event.impl.FailureResponseEvent;
import won.bot.framework.events.event.impl.NeedCreatedEvent;
import won.bot.framework.events.event.impl.NeedProducerExhaustedEvent;
import won.bot.framework.events.listener.EventListener;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.model.FacetType;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
* Creates a need with the specified facets.
* If no facet is specified, the ownerFacet will be used.
*/
public class CreateNeedWithFacetsAction extends BaseEventBotAction
{
    private List<URI> facets;
    private String uriListName;

    /**
     * Creates a need with the specified facets.
     * If no facet is specified, the ownerFacet will be used.
     */
    public CreateNeedWithFacetsAction(EventListenerContext eventListenerContext, String uriListName, URI... facets) {
      super(eventListenerContext);
      if (facets == null || facets.length == 0) {
        //add the default facet if none is present.
        this.facets = new ArrayList<URI>(1);
        this.facets.add(FacetType.OwnerFacet.getURI());
      } else {
        this.facets = Arrays.asList(facets);
      }
      this.uriListName = uriListName;
    }

    /**
     * Creates a need with the specified facets.
     * If no facet is specified, the ownerFacet will be used.
     */
    public CreateNeedWithFacetsAction(final EventListenerContext eventListenerContext, URI... facets)
    {
        this(eventListenerContext, null, facets);
    }

    @Override
    protected void doRun(Event event) throws Exception
    {
        if (getEventListenerContext().getNeedProducer().isExhausted()){
            logger.info("the bot's need producer is exhausted.");
            getEventListenerContext().getEventBus().publish(new NeedProducerExhaustedEvent());
            return;
        }
        final Model needModel = getEventListenerContext().getNeedProducer().create();
        if (needModel == null){
          logger.warn("needproducer failed to produce a need model, aborting need creation");
          return;
        }
        for (URI facetURI:facets){
            WonRdfUtils.FacetUtils.addFacet(needModel,facetURI);
        }
        final URI wonNodeUri = getEventListenerContext().getNodeURISource().getNodeURI();
        logger.debug("creating need on won node {} with content {} ", wonNodeUri, StringUtils.abbreviate(RdfUtils.toString(needModel), 150));
        WonNodeInformationService wonNodeInformationService =
          getEventListenerContext().getWonNodeInformationService();
        final URI needURI = wonNodeInformationService.generateNeedURI(wonNodeUri);
        WonMessage createNeedMessage = createWonMessage(wonNodeInformationService,
          needURI, wonNodeUri, needModel);
      //remember the need URI so we can react to success/failure responses
      EventBotActionUtils.rememberInListIfNamePresent(getEventListenerContext(), needURI, uriListName);

        EventListener successCallback = new EventListener()
        {
          @Override
          public void onEvent(Event event) throws Exception {
            logger.debug("need creation successful, new need URI is {}", needURI);
            getEventListenerContext().getEventBus()
                                     .publish(new NeedCreatedEvent(needURI, wonNodeUri, needModel, null));
          }
        };

        EventListener failureCallback = new EventListener()
        {
          @Override
          public void onEvent(Event event) throws Exception {
            logger.debug("need creation failed for need URI {}, original message URI {}", needURI, ((FailureResponseEvent) event).getOriginalMessageURI());
            EventBotActionUtils.removeFromListIfNamePresent(getEventListenerContext(), needURI, uriListName);
            getEventListenerContext().getEventBus().publish(new NeedCreationFailedEvent(wonNodeUri));
          }
        };
      EventBotActionUtils.makeAndSubscribeResponseListener(needURI,
        createNeedMessage, successCallback, failureCallback, getEventListenerContext());

      logger.debug("registered listeners for response to message URI {}", createNeedMessage.getMessageURI());
      getEventListenerContext().getWonMessageSender().sendWonMessage(createNeedMessage);
      logger.debug("need creation message sent with message URI {}", createNeedMessage.getMessageURI());
    }


  private WonMessage createWonMessage(WonNodeInformationService wonNodeInformationService, URI needURI, URI wonNodeURI,
                                      Model needModel)
    throws WonMessageBuilderException {



    RdfUtils.replaceBaseURI(needModel,needURI.toString());

    WonMessageBuilder builder = new WonMessageBuilder();
    return builder
      .setMessagePropertiesForCreate(
        wonNodeInformationService.generateEventURI(
          wonNodeURI),
        needURI,
        wonNodeURI)
      .addContent(needModel, null)
      .build();
  }
}
