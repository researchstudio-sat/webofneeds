package won.protocol.repository;

import won.protocol.model.MessageEventPlaceholder;

import java.net.URI;
import java.util.List;

public interface MessageEventRepository extends WonRepository<MessageEventPlaceholder> {

  MessageEventPlaceholder findOneByMessageURI(URI URI);

  List<MessageEventPlaceholder> findByParentURI(URI URI);

  MessageEventPlaceholder findOneByCorrespondingRemoteMessageURI(URI uri);

}
