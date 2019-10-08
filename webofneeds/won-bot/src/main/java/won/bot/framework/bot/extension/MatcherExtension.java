package won.bot.framework.bot.extension;

import org.apache.jena.query.Dataset;
import java.net.URI;

public interface MatcherExtension {
    /**
     * The Behaviour defining this extension. For an example, see MatcherBehaviour
     */
    MatcherBehaviour getBehaviour();

    /**
     * Methods to be implemented by the Bot in order to get functionality. These
     * methods are used by BotMatcherProtocolMatcherServiceCallback and can be used
     * as wrapper methods to call methods implemented in the MatcherBehaviour
     */
    void onMatcherRegistered(URI wonNodeURI);

    void onNewAtomCreatedNotificationForMatcher(URI wonNodeURI, URI atomURI, Dataset atomModel);

    void onAtomModifiedNotificationForMatcher(URI wonNodeURI, URI atomURI);

    void onAtomActivatedNotificationForMatcher(URI wonNodeURI, URI atomURI);

    void onAtomDeactivatedNotificationForMatcher(URI wonNodeURI, URI atomURI);
    // void onAtomDeletedNotificationForMatcher(URI wonNodeURI, URI atomURI);
}
