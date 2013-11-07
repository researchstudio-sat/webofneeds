package won.protocol.jms;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * User: LEIH-NB
 * Date: 04.11.13
 */
public interface MessagingService {
    public Future<URI> sendInOutMessage(String methodName, Map headers, Object body, String endpoint);
    public void sendInOnlyMessage(String methodName, Map headers, Object body, String endpoint);
}
