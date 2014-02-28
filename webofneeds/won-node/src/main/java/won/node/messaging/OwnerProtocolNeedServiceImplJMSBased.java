package won.node.messaging;

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
import won.protocol.repository.NeedRepository;
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
public class OwnerProtocolNeedServiceImplJMSBased{// implements //ownerProtocolNeedService{ /*, WonMessageListener*/
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private OwnerProtocolNeedService delegate;

    private OwnerManagementService ownerManagementService;
    @Autowired
    private QueueManagementService queueManagementService;

    private OwnerFacingNeedCommunicationService needCommunicationService;
    @Autowired
    private NeedRepository needRepository;

    private ProducerTemplate producerTemplate;

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

    public URI createNeed(
            @Header("ownerURI") String ownerURI,
            @Header("model") String content,
            @Header("activate") boolean activate,
            @Header("ownerApplicationID") String ownerApplicationID,
            Exchange exchange) throws IllegalNeedContentException, JMSException {

        URI connectionURI = null;
        URI ownerURIconvert = URI.create(ownerURI);
        Model contentconvert = RdfUtils.toModel(content);

        logger.info("createNeed: message received: {} with ownerApp ID {}", content,ownerApplicationID);
        connectionURI = delegate.createNeed(ownerURIconvert, contentconvert, activate,ownerApplicationID );
        exchange.getOut().setBody(connectionURI);

       return connectionURI;
    }

    public void activate(
            @Header("needURI") String needURI) throws NoSuchNeedException {
        logger.info("activateNeed: message received: {}", needURI);

        URI needURIconvert = URI.create(needURI);
        delegate.activate(needURIconvert);
    }

    public void deactivate(
            @Header("needURI") String needURI) throws NoSuchNeedException, NoSuchConnectionException, IllegalMessageForConnectionStateException {
        logger.info("deactivateNeed: message received: {}", needURI);

        URI needURIconvert = URI.create(needURI);
        delegate.deactivate(needURIconvert);
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
            @Header("content")String content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        URI connectionURIConvert = URI.create(connectionURI);
        Model contentConvert = RdfUtils.toModel(content);
        delegate.open(connectionURIConvert, contentConvert);
    }

    public void close(
            @Header("connectionURI")String connectionURI,
            @Header("content")String content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {

        URI connectionURIConvert = URI.create(connectionURI);
        Model contentConvert = RdfUtils.toModel(content);
        delegate.close(connectionURIConvert, contentConvert);
    }

    public void textMessage(
            @Header("connectionURI") String connectionURI,
            @Header("message")String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        URI connectionURIconvert = URI.create(connectionURI);
        Model contentConvert = RdfUtils.toModel(message);
        delegate.textMessage(connectionURIconvert, contentConvert);
    }

    public URI connect(
            @Header("needURI") String needURI,
            @Header("otherNeedURI") String otherNeedURI,
            @Header("content") String content, Exchange exchange) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {
        logger.info("connectNeed: message received: {}", content);

        URI result = null;
        URI needURIConvert = URI.create(needURI);
        URI otherNeedURIConvert = URI.create(otherNeedURI);
        Model contentConvert = RdfUtils.toModel(content);
        result = delegate.connect(needURIConvert,otherNeedURIConvert,contentConvert);
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

    public void setOwnerManagementService(OwnerManagementService ownerManagementService) {
        this.ownerManagementService = ownerManagementService;
    }
}
