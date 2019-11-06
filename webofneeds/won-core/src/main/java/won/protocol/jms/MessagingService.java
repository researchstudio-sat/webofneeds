package won.protocol.jms;

import java.util.Map;

import org.apache.camel.Exchange;

/**
 * User: LEIH-NB Date: 04.11.13
 */
public interface MessagingService {
    void sendInOnlyMessage(Map properties, Map headers, Object body, String endpoint);

    void inspectMessage(Exchange exchange);

    void inspectProperties(Exchange exchange);

    void inspectHeaders(Exchange exchange);

    void send(Exchange exchange, String endpoint);
}
