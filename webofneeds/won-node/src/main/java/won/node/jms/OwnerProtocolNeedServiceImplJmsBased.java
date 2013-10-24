package won.node.jms;

import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.exception.*;
import won.protocol.jms.WonMessageListener;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEvent;
import won.protocol.model.Need;
import won.protocol.owner.OwnerProtocolNeedService;


import javax.jms.Message;
import java.net.URI;
import java.util.Collection;
import java.util.List;

/**
 * User: LEIH-NB
 * Date: 22.10.13
 */
public class OwnerProtocolNeedServiceImplJMSBased implements OwnerProtocolNeedService, WonMessageListener {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    OwnerProtocolNeedService delegate;

    @Override
    public void onMessage(Message message) {
        logger.info("message received: {}", message);
    }

    @Override
    public void consume(URI connectionURI, Message msg) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public URI createNeed(URI ownerURI, Model content, boolean activate) throws IllegalNeedContentException {
        return delegate.createNeed(ownerURI, content, activate);
    }

    @Override
    public void activate(URI needURI) throws NoSuchNeedException {
        delegate.activate(needURI);
    }

    @Override
    public void deactivate(URI needURI) throws NoSuchNeedException {
        delegate.deactivate(needURI);
    }

    @Override
    public Collection<URI> listNeedURIs() {
        return delegate.listNeedURIs();
    }

    @Override
    public Collection<URI> listNeedURIs(int page) {
        return delegate.listNeedURIs(page);
    }

    @Override
    public Collection<URI> listConnectionURIs(URI needURI) throws NoSuchNeedException {
        return delegate.listConnectionURIs(needURI);
    }

    @Override
    public Collection<URI> listConnectionURIs() {
        return delegate.listConnectionURIs();
    }

    @Override
    public Collection<URI> listConnectionURIs(int page) {
        return delegate.listConnectionURIs(page);
    }

    @Override
    public Collection<URI> listConnectionURIs(URI needURI, int page) throws NoSuchNeedException {
        return delegate.listConnectionURIs(needURI, page);
    }

    @Override
    public Need readNeed(URI needURI) throws NoSuchNeedException {
        return delegate.readNeed(needURI);
    }

    @Override
    public Model readNeedContent(URI needURI) throws NoSuchNeedException {
        return delegate.readNeedContent(needURI);
    }

    @Override
    public Connection readConnection(URI connectionURI) throws NoSuchConnectionException {
        return delegate.readConnection(connectionURI);
    }

    @Override
    public List<ConnectionEvent> readEvents(URI connectionURI) throws NoSuchConnectionException {
        return delegate.readEvents(connectionURI);
    }

    @Override
    public Model readConnectionContent(URI connectionURI) throws NoSuchConnectionException {
        return delegate.readConnectionContent(connectionURI);
    }

    @Override
    public void open(URI connectionURI, Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        delegate.open(connectionURI, content);
    }

    @Override
    public void close(URI connectionURI, Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        delegate.close(connectionURI, content);
    }

    @Override
    public void textMessage(URI connectionURI, String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        delegate.textMessage(connectionURI, message);
    }

    @Override
    public URI connect(URI needURI, URI otherNeedURI, Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {
        return delegate.connect(needURI, otherNeedURI, content);
    }

    public void setDelegate(OwnerProtocolNeedService delegate) {
        this.delegate = delegate;
    }
}
