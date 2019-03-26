package won.protocol.service.impl;

import java.net.URI;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import won.protocol.model.unread.UnreadMessageInfoForConnection;
import won.protocol.model.unread.UnreadMessageInfoForNeed;
import won.protocol.repository.MessageEventRepository;

@Component
public class UnreadInformationService {
  @Autowired
  private MessageEventRepository messageEventRepository;

  public UnreadMessageInfoForNeed getUnreadInformation(URI needURI, Collection<URI> lastSeenMessageURIs) {
    List<UnreadMessageInfoForConnection> unreadInfoForConnections = messageEventRepository.getUnreadInfoForNeed(needURI,
        lastSeenMessageURIs);
    UnreadMessageInfoForNeed result = new UnreadMessageInfoForNeed(needURI);
    unreadInfoForConnections.forEach(info -> result.addUnreadMessageInfoForConnection(info));
    return result;
  }

}
