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

/**
 * Expects a CloseFromOtherNeed event from a and closes the local need.
 * If the additional message content of the event ss not a WON_TX.COORDINATION_MESSAGE_COMMIT, an exception is thrown.
 */
public class TwoPhaseCommitDeactivateOnCloseAction extends BaseEventBotAction
{
  public TwoPhaseCommitDeactivateOnCloseAction(EventListenerContext eventListenerContext) {
    super(eventListenerContext);
  }

  @Override
  protected void doRun(Event event) throws Exception {

    //If we receive a close event, it must carry a commit message.
    if(event instanceof CloseFromOtherNeedEvent)
    {
      URI needURI = ((CloseFromOtherNeedEvent) event).getNeedURI();
      Model m = ((CloseFromOtherNeedEvent) event).getContent();
      NodeIterator ni = m.listObjectsOfProperty(m.getProperty(WON_TX.COORDINATION_MESSAGE.getURI().toString()));
      assert ni.hasNext() : "no additional content found in close message, expected a commit";
      String coordinationMessageUri = ni.toList().get(0).asResource().getURI().toString();
      assert coordinationMessageUri.equals(WON_TX.COORDINATION_MESSAGE_COMMIT.getURI().toString()) : "expected a " +
        "Commmit message";
      getEventListenerContext().getOwnerService().deactivate(needURI, null);
      getEventListenerContext().getEventBus().publish(new NeedDeactivatedEvent(needURI));
    }
  }
}
