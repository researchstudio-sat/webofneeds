package won.protocol.jms;

import java.util.Map;

import org.apache.camel.Exchange;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * User: LEIH-NB
 * Date: 04.11.13
 */
public interface MessagingService<T> {
    public void sendInOnlyMessage(Map properties, Map headers, Object body, String endpoint);
    public ListenableFuture<T> sendInOutMessageGeneric(Map properties, Map headers, Object body, String endpoint);
    public void inspectMessage(Exchange exchange);
    public void inspectProperties(Exchange exchange);
    public void inspectHeaders(Exchange exchange);
}
