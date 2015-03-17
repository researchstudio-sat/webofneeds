package won.bot.framework.events.action.impl;

import com.hp.hpl.jena.rdf.model.Model;
import org.apache.commons.lang3.StringUtils;
import won.bot.framework.events.EventListenerContext;
import won.bot.framework.events.action.BaseEventBotAction;
import won.bot.framework.events.action.EventBotActionUtils;
import won.bot.framework.events.event.Event;
import won.bot.framework.events.event.impl.NeedProducerExhaustedEvent;
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
        URI needURI = wonNodeInformationService.generateNeedURI(wonNodeUri);
        getEventListenerContext().getOwnerService().sendWonMessage(createWonMessage(wonNodeInformationService,
                                                                                    needURI, wonNodeUri, needModel));
        logger.debug("need creation finished, new need URI is: {}", needURI);
        EventBotActionUtils.rememberInListIfNamePresent(getEventListenerContext(), needURI, uriListName);
        //TODO: register a listener for the CreateResponse and then publish the NCE
        //getEventListenerContext().getEventBus().publish(new NeedCreatedEvent(needURI, wonNodeUri, needModel,null));
        //TODO: publish a NeedCreationFailedEvent when an error is received from the server
        //getEventListenerContext().getEventBus().publish(new NeedCreationFailedEvent(wonNodeUri));
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
      .addContent(URI.create(needURI.toString()), needModel, null)
      .build();
  }
}
