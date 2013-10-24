package won.owner.protocol.impl;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.owner.service.impl.URIService;
import won.owner.ws.OwnerProtocolNeedClientFactory;
import won.protocol.exception.*;
import won.protocol.model.*;
import won.protocol.owner.OwnerProtocolNeedService;
import won.protocol.repository.*;
import won.protocol.rest.LinkedDataRestClient;
import won.protocol.util.ConnectionModelMapper;
import won.protocol.util.NeedModelMapper;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.WON;
import won.protocol.ws.OwnerProtocolNeedWebServiceEndpoint;
import won.protocol.ws.fault.*;

import javax.jms.JMSException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Stack;

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

    private LinkedDataRestClient linkedDataRestClient;

    private URIService uriService;


    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private NeedRepository needRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private NeedModelMapper needModelMapper;

    @Autowired
    private ConnectionModelMapper connectionModelMapper;

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
    public URI createNeed(URI ownerURI, Model content, boolean activate) throws IllegalNeedContentException {
        return createNeed(ownerURI, content, activate, null);
    }

    @Override
    public URI createNeed(URI ownerURI, Model content, boolean activate, URI wonNodeURI) throws IllegalNeedContentException {
        logger.info("need-facing: CREATE_NEED called for need {}, with content {} and activate {}",
                new Object[]{ownerURI, content, activate});

        URI uri = delegate.createNeed(ownerURI, content, activate, wonNodeURI);
        Need need = new Need();
        need.setState(activate ? NeedState.ACTIVE : NeedState.INACTIVE);
        need.setOwnerURI(ownerURI);
        need.setNeedURI(uri);
        needRepository.saveAndFlush(need);

        return uri;

    }

    @Override
    public URI connect(URI needURI, URI otherNeedURI, Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {
        logger.info("need-facing: CONNECT called for other need {}, own need {} and content {}", new Object[]{needURI, otherNeedURI, content});
        try {
            URI uri = delegate.connect(needURI, otherNeedURI, content);

            List<Connection> existingConnections = connectionRepository.findByConnectionURI(uri);
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
                //Create new connection object
                Connection con = new Connection();
                con.setNeedURI(needURI);
                con.setState(ConnectionState.REQUEST_SENT);
                con.setRemoteNeedURI(otherNeedURI);
                //set new uri
                con.setConnectionURI(uri);
                connectionRepository.saveAndFlush(con);
            }
            return uri;
        } catch (NoSuchNeedException e) {
            logger.warn("caught NoSuchNeedException", e);
        }
        return null;
    }

    @Override
    public Collection<URI> listNeedURIs() {
        logger.debug("need-facing: LIST_NEED_URIS called");
        return getHardcodedCollectionResource(NEED_URI_PATH_PREFIX);
    }

    @Override
    public Collection<URI> listNeedURIs(int page) {
        logger.debug("need-facing: LIST_NEED_URIS called for page {}", page);
        return getHardcodedCollectionResource(NEED_URI_PATH_PREFIX + "?page=" + page);
    }

    @Override
    public Collection<URI> listConnectionURIs(URI needURI) throws NoSuchNeedException {
        logger.debug("need-facing: LIST_CONNECTION_URIS called for need {}", needURI);
        return getHardcodedCollectionResource(needURI, NEED_CONNECTION_URI_PATH_SUFFIX);
    }

    @Override
    public Collection<URI> listConnectionURIs() {
        logger.debug("need-facing: LIST_CONNECTION_URIS called");
        return getHardcodedCollectionResource(CONNECTION_URI_PATH_PREFIX);
    }

    @Override
    public Collection<URI> listConnectionURIs(int page) {
        logger.debug("need-facing: LIST_CONNECTION_URIS called for page {}", page);
        return getHardcodedCollectionResource(CONNECTION_URI_PATH_PREFIX + "?page=" + page);
    }

    @Override
    public Collection<URI> listConnectionURIs(URI needURI, int page) throws NoSuchNeedException {
        logger.debug("need-facing: LIST_CONNECTION_URIS called for need {} and page {}", needURI, page);
        return getHardcodedCollectionResource(needURI, NEED_CONNECTION_URI_PATH_SUFFIX + "?page=" + page);
    }

    @Override
    public Need readNeed(URI needURI) throws NoSuchNeedException {
        logger.debug("need-facing: READ_NEED called for need {}", needURI);

        Need n = needModelMapper.fromModel(readNeedContent(needURI));
        n.setOwnerURI(uriService.getOwnerProtocolOwnerServiceEndpointURI());
        return n;
    }

    @Override
    public Model readNeedContent(URI needURI) throws NoSuchNeedException {
        logger.debug("need-facing: READ_NEED_CONTENT called for need {}", needURI);

        return getHardcodedNeedResource(needURI, "");
    }

    @Override
    public Connection readConnection(URI connectionURI) throws NoSuchConnectionException {
        logger.debug("need-facing: READ_CONNECTION called for connection {}", connectionURI);

        return connectionModelMapper.fromModel(readConnectionContent(connectionURI));
    }

    @Override
    public List<ConnectionEvent> readEvents(final URI connectionURI) throws NoSuchConnectionException {
        return eventRepository.findByConnectionURI(connectionURI);
    }

    @Override
    public Model readConnectionContent(URI connectionURI) throws NoSuchConnectionException {
        logger.debug("need-facing: READ_CONNECTION_CONTENT called for connection {}", connectionURI);
        URI connectionProtocolEndpoint = linkedDataRestClient.getURIPropertyForResource(connectionURI, WON.OWNER_PROTOCOL_ENDPOINT);
        if (connectionProtocolEndpoint == null) throw new NoSuchConnectionException(connectionURI);

        return linkedDataRestClient.readResourceData(URI.create(connectionProtocolEndpoint.toString()));
    }

    private Collection<URI> getHardcodedCollectionResource(URI needURI, String res) throws NoSuchNeedException {
        Model mUris = getHardcodedNeedResource(needURI, res);

        Stack<URI> uris = new Stack<URI>();
        for (Statement stmt : mUris.listStatements().toList()) {
            uris.push(URI.create(stmt.getObject().toString()));
        }

        return uris;
    }

    private Collection<URI> getHardcodedCollectionResource(String res) {
        Model mUris = getHardcodedResource(res);

        Stack<URI> uris = new Stack<URI>();
        for (Statement stmt : mUris.listStatements().toList()) {
            uris.push(URI.create(stmt.getObject().toString()));
        }

        return uris;
    }

    private Model getHardcodedNeedResource(URI needURI, String res) throws NoSuchNeedException {
        if (res.equals(""))
            return linkedDataRestClient.readResourceData(needURI);
        else
            return linkedDataRestClient.readResourceData(URI.create(needURI.toString() + res));
    }

    private Model getHardcodedResource(String res) {
        return linkedDataRestClient.readResourceData(
                URI.create(this.uriService.getDefaultOwnerProtocolNeedServiceEndpointURI().toString() + res));
    }

    public void setLinkedDataRestClient(LinkedDataRestClient linkedDataRestClient) {
        this.linkedDataRestClient = linkedDataRestClient;
    }

    public void setUriService(final URIService uriService) {
        this.uriService = uriService;
    }

    public void setDelegate(OwnerProtocolNeedServiceClientSide delegate) {
        this.delegate = delegate;
    }
}
