package won.protocol.jms;

import org.apache.camel.Exchange;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * User: LEIH-NB
 * Date: 04.11.13
 */
public interface MessagingService<T> {
    public void sendInOnlyMessage(Map properties, Map headers, Object body, String endpoint);
    public Future<T> sendInOutMessageGeneric(Map properties, Map headers, Object body, String endpoint);
    public void inspectMessage(Exchange exchange);
    public void inspectProperties(Exchange exchange);
    public void inspectHeaders(Exchange exchange);
}
