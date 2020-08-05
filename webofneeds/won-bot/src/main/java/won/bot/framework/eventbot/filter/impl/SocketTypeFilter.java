package won.bot.framework.eventbot.filter.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.event.BaseAtomAndConnectionSpecificEvent;
import won.bot.framework.eventbot.event.Event;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Filter that only accepts Events that are directed to the given socketType(s)
 * or socketTypeUri(s)
 */
public class SocketTypeFilter extends EventListenerContextAwareFilter {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final Set<URI> allowedSocketTypes = new HashSet<>();

    public SocketTypeFilter(EventListenerContext context, URI socketType, URI... additionalSocketTypes) {
        super(context);
        allowedSocketTypes.add(socketType);
        if (additionalSocketTypes != null && additionalSocketTypes.length > 0) {
            allowedSocketTypes.addAll(Arrays.asList(additionalSocketTypes));
        }
    }

    @Override
    public boolean accept(Event event) {
        if (event instanceof BaseAtomAndConnectionSpecificEvent) {
            BaseAtomAndConnectionSpecificEvent msgEvent = (BaseAtomAndConnectionSpecificEvent) event;
            URI atomUri = msgEvent.getAtomURI();
            URI socketUri = msgEvent.getSocketURI();
            logger.debug("SocketTypeFilter check for atomUri: " + atomUri + " socketUri: " + socketUri);
            Optional<URI> socketType = WonLinkedDataUtils.getTypeOfSocket(socketUri,
                            getContext().getLinkedDataSource());
            if (socketType.isPresent()) {
                return allowedSocketTypes.contains(socketType.get());
            }
        }
        return false;
    }
}
