package won.bot.framework.events.action.impl;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import won.bot.framework.events.EventListenerContext;
import won.bot.framework.events.action.BaseEventBotAction;
import won.bot.framework.events.event.Event;
import won.bot.framework.events.event.impl.CloseFromOtherNeedEvent;
import won.bot.framework.events.event.impl.NeedDeactivatedEvent;
import won.node.facet.impl.WON_TX;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;

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
      WonMessage wonMessage = ((CloseFromOtherNeedEvent) event).getWonMessage();
      NodeIterator ni = RdfUtils.visitFlattenedToNodeIterator(
        wonMessage.getMessageContent(),
        new RdfUtils.ModelVisitor<NodeIterator>()
        {
          @Override
          public NodeIterator visit(final Model model) {
            return model.listObjectsOfProperty(
              model.createProperty(WON_TX.COORDINATION_MESSAGE.getURI()));
          }
        });
      assert ni.hasNext() : "no additional content found in close message, expected a commit";
      String coordinationMessageUri = ni.toList().get(0).asResource().getURI().toString();
      assert coordinationMessageUri.equals(WON_TX.COORDINATION_MESSAGE_COMMIT.getURI().toString()) : "expected a " +
        "Commmit message";
      getEventListenerContext().getWonMessageSender().sendWonMessage(createWonMessage(needURI));
      getEventListenerContext().getEventBus().publish(new NeedDeactivatedEvent(needURI));
    }
  }

  private WonMessage createWonMessage(URI needURI) throws WonMessageBuilderException {

    WonNodeInformationService wonNodeInformationService =
      getEventListenerContext().getWonNodeInformationService();

    URI localWonNode = WonRdfUtils.NeedUtils.queryWonNode(
      getEventListenerContext().getLinkedDataSource().getDataForResource(needURI));

    return WonMessageBuilder
      .setMessagePropertiesForDeactivate(
        wonNodeInformationService.generateEventURI(
          localWonNode),
        needURI,
        localWonNode)
      .build();
  }

}
