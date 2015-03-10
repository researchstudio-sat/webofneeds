package won.owner.protocol.impl;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hp.hpl.jena.rdf.model.Model;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.message.WonMessage;
import won.protocol.model.*;
import won.protocol.owner.OwnerProtocolNeedServiceClientSide;
import won.protocol.repository.ChatMessageRepository;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.FacetRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;

import java.net.URI;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Implementation of the ownerProtocolNeedService to be used on the owner side. It contains the
 * required business logic to store state and delegates calls to an injected linked data
 * client and to an injected ownerProtocolNeedService implementation.
 * <p/>
 * User: Gabriel
 * Date: 03.12.12
 * Time: 14:42
 * TODO: refactor to separate communication code from business logic!
 */
public class OwnerProtocolNeedServiceClient implements OwnerProtocolNeedServiceClientSide
{
  /* Linked Data default paths */
  private static final String NEED_URI_PATH_PREFIX = "/data/need";
  private static final String CONNECTION_URI_PATH_PREFIX = "/data/connection";
  private static final String NEED_CONNECTION_URI_PATH_SUFFIX = "/connections";
  private static final String NEED_MATCH_URI_PATH_SUFFIX = "/matches";
  final Logger logger = LoggerFactory.getLogger(getClass());
  @Value(value = "${uri.node.default}")
  String wonNodeDefault;
  private ApplicationContext ownerApplicationContext;
  @Autowired
  private FacetRepository facetRepository;
  @Autowired
  private ChatMessageRepository chatMessageRepository;
  @Autowired
  private ConnectionRepository connectionRepository;
  @Autowired
  private NeedRepository needRepository;
  @Autowired
  private Executor executor;

  //ref=ownerProtocolNeedServiceClientJMSBased
  private OwnerProtocolNeedServiceClientSide delegate;

  @Override
  @Transactional(propagation = Propagation.SUPPORTS)
  public void activate(URI needURI, WonMessage wonMessage) throws Exception {
    logger.debug("owner to need: ACTIVATE called for need {}", needURI);
    List<Need> needs = needRepository.findByNeedURI(needURI);
    if (needs.size() != 1)
      throw new NoSuchNeedException(needURI);
    delegate.activate(needURI, wonMessage);
    Need need = needs.get(0);
    need.setState(NeedState.ACTIVE);
    needRepository.save(need);
  }

  @Override
  @Transactional(propagation = Propagation.SUPPORTS)
  public void deactivate(URI needURI, WonMessage wonMessage) throws Exception {
    logger.debug("owner to need: DEACTIVATE called for need {}", needURI);

    List<Need> needs = needRepository.findByNeedURI(needURI);
    if (needs.size() != 1)
      throw new NoSuchNeedException(needURI);
    delegate.deactivate(needURI, wonMessage);
    Need need = needs.get(0);
    need.setState(NeedState.INACTIVE);

    needRepository.save(need);
    List<Connection> cons = connectionRepository.findByNeedURI(needURI);
    if (cons.size() >= 1){
      for(Connection con : cons){
        con.setState(con.getState().transit(ConnectionEventType.OWNER_CLOSE));
        connectionRepository.save(con);
      }
    }
  }

  @Override
  @Transactional(propagation = Propagation.SUPPORTS)
  public void open(URI connectionURI, Model content, WonMessage wonMessage) throws Exception {
    if (logger.isDebugEnabled()) {
      logger.debug("owner to need: OPEN called for connection {} with content {}", connectionURI,
        StringUtils.abbreviate(RdfUtils.toString(content), 200));
    }
    delegate.open(connectionURI, content, wonMessage);
  }

  @Override
  @Transactional(propagation = Propagation.SUPPORTS)
  public void close(final URI connectionURI, Model content, WonMessage wonMessage) throws Exception {
    if (logger.isDebugEnabled()) {
      logger.debug("owner to need: CLOSE called for connection {} with model {}", connectionURI,
        StringUtils.abbreviate(RdfUtils.toString(content), 200));
    }
    delegate.close(connectionURI, content, wonMessage);

  }

  @Override
  @Transactional(propagation = Propagation.SUPPORTS)
  public void sendConnectionMessage(final URI connectionURI, final Model message, WonMessage wonMessage)
    throws Exception {

      URI connectionURIFromWonMessage = wonMessage.getSenderURI();
      URI messageURI = wonMessage.getMessageURI();
      logger.debug("owner to need: MESSAGE called for connection {} with message {}",
                   connectionURIFromWonMessage, RdfUtils.toString(wonMessage.getMessageContent()));
      delegate.sendConnectionMessage(connectionURI, message, wonMessage);
  }

  @Override
  @Transactional(propagation = Propagation.SUPPORTS)
  public String register(URI endpointURI) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  @Transactional(propagation = Propagation.SUPPORTS)
  public ListenableFuture<URI> createNeed(Model content, boolean activate, WonMessage wonMessage)
          throws Exception {
    return createNeed(content, activate, null, wonMessage);
  }

  @Override
  @Transactional(propagation = Propagation.SUPPORTS)
  public synchronized ListenableFuture<URI> createNeed(
          final Model content,
          final boolean activate,
          final URI wonNodeUri,
          final WonMessage wonMessage)
    throws Exception {
    if (logger.isDebugEnabled()) {
      logger.debug("owner to need: CREATE_NEED activate {}, with content {}",
        new Object[]{activate, StringUtils.abbreviate(RdfUtils.toString(content), 200)});
    }
    final ListenableFuture<URI> uri = delegate.createNeed(content, activate, wonNodeUri, wonMessage);
    //asynchronously wait for the result and update the local database.
    //meanwhile, create our own ListenableFuture to pass the result back
    final SettableFuture<URI> result = SettableFuture.create();
    uri.addListener(
      new Runnable()
      {
        @Override
        public void run() {
          //TODO: move the DB part into its own layer or something, because the owner webapp is designed to be syncronous, but the code below may slow down the web-app.
          Need need = new Need();
          try {

            //TODO: this is a temporary solution. owner shall process won messages sent by node.
            need.setNeedURI(uri.get());
            // logger.debug(need.getNeedURI().toString());
            need.setState(activate ? NeedState.ACTIVE : NeedState.INACTIVE);

            if (wonNodeUri == null) need.setWonNodeURI(URI.create(wonNodeDefault));
            else need.setWonNodeURI(wonNodeUri);
            needRepository.save(need);
            //now that we're done, let our callers know the URI
            result.set(need.getNeedURI());
          } catch (Exception e) {
            logger.warn("error while processing result of createNeed call", e);
            result.setException(e);
            result.cancel(true);
          }

        }
      }, executor);

    return result;
  }

  @Override
  @Transactional(propagation = Propagation.SUPPORTS)
  public ListenableFuture<URI> connect(
          final URI needURI,
          final URI otherNeedURI,
          final Model content,
          final WonMessage wonMessage)
    throws Exception {
    if (logger.isDebugEnabled()) {
      logger.debug("owner to need: CONNECT called for need {}, other need {} and content {}",
        new Object[]{needURI, otherNeedURI, StringUtils.abbreviate(RdfUtils.toString(content), 200)});
    }

    final ListenableFuture<URI> uri = delegate.connect(needURI, otherNeedURI, content, wonMessage);
    //asynchronously wait for the result and update the local database.
    //meanwhile, create our own ListenableFuture to pass the result back
    final SettableFuture<URI> result = SettableFuture.create();

    //now perform the connection state change or the creation of a new connection object
    //locally.
    uri.addListener(new Runnable(){
      @Override
      public void run() {
        //find out the facet to connect with
        final URI facetURI = WonRdfUtils.FacetUtils.getFacet(wonMessage);
        //save the connection object in the database
        Connection con = null;
        URI connectionURI = null;
        try {
          //Create new connection object
          connectionURI = uri.get();
          if (logger.isDebugEnabled()){
            logger.debug("connecting with this local connection URI: {}", connectionURI);
          }
          con = connectionRepository.findOneByConnectionURI(connectionURI);
          if (con != null) {
            //connection already known. check if the data is consistent
            String problem = null;
            if (!needURI.equals(con.getNeedURI())){
              problem = "Different needURI";
            } else if (con.getRemoteNeedURI() != null && !con.getRemoteNeedURI().equals(otherNeedURI)){
              problem = "Different remoteNeedURI";
            } else if (con.getTypeURI() != null && con.getTypeURI() != facetURI) {
              problem = "Different facetURI";
            }
            if (problem != null) {
              logger.warn("inconsistent data detected - connect returned this new local connection URI: {}, " +
                "which is already known with this data: {}. Problem: " + problem+".", connectionURI, con);
              throw new IllegalArgumentException("Connect led to inconsistent data! See log for details");
            }
            con.setTypeURI(facetURI);
            con.setState(ConnectionState.REQUEST_SENT);
          } else {
            con = new Connection();
            con.setNeedURI(needURI);
            con.setState(ConnectionState.REQUEST_SENT);
            con.setTypeURI(facetURI);
            con.setRemoteNeedURI(otherNeedURI);
            con.setConnectionURI(uri.get());
            if (logger.isDebugEnabled()) {
              logger.debug("saving connection: {}", con);
            }
          }
          connectionRepository.save(con);
          result.set(con.getConnectionURI());
        } catch (Exception e) {
          logger.info("Error creating connection {}. Stacktrace follows", con);
          logger.warn("Error creating connection ", e);
          result.cancel(true);
        }
      }
    }, this.executor);

    return result;
  }

  public void sendWonMessage(WonMessage wonMessage) throws Exception {
    delegate.sendWonMessage(wonMessage);
  }

  public void setDelegate(OwnerProtocolNeedServiceClientSide delegate) {
    this.delegate = delegate;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.ownerApplicationContext = applicationContext;
  }


}
