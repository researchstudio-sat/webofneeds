package won.owner.protocol.impl;

import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.exception.*;
import won.protocol.model.*;
import won.protocol.owner.OwnerProtocolNeedServiceClientSide;
import won.protocol.repository.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Implementation of the OwnerProtocolNeedService to be used on the owner side. It contains the
 * required business logic to store state and delegates calls to an injected linked data
 * client and to an injected OwnerProtocolNeedService implementation.
 * <p/>
 * User: Gabriel
 * Date: 03.12.12
 * Time: 14:42
 * TODO: refactor to separate communication code from business logic!
 * * introduce new member of type OwnerProtocolNeedService (that forwards to such a service)
 * * extract the code for proxy creation into a class implementing that interface, so that
 * connecting to a WS-based service is done in that class only (and not here)
 * * implement a JMS-based implementation of that interface and change spring config so it is used here
 */
public class OwnerProtocolNeedServiceClient implements OwnerProtocolNeedServiceClientSide {
    /* Linked Data default paths */
    private static final String NEED_URI_PATH_PREFIX = "/data/need";
    private static final String CONNECTION_URI_PATH_PREFIX = "/data/connection";
    private static final String NEED_CONNECTION_URI_PATH_SUFFIX = "/connections";
    private static final String NEED_MATCH_URI_PATH_SUFFIX = "/matches";

    final Logger logger = LoggerFactory.getLogger(getClass());





    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private NeedRepository needRepository;


    //ref=ownerProtocolNeedServiceClientJMSBased
    private OwnerProtocolNeedServiceClientSide delegate;

    @Override
    public void activate(URI needURI) throws NoSuchNeedException {
        logger.info("need-facing: ACTIVATE called for need {}", needURI);
        List<Need> needs = needRepository.findByNeedURI(needURI);
        if (needs.size() != 1)
            throw new NoSuchNeedException(needURI);
        delegate.activate(needURI);
        Need need = needs.get(0);
        need.setState(NeedState.ACTIVE);
        needRepository.saveAndFlush(need);
    }

    @Override
    public void deactivate(URI needURI) throws NoSuchNeedException {
        logger.info(MessageFormat.format("need-facing: DEACTIVATE called for need {0}", needURI));

        List<Need> needs = needRepository.findByNeedURI(needURI);
        if (needs.size() != 1)
            throw new NoSuchNeedException(needURI);
        delegate.deactivate(needURI);
        Need need = needs.get(0);
        need.setState(NeedState.INACTIVE);
        needRepository.saveAndFlush(need);

    }

    @Override
    public void open(URI connectionURI, Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        logger.info(MessageFormat.format("need-facing: OPEN called for connection {0} with model {1}", connectionURI, content));
        List<Connection> cons = connectionRepository.findByConnectionURI(connectionURI);
        if (cons.size() != 1)
            throw new NoSuchConnectionException(connectionURI);


        delegate.open(connectionURI, content);

        Connection con = cons.get(0);
        con.setState(con.getState().transit(ConnectionEventType.OWNER_OPEN));
        connectionRepository.saveAndFlush(con);

    }

    @Override
    public void close(final URI connectionURI, Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        logger.info(MessageFormat.format("need-facing: CLOSE called for connection {0} with model {1}", connectionURI, content));
        List<Connection> cons = connectionRepository.findByConnectionURI(connectionURI);
        if (cons.size() != 1)
            throw new NoSuchConnectionException(connectionURI);
        delegate.close(connectionURI, content);
        Connection con = cons.get(0);
        con.setState(con.getState().transit(ConnectionEventType.OWNER_CLOSE));
        connectionRepository.saveAndFlush(con);

    }

    @Override
    public void textMessage(final URI connectionURI, final String message)
            throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        logger.debug("need-facing: SEND_TEXT_MESSAGE called for connection {} with message {}", connectionURI, message);

        List<Connection> cons = connectionRepository.findByConnectionURI(connectionURI);
        if (cons.isEmpty())
            throw new NoSuchConnectionException(connectionURI);
        Connection con = cons.get(0);
        delegate.textMessage(connectionURI, message);

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setCreationDate(new Date());
        chatMessage.setLocalConnectionURI(connectionURI);
        chatMessage.setMessage(message);
        chatMessage.setOriginatorURI(con.getNeedURI());

        //save in the db
        chatMessageRepository.saveAndFlush(chatMessage);


    }


    @Override
    public Future<String> register() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Future<URI> createNeed(URI ownerURI, Model content, boolean activate) throws IllegalNeedContentException, ExecutionException, InterruptedException, IOException, URISyntaxException {
        return createNeed(ownerURI, content, activate, null);
    }

    @Override
    public Future<URI> createNeed(final URI ownerURI, final Model content, final boolean activate, final URI wonNodeURI) throws IllegalNeedContentException, ExecutionException, InterruptedException, IOException, URISyntaxException {
        logger.info("need-facing: CREATE_NEED called for need {}, with content {} and activate {}",
                new Object[]{ownerURI, content, activate});

        final Future<URI> uri = delegate.createNeed(ownerURI, content, activate, wonNodeURI);

        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        Need need = new Need();
                        try {
                            need.setNeedURI(uri.get());
                           // logger.info(need.getNeedURI().toString());
                            need.setState(activate ? NeedState.ACTIVE : NeedState.INACTIVE);
                            need.setOwnerURI(ownerURI);
                            needRepository.saveAndFlush(need);
                            logger.info("saving URI", need.getNeedURI().toString());
                        } catch (InterruptedException e) {
                            logger.warn("interrupted",e);
                        } catch (ExecutionException e) {
                            logger.warn("ExecutionException caught",e);
                        }


                    }
                }
        ).start();

        return uri;

    }

    @Override
    public Future<URI> connect(final URI needURI, final  URI otherNeedURI, final Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException, ExecutionException, InterruptedException {
        logger.info("need-facing: CONNECT called for other need {}, own need {} and content {}", new Object[]{needURI, otherNeedURI, content});

            final Future<URI> uri = delegate.connect(needURI, otherNeedURI, content);

            List<Connection> existingConnections = connectionRepository.findByConnectionURI(uri.get());
            if (existingConnections.size() > 0) {
                for (Connection conn : existingConnections) {
                    if (ConnectionState.CONNECTED == conn.getState() ||
                            ConnectionState.REQUEST_SENT == conn.getState()) {
                        throw new ConnectionAlreadyExistsException(conn.getConnectionURI(), needURI, otherNeedURI);
                    } else {
                        conn.setState(conn.getState().transit(ConnectionEventType.OWNER_OPEN));
                        connectionRepository.saveAndFlush(conn);
                    }
                }
            } else {
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {

                                //set new uri
                                try {
                                    //Create new connection object
                                    Connection con = new Connection();
                                    con.setNeedURI(needURI);
                                    con.setState(ConnectionState.REQUEST_SENT);
                                    con.setRemoteNeedURI(otherNeedURI);
                                    con.setConnectionURI(uri.get());
                                    connectionRepository.saveAndFlush(con);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                                } catch (ExecutionException e) {
                                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                                }

                            }
                        }
                ).start();

            }


        return uri;
    }




    public void setDelegate(OwnerProtocolNeedServiceClientSide delegate) {
        this.delegate = delegate;
    }
}
