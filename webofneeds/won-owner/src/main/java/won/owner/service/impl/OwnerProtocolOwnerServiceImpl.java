package won.owner.service.impl;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.exception.*;
import won.protocol.model.*;
import won.protocol.owner.OwnerProtocolOwnerService;
import won.protocol.repository.ChatMessageRepository;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.MatchRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.util.DataAccessUtils;
import won.protocol.vocabulary.WON;

import java.net.URI;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gabriel
 * Date: 03.12.12
 * Time: 14:12
 */
public class OwnerProtocolOwnerServiceImpl implements OwnerProtocolOwnerService {

    final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private NeedRepository needRepository;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    //TODO: refactor this to use DataAccessService

    @Override
    public void hint(final URI ownNeedURI, final URI otherNeedURI, final double score, final URI originatorURI, final Model content) throws NoSuchNeedException, IllegalMessageForNeedStateException {
        logger.info("node-facing: HINT called for own need {}, other need {}, with score {} from originator {} and content {}",
                new Object[]{ownNeedURI, otherNeedURI, score, originatorURI, content});

        if (ownNeedURI == null) throw new IllegalArgumentException("needURI is not set");
        if (otherNeedURI == null) throw new IllegalArgumentException("otherNeedURI is not set");
        if (score < 0 || score > 1) throw new IllegalArgumentException("score is not in [0,1]");
        if (originatorURI == null) throw new IllegalArgumentException("originator is not set");
        if (ownNeedURI.equals(otherNeedURI)) throw new IllegalArgumentException("needURI and otherNeedURI are the same");


        //Load need (throws exception if not found)
        Need need = DataAccessUtils.loadNeed(needRepository, ownNeedURI);
        if (! isNeedActive(need)) throw new IllegalMessageForNeedStateException(ownNeedURI, ConnectionEventType.MATCHER_HINT.name(), need.getState());

        List<Match> matches = matchRepository.findByFromNeedAndToNeedAndOriginator(ownNeedURI, otherNeedURI, originatorURI);
        Match match = null;
        if (matches.size() > 0){
          match = matches.get(0);
        } else {
          //save match
          match = new Match();
          match.setFromNeed(ownNeedURI);
          match.setToNeed(otherNeedURI);
          match.setOriginator(originatorURI);
        }
        match.setScore(score);
        matchRepository.saveAndFlush(match);
    }

    private boolean isNeedActive(final Need need) {
        return NeedState.ACTIVE == need.getState();
    }

    @Override
    public void connect(final URI ownNeedURI, final URI otherNeedURI, final URI ownConnectionURI,
                        final Model content) throws NoSuchNeedException, ConnectionAlreadyExistsException, IllegalMessageForNeedStateException
    {
        logger.info("node-facing: CONNECTION_REQUESTED called for own need {}, other need {}, own connection {} and content ''{}''", new Object[]{ownNeedURI,otherNeedURI,ownConnectionURI, content});
        if (ownNeedURI == null) throw new IllegalArgumentException("needURI is not set");
        if (otherNeedURI == null) throw new IllegalArgumentException("otherNeedURI is not set");
        if (ownConnectionURI == null) throw new IllegalArgumentException("otherConnectionURI is not set");
        if (ownNeedURI.equals(otherNeedURI)) throw new IllegalArgumentException("needURI and otherNeedURI are the same");

        //Load need (throws exception if not found)
        Need need = DataAccessUtils.loadNeed(needRepository, ownNeedURI);
        if (!isNeedActive(need))
          throw new IllegalMessageForNeedStateException(ownNeedURI, ConnectionEventType.PARTNER_OPEN.name(), need.getState());

        Resource baseRes = content.getResource(content.getNsPrefixURI(""));
        StmtIterator stmtIterator = baseRes.listProperties(WON.HAS_FACET);

        if (!stmtIterator.hasNext()) {
          throw new IllegalArgumentException("at least one RDF node must be of type won:" + WON.HAS_FACET.getLocalName());
        }

        URI facetURI =  URI.create(stmtIterator.next().getObject().asResource().getURI());

        List<Connection> connections = connectionRepository.findByNeedURIAndRemoteNeedURI(ownNeedURI, otherNeedURI);
        Connection con = null;

        for(Connection c : connections) {
          //TODO: check remote need type as well or create GroupMemberFacet
          if (facetURI.equals(c.getTypeURI()))
            con = c;
        }

        //TODO: impose unique constraint on connections
        if(con != null) {
          if(ConnectionEventType.PARTNER_OPEN.isMessageAllowed(con.getState())) {
            //TODO: Move this to the transition() - Method in ConnectionState
            con.setState(con.getState().transit(ConnectionEventType.PARTNER_OPEN));
            con = connectionRepository.saveAndFlush(con);
          } else {
            throw new ConnectionAlreadyExistsException(con.getConnectionURI(), con.getNeedURI(), con.getRemoteNeedURI());
          }
        }

        if (con == null) {
          /* Create connection */
          con = new Connection();
          con.setNeedURI(ownNeedURI);
          con.setState(ConnectionState.REQUEST_RECEIVED);
          con.setRemoteNeedURI(otherNeedURI);
          con.setConnectionURI(ownConnectionURI);
          con.setTypeURI(facetURI);
          connectionRepository.saveAndFlush(con);

          //TODO: do we save the connection content? where? as a chat content?
        }
    }

    @Override
    public void open(URI connectionURI, Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        logger.info("node-facing: OPEN called for connection {} with content {}.", connectionURI, content);
        if (connectionURI == null) throw new IllegalArgumentException("connectionURI is not set");

        //load connection, checking if it exists
        Connection con = DataAccessUtils.loadConnection(connectionRepository, connectionURI);
        //set new state and save in the db
        con.setState(con.getState().transit(ConnectionEventType.OWNER_OPEN));
        //save in the db
        connectionRepository.saveAndFlush(con);
    }

    @Override
    public void close(final URI connectionURI, Model content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
    {
        logger.info("node-facing: CLOSE called for connection {}", connectionURI);
        if (connectionURI == null) throw new IllegalArgumentException("connectionURI is not set");

        //load connection, checking if it exists
        Connection con = DataAccessUtils.loadConnection(connectionRepository, connectionURI);
        //set new state and save in the db
        con.setState(con.getState().transit(ConnectionEventType.OWNER_CLOSE));
        //save in the db
        connectionRepository.saveAndFlush(con);
    }

    @Override
    public void textMessage(final URI connectionURI, final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
    {
        logger.info("node-facing: SEND_TEXT_MESSAGE called for connection {} with message {}", connectionURI, message);
        if (connectionURI == null) throw new IllegalArgumentException("connectionURI is not set");
        if (message == null) throw new IllegalArgumentException("message is not set");
        //load connection, checking if it exists
        Connection con = DataAccessUtils.loadConnection(connectionRepository, connectionURI);

        //perform state transit (should not result in state change)
        //ConnectionState nextState = performStateTransit(con, ConnectionEventType.OWNER_MESSAGE);
        //construct chatMessage object to store in the db
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setCreationDate(new Date());
        chatMessage.setLocalConnectionURI(con.getConnectionURI());
        chatMessage.setMessage(message);
        chatMessage.setOriginatorURI(con.getRemoteNeedURI());
        //save in the db
        chatMessageRepository.saveAndFlush(chatMessage);
    }
}
