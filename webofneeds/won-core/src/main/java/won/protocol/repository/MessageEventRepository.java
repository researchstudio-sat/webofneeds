package won.protocol.repository;

import won.protocol.message.MessageEvent;
import won.protocol.model.MessageEventPlaceholder;

import java.net.URI;
import java.util.List;

public interface MessageEventRepository extends WonRepository<MessageEventPlaceholder> {

  List<MessageEvent> findByMessageURI(URI URI);

  List<MessageEvent> findByParentURI(URI URI);

}
