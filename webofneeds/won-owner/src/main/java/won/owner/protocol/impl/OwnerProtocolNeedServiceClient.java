package won.owner.protocol.impl;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.RDF;
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

import java.net.MalformedURLException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Stack;

/**
 * User: Gabriel
 * Date: 03.12.12
 * Time: 14:42
 */
public class OwnerProtocolNeedServiceClient implements OwnerProtocolNeedService
{
  /* Linked Data default paths */
  private static final String NEED_URI_PATH_PREFIX = "/data/need";
  private static final String CONNECTION_URI_PATH_PREFIX = "/data/connection";
  private static final String NEED_CONNECTION_URI_PATH_SUFFIX = "/connections";
  private static final String NEED_MATCH_URI_PATH_SUFFIX = "/matches";

  final Logger logger = LoggerFactory.getLogger(getClass());

  private LinkedDataRestClient linkedDataRestClient;

  private URIService uriService;

  @Autowired
  private OwnerProtocolNeedClientFactory clientFactory;

  @Autowired
  private ChatMessageRepository chatMessageRepository;

  @Autowired
  private ConnectionRepository connectionRepository;

  @Autowired
  private NeedRepository needRepository;

  @Autowired
  private EventRepository eventRepository;

  @Autowired
  private FacetRepository facetRepository;

  @Autowired
  private MatchRepository matchRepository;

  @Autowired
  private NeedModelMapper needModelMapper;

  @Autowired
  private ConnectionModelMapper connectionModelMapper;

  @Override
  public void activate(URI needURI) throws NoSuchNeedException
  {
    logger.info("need-facing: ACTIVATE called for need {}", needURI);
    try {
      OwnerProtocolNeedWebServiceEndpoint proxy = clientFactory.getOwnerProtocolEndpointForNeed(needURI);

      List<Need> needs = needRepository.findByNeedURI(needURI);
      if (needs.size() != 1)
        throw new NoSuchNeedException(needURI);

      proxy.activate(needURI);

      Need need = needs.get(0);
      need.setState(NeedState.ACTIVE);
      needRepository.saveAndFlush(need);
    } catch (MalformedURLException e) {
      logger.warn("couldn't create URL for needProtocolEndpoint", e);
    } catch (NoSuchNeedFault noSuchNeedFault) {
      throw NoSuchNeedFault.toException(noSuchNeedFault);
    }
  }

  @Override
  public void deactivate(URI needURI) throws NoSuchNeedException
  {
    logger.info(MessageFormat.format("need-facing: DEACTIVATE called for need {0}", needURI));
    try {
      OwnerProtocolNeedWebServiceEndpoint proxy = clientFactory.getOwnerProtocolEndpointForNeed(needURI);
      List<Need> needs = needRepository.findByNeedURI(needURI);
      if (needs.size() != 1)
        throw new NoSuchNeedException(needURI);

      proxy.deactivate(needURI);

      Need need = needs.get(0);
      need.setState(NeedState.INACTIVE);
      needRepository.saveAndFlush(need);
    } catch (MalformedURLException e) {
      logger.warn("couldn't create URL for needProtocolEndpoint", e);
    } catch (NoSuchNeedFault noSuchNeedFault) {
      throw NoSuchNeedFault.toException(noSuchNeedFault);
    }
  }

  @Override
  public void open(URI connectionURI, Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    logger.info(MessageFormat.format("need-facing: OPEN called for connection {0} with model {1}", connectionURI, content));
    try {
      OwnerProtocolNeedWebServiceEndpoint proxy = clientFactory.getOwnerProtocolEndpointForConnection(connectionURI);
      List<Connection> cons = connectionRepository.findByConnectionURI(connectionURI);
      if (cons.size() != 1)
        throw new NoSuchConnectionException(connectionURI);

      proxy.open(connectionURI, RdfUtils.toString(content));

      Connection con = cons.get(0);
      con.setState(con.getState().transit(ConnectionEventType.OWNER_OPEN));
      connectionRepository.saveAndFlush(con);
    } catch (MalformedURLException e) {
      logger.warn("couldn't create URL for needProtocolEndpoint", e);
    } catch (IllegalMessageForConnectionStateFault illegalMessageForConnectionStateFault) {
      throw IllegalMessageForConnectionStateFault.toException(illegalMessageForConnectionStateFault);
    } catch (NoSuchConnectionFault noSuchConnectionFault) {
      throw NoSuchConnectionFault.toException(noSuchConnectionFault);
    }
  }

  @Override
  public void close(final URI connectionURI, Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    logger.info(MessageFormat.format("need-facing: CLOSE called for connection {0} with model {1}", connectionURI, content));
    try {
      OwnerProtocolNeedWebServiceEndpoint proxy = clientFactory.getOwnerProtocolEndpointForConnection(connectionURI);
      List<Connection> cons = connectionRepository.findByConnectionURI(connectionURI);
      if (cons.size() != 1)
        throw new NoSuchConnectionException(connectionURI);

      try {
        proxy.close(connectionURI, RdfUtils.toString(content));
      } catch (NoSuchConnectionFault noSuchConnectionFault) {
        throw NoSuchConnectionFault.toException(noSuchConnectionFault);
      } catch (IllegalMessageForConnectionStateFault illegalMessageForConnectionStateFault) {
        throw IllegalMessageForConnectionStateFault.toException(illegalMessageForConnectionStateFault);
      }

      Connection con = cons.get(0);
      con.setState(con.getState().transit(ConnectionEventType.OWNER_CLOSE));
      connectionRepository.saveAndFlush(con);
    } catch (MalformedURLException e) {
      logger.warn("couldn't create URL for needProtocolEndpoint", e);
    }
  }

  @Override
  public void textMessage(final URI connectionURI, final String message)
      throws NoSuchConnectionException, IllegalMessageForConnectionStateException
  {
    logger.debug("need-facing: SEND_TEXT_MESSAGE called for connection {} with message {}", connectionURI, message);
    try {
      OwnerProtocolNeedWebServiceEndpoint proxy = clientFactory.getOwnerProtocolEndpointForConnection(connectionURI);
      List<Connection> cons = connectionRepository.findByConnectionURI(connectionURI);
      if (cons.isEmpty())
        throw new NoSuchConnectionException(connectionURI);
      Connection con = cons.get(0);

      //send text message
      proxy.textMessage(connectionURI, message);

      ChatMessage chatMessage = new ChatMessage();
      chatMessage.setCreationDate(new Date());
      chatMessage.setLocalConnectionURI(connectionURI);
      chatMessage.setMessage(message);
      chatMessage.setOriginatorURI(con.getNeedURI());

      //save in the db
      chatMessageRepository.saveAndFlush(chatMessage);

    } catch (MalformedURLException e) {
      logger.warn("couldn't create URL for needProtocolEndpoint", e);
    } catch (IllegalMessageForConnectionStateFault illegalMessageForConnectionStateFault) {
      throw IllegalMessageForConnectionStateFault.toException(illegalMessageForConnectionStateFault);
    } catch (NoSuchConnectionFault noSuchConnectionFault) {
      throw NoSuchConnectionFault.toException(noSuchConnectionFault);
    }
  }

  @Override
  public URI createNeed(URI ownerURI, Model content, boolean activate) throws IllegalNeedContentException
  {
    return createNeed(ownerURI, content, activate, null);
  }

  public URI createNeed(URI ownerURI, Model content, boolean activate, String wonURI) throws IllegalNeedContentException
  {
    logger.info("need-facing: CREATE_NEED called for need {}, with content {} and activate {}",
        new Object[]{ownerURI, content, activate});
    try {
      OwnerProtocolNeedWebServiceEndpoint proxy = clientFactory.getOwnerProtocolEndpoint(wonURI == null ? null : URI.create(wonURI));
      content.setNsPrefix("",ownerURI.toString());
      String modelAsString = RdfUtils.toString(content);
      logger.info("model as String: \n "  + modelAsString);

      URI uri = proxy.createNeed(ownerURI, modelAsString , activate);

      Need need = new Need();
      need.setState(activate ? NeedState.ACTIVE : NeedState.INACTIVE);
      need.setOwnerURI(ownerURI);
      need.setNeedURI(uri);
      needRepository.saveAndFlush(need);

        ResIterator needIt = content.listSubjectsWithProperty(RDF.type, WON.NEED);
        if (!needIt.hasNext()) throw new IllegalArgumentException("at least one RDF node must be of type won:Need");

        Resource needRes = needIt.next();
        logger.debug("processing need resource {}", needRes.getURI());

        StmtIterator stmtIterator = content.listStatements(needRes, WON.HAS_FACET, (RDFNode) null);
        if(!stmtIterator.hasNext())
            throw new IllegalArgumentException("at least one RDF node must be of type won:HAS_FACET");
        else
            do {
                Facet facet = new Facet();
                facet.setNeedURI(need.getNeedURI());
                facet.setTypeURI(URI.create(stmtIterator.next().getObject().asResource().getURI()));
                facetRepository.save(facet);
            } while(stmtIterator.hasNext());

        return uri;
    } catch (MalformedURLException e) {
      logger.warn("couldn't create URL for needProtocolEndpoint", e);
    } catch (NoSuchNeedException e) {
      logger.warn("caught NoSuchNeedException:", e);
    } catch (IllegalNeedContentFault illegalNeedContentFault) {
      throw IllegalNeedContentFault.toException(illegalNeedContentFault);
    }
    return null;
  }

  @Override
  public URI connect(URI needURI, URI otherNeedURI, Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException
  {
    logger.info("need-facing: CONNECT called for other need {}, own need {} and content {}", new Object[]{needURI, otherNeedURI, content});
    try {
      OwnerProtocolNeedWebServiceEndpoint proxy = clientFactory.getOwnerProtocolEndpointForNeed(needURI);
      URI uri = proxy.connect(needURI, otherNeedURI, RdfUtils.toString(content));

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
    } catch (MalformedURLException e) {
      logger.warn("couldn't create URL for needProtocolEndpoint", e);
    } catch (NoSuchNeedException e) {
      logger.warn("caught NoSuchNeedException", e);
    } catch (NoSuchNeedFault noSuchNeedFault) {
      throw NoSuchNeedFault.toException(noSuchNeedFault);
    } catch (ConnectionAlreadyExistsFault connectionAlreadyExistsFault) {
      throw ConnectionAlreadyExistsFault.toException(connectionAlreadyExistsFault);
    } catch (IllegalMessageForNeedStateFault illegalMessageForNeedStateFault) {
      throw IllegalMessageForNeedStateFault.toException(illegalMessageForNeedStateFault);
    }
    return null;
  }

  @Override
  public Collection<URI> listNeedURIs()
  {
    logger.debug("need-facing: LIST_NEED_URIS called");
    return getHardcodedCollectionResource(NEED_URI_PATH_PREFIX);
  }

  @Override
  public Collection<URI> listNeedURIs(int page)
  {
    logger.debug("need-facing: LIST_NEED_URIS called for page {}", page);
    return getHardcodedCollectionResource(NEED_URI_PATH_PREFIX + "?page=" + page);
  }

  @Override
  public Collection<URI> listConnectionURIs(URI needURI) throws NoSuchNeedException
  {
    logger.debug("need-facing: LIST_CONNECTION_URIS called for need {}", needURI);
    return getHardcodedCollectionResource(needURI, NEED_CONNECTION_URI_PATH_SUFFIX);
  }

  @Override
  public Collection<URI> listConnectionURIs()
  {
    logger.debug("need-facing: LIST_CONNECTION_URIS called");
    return getHardcodedCollectionResource(CONNECTION_URI_PATH_PREFIX);
  }

  @Override
  public Collection<URI> listConnectionURIs(int page)
  {
    logger.debug("need-facing: LIST_CONNECTION_URIS called for page {}", page);
    return getHardcodedCollectionResource(CONNECTION_URI_PATH_PREFIX + "?page=" + page);
  }

  @Override
  public Collection<URI> listConnectionURIs(URI needURI, int page) throws NoSuchNeedException
  {
    logger.debug("need-facing: LIST_CONNECTION_URIS called for need {} and page {}", needURI, page);
    return getHardcodedCollectionResource(needURI, NEED_CONNECTION_URI_PATH_SUFFIX + "?page=" + page);
  }

  @Override
  public Need readNeed(URI needURI) throws NoSuchNeedException
  {
    logger.debug("need-facing: READ_NEED called for need {}", needURI);

    Need n = needModelMapper.fromModel(readNeedContent(needURI));
    n.setOwnerURI(uriService.getOwnerProtocolOwnerServiceEndpointURI());
    return n;
  }

  @Override
  public Model readNeedContent(URI needURI) throws NoSuchNeedException
  {
    logger.debug("need-facing: READ_NEED_CONTENT called for need {}", needURI);

    return getHardcodedNeedResource(needURI, "");
  }

  @Override
  public Connection readConnection(URI connectionURI) throws NoSuchConnectionException
  {
    logger.debug("need-facing: READ_CONNECTION called for connection {}", connectionURI);

    return connectionModelMapper.fromModel(readConnectionContent(connectionURI));
  }

  @Override
  public List<ConnectionEvent> readEvents(final URI connectionURI) throws NoSuchConnectionException
  {
    return eventRepository.findByConnectionURI(connectionURI);
  }

  @Override
  public Model readConnectionContent(URI connectionURI) throws NoSuchConnectionException
  {
    logger.debug("need-facing: READ_CONNECTION_CONTENT called for connection {}", connectionURI);
    URI connectionProtocolEndpoint = linkedDataRestClient.getURIPropertyForResource(connectionURI, WON.OWNER_PROTOCOL_ENDPOINT);
    if (connectionProtocolEndpoint == null) throw new NoSuchConnectionException(connectionURI);

    return linkedDataRestClient.readResourceData(URI.create(connectionProtocolEndpoint.toString()));
  }

  private Collection<URI> getHardcodedCollectionResource(URI needURI, String res) throws NoSuchNeedException
  {
    Model mUris = getHardcodedNeedResource(needURI, res);

    Stack<URI> uris = new Stack<URI>();
    for (Statement stmt : mUris.listStatements().toList()) {
      uris.push(URI.create(stmt.getObject().toString()));
    }

    return uris;
  }

  private Collection<URI> getHardcodedCollectionResource(String res)
  {
    Model mUris = getHardcodedResource(res);

    Stack<URI> uris = new Stack<URI>();
    for (Statement stmt : mUris.listStatements().toList()) {
      uris.push(URI.create(stmt.getObject().toString()));
    }

    return uris;
  }

  private Model getHardcodedNeedResource(URI needURI, String res) throws NoSuchNeedException
  {
    if (res.equals(""))
      return linkedDataRestClient.readResourceData(needURI);
    else
      return linkedDataRestClient.readResourceData(URI.create(needURI.toString() + res));
  }

  private Model getHardcodedResource(String res)
  {
    return linkedDataRestClient.readResourceData(
        URI.create(this.uriService.getDefaultOwnerProtocolNeedServiceEndpointURI().toString() + res));
  }

  public void setLinkedDataRestClient(LinkedDataRestClient linkedDataRestClient)
  {
    this.linkedDataRestClient = linkedDataRestClient;
  }

  public void setUriService(final URIService uriService)
  {
    this.uriService = uriService;
  }

  public void setClientFactory(final OwnerProtocolNeedClientFactory clientFactory)
  {
    this.clientFactory = clientFactory;
  }
}
