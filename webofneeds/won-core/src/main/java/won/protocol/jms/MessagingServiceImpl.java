package won.protocol.jms;

import com.google.common.util.concurrent.SettableFuture;
import org.apache.camel.*;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.spi.Synchronization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import won.protocol.model.OwnerApplication;

import java.net.URI;
import java.util.*;
import java.util.concurrent.Future;

/**
 * User: LEIH-NB
 * Date: 04.11.13
 */
public class MessagingServiceImpl<T> implements ApplicationContextAware,MessagingService,CamelContextAware {
    private CamelContext camelContext;
    private ProducerTemplate producerTemplate;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private ApplicationContext applicationContext;

    /**
     * This method shall be used for Request-Reply messaging.
     * @param properties
     * @param headers
     * @param body
     * @param endpoint
     * @return
     */
    public Future<T> sendInOutMessageGeneric(Map properties, Map headers, Object body, String endpoint){
        Exchange exchange = new DefaultExchange(getCamelContext());
        //TODO: the method name shall be set in the header of the message.
        Endpoint ep = getCamelContext().getEndpoint(endpoint);
        if (properties!=null){
            if(properties.containsKey("methodName"))
                exchange.setProperty("methodName", properties.get("methodName"));
        }
        if (headers!=null)
            exchange.getIn().setHeaders(headers);
        exchange.getIn().setBody(body);
        exchange.setPattern(ExchangePattern.InOut);
        final SettableFuture<T> result = SettableFuture.create();

        producerTemplate.asyncCallback(ep,exchange, new Synchronization() {
            @Override
            public void onComplete(Exchange exchange) {
                T resultObject = (T)exchange.getOut().getBody();
                result.set(resultObject);
            }

            @Override
            public void onFailure(Exchange exchange) {
                result.cancel(true);
            }
        });




        return result;
    }

    @Override
    public void inspectMessage(Exchange exchange) {
        inspectProperties(exchange);
        inspectHeaders(exchange);
        if(exchange.getIn().getBody()!=null)
        logger.info(exchange.getIn().getBody().toString());
    }
    public void inspectProperties(Exchange exchange){
        Map properties = (Map) exchange.getProperties();
        Iterator iter =  properties.entrySet().iterator();
        logger.info("WIRETAP: properties size: "+properties.size());
        while(iter.hasNext()){
            Map.Entry pairs = (Map.Entry)iter.next();
            logger.info("key: "+pairs.getKey()+" value: "+pairs.getValue());
        }

    }


    public void inspectHeaders(Exchange exchange){
        Map headers = (Map) exchange.getIn().getHeaders();
        Iterator iter =  headers.entrySet().iterator();
        logger.info("WIRETAP: headers size: "+headers.size());
        while(iter.hasNext()){
            Map.Entry pairs = (Map.Entry)iter.next();
            if(pairs.getValue()!=null)
                logger.info("key: "+pairs.getKey()+" value: "+pairs.getValue());
        }

    }

    public void sendInOnlyMessage(Map properties, Map headers, Object body, String endpoint){
        Exchange exchange = new DefaultExchange(getCamelContext());
        Endpoint ep = getCamelContext().getEndpoint(endpoint);
        if (properties!=null){
            if(properties.containsKey("methodName"))
                exchange.setProperty("methodName", properties.get("methodName"));
            if (properties.containsKey("protocol"))
                exchange.setProperty("protocol",properties.get("protocol"));
        }
        if (headers!=null)
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

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
