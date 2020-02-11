package won.bot.framework.extensions.serviceatom.filter;

import won.bot.framework.bot.context.BotContextWrapper;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.MessageEvent;
import won.bot.framework.eventbot.filter.impl.EventListenerContextAwareFilter;
import won.bot.framework.extensions.serviceatom.ServiceAtomContext;
import won.protocol.message.WonMessage;

import java.net.URI;
import java.util.Objects;

/**
 * Filter that only accepts Events between the serviceAtom and other
 * createdAtoms
 */
public class ServiceAtomCreatedAtomRelationFilter extends EventListenerContextAwareFilter {
    public ServiceAtomCreatedAtomRelationFilter(EventListenerContext context) {
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
                    return getContext().getBotContextWrapper().isAtomKnown(senderAtomUri);
                } else if (Objects.equals(wonMessage.getSenderAtomURI(), serviceAtomUri)) {
                    URI recipientAtomUri = wonMessage.getRecipientAtomURI();
                    return getContext().getBotContextWrapper().isAtomKnown(recipientAtomUri);
                }
                return false;
            }
        }
        return false;
    }
}
