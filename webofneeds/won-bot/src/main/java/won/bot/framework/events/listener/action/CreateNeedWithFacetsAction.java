package won.bot.framework.events.listener.action;

import com.google.common.util.concurrent.ListenableFuture;
import com.hp.hpl.jena.rdf.model.Model;
import org.apache.commons.lang3.StringUtils;
import won.bot.framework.events.event.NeedCreatedEvent;
import won.bot.framework.events.listener.EventListenerContext;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

/**
* User: fkleedorfer
* Date: 28.03.14
*/
public class CreateNeedWithFacetsAction extends EventBotAction
{
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
                        getEventListenerContext().getEventBus().publish(new NeedCreatedEvent(uri, wonNodeUri, needModel,null));
                    } catch (Exception e){
                        logger.warn("createNeed failed", e);
                    }
                }
            }
        }, getEventListenerContext().getExecutor());
    }
}
