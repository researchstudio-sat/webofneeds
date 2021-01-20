package won.node.springsecurity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.cryptography.webid.AccessControlRules;
import won.node.service.nodeconfig.URIService;
import won.protocol.repository.AtomMessageContainerRepository;
import won.protocol.repository.ConnectionMessageContainerRepository;
import won.protocol.repository.MessageEventRepository;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.List;

/**
 * User: ypanchenko Date: 28.07.2015
 */
public class WonDefaultAccessControlRules implements AccessControlRules {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    // TODO this is tepmorary, untill the acl source is defined
    @Autowired
    protected MessageEventRepository messageEventRepository;
    @Autowired
    protected AtomMessageContainerRepository atomMessageContainerRepository;
    @Autowired
    protected ConnectionMessageContainerRepository connectionMessageContainerRepository;
    @Autowired
    protected URIService uriService;

    public WonDefaultAccessControlRules() {
    }

    public boolean isAccessPermitted(String resourceUriString, List<String> requesterWebIDs) {
        URI resourceUri = uriService.toResourceURIIfPossible(URI.create(resourceUriString));
        if (requesterWebIDs.isEmpty()) {
            // no client cert with webID provided - show only atom/connections
            if (uriService.isAtomURI(resourceUri) || uriService.isConnectionContainerURI(resourceUri)) {
                return true;
            } else {
                return false;
            }
        }
        String firstWebId = requesterWebIDs.get(0);
        if (requesterWebIDs.size() > 1) {
            logger.warn("received more than 1 requester webids, only using first one: ", firstWebId);
        }
        URI webId = URI.create(firstWebId);
        if (uriService.isAtomURI(resourceUri)) {
            if (logger.isDebugEnabled()) {
                logger.debug("allowing access to atom {} with webID {}", resourceUri, firstWebId);
            }
            return true;
        } else if (uriService.isMessageURI(resourceUri)) {
            if (logger.isDebugEnabled()) {
                logger.debug("checking access for event {} with webID {} ({} of {})",
                                new Object[] { resourceUri, firstWebId, 1, requesterWebIDs.size() });
            }
            URI messageUri = uriService.toGenericMessageURI(resourceUri);
            return messageEventRepository.isReadPermittedForWebID(messageUri, webId);
        } else if (uriService.isConnectionMessagesURI(resourceUri)) {
            if (logger.isDebugEnabled()) {
                logger.debug("checking access for connectionEvent{} with webID {} ({} of {})",
                                new Object[] { resourceUri, firstWebId, 1, requesterWebIDs.size() });
            }
            return connectionMessageContainerRepository.isReadPermittedForWebID(
                            uriService.getConnectionURIofConnectionMessagesURI(resourceUri), webId);
        } else if (uriService.isAtomMessagesURI(resourceUri)) {
            if (logger.isDebugEnabled()) {
                logger.debug("checking access for atomEvent {} with webID {} ({} of {})",
                                new Object[] { resourceUri, firstWebId, 1, requesterWebIDs.size() });
            }
            return this.atomMessageContainerRepository
                            .isReadPermittedForWebID(uriService.getAtomURIofAtomMessagesURI(resourceUri), webId);
        } else if (uriService.isAtomUnreadURI(resourceUri)) {
            if (logger.isDebugEnabled()) {
                logger.debug("checking access for unreadEventsRequest {} with webID {} ({} of {})",
                                new Object[] { resourceUri, firstWebId, 1, requesterWebIDs.size() });
            }
            // only the atom itself can get unread events
            return webId.equals(uriService.getAtomURIofAtomUnreadURI(resourceUri));
        }
        if (logger.isDebugEnabled()) {
            logger.debug("request could not be categorized, denying: {} with webID {} ({} of {})",
                            new Object[] { resourceUri, firstWebId, 1, requesterWebIDs.size() });
        }
        return false;
    }
}
