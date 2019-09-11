package won.protocol.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.protocol.model.unread.UnreadMessageInfoForAtom;
import won.protocol.model.unread.UnreadMessageInfoForConnection;
import won.protocol.repository.MessageEventRepository;

import java.net.URI;
import java.util.Collection;
import java.util.List;

@Component
public class UnreadInformationService {
    @Autowired
    private MessageEventRepository messageEventRepository;

    public UnreadMessageInfoForAtom getUnreadInformation(URI atomURI, Collection<URI> lastSeenMessageURIs) {
        List<UnreadMessageInfoForConnection> unreadInfoForConnections = messageEventRepository
                        .getUnreadInfoForAtom(atomURI, lastSeenMessageURIs);
        UnreadMessageInfoForAtom result = new UnreadMessageInfoForAtom(atomURI);
        unreadInfoForConnections.forEach(result::addUnreadMessageInfoForConnection);
        return result;
    }
}
