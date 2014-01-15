package won.node.service.impl;

import com.hp.hpl.jena.rdf.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.exception.*;
import won.protocol.model.*;
import won.protocol.repository.*;
import won.protocol.util.DataAccessUtils;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.WON;

import java.net.URI;
import java.util.Date;
import java.util.List;

/**
 * User: gabriel
 * Date: 06/11/13
 */
public class DataAccessService {
  private RDFStorageService rdfStorageService;
  private URIService URIService;

  @Autowired
  private ChatMessageRepository chatMessageRepository;
  @Autowired
  private NeedRepository needRepository;
  @Autowired
  private ConnectionRepository connectionRepository;
  @Autowired
  private EventRepository eventRepository;
  @Autowired
  private FacetRepository facetRepository;

  public Connection createConnection(final URI needURI, final URI otherNeedURI, final URI otherConnectionURI,
                                      final Model content, final ConnectionState connectionState, final ConnectionEventType connectionEventType)
      throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {

    if (needURI == null) throw new IllegalArgumentException("needURI is not set");
    if (otherNeedURI == null) throw new IllegalArgumentException("otherNeedURI is not set");
    if (needURI.equals(otherNeedURI)) throw new IllegalArgumentException("needURI and otherNeedURI are the same");

    //Load need (throws exception if not found)
    Need need = DataAccessUtils.loadNeed(needRepository, needURI);
    if (!isNeedActive(need))
      throw new IllegalMessageForNeedStateException(needURI, connectionEventType.name(), need.getState());

    URI facetURI =  getFacet(content, WON.HAS_FACET);

    //TODO: create a proper exception if a facet is not supported by a need
    if(facetRepository.findByNeedURIAndTypeURI(needURI, facetURI).size() < 0) throw new RuntimeException("Facet is not supported by Need: " + facetURI);

    List<Connection> connections = connectionRepository.findByNeedURIAndRemoteNeedURI(needURI, otherNeedURI);
    Connection con = getConnection(connections, facetURI, connectionEventType);

    if (con == null) {
      /* Create connection */
      con = new Connection();
      con.setNeedURI(needURI);
      con.setState(connectionState);
      con.setRemoteNeedURI(otherNeedURI);
      con.setRemoteConnectionURI(otherConnectionURI);
      con.setTypeURI(facetURI);
      //save connection (this creates a new id)
      con = connectionRepository.saveAndFlush(con);
      //create and set new uri
      con.setConnectionURI(URIService.createConnectionURI(con));
      con = connectionRepository.saveAndFlush(con);

      //TODO: do we save the connection content? where? as a chat content?
    }

    return con;
  }

  public URI getFacet(Model content, Property property) {

    Resource baseRes = content.getResource(content.getNsPrefixURI(""));
    StmtIterator stmtIterator = baseRes.listProperties(property);

    if (!stmtIterator.hasNext()) {
      throw new IllegalArgumentException("at least one RDF node must be of type won:" + property.getLocalName());
    }

    return URI.create(stmtIterator.next().getObject().asResource().getURI());
  }

  public Connection getConnection(List<Connection> connections, URI facetURI, ConnectionEventType eventType)
      throws ConnectionAlreadyExistsException {
    Connection con = null;

    for(Connection c : connections) {
      //TODO: check remote need type as well or create GroupMemberFacet
      if (facetURI.equals(c.getTypeURI()))
        con = c;
    }

    /**
     * check if there already exists a connection between those two
     * we have multiple options:
     * a) no connection exists -> create new
     * b) a connection exists in state CONNECTED -> error message
     * c) a connection exists in state REQUEST_SENT. The call must be a
     * duplicate (or re-sent after the remote end hasn't replied for some time) -> error message
     * d) a connection exists in state REQUEST_RECEIVED. The remote end tried to connect before we did.
     * -> error message
     * e) a connection exists in state CLOSED -> create new
     */

    //TODO: impose unique constraint on connections
    if(con != null) {
        if(con.getState()== ConnectionState.CONNECTED || con.getState()==ConnectionState.REQUEST_SENT)
            throw new ConnectionAlreadyExistsException(con.getConnectionURI(), con.getNeedURI(), con.getRemoteNeedURI());
      /*if(!eventType.isMessageAllowed(con.getState())){
        throw new ConnectionAlreadyExistsException(con.getConnectionURI(), con.getNeedURI(), con.getRemoteNeedURI());
      }*/ else {
        //TODO: Move this to the transition() - Method in ConnectionState
        con.setState(con.getState().transit(eventType));
        con = connectionRepository.saveAndFlush(con);
      }
    }

    return con;
  }

  public ConnectionEvent createConnectionEvent(final URI connectionURI, final URI originator,
                                                final ConnectionEventType connectionEventType) {
    ConnectionEvent event = new ConnectionEvent();
    event.setConnectionURI(connectionURI);
    event.setType(connectionEventType);
    event.setOriginatorUri(originator);
    eventRepository.saveAndFlush(event);

    return event;
  }

  public Connection nextConnectionState(URI connectionURI, ConnectionEventType connectionEventType)
      throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
    if (connectionURI == null) throw new IllegalArgumentException("connectionURI is not set");
    //load connection, checking if it exists
    Connection con = DataAccessUtils.loadConnection(connectionRepository, connectionURI);
    //perform state transit
    ConnectionState nextState = performStateTransit(con, connectionEventType);
    //set new state and save in the db
    con.setState(nextState);
    //save in the db
    return connectionRepository.saveAndFlush(con);
  }

  public Connection saveChatMessage(Connection con, String message) throws NoSuchConnectionException {          //load connection, checking if it exists
    if (con == null) throw new IllegalArgumentException("connectionURI is not set");
    if (message == null) throw new IllegalArgumentException("message is not set");

    //construct chatMessage object to store in the db
    ChatMessage chatMessage = new ChatMessage();
    chatMessage.setCreationDate(new Date());
    chatMessage.setLocalConnectionURI(con.getConnectionURI());
    chatMessage.setMessage(message);
    chatMessage.setOriginatorURI(con.getNeedURI());
    //save in the db
    chatMessageRepository.saveAndFlush(chatMessage);

    return con;
  }

  public void saveAdditionalContentForEvent(final Model content, final Connection con, final ConnectionEvent event) {
    saveAdditionalContentForEvent(content, con, event, null);
  }

  public void saveAdditionalContentForEvent(final Model content, final Connection con, final ConnectionEvent event,
                                            final Double score) {
    rdfStorageService.storeContent(event,
        RdfUtils.createContentForEvent(
            this.URIService.createEventURI(con, event), content, con, event, score));
  }
    /*
    public void saveAdditionalContentForEventReplace(final Model content, final Connection con, final ConnectionEvent event)
    {
        //TODO: define what content may contain and check that here! May content contain any RDF or must it be linked to the <> node?
        Model extraDataModel = ModelFactory.createDefaultModel();
        Resource eventNode = extraDataModel.createResource(this.URIService.createEventURI(con,event).toString());
        extraDataModel.setNsPrefix("",eventNode.getURI().toString());
        if (content != null) {
            RdfUtils.replaceBaseResource(content, eventNode);
            rdfStorageService.storeContent(event, extraDataModel);
        }
    }
    */

    public void updateRemoteConnectionURI(Connection con, URI remoteConnectionURI) {
    con.setRemoteConnectionURI(remoteConnectionURI);
    connectionRepository.saveAndFlush(con);
  }

  private boolean isNeedActive(final Need need) {
    return NeedState.ACTIVE == need.getState();
  }

  /**
   * Calculates the connectionState resulting from the message in the current connection state.
   * Checks if the specified message is allowed in the connection's state and throws an exception if not.
   *
   * @param con
   * @param msg
   * @return
   * @throws won.protocol.exception.IllegalMessageForConnectionStateException
   *          if the message is not allowed in the connection's current state
   */
  private ConnectionState performStateTransit(Connection con, ConnectionEventType msg) throws IllegalMessageForConnectionStateException
  {
    if (!msg.isMessageAllowed(con.getState())) {
      throw new IllegalMessageForConnectionStateException(con.getConnectionURI(), msg.name(), con.getState());
    }
    return con.getState().transit(msg);
  }

  public void setURIService(URIService URIService) {
    this.URIService = URIService;
  }

  public void setRdfStorageService(RDFStorageService rdfStorageService) {
    this.rdfStorageService = rdfStorageService;
  }
}
