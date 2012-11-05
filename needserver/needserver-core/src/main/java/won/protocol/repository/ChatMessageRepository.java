package won.protocol.repository;

import org.springframework.stereotype.Repository;
import won.protocol.model.ChatMessage;

/**
 * Created with IntelliJ IDEA.
 * User: Gabriel
 * Date: 04.11.12
 * Time: 16:56
 * To change this template use File | Settings | File Templates.
 */
@Repository
public interface ChatMessageRepository extends WonRepository<ChatMessage> {
}
