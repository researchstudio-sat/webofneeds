package won.node.facet.impl;

import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import won.protocol.exception.ConnectionAlreadyExistsException;
import won.protocol.exception.IllegalMessageForConnectionStateException;
import won.protocol.exception.IllegalMessageForNeedStateException;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.exception.NoSuchNeedException;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.model.FacetType;
import won.protocol.repository.ConnectionRepository;
import won.protocol.vocabulary.WON;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 9.12.13.
 * Time: 19.20
 * To change this template use File | Settings | File Templates.
 */
public class CoordinatorFacetImpl extends AbstractFacet
{
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private ConnectionRepository connectionRepository;

  @Override
  public FacetType getFacetType() {
    return FacetType.CoordinatorFacet;
  }

  @Override
  public void connectFromOwner(final Connection con, final Model content, final WonMessage wonMessage)
          throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {
    logger.debug("Coordinator: ConntectFromOwner");
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

//    try {
//      final ListenableFuture<URI> remoteConnectionURI = needProtocolNeedService.connect(con.getRemoteNeedURI(),
//        con.getNeedURI(), connectionForRunnable.getConnectionURI(), remoteFacetModel, wonMessage);
//      this.executorService.execute(new Runnable(){
//        @Override
//        public void run() {
//          try{
//            if (logger.isDebugEnabled()) {
//              logger.debug("saving remote connection URI");
//            }
//            dataService.updateRemoteConnectionURI(con, remoteConnectionURI.get());
//          } catch (Exception e) {
//            logger.warn("Error saving connection {}. Stacktrace follows", con);
//            logger.warn("Error saving connection ", e);
//          }
//        }
//      });
//    } catch (WonProtocolException e) {
//      // we can't connect the connection. we send a close back to the owner
//      // TODO should we introduce a new protocol method connectionFailed (because it's not an owner deny but some protocol-level error)?
//      // For now, we call the close method as if it had been called from the remote side
//      // TODO: even with this workaround, it would be good to send a content along with the close (so we can explain what happened).
////      try {
////        Connection c = closeConnectionLocally(connectionForRunnable, content);
////          // ToDo (FS): should probably not be the same wonMessage!?
////        needFacingConnectionCommunicationService.close(c.getConnectionURI(), content, wonMessage);
////      } catch (NoSuchConnectionException e1) {
////        logger.warn("caught NoSuchConnectionException:", e1);
////      } catch (IllegalMessageForConnectionStateException e1) {
////        logger.warn("caught IllegalMessageForConnectionStateException:", e1);
////      }
//    }
//    catch (InterruptedException e) {
//      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//    } catch (ExecutionException e) {
//      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//    } catch (Exception e) {
//      logger.debug("caught Exception", e);
//    }
  }

  public void openFromOwner(final Connection con, final Model content, final WonMessage wonMessage)
          throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
    //inform the other side
    logger.debug("Coordinator: OpenFromOwner");

    if (con.getRemoteConnectionURI() != null) {
//      executorService.execute(new Runnable() {
//        @Override
//        public void run() {
//          try {
//            needFacingConnectionClient.open(con, content, wonMessage);
//          } catch (WonProtocolException e) {
//            logger.debug("caught Exception:", e);
//          } catch (Exception e) {
//            logger.debug("caught Exception", e);
//          }
//        }
//      });
    }
  }


  public void openFromNeed(final Connection con, final Model content, final WonMessage wonMessage)
          throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
    //inform the need side
    logger.debug("Coordinator: OpenFromNeed");

    executorService.execute(new Runnable()
    {
      @Override
      public void run()
      {
//        try {
          //      ownerFacingConnectionClient.open(con.getConnectionURI(), content);

          List<Connection>  cons = connectionRepository.findByNeedURIAndStateAndTypeURI(con.getNeedURI(),
            ConnectionState.REQUEST_SENT, FacetType.CoordinatorFacet.getURI());
          boolean fAllVotesReceived = cons.isEmpty();
          if(fAllVotesReceived){
            Model myContent = ModelFactory.createDefaultModel();
            myContent.setNsPrefix("","no:uri");
            Resource baseResource = myContent.createResource("no:uri");
            baseResource.addProperty(WON_TX.COORDINATION_MESSAGE, WON_TX.COORDINATION_MESSAGE_COMMIT);
            //TODO: use new system
            //ownerFacingConnectionClient.open(con.getConnectionURI(), myContent, wonMessage);
            globalCommit(con);
          }
          else{
            logger.debug("Wait for votes of: ");
            for(Connection c : cons)
            {
              logger.debug("   " + c.getConnectionURI() + " " + c.getNeedURI() + " " + c.getRemoteNeedURI());
            }
            //TODO: use new system
            //ownerFacingConnectionClient.open(con.getConnectionURI(), content, wonMessage);
          }

//        } catch (WonProtocolException e) {
//          logger.debug("caught Exception:", e);
//        }
      }
    });
  }

  @Override
  public void closeFromNeed(final Connection con, final Model content, final WonMessage wonMessage)
          throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
    //inform the need side
    logger.debug("Coordinator: closeFromOwner");
    executorService.execute(new Runnable()
    {
      @Override
      public void run()
      {
//        try {
          //TODO: use new system
          //ownerFacingConnectionClient.close(con.getConnectionURI(), content, wonMessage);
          globalAbort(con);
//        } catch (WonProtocolException e) {
//          logger.warn("caught WonProtocolException:", e);
//        }
      }
    });
  }

  private void globalAbort(Connection con) {
    Model myContent = ModelFactory.createDefaultModel();
    myContent.setNsPrefix("","no:uri");
    Resource baseResource = myContent.createResource("no:uri");
    baseResource.addProperty(WON_TX.COORDINATION_MESSAGE, WON_TX.COORDINATION_MESSAGE_ABORT);
    abortTransaction(con, myContent);
  }

  public void globalCommit(Connection con)
  {
    Model myContent = ModelFactory.createDefaultModel();
    myContent.setNsPrefix("","no:uri");
    Resource baseResource = myContent.createResource("no:uri");
    baseResource.addProperty(WON_TX.COORDINATION_MESSAGE, WON_TX.COORDINATION_MESSAGE_COMMIT);
    commitTransaction(con, myContent);
  }

  public void abortTransaction(Connection con, Model content)
  {
    List<Connection> cons = connectionRepository.findByNeedURI(con.getNeedURI());
    try{
      for(Connection c : cons)
      {
        if(c.getState()!=ConnectionState.CLOSED)
        {
          Model myContent = ModelFactory.createDefaultModel();
          myContent.setNsPrefix("","no:uri");
          Resource res = myContent.createResource("no:uri");
          if(c.getState() == ConnectionState.CONNECTED || c.getState() == ConnectionState.REQUEST_SENT)
          {
            if (res == null) {
              logger.debug("no default prexif specified in model, could not obtain additional content, using ABORTED message");
            }
            res.removeAll(WON_TX.COORDINATION_MESSAGE);
            res.addProperty(WON_TX.COORDINATION_MESSAGE, WON_TX.COORDINATION_MESSAGE_ABORT);
          }
          //todo: use new system
          //closeConnectionLocally(c, content);
          //needFacingConnectionClient.close(c, myContent, null);  //Abort sent to participant
        }
      }
//    }  catch (WonProtocolException e) {
//      logger.warn("caught WonProtocolException:", e);
    } catch (Exception e) {
      logger.debug("caught Exception", e);
    }
  }

  public void commitTransaction(Connection con, Model content)
  {
    List<Connection> cons = connectionRepository.findByNeedURIAndStateAndTypeURI(con.getNeedURI(),
      ConnectionState.CONNECTED, FacetType.CoordinatorFacet.getURI());
    try{
      for(Connection c : cons)
      {
        if(c.getState()==ConnectionState.CONNECTED)
        {
          //emulate a close by owner
          //todo: use new system
          //c = closeConnectionLocally(c, content);
          //tell the partner
          //needFacingConnectionClient.close(c, content, null);
        }
      }
      logger.debug("Transaction commited!");
//    }  catch (WonProtocolException e) {
//      logger.warn("caught WonProtocolException:", e);
    } catch (Exception e) {
      logger.debug("caught Exception",e);
    }
  }



}
