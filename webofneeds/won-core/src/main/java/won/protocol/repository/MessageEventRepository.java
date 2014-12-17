package won.protocol.repository;

import won.protocol.model.MessageEventPlaceholder;

import java.net.URI;
import java.util.List;

public interface MessageEventRepository extends WonRepository<MessageEventPlaceholder> {

  List<MessageEventPlaceholder> findByMessageURI(URI URI);

  List<MessageEventPlaceholder> findByParentURI(URI URI);

}
