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
 * Filter that only accepts Events regarding the serviceAtom
 */
public class ServiceAtomFilter extends EventListenerContextAwareFilter {
    public ServiceAtomFilter(EventListenerContext context) {
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
                return Objects.equals(wonMessage.getSenderAtomURI(), serviceAtomUri)
                                || Objects.equals(wonMessage.getRecipientAtomURI(), serviceAtomUri);
            }
        }
        return false;
    }
}
