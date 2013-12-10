package won.node.facet.impl;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.RDF;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.node.service.impl.DataAccessService;
import won.node.service.impl.NeedFacingConnectionCommunicationServiceImpl;
import won.node.service.impl.OwnerFacingConnectionCommunicationServiceImpl;
import won.protocol.exception.*;
import won.protocol.model.Connection;
import won.protocol.model.FacetType;
import won.protocol.model.Need;
import won.protocol.model.NeedState;
import won.protocol.need.NeedProtocolNeedClientSide;
import won.protocol.owner.OwnerProtocolOwnerServiceClientSide;
import won.protocol.vocabulary.WON;

import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 16.09.13
 * Time: 17:09
 * To change this template use File | Settings | File Templates.
 */
public abstract class Facet {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Client talking another need via the need protocol
   */
  protected NeedProtocolNeedClientSide needProtocolNeedService;
  /**
   * Client talking to the owner side via the owner protocol
   */
  protected OwnerProtocolOwnerServiceClientSide ownerProtocolOwnerService;

  /**
   * Client talking to this need service from the need side
   */
  protected NeedFacingConnectionCommunicationServiceImpl needFacingConnectionCommunicationService;
  /**
   * Client talking to this need service from the owner side
   */
  protected OwnerFacingConnectionCommunicationServiceImpl ownerFacingConnectionCommunicationService;

  protected NeedProtocolNeedClientSide needFacingConnectionClient;
  protected OwnerProtocolOwnerServiceClientSide ownerFacingConnectionClient;

  protected won.node.service.impl.URIService URIService;

  protected ExecutorService executorService;

  protected DataAccessService dataService;

  public abstract FacetType getFacetType();

  public void openFromOwner(final Connection con, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
    //inform the other side
    if (con.getRemoteConnectionURI() != null) {
      executorService.execute(new Runnable() {
        @Override
        public void run() {
          try {
            needFacingConnectionClient.open(con.getRemoteConnectionURI(), content);
          } catch (WonProtocolException e) {
            logger.debug("caught Exception:", e);
          }
        }
      });
    }
  }

  public void closeFromOwner(final Connection con, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
    //inform the other side
    if (con.getRemoteConnectionURI() != null) {
      executorService.execute(new Runnable()
      {
        @Override
        public void run()
        {
          try {
            needFacingConnectionClient.close(con.getRemoteConnectionURI(), content);
          } catch (WonProtocolException e) {
            logger.warn("caught WonProtocolException:", e);
          }
        }
      });
    }
  }

  public void textMessageFromOwner(final Connection con, final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
    final URI remoteConnectionURI = con.getRemoteConnectionURI();
    //inform the other side
    executorService.execute(new Runnable() {
      @Override
      public void run() {
        try {
          needFacingConnectionClient.textMessage(remoteConnectionURI, message);
        } catch (WonProtocolException e) {
          logger.warn("caught WonProtocolException:", e);
        }
      }
    });
  }

  public void openFromNeed(final Connection con, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
    //inform the need side
    executorService.execute(new Runnable()
    {
      @Override
      public void run()
      {
        try {
          ownerFacingConnectionClient.open(con.getConnectionURI(), content);
        } catch (WonProtocolException e) {
          logger.debug("caught Exception:", e);
        }
      }
    });
  }

  public void closeFromNeed(final Connection con, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
    //inform the need side
    executorService.execute(new Runnable()
    {
      @Override
      public void run()
      {
        try {
          ownerFacingConnectionClient.close(con.getConnectionURI(), content);
        } catch (WonProtocolException e) {
          logger.warn("caught WonProtocolException:", e);
        }
      }
    });
  }

  public void textMessageFromNeed(final Connection con, final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
    //send to the need side
    executorService.execute(new Runnable() {
      @Override
      public void run() {
        try {
          ownerFacingConnectionClient.textMessage(con.getConnectionURI(), message);
        } catch (WonProtocolException e) {
          logger.warn("caught WonProtocolException:", e);
        }
      }
    });
  }

  public void hint(final Connection con, final double score, final URI originator, final Model content)
      throws NoSuchNeedException, IllegalMessageForNeedStateException {

    ResIterator remoteFacetIt = content.listSubjectsWithProperty(RDF.type, WON.HAS_REMOTE_FACET);
    if (!remoteFacetIt.hasNext())
      throw new IllegalArgumentException("at least one RDF node must be of type won:RemoteFacet");

    //TODO: This should just remove RemoteFacet from content and replace the value of Facet with the one from RemoteFacet
    final Model remoteFacetModel = remoteFacetIt.next().getModel();

    executorService.execute(new Runnable() {
      @Override
      public void run() {
        //here, we don't really need to handle exceptions, as we don't want to flood matching services with error messages
        try {
          ownerProtocolOwnerService.hint(con.getNeedURI(), con.getRemoteNeedURI(), score, originator, remoteFacetModel);
        } catch (NoSuchNeedException e) {
          logger.warn("error sending hint message to owner - no such need:", e);
        } catch (IllegalMessageForNeedStateException e) {
          logger.warn("error sending hint content to owner - illegal need state:", e);
        } catch (Exception e) {
          logger.warn("error sending hint content to owner:", e);
        }
      }
    });
  }

  public void connectFromNeed(final Connection con, final Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {


    final Connection connectionForRunnable = con;
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

  public void connectFromOwner(final Connection con, final Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {
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
    RDFDataMgr.write(System.out, remoteFacetModel, Lang.TTL);

    final Connection connectionForRunnable = con;
    //send to need
    executorService.execute(new Runnable() {
      @Override
      public void run() {
        try {
          Future<URI> remoteConnectionURI = needProtocolNeedService.connect(con.getRemoteNeedURI(), con.getNeedURI(), connectionForRunnable.getConnectionURI(), remoteFacetModel);
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
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ExecutionException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
      }
    });

  }

  private boolean isNeedActive(final Need need) {
    return NeedState.ACTIVE == need.getState();
  }

  public void setOwnerFacingConnectionClient(OwnerProtocolOwnerServiceClientSide ownerFacingConnectionClient) {
    this.ownerFacingConnectionClient = ownerFacingConnectionClient;
  }

  public void setDataService(DataAccessService dataService) {
    this.dataService = dataService;
  }

  public void setExecutorService(ExecutorService executorService) {
    this.executorService = executorService;
  }

  public void setURIService(won.node.service.impl.URIService URIService) {
    this.URIService = URIService;
  }

  public void setNeedFacingConnectionClient(NeedProtocolNeedClientSide needFacingConnectionClient) {
    this.needFacingConnectionClient = needFacingConnectionClient;
  }

  public void setOwnerFacingConnectionCommunicationService(OwnerFacingConnectionCommunicationServiceImpl ownerFacingConnectionCommunicationService) {
    this.ownerFacingConnectionCommunicationService = ownerFacingConnectionCommunicationService;
  }

  public void setNeedFacingConnectionCommunicationService(NeedFacingConnectionCommunicationServiceImpl needFacingConnectionCommunicationService) {
    this.needFacingConnectionCommunicationService = needFacingConnectionCommunicationService;
  }

  public void setNeedProtocolNeedService(NeedProtocolNeedClientSide needProtocolNeedServiceClient) {
    this.needProtocolNeedService = needProtocolNeedServiceClient;
  }

  public void setOwnerProtocolOwnerService(OwnerProtocolOwnerServiceClientSide ownerProtocolOwnerService) {
    this.ownerProtocolOwnerService = ownerProtocolOwnerService;
  }
}
