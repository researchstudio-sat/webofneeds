package won.bot.framework.events.action.impl;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import won.bot.framework.events.EventListenerContext;
import won.bot.framework.events.action.BaseEventBotAction;
import won.bot.framework.events.event.Event;
import won.bot.framework.events.event.impl.CloseFromOtherNeedEvent;
import won.bot.framework.events.event.impl.NeedDeactivatedEvent;
import won.node.facet.impl.WON_TX;

import java.net.URI;
import java.util.List;

/**
 * User: Danijel
 * Date: 21.5.14.
 */
public class TwoPhaseCommitNoVoteDeactivateAllNeedsAction extends BaseEventBotAction
{
  public TwoPhaseCommitNoVoteDeactivateAllNeedsAction(EventListenerContext eventListenerContext) {
    super(eventListenerContext);
  }

  @Override
  protected void doRun(Event event) throws Exception {

    //check the global COORDINATION_MESSAGE (must be ABORT)
    if(event instanceof CloseFromOtherNeedEvent)
    {
      Model m = ((CloseFromOtherNeedEvent) event).getContent();
      NodeIterator ni = m.listObjectsOfProperty(m.getProperty(WON_TX.COORDINATION_MESSAGE.getURI().toString()));
      if(ni.hasNext())
      {
        String coordinationMessageUri = ni.toList().get(0).asResource().getURI().toString();
        if(coordinationMessageUri.equals(WON_TX.COORDINATION_MESSAGE_ABORT.getURI().toString()))
          logger.debug("Sent COORDINATION_MESSAGE: {}", coordinationMessageUri);
        else
          logger.error("Content of the COORDINATION_MESSAGE must be: {}. Currently it is: {}",
            WON_TX.COORDINATION_MESSAGE_ABORT.getURI(), coordinationMessageUri);
      }
    }
    List<URI> toDeactivate = getEventListenerContext().getBotContext().listNeedUris();
    for (URI uri: toDeactivate){
      getEventListenerContext().getOwnerService().deactivate(uri);
      getEventListenerContext().getEventBus().publish(new NeedDeactivatedEvent(uri));
    }
  }
}
