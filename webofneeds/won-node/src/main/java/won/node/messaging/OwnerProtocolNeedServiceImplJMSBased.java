package won.node.messaging;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import org.apache.camel.Exchange;
import org.apache.camel.Header;
import org.apache.camel.ProducerTemplate;
import org.apache.jena.riot.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.exception.*;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDecoder;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEvent;
import won.protocol.model.Need;
import won.protocol.owner.OwnerProtocolNeedService;
import won.protocol.service.ApplicationManagementService;
import won.protocol.service.OwnerFacingNeedCommunicationService;
import won.protocol.service.QueueManagementService;
import won.protocol.util.RdfUtils;

import javax.jms.JMSException;
import java.net.URI;
import java.util.Collection;
import java.util.List;

/**
 * User: LEIH-NB
 * Date: 22.10.13
 */
public class OwnerProtocolNeedServiceImplJMSBased{// implements //ownerProtocolNeedService{ /*, WonMessageListener*/
    private final Logger logger = LoggerFactory.getLogger(getClass());



    @Autowired
    private OwnerProtocolNeedService delegate;

    private ApplicationManagementService ownerManagementService;
    @Autowired
    private QueueManagementService queueManagementService;

    private OwnerFacingNeedCommunicationService needCommunicationService;

    private ProducerTemplate producerTemplate;

    public List<String> getEndpointsForOwnerApplication(
            @Header("ownerApplicationID") String ownerApplicationID, Exchange exchange){
        logger.debug("get endpoints: message received");
        List<String> endpoints = queueManagementService.getEndpointsForOwnerApplication(ownerApplicationID);
        return endpoints;
    }

    public String registerOwnerApplication(
            Exchange exchange) throws IllegalNeedContentException, JMSException {
        logger.debug("register: message received");
        String ownerApplicationId = ownerManagementService.registerOwnerApplication();

        return ownerApplicationId;
    }

    public URI createNeed(
            @Header("model") String content,
            @Header("activate") boolean activate,
            @Header("ownerApplicationID") String ownerApplicationID,
            @Header("wonMessage") String wonMessageString,
            Exchange exchange) throws IllegalNeedContentException, JMSException {

        URI needURI = null;
        Model contentconvert = RdfUtils.toModel(content);
        WonMessage wonMessage = WonMessageDecoder.decode(Lang.TRIG,wonMessageString);
        //Dataset messageEvent = RdfUtils.toDataset(messageEventString);
        logger.debug("createNeed: message received: {} with ownerApp ID {}", content,ownerApplicationID);
        logger.debug("createNeed: message event received: {} with ownerApp ID {}",
                     RdfUtils.toString(wonMessage.getMessageContent()),
                     ownerApplicationID);

        needURI = delegate.createNeed(
                contentconvert,
                activate,
                ownerApplicationID,
                wonMessage);
        exchange.getOut().setBody(needURI);

       return needURI;
    }

    public void activate(
            @Header("needURI") String needURI,
            @Header("messageEvent") String messageEventString)
            throws NoSuchNeedException {
        logger.debug("activateNeed: message received: {}", needURI);

        URI needURIconvert = URI.create(needURI);
        Dataset messageEvent = RdfUtils.toDataset(messageEventString);

        delegate.activate(needURIconvert, messageEvent);
    }

    public void deactivate(
            @Header("needURI") String needURI,
            @Header("messageEvent") String messageEventString)
            throws NoSuchNeedException, NoSuchConnectionException, IllegalMessageForConnectionStateException {
        logger.debug("deactivateNeed: message received: {}", needURI);

        URI needURIconvert = URI.create(needURI);
        Dataset messageEvent = RdfUtils.toDataset(messageEventString);

        delegate.deactivate(needURIconvert, messageEvent);
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

    public void open(
            @Header("connectionURI")String connectionURI,
            @Header("content")String content,
            @Header("messageEvent") String messageEventString)
      throws NoSuchConnectionException, IllegalMessageForConnectionStateException, IllegalMessageForNeedStateException {

        URI connectionURIConvert = URI.create(connectionURI);
        Model contentConvert = RdfUtils.toModel(content);
        Dataset messageEvent = RdfUtils.toDataset(messageEventString);

        delegate.open(connectionURIConvert, contentConvert, messageEvent);
    }

    public void close(
            @Header("connectionURI")String connectionURI,
            @Header("content")String content,
            @Header("messageEvent") String messageEventString)
            throws NoSuchConnectionException, IllegalMessageForConnectionStateException {

        URI connectionURIConvert = URI.create(connectionURI);
        Model contentConvert = RdfUtils.toModel(content);
        Dataset messageEvent = RdfUtils.toDataset(messageEventString);

        delegate.close(connectionURIConvert, contentConvert, messageEvent);
    }

    public void sendMessage(
      @Header("connectionURI") String connectionURI,
      @Header("message") String message,
      @Header("messageEvent") String messageEventString)
            throws NoSuchConnectionException, IllegalMessageForConnectionStateException {

        URI connectionURIconvert = URI.create(connectionURI);
        Model contentConvert = RdfUtils.toModel(message);
        Dataset messageEvent = RdfUtils.toDataset(messageEventString);

        delegate.sendMessage(connectionURIconvert, contentConvert, messageEvent);
    }

    public URI connect(
            @Header("needURI") String needURI,
            @Header("otherNeedURI") String otherNeedURI,
            @Header("content") String content, Exchange exchange,
            @Header("messageEvent") String messageEventString)
            throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {
        logger.debug("connectNeed: message received: {}", content);

        URI result = null;
        URI needURIConvert = URI.create(needURI);
        URI otherNeedURIConvert = URI.create(otherNeedURI);
        Model contentConvert = RdfUtils.toModel(content);
        Dataset messageEvent = RdfUtils.toDataset(messageEventString);

        result = delegate.connect(needURIConvert,otherNeedURIConvert,contentConvert, messageEvent);
       // result = needCommunicationService.connect(needURIConvert, otherNeedURIConvert, contentConvert);

        return result;
    }

    public void setDelegate(OwnerProtocolNeedService delegate) {
        this.delegate = delegate;
    }


    public void setProducerTemplate(ProducerTemplate producerTemplate) {
        this.producerTemplate = producerTemplate;
    }

    public void setNeedCommunicationService(OwnerFacingNeedCommunicationService needCommunicationService) {
        this.needCommunicationService = needCommunicationService;
    }

    public void setOwnerManagementService(ApplicationManagementService ownerManagementService) {
        this.ownerManagementService = ownerManagementService;
    }
}
