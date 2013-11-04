package won.owner.protocol.impl;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.owner.protocol.impl.OwnerProtocolNeedReadService;
import won.owner.service.impl.URIService;
import won.protocol.exception.*;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionEvent;
import won.protocol.model.Need;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.EventRepository;
import won.protocol.repository.MatchRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.rest.LinkedDataRestClient;
import won.protocol.util.ConnectionModelMapper;
import won.protocol.util.NeedModelMapper;
import won.protocol.vocabulary.WON;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 04.04.13
 * Time: 14:07
 * To change this template use File | Settings | File Templates.
 */
public class OwnerProtocolNeedReadServiceImpl implements OwnerProtocolNeedReadService {
   // private OwnerProtocolNeedServiceClientSide ownerService;
    private static final String NEED_URI_PATH_PREFIX = "/data/need";
    private static final String CONNECTION_URI_PATH_PREFIX = "/data/connection";
    private static final String NEED_CONNECTION_URI_PATH_SUFFIX = "/connections";
    private static final String NEED_MATCH_URI_PATH_SUFFIX = "/matches";
    private URIService uriService;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private LinkedDataRestClient linkedDataRestClient;

    @Autowired
    private NeedRepository needRepository;

    @Autowired
    private ConnectionRepository connectionRepository;
    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private NeedModelMapper needModelMapper;

    @Autowired
    private ConnectionModelMapper connectionModelMapper;


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

    @Override
    public Future<URI> createNeed(URI ownerURI, Model content, boolean activate) throws IllegalNeedContentException, ExecutionException, InterruptedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void activate(URI needURI) throws NoSuchNeedException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void deactivate(URI needURI) throws NoSuchNeedException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Future<URI> createNeed(URI ownerURI, Model content, boolean activate, URI wonNodeURI) throws IllegalNeedContentException, ExecutionException, InterruptedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void open(URI connectionURI, Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void close(URI connectionURI, Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void textMessage(URI connectionURI, String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Future<URI> connect(URI needURI, URI otherNeedURI, Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException, ExecutionException, InterruptedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
