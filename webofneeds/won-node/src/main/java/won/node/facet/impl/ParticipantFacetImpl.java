package won.node.facet.impl;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.exception.*;
import won.protocol.model.Connection;
import won.protocol.model.FacetType;
import won.protocol.repository.ConnectionRepository;
import won.protocol.vocabulary.WON;

import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 9.12.13.
 * Time: 19.19
 * To change this template use File | Settings | File Templates.
 */
public class ParticipantFacetImpl extends AbstractFacet
{
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private ConnectionRepository connectionRepository;


  @Override
  public FacetType getFacetType() {
    return FacetType.ParticipantFacet;
  }

  public void connectFromNeed(final Connection con, final Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {
    final Connection connectionForRunnable = con;
    logger.debug("Participant: ConnectFromNeed");

    executorService.execute(new Runnable() {
      @Override
      public void run() {
        try {
          ownerProtocolOwnerService.connect(con.getNeedURI(), con.getRemoteNeedURI(), connectionForRunnable.getConnectionURI(), content);
        } catch (WonProtocolException e) {
          // we can't connect the connection. we send a deny back to the owner
          // TODO should we introduce a new protocol method connectionFailed (because it's not an owner deny but some protocol-level error)?
          // For now, we call the close method as if it had been called from the owner side
          // TODO: even with this workaround, it would be good to send a content along with the close (so we can explain what happened).
          try {
            ownerFacingConnectionCommunicationService.close(connectionForRunnable.getConnectionURI(), content);
          } catch (NoSuchConnectionException e1) {
            logger.warn("caught NoSuchConnectionException:", e1);
          } catch (IllegalMessageForConnectionStateException e1) {
            logger.warn("caught IllegalMessageForConnectionStateException:", e1);
          }
        }
      }
    });
  }

  @Override
  public void openFromOwner(final Connection con, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
    //inform the other side
    logger.debug("Participant: OpenFromOwner");
    if (con.getRemoteConnectionURI() != null) {
      executorService.execute(new Runnable() {
        @Override
        public void run() {
          try {
            needFacingConnectionClient.open(con, content);
          } catch (WonProtocolException e) {
            logger.debug("caught Exception:", e);
          } catch (Exception e) {
            logger.debug("caught Exception", e);
          }
        }
      });
    }
  }

  @Override
  public void closeFromOwner(final Connection con, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
    //inform the other side
    logger.debug("Participant: CloseFromOwner");
    if (con.getRemoteConnectionURI() != null) {
      executorService.execute(new Runnable()
      {
        @Override
        public void run()
        {
          try {
            needFacingConnectionClient.close(con, content);
          } catch (WonProtocolException e) {
            logger.warn("caught WonProtocolException:", e);
          } catch (Exception e) {
            logger.debug("caught Exception", e);
          }
        }
      });
    }
  }

  public void closeFromNeed(final Connection con, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
    //inform the need side
    logger.debug("Participant: CloseFromNeed");
    //TODO: create utilities for accessing addtional content
    Resource res = content.getResource(content.getNsPrefixURI(""));
    Resource message = null;
    if (res == null) {
      logger.debug("no default prexif specified in model, could not obtain additional content, using ABORTED message");
      message = WON_TX.COORDINATION_MESSAGE_ABORT;
    }
    //TODO: make sure there is only one message in the content
    message = res.getPropertyResourceValue(WON_TX.COORDINATION_MESSAGE);
    final Resource msgForRunnable = message;

    executorService.execute(new Runnable()
    {
      @Override
      public void run()
      {
        try {
          //if (msgForRunnable == WON_TX.COORDINATION_MESSAGE_ABORT){
          if(msgForRunnable!=null){
            if (msgForRunnable.equals(WON_TX.COORDINATION_MESSAGE_ABORT)){
              logger.debug("Abort the following connection: "+con.getConnectionURI()+" "+con.getNeedURI()+" "+con.getRemoteNeedURI() +" "+con.getState()+ " "+con.getTypeURI());
            }
            else {
              logger.debug("Committed: "+con.getConnectionURI()+" "+con.getNeedURI()+" "+con.getRemoteNeedURI() +" "+con.getState()+ " "+con.getTypeURI());
            }
            ownerFacingConnectionClient.close(con.getConnectionURI(), content);
          }

        } catch (WonProtocolException e) {
          logger.warn("caught WonProtocolException:", e);
        }
      }
    });
  }

  public void compensate(Connection con , Model content){  //todo
//TODO: create utilities for accessing addtional content
       /* Resource res = content.getResource(content.getNsPrefixURI(""));
        if (res == null) {
            logger.debug("no default prexif specified in model, could not obtain additional content, using ABORTED message");
        }
        res.removeAll(WON_TX.COORDINATION_MESSAGE);
        res.addProperty(WON_TX.COORDINATION_MESSAGE, WON_TX.COORDINATION_MESSAGE_ABORT_AND_COMPENSATE);*/

    logger.debug("Compensated:   "+con.getConnectionURI()+" "+con.getNeedURI()+" "+con.getRemoteNeedURI() +" "+con.getState()+ " "+con.getTypeURI());
  }
































  @Override
  public void connectFromOwner(final Connection con, final Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {
    logger.debug("Participant: ConntectFromOwner");

    Resource baseRes = content.getResource(content.getNsPrefixURI(""));

    StmtIterator stmtIterator = baseRes.listProperties(WON.HAS_REMOTE_FACET);
    if (!stmtIterator.hasNext())
      throw new IllegalArgumentException("at least one RDF node must be of type won:hasRemoteFacet");

    //TODO: This should just remove RemoteFacet from content and replace the value of Facet with the one from RemoteFacet

    final Model remoteFacetModel = ModelFactory.createDefaultModel();

    remoteFacetModel.setNsPrefix("", "no:uri");
    baseRes = remoteFacetModel.createResource(remoteFacetModel.getNsPrefixURI(""));
    Resource remoteFacetResource = stmtIterator.next().getObject().asResource();
    baseRes.addProperty(WON.HAS_FACET, remoteFacetModel.createResource(remoteFacetResource.getURI()));

    final Connection connectionForRunnable = con;
    //send to need
    executorService.execute(new Runnable() {
      @Override
      public void run() {

        try {
          Future<URI> remoteConnectionURI = needProtocolNeedService.connect(con.getRemoteNeedURI(),con.getNeedURI(), connectionForRunnable.getConnectionURI(), remoteFacetModel);
          dataService.updateRemoteConnectionURI(con, remoteConnectionURI.get());

        } catch (WonProtocolException e) {
          // we can't connect the connection. we send a close back to the owner
          // TODO should we introduce a new protocol method connectionFailed (because it's not an owner deny but some protocol-level error)?
          // For now, we call the close method as if it had been called from the remote side
          // TODO: even with this workaround, it would be good to send a content along with the close (so we can explain what happened).
          try {
            needFacingConnectionCommunicationService.close(connectionForRunnable.getConnectionURI(), content);
          } catch (NoSuchConnectionException e1) {
            logger.warn("caught NoSuchConnectionException:", e1);
          } catch (IllegalMessageForConnectionStateException e1) {
            logger.warn("caught IllegalMessageForConnectionStateException:", e1);
          }
        }
        catch (InterruptedException e) {
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ExecutionException e) {
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (Exception e) {
          logger.debug("caught Exception",e);
        }
      }
    });

  }



}
