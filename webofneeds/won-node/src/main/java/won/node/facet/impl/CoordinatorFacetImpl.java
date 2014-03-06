package won.node.facet.impl;

import com.hp.hpl.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.exception.*;
import won.protocol.model.Connection;
import won.protocol.model.ConnectionState;
import won.protocol.model.FacetType;
import won.protocol.repository.ConnectionRepository;
import won.protocol.vocabulary.WON;

import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created with IntelliJ IDEA.
 * User: Danijel
 * Date: 9.12.13.
 * Time: 19.20
 * To change this template use File | Settings | File Templates.
 */
public class CoordinatorFacetImpl extends Facet {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ConnectionRepository connectionRepository;

    @Override
    public FacetType getFacetType() {
        return FacetType.CoordinatorFacet;
    }

    @Override
    public void connectFromOwner(final Connection con, final Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException {
        logger.info("Coordinator: ConntectFromOwner");
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

        // test
       /* Resource participant = remoteFacetModel.createResource(WON_TX.BASE_URI+con.getRemoteNeedURI()); //participant URI
        remoteFacetModel.add(WON_TX.COORDINATOR,WON_TX.COORDINATOR_VOTE_REQUEST, participant);
        RDFDataMgr.write(System.out, remoteFacetModel, Lang.TTL);*/

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
                    logger.debug("caught Exception", e);
                }
            }
        });

    }

    public void openFromOwner(final Connection con, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        //inform the other side
        logger.info("Coordinator: OpenFromOwner");

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


    public void openFromNeed(final Connection con, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        //inform the need side
        logger.info("Coordinator: OpenFromNeed");

        executorService.execute(new Runnable()
        {
            @Override
            public void run()
            {
                try {
              //      ownerFacingConnectionClient.open(con.getConnectionURI(), content);

                    List<Connection>  cons = connectionRepository.findByNeedURIAndStateAndTypeURI(con.getNeedURI(),
                            ConnectionState.REQUEST_SENT, FacetType.CoordinatorFacet.getURI());
                    boolean fAllVotesReceived = cons.isEmpty();
                    if(fAllVotesReceived){
                        Model myContent = ModelFactory.createDefaultModel();
                        myContent.setNsPrefix("","no:uri");
                        Resource baseResource = myContent.createResource("no:uri");
                        baseResource.addProperty(WON_TX.COORDINATION_MESSAGE, WON_TX.COORDINATION_MESSAGE_COMMIT);

                        ownerFacingConnectionClient.open(con.getConnectionURI(), myContent);

                        globalCommit(con);
                    }
                    else{
                        logger.info("Wait for votes of: ");
                        for(Connection c : cons)
                        {
                            logger.info("   "+c.getConnectionURI()+" "+c.getNeedURI()+" "+c.getRemoteNeedURI());
                        }
                        ownerFacingConnectionClient.open(con.getConnectionURI(), content);
                    }

                } catch (WonProtocolException e) {
                    logger.debug("caught Exception:", e);
                }
            }
        });
    }

    @Override
    public void closeFromNeed(final Connection con, final Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        //inform the need side
        logger.info("Coordinator: closeFromOwner");
        executorService.execute(new Runnable()
        {
            @Override
            public void run()
            {
                try {
                    //send close to the coordinator's owner (TODO: necessary?)
                    ownerFacingConnectionClient.close(con.getConnectionURI(), content);
                    globalAbort(con);

                } catch (WonProtocolException e) {
                    logger.warn("caught WonProtocolException:", e);
                }
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
                    ownerFacingConnectionClient.close(c.getConnectionURI(), content);

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
                    needFacingConnectionClient.close(c, myContent);  //Abort sent to participant
                }
            }
        }  catch (WonProtocolException e) {
            logger.warn("caught WonProtocolException:", e);
        } catch (Exception e) {
            logger.debug("caught Exception", e);
        }
    }

    public void commitTransaction(Connection con, Model content)
    {
        List<Connection> cons = connectionRepository.findByNeedURI(con.getNeedURI());
        try{
            for(Connection c : cons)
            {
                //proveri ownerFacingConnectionClient.close(c.getConnectionURI(), content);
                if(c.getState()!=ConnectionState.CLOSED)
                {
                    ownerFacingConnectionClient.close(c.getConnectionURI(), content);
                    needFacingConnectionClient.close(c, content);
                }
            }
            logger.info("Transaction commited!");
        }  catch (WonProtocolException e) {
            logger.warn("caught WonProtocolException:", e);
        } catch (Exception e) {
            logger.debug("caught Exception",e);
        }
    }




}
