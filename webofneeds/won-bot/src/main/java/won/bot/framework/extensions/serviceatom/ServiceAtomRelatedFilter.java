package won.bot.framework.extensions.serviceatom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.bot.context.BotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.MessageEvent;
import won.bot.framework.eventbot.filter.impl.EventListenerContextAwareFilter;
import won.protocol.message.WonMessage;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Objects;

/**
 * Filter that only accepts Events between the serviceAtom and other
 * createdAtoms
 */
public class ServiceAtomRelatedFilter extends EventListenerContextAwareFilter {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    ServiceAtomRelatedFilter(EventListenerContext context) {
        super(context);
    }

    @Override
    public boolean accept(Event event) {
        BotContextWrapper botContextWrapper = getContext().getBotContextWrapper();
        if (botContextWrapper instanceof ServiceAtomContext && event instanceof MessageEvent) {
            ServiceAtomContext serviceAtomContext = (ServiceAtomContext) botContextWrapper;
            URI serviceAtomUri = serviceAtomContext.getServiceAtomUri();
            if (Objects.nonNull(serviceAtomUri)) {
                MessageEvent messageEvent = (MessageEvent) event;
                WonMessage wonMessage = messageEvent.getWonMessage();
                if (Objects.equals(wonMessage.getRecipientAtomURI(), serviceAtomUri)) {
                    URI senderAtomUri = wonMessage.getSenderAtomURI();
                    return getContext().getBotContext().isAtomKnown(senderAtomUri);
                } else if (Objects.equals(wonMessage.getSenderAtomURI(), serviceAtomUri)) {
                    URI recipientAtomUri = wonMessage.getRecipientAtomURI();
                    return getContext().getBotContext().isAtomKnown(recipientAtomUri);
                }
                return false;
            }
        }
        return false;
    }
}
