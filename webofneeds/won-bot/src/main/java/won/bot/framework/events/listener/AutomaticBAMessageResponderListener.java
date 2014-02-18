package won.bot.framework.events.listener;

import com.hp.hpl.jena.rdf.model.Model;
import won.bot.framework.events.Event;
import won.bot.framework.events.event.OpenFromOtherNeedEvent;
import won.bot.framework.events.event.BAStateChangeEvent;
import won.protocol.model.ConnectionState;
import won.protocol.model.FacetType;
import won.protocol.util.WonRdfUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 18.2.14.
 * Time: 14.49
 * To change this template use File | Settings | File Templates.
 */
public class AutomaticBAMessageResponderListener extends BaseEventListener
{
    private int targetNumberOfMessages = -1;
    private int numberOfMessagesSent = 0;
    private long millisTimeoutBeforeReply = 1000;
    private Object monitor = new Object();

    public AutomaticBAMessageResponderListener(final EventListenerContext context, final int targetNumberOfMessages, final long millisTimeoutBeforeReply)
    {
        super(context);
        this.targetNumberOfMessages = targetNumberOfMessages;
        this.millisTimeoutBeforeReply = millisTimeoutBeforeReply;
    }

    @Override
    public void doOnEvent(final Event event) throws Exception
    {
        if (event instanceof BAStateChangeEvent){
            handleMessageEvent((BAStateChangeEvent) event);
        } else if (event instanceof OpenFromOtherNeedEvent) {
            handleOpenEvent((OpenFromOtherNeedEvent) event);
        }

    }

    /**
     * React to open event by sending a message.
     *
     * @param openEvent
     */
    private void handleOpenEvent(final OpenFromOtherNeedEvent openEvent)
    {
        logger.debug("got open event for need: {}, connection state is: {}", openEvent.getCon().getNeedURI(), openEvent.getCon().getState());
        if (openEvent.getCon().getState() == ConnectionState.CONNECTED){
            logger.debug("replying to open with message");
            getEventListenerContext().getTaskScheduler().schedule(new Runnable()
            {
                @Override
                public void run()
                {
                    URI connectionUri = openEvent.getCon().getConnectionURI();
                    try {
                        countMessageAndUnsubscribeIfNecessary();
                        String message = "Open message";
                        getEventListenerContext().getOwnerService().textMessage(connectionUri, WonRdfUtils.MessageUtils.textMessage(message));
                    } catch (Exception e) {
                        logger.warn("could not send message via connection {}", connectionUri, e);
                    }
                }
            }, new Date(System.currentTimeMillis() + millisTimeoutBeforeReply));
        }
    }

    private void handleMessageEvent(final BAStateChangeEvent messageEvent){
        logger.debug("got message '{}' for need: {}", messageEvent.getMessage().getMessage(), messageEvent.getCon().getNeedURI());
        getEventListenerContext().getTaskScheduler().schedule(new Runnable(){
            @Override
            public void run()
            {
                String message = generateMessage(messageEvent.getFacetType());
                Model messageContent = WonRdfUtils.MessageUtils.textMessage(message);
                URI connectionUri = messageEvent.getCon().getConnectionURI();
                try {
                    getEventListenerContext().getOwnerService().textMessage(connectionUri, messageContent);
                    countMessageAndUnsubscribeIfNecessary();
                } catch (Exception e) {
                    logger.warn("could not send message via connection {}", connectionUri, e);
                }
            }
        }, new Date(System.currentTimeMillis() + this.millisTimeoutBeforeReply));
    }

    private void countMessageAndUnsubscribeIfNecessary()
    {
        synchronized (monitor){
            numberOfMessagesSent++;
            if (targetNumberOfMessages > 0 && numberOfMessagesSent >= targetNumberOfMessages){
                unsubscribe();
            }
        }
    }



    private void unsubscribe()
    {
        logger.debug("unsubscribing from BAStateChangeEvent");
        getEventListenerContext().getEventBus().unsubscribe(BAStateChangeEvent.class, this);
    }

    private String generateMessage(FacetType facetType)
    {
        String message = new String();
        Random randomGenerator = null;
        int index = -1;
        ArrayList<String> list = new ArrayList<String>();
        if (facetType.equals(FacetType.BAPCParticipantFacet))
        {
            list.add("MESSAGE_COMPLETED");
            list.add("MESSAGE_EXIT");
            list.add("MESSAGE_FAIL");
            list.add("MESSAGE_CANCELED");
            list.add("MESSAGE_COMPENSATED");
            list.add("MESSAGE_CLOSED");
            list.add("MESSAGE_CANCEL"); //can not be sent by Participant
            list.add("MESSAGE_NOTVALID");

        }
        else if(facetType.equals(FacetType.BAPCCoordinatorFacet))
        {
            list.add("MESSAGE_CANCEL");
            list.add("MESSAGE_CLOSE");
            list.add("MESSAGE_COMPENSATE");
            list.add("MESSAGE_NOTCOMPLETED");
            list.add("MESSAGE_FAILED");
            list.add("MESSAGE_EXITED");
            list.add("MESSAGE_COMPENSATED"); //can not be sent by Coordinator
            list.add("MESSAGE_NOTVALID");
        }
        else if(facetType.equals(FacetType.BACCCoordinatorFacet))
        {
            //todo
        }
        else if(facetType.equals(FacetType.BACCParticipantFacet))
        {
            //todo
        }
        else
        {
            logger.info("FacetType is not supported!");
            return null;
        }

        index = randomGenerator.nextInt(list.size());
        message = list.get(index);
        return message;
    }
}
