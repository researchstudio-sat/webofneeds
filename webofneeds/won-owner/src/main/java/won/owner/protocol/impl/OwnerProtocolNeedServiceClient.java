package won.owner.protocol.impl;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.RDF;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import won.protocol.exception.*;
import won.protocol.model.*;
import won.protocol.owner.OwnerProtocolNeedServiceClientSide;
import won.protocol.repository.ChatMessageRepository;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.FacetRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.WON;

import java.net.URI;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Implementation of the OwnerProtocolNeedService to be used on the owner side. It contains the
 * required business logic to store state and delegates calls to an injected linked data
 * client and to an injected OwnerProtocolNeedService implementation.
 * <p/>
 * User: Gabriel
 * Date: 03.12.12
 * Time: 14:42
 * TODO: refactor to separate communication code from business logic!
 * * introduce new member of type OwnerProtocolNeedService (that forwards to such a service)
 * * extract the code for proxy creation into a class implementing that interface, so that
 * connecting to a WS-based service is done in that class only (and not here)
 * * implement a JMS-based implementation of that interface and change spring config so it is used here
 */
public class OwnerProtocolNeedServiceClient implements OwnerProtocolNeedServiceClientSide
{
    /* Linked Data default paths */
    private static final String NEED_URI_PATH_PREFIX = "/data/need";
    private static final String CONNECTION_URI_PATH_PREFIX = "/data/connection";
    private static final String NEED_CONNECTION_URI_PATH_SUFFIX = "/connections";
    private static final String NEED_MATCH_URI_PATH_SUFFIX = "/matches";
    private ApplicationContext ownerApplicationContext;
    final Logger logger = LoggerFactory.getLogger(getClass());

    @Value(value = "${uri.prefix.node.default}")
    String wonNodeDefault;

    @Autowired
    private FacetRepository facetRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private NeedRepository needRepository;


    //ref=ownerProtocolNeedServiceClientJMSBased
    private OwnerProtocolNeedServiceClientSide delegate;

    @Override
    public void activate(URI needURI) throws NoSuchNeedException {
        logger.debug("need-facing: ACTIVATE called for need {}", needURI);
        List<Need> needs = needRepository.findByNeedURI(needURI);
        if (needs.size() != 1)
            throw new NoSuchNeedException(needURI);
        delegate.activate(needURI);
        Need need = needs.get(0);
        need.setState(NeedState.ACTIVE);
        needRepository.saveAndFlush(need);
    }

    @Override
    public void deactivate(URI needURI) throws NoSuchNeedException {
        logger.debug(MessageFormat.format("need-facing: DEACTIVATE called for need {0}", needURI));

        List<Need> needs = needRepository.findByNeedURI(needURI);
        if (needs.size() != 1)
            throw new NoSuchNeedException(needURI);
        delegate.deactivate(needURI);
        Need need = needs.get(0);
        need.setState(NeedState.INACTIVE);
        needRepository.saveAndFlush(need);

    }

    @Override
    public void open(URI connectionURI, Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
    {
        if (logger.isDebugEnabled()) {
          logger.debug(MessageFormat.format("need-facing: OPEN called for connection {0} with model {1}", connectionURI, StringUtils.abbreviate(RdfUtils.toString(content),200)));
        }
        List<Connection> cons = connectionRepository.findByConnectionURI(connectionURI);
        if (cons.size() != 1)
            throw new NoSuchConnectionException(connectionURI);


        delegate.open(connectionURI, content);

        Connection con = cons.get(0);
        con.setState(con.getState().transit(ConnectionEventType.OWNER_OPEN));
        connectionRepository.saveAndFlush(con);

    }

    @Override
    public void close(final URI connectionURI, Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
    {
        if (logger.isDebugEnabled()) {
          logger.debug(MessageFormat.format("need-facing: CLOSE called for connection {0} with model {1}", connectionURI, StringUtils.abbreviate(RdfUtils.toString(content),200)));
        }
        List<Connection> cons = connectionRepository.findByConnectionURI(connectionURI);
        if (cons.size() != 1)
            throw new NoSuchConnectionException(connectionURI);
        delegate.close(connectionURI, content);
        Connection con = cons.get(0);
        con.setState(con.getState().transit(ConnectionEventType.OWNER_CLOSE));
        connectionRepository.saveAndFlush(con);

    }

    @Override
    public void textMessage(final URI connectionURI, final String message)
            throws NoSuchConnectionException, IllegalMessageForConnectionStateException
    {
        logger.debug("need-facing: SEND_TEXT_MESSAGE called for connection {} with message {}", connectionURI, message);

        List<Connection> cons = connectionRepository.findByConnectionURI(connectionURI);
        if (cons.isEmpty())
            throw new NoSuchConnectionException(connectionURI);
        Connection con = cons.get(0);
        delegate.textMessage(connectionURI, message);

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setCreationDate(new Date());
        chatMessage.setLocalConnectionURI(connectionURI);
        chatMessage.setMessage(message);
        chatMessage.setOriginatorURI(con.getNeedURI());

        //save in the db
        chatMessageRepository.saveAndFlush(chatMessage);


    }


    @Override
    public String register(URI endpointURI)
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Future<URI> createNeed(URI ownerURI, Model content, boolean activate) throws Exception {
        return createNeed(ownerURI, content, activate, null);
    }

    @Override
    public Future<URI> createNeed(final URI ownerURI, final Model content, final boolean activate, final URI wonNodeURI) throws Exception {
        if (logger.isDebugEnabled()) {
          logger.debug("need-facing: CREATE_NEED called for need {}, with content {} and activate {}",
                new Object[]{ownerURI, StringUtils.abbreviate(RdfUtils.toString(content),200), activate});
        }


        final Future<URI> uri = delegate.createNeed(ownerURI, content, activate, wonNodeURI);
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        //TODO: move the DB part into its own layer or something, because the owner webapp is designed to be syncronous, but the code below may slow down the web-app.
                        Need need = new Need();
                        try {
                            need.setNeedURI(uri.get());
                           // logger.debug(need.getNeedURI().toString());
                            need.setState(activate ? NeedState.ACTIVE : NeedState.INACTIVE);
                            need.setOwnerURI(ownerURI);

                            if (wonNodeURI==null) need.setWonNodeURI(URI.create(wonNodeDefault));
                            else need.setWonNodeURI(wonNodeURI);
                            needRepository.saveAndFlush(need);
                            needRepository.findByNeedURI(need.getNeedURI());
                            logger.debug("saving URI", need.getNeedURI().toString());

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

                        } catch (InterruptedException e) {
                            logger.warn("interrupted",e);
                        } catch (ExecutionException e) {
                            logger.warn("ExecutionException caught",e);
                        }


                    }
                }
        ).start();



        return uri;
    }

    @Override
    public Future<URI> connect(final URI needURI, final  URI otherNeedURI, final Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException, ConnectionAlreadyExistsException, ExecutionException, InterruptedException
    {
      if (logger.isDebugEnabled()) {
        logger.debug("need-facing: CONNECT called for need {}, other need {} and content {}", new Object[]{needURI, otherNeedURI, StringUtils.abbreviate(RdfUtils.toString(content),200)});
      }

            final Future<URI> uri = delegate.connect(needURI, otherNeedURI, content);
            Resource baseRes = content.getResource(content.getNsPrefixURI(""));
            StmtIterator stmtIterator = baseRes.listProperties(WON.HAS_FACET);

            if (!stmtIterator.hasNext()) {
                throw new IllegalArgumentException("at least one RDF node must be of type won:" + WON.HAS_FACET.getLocalName());
            }
            URI facetURI =  URI.create(stmtIterator.next().getObject().asResource().getURI());
            List<Connection> existingConnections = connectionRepository.findByConnectionURI(uri.get());
            if (existingConnections.size() > 0) {
                for (Connection conn : existingConnections) {
                    if (facetURI.equals(conn.getTypeURI()))
                        if (! ConnectionEventType.OWNER_OPEN.isMessageAllowed(conn.getState())) {
                            throw new ConnectionAlreadyExistsException(conn.getConnectionURI(), needURI, otherNeedURI);
                        } else {
                            conn.setState(conn.getState().transit(ConnectionEventType.OWNER_OPEN));
                            connectionRepository.saveAndFlush(conn);
                        }
                }
            }  else {
                    new Thread(
                            new Runnable() {
                                @Override
                                public void run() {

                                    //set new uri
                                    try {
                                        //Create new connection object
                                        Connection con = new Connection();
                                        con.setNeedURI(needURI);
                                        con.setState(ConnectionState.REQUEST_SENT);
                                        con.setRemoteNeedURI(otherNeedURI);
                                        con.setConnectionURI(uri.get());
                                        connectionRepository.saveAndFlush(con);



                                    } catch (InterruptedException e) {
                                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                                    } catch (ExecutionException e) {
                                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                                    }

                                }
                            }
                    ).start();

                }



        return uri;
    }




    public void setDelegate(OwnerProtocolNeedServiceClientSide delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.ownerApplicationContext = applicationContext;
    }
}
