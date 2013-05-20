package won.owner.protocol.impl;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.owner.service.impl.URIService;
import won.owner.ws.OwnerProtocolNeedWebServiceClient;
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

import java.io.StringWriter;
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
  /* default wsdl location */
  private static final String WSDL_LOCATION = "?wsdl";

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

  @Autowired
  private RdfUtils rdfUtils;

    @Override
    public void deactivate(URI needURI) throws NoSuchNeedException {
        logger.info(MessageFormat.format("need-facing: DEACTIVATE called for need {0}", needURI));
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy =getOwnerProtocolEndpointForNeed(needURI);
            List<Need> needs = needRepository.findByNeedURI(needURI);
            if(needs.size() != 1)
                throw new NoSuchNeedException(needURI);

            proxy.deactivate(needURI);
                  Need need = needs.get(0);
            need.setState(NeedState.INACTIVE);
            needRepository.saveAndFlush(need);
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        }
    }

    @Override
    public void open(URI connectionURI, Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        logger.info(MessageFormat.format("need-facing: OPEN called for connection {0} with model {1}", connectionURI, content));
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForConnection(connectionURI);
            List<Connection> cons = connectionRepository.findByConnectionURI(connectionURI);
            if(cons.size() != 1)
                throw new NoSuchConnectionException(connectionURI);

            proxy.open(connectionURI, rdfUtils.toString(content));

            Connection con = cons.get(0);
            con.setState(con.getState().transit(ConnectionEventType.OWNER_OPEN));
            connectionRepository.saveAndFlush(con);
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        }
    }

    @Override
    public void close(final URI connectionURI, Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
    {
        logger.info(MessageFormat.format("need-facing: CLOSE called for connection {0} with model {1}", connectionURI, content));
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForConnection(connectionURI);
            List<Connection> cons = connectionRepository.findByConnectionURI(connectionURI);
            if(cons.size() != 1)
                throw new NoSuchConnectionException(connectionURI);

            proxy.close(connectionURI, rdfUtils.toString(content));

            Connection con = cons.get(0);
            con.setState(con.getState().transit(ConnectionEventType.OWNER_CLOSE));
            connectionRepository.saveAndFlush(con);
        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } 
    }

    @Override
    public void sendTextMessage(final URI connectionURI, final String message)
            throws NoSuchConnectionException, IllegalMessageForConnectionStateException
    {
        logger.debug(MessageFormat.format("need-facing: SEND_TEXT_MESSAGE called for connection {0} with message {1}", connectionURI, message));
        try {
            OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForConnection(connectionURI);
            List<Connection> cons = connectionRepository.findByConnectionURI(connectionURI);
            if(cons.isEmpty())
                throw new NoSuchConnectionException(connectionURI);
            Connection con = cons.get(0);

            //send text message
            proxy.sendTextMessage(connectionURI, message);

            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setCreationDate(new Date());
            chatMessage.setLocalConnectionURI(connectionURI);
            chatMessage.setMessage(message);
            chatMessage.setOriginatorURI(con.getNeedURI());

            //save in the db
            chatMessageRepository.saveAndFlush(chatMessage);

        } catch (MalformedURLException e) {
            logger.warn("couldn't create URL for needProtocolEndpoint", e);
        } 
    }

  public void setLinkedDataRestClient(LinkedDataRestClient linkedDataRestClient)
  {
    this.linkedDataRestClient = linkedDataRestClient;
  }

  public void setUriService(final URIService uriService)
  {
    this.uriService = uriService;
  }

  @Override
  public URI createNeed(URI ownerURI, Model content, boolean activate) throws IllegalNeedContentException
  {
    logger.info(MessageFormat.format("need-facing: CREATE_NEED called for need {0}, with content {1} and activate {2}", ownerURI, content, activate));
    try {
      OwnerProtocolNeedWebServiceEndpoint proxy = getHardcodedOwnerProtocolEndpointForNeed();

      URI uri = proxy.createNeed(ownerURI, rdfUtils.toString(content), activate);

      Need need = new Need();
      need.setState(activate ? NeedState.ACTIVE : NeedState.INACTIVE);
      need.setOwnerURI(ownerURI);
      need.setNeedURI(uri);
      needRepository.saveAndFlush(need);

      return uri;
    } catch (MalformedURLException e) {
      logger.warn("couldn't create URL for needProtocolEndpoint", e);
    } catch (NoSuchNeedException e) {
      e.printStackTrace();
    }
    return null;
  }

  public URI createNeed(URI ownerURI, Model content, boolean activate, String wonURI) throws IllegalNeedContentException
  {
    logger.info(MessageFormat.format("need-facing: CREATE_NEED called for need {0}, with content {1} and activate {2}", ownerURI, content, activate));
    try {
      OwnerProtocolNeedWebServiceEndpoint proxy = getHardcodedOwnerProtocolEndpointForNeed(wonURI);

      URI uri = proxy.createNeed(ownerURI, rdfUtils.toString(content), activate);

      Need need = new Need();
      need.setState(activate ? NeedState.ACTIVE : NeedState.INACTIVE);
      need.setOwnerURI(ownerURI);
      need.setNeedURI(uri);
      needRepository.saveAndFlush(need);

      return uri;
    } catch (MalformedURLException e) {
      logger.warn("couldn't create URL for needProtocolEndpoint", e);
    } catch (NoSuchNeedException e) {
      e.printStackTrace();
    }
    return null;
  }


  @Override
  public void activate(URI needURI) throws NoSuchNeedException
  {
    logger.info(MessageFormat.format("need-facing: ACTIVATE called for need {0}", needURI));
    try {
      OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForNeed(needURI);

      List<Need> needs = needRepository.findByNeedURI(needURI);
      if (needs.size() != 1)
        throw new NoSuchNeedException(needURI);

      proxy.activate(needURI);

      Need need = needs.get(0);
      need.setState(NeedState.ACTIVE);
      needRepository.saveAndFlush(need);
    } catch (MalformedURLException e) {
      logger.warn("couldn't create URL for needProtocolEndpoint", e);
    }
  }

  //TODO: add connect Method which takes a connectionURI as a argument
  @Override
  public URI connect(URI needURI, URI otherNeedURI, Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException
  {
    logger.info(MessageFormat.format("need-facing: CONNECT_TO called for other need {0}, own need {1} and content {2}", needURI, otherNeedURI, content));
    try {
      OwnerProtocolNeedWebServiceEndpoint proxy = getOwnerProtocolEndpointForNeed(needURI);

      URI uri = proxy.connect(needURI, otherNeedURI, rdfUtils.toString(content));

      List<Connection> existingConnections = connectionRepository.findByConnectionURI(uri);
      if (existingConnections.size() > 0){
        for(Connection conn: existingConnections){
            //TODO: Move this to the transition() - Method in ConnectionState
            if (ConnectionState.CONNECTED == conn.getState() ||
                    ConnectionState.REQUEST_SENT == conn.getState()) {
                throw new ConnectionAlreadyExistsException(conn.getConnectionURI(),needURI,otherNeedURI);
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
    logger.debug(MessageFormat.format("need-facing: LIST_NEED_URIS called for page {0}", page));
    return getHardcodedCollectionResource(NEED_URI_PATH_PREFIX + "?page=" + page);
  }

  @Override
  public Collection<URI> listConnectionURIs(URI needURI) throws NoSuchNeedException
  {
    logger.debug(MessageFormat.format("need-facing: LIST_CONNECTION_URIS called for need {0}", needURI));

    // TODO: probably wrong
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
    logger.debug(MessageFormat.format("need-facing: LIST_CONNECTION_URIS called for page {0}", page));
    return getHardcodedCollectionResource(CONNECTION_URI_PATH_PREFIX + "?page=" + page);
  }

  @Override
  public Collection<URI> listConnectionURIs(URI needURI, int page) throws NoSuchNeedException
  {
    logger.debug(MessageFormat.format("need-facing: LIST_CONNECTION_URIS called for need {0} and page {1}", needURI, page));
    return getHardcodedCollectionResource(needURI, NEED_CONNECTION_URI_PATH_SUFFIX + "?page=" + page);
  }


  @Override
  public Collection<Match> listMatches(URI needURI) throws NoSuchNeedException
  {
    logger.debug(MessageFormat.format("need-facing: GET_MATCHES called for need {0}", needURI));
    //TODO: implement this
    return null;
  }

  @Override
  public Collection<Match> listMatches(URI needURI, int page) throws NoSuchNeedException
  {
    logger.debug(MessageFormat.format("need-facing: GET_MATCHES called for need {0} and page {1}", needURI, page));
    //TODO: implement this
    return null;
  }

  @Override
  public Need readNeed(URI needURI) throws NoSuchNeedException
  {
    logger.debug(MessageFormat.format("need-facing: READ_NEED called for need {0}", needURI));

    Need n = needModelMapper.fromModel(readNeedContent(needURI));
    n.setOwnerURI(uriService.getOwnerProtocolOwnerServiceEndpointURI());
    return n;
  }

  @Override
  public Model readNeedContent(URI needURI) throws NoSuchNeedException
  {
    logger.debug(MessageFormat.format("need-facing: READ_NEED_CONTENT called for need {0}", needURI));

    return getHardcodedNeedResource(needURI, "");
  }

  @Override
  public Connection readConnection(URI connectionURI) throws NoSuchConnectionException
  {
    logger.debug(MessageFormat.format("need-facing: READ_CONNECTION called for connection {0}", connectionURI));

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
    logger.debug(MessageFormat.format("need-facing: READ_CONNECTION_CONTENT called for connection {0}", connectionURI));
    URI connectionProtocolEndpoint = linkedDataRestClient.getURIPropertyForResource(connectionURI, WON.OWNER_PROTOCOL_ENDPOINT);
    logger.debug("need protocol endpoint of need {} is {}", connectionURI.toString(), connectionProtocolEndpoint.toString());
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

  private OwnerProtocolNeedWebServiceEndpoint getOwnerProtocolEndpointForNeed(URI needURI) throws NoSuchNeedException, MalformedURLException
  {
    //TODO: fetch endpoint information for the need and store in db?
    URI needProtocolEndpoint = linkedDataRestClient.getURIPropertyForResource(needURI, WON.OWNER_PROTOCOL_ENDPOINT);
    logger.debug("need protocol endpoint of need {} is {}", needURI.toString(), needProtocolEndpoint.toString());
    if (needProtocolEndpoint == null) throw new NoSuchNeedException(needURI);
    OwnerProtocolNeedWebServiceClient client = new OwnerProtocolNeedWebServiceClient(URI.create(needProtocolEndpoint.toString() + WSDL_LOCATION).toURL());
    return client.getOwnerProtocolOwnerWebServiceEndpointPort();
  }

  //TODO: workaround until we can work with multiple WON nodes: protocol URI is hard-coded in spring properties
  private OwnerProtocolNeedWebServiceEndpoint getHardcodedOwnerProtocolEndpointForNeed(String ownerProtocolWONURI) throws NoSuchNeedException, MalformedURLException
  {
    //TODO: fetch endpoint information for the need and store in db?
    OwnerProtocolNeedWebServiceClient client = new OwnerProtocolNeedWebServiceClient(URI.create(ownerProtocolWONURI + WSDL_LOCATION).toURL());
    return client.getOwnerProtocolOwnerWebServiceEndpointPort();
  }

  //TODO: workaround until we can work with multiple WON nodes: protocol URI is hard-coded in spring properties
  private OwnerProtocolNeedWebServiceEndpoint getHardcodedOwnerProtocolEndpointForNeed() throws NoSuchNeedException, MalformedURLException
  {

    //TODO: fetch endpoint information for the need and store in db?
    OwnerProtocolNeedWebServiceClient client = new OwnerProtocolNeedWebServiceClient(URI.create((this.uriService.getDefaultOwnerProtocolNeedServiceEndpointURI().toString() + WSDL_LOCATION)).toURL());
    return client.getOwnerProtocolOwnerWebServiceEndpointPort();
  }

  private OwnerProtocolNeedWebServiceEndpoint getOwnerProtocolEndpointForConnection(URI connectionURI) throws NoSuchConnectionException, MalformedURLException
  {
    //TODO: fetch endpoint information for the need and store in db?
    URI needProtocolEndpoint = linkedDataRestClient.getURIPropertyForResource(connectionURI, WON.OWNER_PROTOCOL_ENDPOINT);
    logger.debug("need protocol endpoint of connection {} is {}", connectionURI.toString(), needProtocolEndpoint.toString());
    if (needProtocolEndpoint == null) throw new NoSuchConnectionException(connectionURI);
    OwnerProtocolNeedWebServiceClient client = new OwnerProtocolNeedWebServiceClient(URI.create(needProtocolEndpoint.toString() + WSDL_LOCATION).toURL());
    return client.getOwnerProtocolOwnerWebServiceEndpointPort();
  }
}
