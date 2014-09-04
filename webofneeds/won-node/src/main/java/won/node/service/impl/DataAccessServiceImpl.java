package won.node.service.impl;

import com.hp.hpl.jena.graph.TripleBoundary;
import com.hp.hpl.jena.rdf.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.repository.rdfstorage.RDFStorageService;
import won.protocol.exception.*;
import won.protocol.model.*;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.EventRepository;
import won.protocol.repository.FacetRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.util.DataAccessUtils;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;
import won.protocol.vocabulary.WON;

import java.net.URI;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * User: gabriel
 * Date: 06/11/13
 */
public class DataAccessServiceImpl implements won.node.service.DataAccessService
{
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private RDFStorageService rdfStorageService;
  private URIService URIService;

  @Autowired
  private NeedRepository needRepository;
  @Autowired
  private ConnectionRepository connectionRepository;
  @Autowired
  private EventRepository eventRepository;
  @Autowired
  private FacetRepository facetRepository;

  /**
   * Creates a new Connection object. Expects <> won:hasFacet [FACET] in the RDF content, will throw exception if it's not there.
   * @param needURI
   * @param otherNeedURI
   * @param otherConnectionURI
   * @param content
   * @param connectionState
   * @param connectionEventType
   * @return
   * @throws NoSuchNeedException
   * @throws IllegalMessageForNeedStateException
   * @throws ConnectionAlreadyExistsException
   */
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

    URI facetURI =  getFacet(content);
    if (facetURI == null) throw new IllegalArgumentException("at least one RDF node must be of type won:" + WON.HAS_FACET.getLocalName());
    //TODO: create a proper exception if a facet is not supported by a need
    if(facetRepository.findByNeedURIAndTypeURI(needURI, facetURI).isEmpty()) throw new RuntimeException("Facet is not supported by Need: " + facetURI);

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
      con = connectionRepository.save(con);
      //create and set new uri
      con.setConnectionURI(URIService.createConnectionURI(con));
      con = connectionRepository.save(con);

      //TODO: do we save the connection content? where? as a chat content?
    }

    return con;
  }

  @Override
  public Collection<URI> getSupportedFacets(URI needUri) throws NoSuchNeedException
  {
    List<URI> ret = new LinkedList<URI>();
    Need need = DataAccessUtils.loadNeed(needRepository, needUri);
    Model content = rdfStorageService.loadModel(need.getNeedURI());
    if (content == null) return ret;
    Resource baseRes = content.getResource(content.getNsPrefixURI(""));
    StmtIterator stmtIterator = baseRes.listProperties(WON.HAS_FACET);
    while (stmtIterator.hasNext()) {
      RDFNode object = stmtIterator.nextStatement().getObject();
      if (object.isURIResource()){
        ret.add(URI.create(object.toString()));
      }
    }
    return ret;
  }

  /**
   * Returns the first facet found in the model, attached to the null relative URI '<>'.
   * Returns null if there is no such facet.
   * @param content
   * @return
   */
  @Override
  public URI getFacet(Model content) {
    return WonRdfUtils.FacetUtils.getFacet(content);
  }

  /**
   * Adds a triple to the model of the form <> won:hasFacet [facetURI].
   * @param content
   * @param facetURI
   */
  @Override
  public void addFacet(final Model content, final URI facetURI)
  {
    WonRdfUtils.FacetUtils.addFacet(content, facetURI);
  }

  @Override
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
        //TODO: Move this to the transition() - Method in ATConnectionState
        con.setState(con.getState().transit(eventType));
        con = connectionRepository.save(con);
      }
    }

    return con;
  }

  @Override
  public ConnectionEvent createConnectionEvent(final URI connectionURI, final URI originator,
    final ConnectionEventType connectionEventType) {
    ConnectionEvent event = new ConnectionEvent();
    event.setConnectionURI(connectionURI);
    event.setType(connectionEventType);
    event.setOriginatorUri(originator);
    eventRepository.save(event);

    return event;
  }

  @Override
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
    return connectionRepository.save(con);
  }

  /**
   * Adds feedback, represented by the subgraph reachable from feedback, to the RDF description of the
   * item identified by forResource
   * @param forResource
   * @param feedback
   * @return true if feedback could be added false otherwise
   */
  @Override
  public boolean addFeedback(final URI forResource, final Resource feedback){
    //TODO: concurrent modifications to the model for this resource result in side-effects.
    //think about locking.
    logger.debug("adding feedback to resource {}", forResource);
    Model model = rdfStorageService.loadModel(forResource);
    if (model == null) {
      logger.debug("could not add feedback to resource {}: no such resource found", forResource );
      return false;
    }
    Resource mainRes = model.getResource(forResource.toString());
    if (mainRes == null){
      logger.debug("could not add feedback to resource {}: resource not found in model");
      return false;
    }

    mainRes.addProperty(WON.HAS_FEEDBACK, feedback);
    ModelExtract extract = new ModelExtract(new StatementTripleBoundary(TripleBoundary.stopNowhere));
    model.add(extract.extract(feedback, feedback.getModel()));
    logger.debug("done adding feedback for resource {}, storing...", forResource);
    rdfStorageService.storeModel(forResource, model);
    logger.debug("stored feedback");
    return true;
  }

  @Override
  public void saveAdditionalContentForEvent(final Model content, final Connection con, final ConnectionEvent event) {
    saveAdditionalContentForEvent(content, con, event, null);
  }

  @Override
  public void saveAdditionalContentForEvent(final Model content, final Connection con, final ConnectionEvent event,
    final Double score) {
    rdfStorageService.storeModel(event,
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
            rdfStorageService.storeModel(event, extraDataModel);
        }
    }
    */

  @Override
  public void updateRemoteConnectionURI(Connection con, URI remoteConnectionURI) {
    if (logger.isDebugEnabled()) {
      logger.debug("updating remote connection URI of con {} to {}", con, remoteConnectionURI);
    }
    con.setRemoteConnectionURI(remoteConnectionURI);
    connectionRepository.save(con);
  }

  private boolean isNeedActive(final Need need) {
    return NeedState.ACTIVE == need.getState();
  }

  /**
   * Calculates the ATConnectionState resulting from the message in the current connection state.
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

  @Override
  public void setURIService(URIService URIService) {
    this.URIService = URIService;
  }

  @Override
  public void setRdfStorageService(RDFStorageService rdfStorageService) {
    this.rdfStorageService = rdfStorageService;
  }


}
