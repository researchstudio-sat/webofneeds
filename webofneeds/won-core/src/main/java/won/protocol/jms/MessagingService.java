package won.protocol.jms;

import java.util.Map;

import org.apache.camel.Exchange;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * User: LEIH-NB Date: 04.11.13
 */
public interface MessagingService<T> {
    void sendInOnlyMessage(Map properties, Map headers, Object body, String endpoint);

    ListenableFuture<T> sendInOutMessageGeneric(Map properties, Map headers, Object body, String endpoint);

    void inspectMessage(Exchange exchange);

    void inspectProperties(Exchange exchange);

    void inspectHeaders(Exchange exchange);
}
