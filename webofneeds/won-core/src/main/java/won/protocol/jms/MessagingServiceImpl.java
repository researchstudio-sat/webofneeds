package won.protocol.jms;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.apache.camel.*;
import org.apache.camel.spi.Synchronization;
import org.apache.camel.support.DefaultExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.Map;

/**
 * User: LEIH-NB Date: 04.11.13
 */
@Component
public class MessagingServiceImpl<T> implements ApplicationContextAware, MessagingService, CamelContextAware {
    private static final long DEFAULT_JMS_EXPIRATION_TIME = 0;
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private CamelContext camelContext;
    private ProducerTemplate producerTemplate;
    private ApplicationContext applicationContext;

    /**
     * This method shall be used for Request-Reply messaging.
     *
     * @param properties
     * @param headers
     * @param body
     * @param endpoint
     * @return
     */
    public ListenableFuture<String> sendInOutMessageGeneric(Map properties, Map headers, Object body,
                    String endpoint) {
        Exchange exchange = new DefaultExchange(getCamelContext());
        // TODO: the method name shall be set in the header of the message.
        Endpoint ep = getCamelContext().getEndpoint(endpoint);
        if (properties != null) {
            if (properties.containsKey("methodName")) {
                exchange.setProperty("methodName", properties.get("methodName"));
            }
        }
        if (headers != null) {
            exchange.getIn().setHeaders(headers);
        }
        // exchange.getIn().getHeaders().put("CamelJmsRequestTimeout",DEFAULT_JMS_EXPIRATION_TIME);
        // exchange.setProperty("JMSExpiration",DEFAULT_JMS_EXPIRATION_TIME);
        exchange.getIn().setBody(body);
        // exchange.getOut().setBody(body);
        exchange.setPattern(ExchangePattern.InOut);
        final SettableFuture<String> result = SettableFuture.create();
        logger.debug("sending inout message");
        producerTemplate.asyncCallback(ep, exchange, new Synchronization() {
            @Override
            public void onComplete(Exchange exchange) {
                String resultObject = (String) exchange.getOut().getBody();
                result.set(resultObject);
            }

            @Override
            public void onFailure(Exchange exchange) {
                if (exchange.getException() != null) {
                    logger.warn("caught exception while sending jms message", exchange.getException());
                }
                result.cancel(true);
            }
        });
        return result;
    }

    @Override
    public void inspectMessage(Exchange exchange) {
        if (!logger.isDebugEnabled()) {
            return;
        }
        inspectProperties(exchange);
        inspectHeaders(exchange);
        if (exchange.getIn().getBody() != null) {
            logger.debug(exchange.getIn().getBody().toString());
        }
    }

    public void inspectProperties(Exchange exchange) {
        Map properties = exchange.getProperties();
        Iterator iter = properties.entrySet().iterator();
        if (logger.isDebugEnabled()) {
            logger.debug("WIRETAP: properties size: " + properties.size());
            while (iter.hasNext()) {
                Map.Entry pairs = (Map.Entry) iter.next();
                logger.debug("key: " + pairs.getKey() + " value: " + pairs.getValue());
            }
        }
    }

    public void inspectBody(Exchange exchange) {
        exchange.getIn().getBody();
    }

    public void inspectHeaders(Exchange exchange) {
        Map headers = exchange.getIn().getHeaders();
        Iterator iter = headers.entrySet().iterator();
        if (logger.isDebugEnabled()) {
            logger.debug("WIRETAP: headers size: " + headers.size());
            while (iter.hasNext()) {
                Map.Entry pairs = (Map.Entry) iter.next();
                if (pairs.getValue() != null) {
                    logger.debug("key: " + pairs.getKey() + " value: " + pairs.getValue());
                }
            }
        }
    }

    public void sendInOnlyMessage(Map properties, Map headers, Object body, String endpoint) {
        Exchange exchange = new DefaultExchange(getCamelContext());
        exchange.setPattern(ExchangePattern.InOnly);
        Endpoint ep = getCamelContext().getEndpoint(endpoint);
        if (properties != null) {
            if (properties.containsKey("methodName")) {
                exchange.setProperty("methodName", properties.get("methodName"));
            }
            if (properties.containsKey("protocol")) {
                exchange.setProperty("protocol", properties.get("protocol"));
            }
        }
        if (headers != null) {
            exchange.getIn().setHeaders(headers);
        }
        exchange.getIn().setBody(body);
        producerTemplate.send(ep, exchange);
        if (exchange.getException() != null) {
            logger.warn("caught exception while sending jms message", exchange.getException());
        }
    }

    @Override
    public void send(Exchange exchange, String endpoint) {
        Endpoint ep = getCamelContext().getEndpoint(endpoint);
        exchange.setPattern(ExchangePattern.InOnly);
        producerTemplate.send(ep, exchange);
    }

    @Override
    public CamelContext getCamelContext() {
        return this.camelContext;
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    public void setProducerTemplate(ProducerTemplate producerTemplate) {
        this.producerTemplate = producerTemplate;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
