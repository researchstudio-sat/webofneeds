package won.node.springsecurity;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import won.cryptography.webid.AccessControlRules;
import won.node.service.impl.URIService;
import won.protocol.repository.ConnectionEventContainerRepository;
import won.protocol.repository.MessageEventRepository;
import won.protocol.repository.NeedEventContainerRepository;

/**
 * User: ypanchenko Date: 28.07.2015
 */
public class WonDefaultAccessControlRules implements AccessControlRules {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    // TODO this is tepmorary, untill the acl source is defined
    @Autowired
    protected MessageEventRepository messageEventRepository;
    @Autowired
    protected NeedEventContainerRepository needEventContainerRepository;
    @Autowired
    protected ConnectionEventContainerRepository connectionEventContainerRepository;
    @Autowired
    protected URIService uriService;

    public WonDefaultAccessControlRules() {
    }

    public boolean isAccessPermitted(String resourceUriString, List<String> requesterWebIDs) {
        // TODO retrieve from an acl source for a resource instead of this temporary
        // approach
        // specific for the message event resources
        URI resourceUri = uriService.toResourceURIIfPossible(URI.create(resourceUriString));
        String firstWebId = requesterWebIDs.get(0);
        if (requesterWebIDs.size() > 1) {
            logger.warn("received more than 1 requester webids, only using first one: ", firstWebId);
        }
        URI webId = URI.create(firstWebId);
        if (uriService.isEventURI(resourceUri)) {
            if (logger.isDebugEnabled()) {
                logger.debug("checking access for event {} with webID {} ({} of {})",
                                new Object[] { resourceUri, firstWebId, 1, requesterWebIDs.size() });
            }
            return messageEventRepository.isReadPermittedForWebID(resourceUri, webId);
        } else if (uriService.isConnectionEventsURI(resourceUri)) {
            if (logger.isDebugEnabled()) {
                logger.debug("checking access for connectionEvent{} with webID {} ({} of {})",
                                new Object[] { resourceUri, firstWebId, 1, requesterWebIDs.size() });
            }
            return connectionEventContainerRepository.isReadPermittedForWebID(
                            uriService.getConnectionURIofConnectionEventsURI(resourceUri), webId);
        } else if (uriService.isNeedEventsURI(resourceUri)) {
            if (logger.isDebugEnabled()) {
                logger.debug("checking access for needEvent {} with webID {} ({} of {})",
                                new Object[] { resourceUri, firstWebId, 1, requesterWebIDs.size() });
            }
            return this.needEventContainerRepository
                            .isReadPermittedForWebID(uriService.getNeedURIofNeedEventsURI(resourceUri), webId);
        } else if (uriService.isNeedUnreadURI(resourceUri)) {
            if (logger.isDebugEnabled()) {
                logger.debug("checking access for unreadEventsRequest {} with webID {} ({} of {})",
                                new Object[] { resourceUri, firstWebId, 1, requesterWebIDs.size() });
            }
            // only the need itself can get unread events
            return webId.equals(uriService.getNeedURIofNeedUnreadURI(resourceUri));
        }
        if (logger.isDebugEnabled()) {
            logger.debug("request could not be categorized, denying: {} with webID {} ({} of {})",
                            new Object[] { resourceUri, firstWebId, 1, requesterWebIDs.size() });
        }
        return false;
    }
}
