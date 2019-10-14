package won.bot.framework.extensions.matcher;

import org.apache.jena.query.Dataset;

import java.net.URI;

@FunctionalInterface
public interface MatcherExtension {
    /**
     * The Behaviour defining this extension. For an example, see MatcherBehaviour
     */
    MatcherBehaviour getMatcherBehaviour();

    default void onMatcherRegistered(final URI wonNodeUri) {
        if (getMatcherBehaviour().isActive()) {
            getMatcherBehaviour().getEventBus().publish(new MatcherExtensionRegisterSucceededEvent(wonNodeUri));
        }
    }

    default void onNewAtomCreatedNotificationForMatcher(final URI wonNodeURI, final URI atomURI,
                    final Dataset atomModel) {
        if (getMatcherBehaviour().isActive()) {
            Dataset dataset = getMatcherBehaviour().getEventListenerContext().getLinkedDataSource()
                            .getDataForResource(atomURI);
            getMatcherBehaviour().getEventBus().publish(new MatcherExtensionAtomCreatedEvent(atomURI, dataset));
        }
    }

    default void onAtomModifiedNotificationForMatcher(final URI wonNodeURI, final URI atomURI) {
        if (getMatcherBehaviour().isActive()) {
            getMatcherBehaviour().getEventBus().publish(new MatcherExtensionAtomModifiedEvent(atomURI));
        }
    }

    default void onAtomActivatedNotificationForMatcher(final URI wonNodeURI, final URI atomURI) {
        if (getMatcherBehaviour().isActive()) {
            getMatcherBehaviour().getEventBus().publish(new MatcherExtensionAtomActivatedEvent(atomURI));
        }
    }

    default void onAtomDeactivatedNotificationForMatcher(final URI wonNodeURI, final URI atomURI) {
        if (getMatcherBehaviour().isActive()) {
            getMatcherBehaviour().getEventBus().publish(new MatcherExtensionAtomDeactivatedEvent(atomURI));
        }
    }
    // void onAtomDeletedNotificationForMatcher(URI wonNodeURI, URI atomURI);
}
