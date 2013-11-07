package won.protocol.jms;

import com.google.common.util.concurrent.SettableFuture;
import org.apache.camel.*;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.spi.Synchronization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.jms.MessagingService;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * User: LEIH-NB
 * Date: 04.11.13
 */
public class MessagingServiceImpl implements MessagingService,CamelContextAware {
    private CamelContext camelContext;
    private ProducerTemplate producerTemplate;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public Future<URI> sendInOutMessage(String methodName, Map headers, Object body, String endpoint){
        Exchange exchange = new DefaultExchange(getCamelContext());

        Endpoint ep = getCamelContext().getEndpoint("outgoingMessages");
        exchange.setProperty("methodName", methodName);
        exchange.getIn().setHeaders(headers);
        exchange.getIn().setBody(body);
        exchange.setPattern(ExchangePattern.InOut);
        final SettableFuture<URI> result = SettableFuture.create();

        producerTemplate.asyncCallback(ep,exchange, new Synchronization() {
            @Override
            public void onComplete(Exchange exchange) {
                URI resultObject = (URI)exchange.getOut().getBody();
                result.set(resultObject);
            }

            @Override
            public void onFailure(Exchange exchange) {
                result.cancel(true);
            }
        });

        logger.info("sending InOut Message: "+ methodName);


        return result;
    }
    public void sendInOnlyMessage(String methodName, Map headers, Object body, String endpoint){
        Exchange exchange = new DefaultExchange(getCamelContext());
        Endpoint ep = getCamelContext().getEndpoint(endpoint);
        exchange.setProperty("methodName", methodName);
        exchange.getIn().setHeaders(headers);
        exchange.getIn().setBody(body);
        producerTemplate.send(ep, exchange);
    }
    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public CamelContext getCamelContext() {
        return this.camelContext;
    }

    public void setProducerTemplate(ProducerTemplate producerTemplate) {
        this.producerTemplate = producerTemplate;
    }
}
