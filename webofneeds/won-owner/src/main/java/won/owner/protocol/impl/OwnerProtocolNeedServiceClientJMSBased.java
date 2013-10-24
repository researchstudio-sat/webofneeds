/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.owner.protocol.impl;
import javax.jms.Destination;
import com.hp.hpl.jena.rdf.model.Model;
import org.apache.camel.*;
import org.apache.camel.impl.DefaultExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.owner.ws.OwnerProtocolNeedClientFactory;
import won.protocol.exception.*;
import won.protocol.model.ConnectionEvent;
import won.protocol.model.Need;
import won.protocol.util.RdfUtils;

import javax.jms.JMSException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: LEIH-NB
 * Date: 17.10.13
 */
public class OwnerProtocolNeedServiceClientJMSBased implements OwnerProtocolNeedServiceClientSide, CamelContextAware {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private MessageProducer messageProducer;
    private OwnerProtocolNeedClientFactory clientFactory;
    private ProducerTemplate producerTemplate;
    private Destination destination;
    private org.springframework.jms.connection.CachingConnectionFactory con;
    private CamelContext camelContext;


    @Override
    public URI connect(URI needURI, URI otherNeedURI, Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {
       throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public void deactivate(URI needURI) throws NoSuchNeedException {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public void activate(URI needURI) throws NoSuchNeedException {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public URI createNeed(URI ownerURI, Model content, boolean activate) throws IllegalNeedContentException {
        return createNeed(ownerURI, content, activate,null);

    }

    @Override
    public void textMessage(URI connectionURI, String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        try {
            URI brokerURI = new URI("vm://localhost");
            messageProducer.textMessage(brokerURI, message);

        } catch (URISyntaxException e) {
            logger.warn("Wrong URI syntax", e);  //To change body of catch statement use File | Settings | File Templates.
        } catch (JMSException e) {
            logger.warn("JMS Exception", e);  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    @Override
    public void close(URI connectionURI, Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public void open(URI connectionURI, Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public URI createNeed(URI ownerURI, Model content, boolean activate, URI wonNodeURI) throws IllegalNeedContentException {
        //throw new UnsupportedOperationException("not yet implemented");

                Exchange exchange = new DefaultExchange(getCamelContext());
              //  exchange.setPattern("InOut");
              //  NeedMessageCreator nmc = new NeedMessageCreatorImpl(ownerURI, content, activate);
              //  Session session = connection.createSession(false,Session.AUTO_ACKNOWLEDGE);
               // Message message = needMessageCreator.createMessage(session, ownerURI, content, activate);
                Endpoint ep = getCamelContext().getEndpoint("needMessageQueue");
                Map mapMessage = new HashMap();
                mapMessage.put("ownerURI", ownerURI.toString());
                mapMessage.put("model", RdfUtils.toString(content));
                mapMessage.put("activate", activate);
                exchange.getIn().setBody(mapMessage);
                //camelContext.
                producerTemplate.send(ep,exchange);
                logger.info("sending create message:owner-uri");
                return null;
              //  Endpoint endpoint = c
                // Exchange exchange =

           // OwnerProtocolNeedWebServiceEndpoint proxy = clientFactory.getOwnerProtocolEndpoint(wonNodeURI);
           // content.setNsPrefix("",ownerURI.toString());
            //String modelAsString = RdfUtils.toString(content);
            //return proxy.createNeed(ownerURI, modelAsString , activate);
  /*     } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } catch (NoSuchNeedException e) {
            logger.warn("caught NoSuchNeedException:", e);
        } catch (IllegalNeedContentFault illegalNeedContentFault) {
            throw IllegalNeedContentFault.toException(illegalNeedContentFault);   */

    }

    @Override
    public Collection<URI> listNeedURIs() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Collection<URI> listNeedURIs(int page) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Collection<URI> listConnectionURIs(URI needURI) throws NoSuchNeedException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Collection<URI> listConnectionURIs() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Collection<URI> listConnectionURIs(int page) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Collection<URI> listConnectionURIs(URI needURI, int page) throws NoSuchNeedException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Need readNeed(URI needURI) throws NoSuchNeedException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Model readNeedContent(URI needURI) throws NoSuchNeedException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public won.protocol.model.Connection readConnection(URI connectionURI) throws NoSuchConnectionException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public List<ConnectionEvent> readEvents(URI connectionURI) throws NoSuchConnectionException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Model readConnectionContent(URI connectionURI) throws NoSuchConnectionException {
        throw new UnsupportedOperationException("not implemented");
    }
    public void setClientFactory(OwnerProtocolNeedClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public void setMessageProducer(MessageProducer messageProducer) {
        this.messageProducer = messageProducer;
    }

    public void setProducerTemplate(ProducerTemplate producerTemplate) {

        this.producerTemplate = producerTemplate;
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }



    @Override
    public CamelContext getCamelContext() {
        return camelContext;
    }
}
