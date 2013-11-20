package won.node.jms;

import com.hp.hpl.jena.rdf.model.Model;
import org.apache.camel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.exception.*;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEvent;
import won.protocol.model.Need;
import won.protocol.owner.OwnerProtocolNeedService;
import won.protocol.service.*;
import won.protocol.util.RdfUtils;
import javax.jms.JMSException;
import java.net.URI;
import java.util.Collection;
import java.util.List;

/**
 * User: LEIH-NB
 * Date: 22.10.13
 */
public class OwnerProtocolNeedServiceImplJMSBased {//implements OwnerProtocolNeedService /*, WonMessageListener*/ {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private OwnerProtocolNeedService delegate;
    private ConnectionCommunicationService connectionCommunicationService;
   // private ActiveMQComponent activeMQComponent;
    private NeedManagementService needManagementService;
    private OwnerManagementService ownerManagementService;
    @Autowired
    private QueueManagementService queueManagementService;

    private OwnerFacingNeedCommunicationService needCommunicationService;

    private ProducerTemplate producerTemplate;
    /*
    @Override
    public void onMessage(Message message) {
        logger.info("onMessage: message received: {}", message);
    }

    @Override
    public void consume(URI connectionURI, Message msg) {
        //To change body of implemented methods use File | Settings | File Templates.
    } */

   // @Consume(uri="bean:activemq:queue:WON.REGISTER")

    public List<String> getEndpointsForOwnerApplication(
            @Header("ownerApplicationID") String ownerApplicationID, Exchange exchange){
        logger.info("get endpoints: message received");
        List<String> endpoints = queueManagementService.getEndpointsForOwnerApplication(ownerApplicationID);
        return endpoints;
    }
    public String registerOwnerApplication(
            Exchange exchange) throws IllegalNeedContentException, JMSException {
        logger.info("register: message received");
        String ownerApplicationId = ownerManagementService.registerOwnerApplication();
        return ownerApplicationId;
    }

    @Consume(uri="bean:activemq:queue:WON.CREATENEED")
    public URI createNeed(
            @Header("ownerURI") String ownerURI,
            @Header("model") String content,
            @Header("activate") boolean activate,
            @Header("ownerApplicationID") String ownerApplicationID,
            Exchange exchange) throws IllegalNeedContentException, JMSException {
        URI connectionURI = null;
        URI ownerURIconvert = URI.create(ownerURI);
        Model contentconvert = RdfUtils.toModel(content);

        logger.info(ownerURI);
        logger.info("pattern: ", exchange.getPattern());
        logger.info(exchange.getProperties().toString());
        logger.info("createNeed: message received: {} with ownerApp ID {}", content,ownerApplicationID);

        connectionURI = needManagementService.createNeed(ownerURIconvert, contentconvert, activate, ownerApplicationID);
        exchange.getOut().setBody(connectionURI);
       return connectionURI;
    }

    @Consume(uri="bean:activemq:queue:WON.ACTIVATENEED")
    public void activate(@Header("needURI") String needURI) throws NoSuchNeedException {
        URI needURIconvert = URI.create(needURI);
        logger.info("activateNeed: message received: {}", needURI);
        needManagementService.activate(needURIconvert);
    }
    @Consume(uri="bean:activemq:queue:WON.DEACTIVATENEED")
    public void deactivate(@Header("needURI") String needURI) throws NoSuchNeedException{

        URI needURIconvert = URI.create(needURI);
        logger.info("deactivateNeed: message received: {}", needURI);
        needManagementService.deactivate(needURIconvert);
    }

    //@Override
    public Collection<URI> listNeedURIs() {
        return delegate.listNeedURIs();
    }

    //@Override
    public Collection<URI> listNeedURIs(int page) {
        return delegate.listNeedURIs(page);
    }

    //@Override
    public Collection<URI> listConnectionURIs(URI needURI) throws NoSuchNeedException {
        return delegate.listConnectionURIs(needURI);
    }

    //@Override
    public Collection<URI> listConnectionURIs() {
        return delegate.listConnectionURIs();
    }

   // @Override
    public Collection<URI> listConnectionURIs(int page) {
        return delegate.listConnectionURIs(page);
    }

   // @Override
    public Collection<URI> listConnectionURIs(URI needURI, int page) throws NoSuchNeedException {
        return delegate.listConnectionURIs(needURI, page);
    }

   // @Override
    public Need readNeed(URI needURI) throws NoSuchNeedException {
        return delegate.readNeed(needURI);
    }

   // @Override
    public Model readNeedContent(URI needURI) throws NoSuchNeedException {
        return delegate.readNeedContent(needURI);
    }

  //  @Override
    public Connection readConnection(URI connectionURI) throws NoSuchConnectionException {
        return delegate.readConnection(connectionURI);
    }

   // @Override
    public List<ConnectionEvent> readEvents(URI connectionURI) throws NoSuchConnectionException {
        return delegate.readEvents(connectionURI);
    }

   // @Override
    public Model readConnectionContent(URI connectionURI) throws NoSuchConnectionException {
        return delegate.readConnectionContent(connectionURI);
    }

    @Consume(uri="bean:activemq:queue:WON.OPEN")
    public void open(@Header("connectionURI")String connectionURI, @Header("content")String content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        URI connectionURIConvert = URI.create(connectionURI);
        Model contentConvert = RdfUtils.toModel(content);
        connectionCommunicationService.open(connectionURIConvert, contentConvert);
    }

    @Consume(uri="bean:activemq:queue:WON.CLOSE")
    public void close(@Header("connectionURI")String connectionURI, @Header("content")String content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        URI connectionURIConvert = URI.create(connectionURI);
        Model contentConvert = RdfUtils.toModel(content);
        connectionCommunicationService.open(connectionURIConvert, contentConvert);
    }

    @Consume(uri="bean:activemq:queue:WON.TEXTMESSAGE")
    public void textMessage(@Header("connectionURI") String connectionURI, @Header("message")String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        URI connectionURIconvert = URI.create(connectionURI);

        connectionCommunicationService.textMessage(connectionURIconvert, message);
    }


    @Consume(uri="bean:activemq:queue:WON.CONNECTNEED")
    public URI connect(@Header("needURI") String needURI, @Header("otherNeedURI") String otherNeedURI, @Header("content") String content, Exchange exchange) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {
        URI result = null;
        URI needURIConvert = URI.create(needURI);
        URI otherNeedURIConvert = URI.create(otherNeedURI);
        Model contentConvert = RdfUtils.toModel(content);
        logger.info(needURIConvert.toString());
        logger.info("pattern: ", exchange.getPattern());
        logger.info(exchange.getProperties().toString());
        logger.info("connectNeed: message received: {}", content);
        result = needCommunicationService.connect(needURIConvert, otherNeedURIConvert, contentConvert);


        return result;
    }

    public void setDelegate(OwnerProtocolNeedService delegate) {
        this.delegate = delegate;
    }

    public NeedManagementService getNeedManagementService() {
        return needManagementService;
    }

    public void setNeedManagementService(NeedManagementService needManagementService) {
        this.needManagementService = needManagementService;
    }

   /* public void setActiveMQComponent(ActiveMQComponent activeMQComponent) {
        this.activeMQComponent = activeMQComponent;
    }              */

    public void setProducerTemplate(ProducerTemplate producerTemplate) {
        this.producerTemplate = producerTemplate;
    }

    public void setConnectionCommunicationService(ConnectionCommunicationService connectionCommunicationService) {
        this.connectionCommunicationService = connectionCommunicationService;
    }
    public void setNeedCommunicationService(OwnerFacingNeedCommunicationService needCommunicationService) {
        this.needCommunicationService = needCommunicationService;
    }

    public void setOwnerManagementService(OwnerManagementService ownerManagementService) {
        this.ownerManagementService = ownerManagementService;
    }
}
