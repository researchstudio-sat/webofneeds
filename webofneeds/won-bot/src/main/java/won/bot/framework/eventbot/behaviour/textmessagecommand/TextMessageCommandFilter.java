package won.bot.framework.eventbot.behaviour.textmessagecommand;

import org.apache.commons.lang3.StringUtils;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.MessageEvent;
import won.bot.framework.eventbot.filter.impl.EventListenerContextAwareFilter;
import won.protocol.message.WonMessage;
import won.protocol.util.WonRdfUtils;

/**
 * Filter that accepts Events if they are valid botCommand patterns named list.
 */
public class TextMessageCommandFilter extends EventListenerContextAwareFilter {
    public final TextMessageCommandBehaviour usageBehaviour;

    public TextMessageCommandFilter(EventListenerContext context, TextMessageCommandBehaviour usageBehaviour) {
        super(context);
        this.usageBehaviour = usageBehaviour;
    }

    @Override
    public boolean accept(Event event) {
        if (event instanceof MessageEvent) {
            MessageEvent messageEvent = (MessageEvent) event;
            String message = extractTextMessageFromWonMessage(((MessageEvent) event).getWonMessage());
            return usageBehaviour.isMatchingBotCommand(message);
        }
        return false;
    }

    private String extractTextMessageFromWonMessage(WonMessage wonMessage) {
        if (wonMessage == null)
            return null;
        String message = WonRdfUtils.MessageUtils.getTextMessage(wonMessage);
        return StringUtils.trim(message);
    }
}