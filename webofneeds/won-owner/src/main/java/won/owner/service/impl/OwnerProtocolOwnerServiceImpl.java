package won.owner.service.impl;

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
import won.protocol.service.ConnectionCommunicationService;
import won.protocol.service.OwnerFacingNeedCommunicationService;
import org.springframework.util.*;
import won.protocol.util.DataAccessUtils;

import java.net.URI;
import java.text.MessageFormat;
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

    @Override
    public void hintReceived(final URI ownNeedURI, final URI otherNeedURI, final double score, final URI originatorURI) throws NoSuchNeedException, IllegalMessageForNeedStateException {
        logger.info(MessageFormat.format("node-facing: HINT_RECEIVED called for own need {0}, other need {1}, with score {2} from originator {3}", ObjectUtils.nullSafeToString(new Object[]{ownNeedURI, otherNeedURI, score, originatorURI})));

        if (ownNeedURI == null) throw new IllegalArgumentException("needURI is not set");
        if (otherNeedURI == null) throw new IllegalArgumentException("otherNeedURI is not set");
        if (score < 0 || score > 1) throw new IllegalArgumentException("score is not in [0,1]");
        if (originatorURI == null) throw new IllegalArgumentException("originator is not set");
        if (ownNeedURI.equals(otherNeedURI)) throw new IllegalArgumentException("needURI and otherNeedURI are the same");


        //Load need (throws exception if not found)
        Need need = DataAccessUtils.loadNeed(needRepository, ownNeedURI);
        if (! isNeedActive(need)) throw new IllegalMessageForNeedStateException(ownNeedURI, NeedMessage.HINT.name(), need.getState());

        //save match
        Match match = new Match();
        match.setFromNeed(ownNeedURI);
        match.setToNeed(otherNeedURI);
        match.setScore(score);
        match.setOriginator(originatorURI);
        matchRepository.saveAndFlush(match);
    }

    private boolean isNeedActive(final Need need) {
        return NeedState.ACTIVE == need.getState();
    }

    @Override
    public void connectionRequested(final URI ownNeedURI, final URI otherNeedURI, final URI ownConnectionURI, final String message) throws NoSuchNeedException, ConnectionAlreadyExistsException, IllegalMessageForNeedStateException
    {
        logger.info(MessageFormat.format("node-facing: CONNECTION_REQUESTED called for own need {0}, other need {1}, own connection {2} and message ''{3}''", ownNeedURI,otherNeedURI,ownConnectionURI,message));
        if (ownNeedURI == null) throw new IllegalArgumentException("needURI is not set");
        if (otherNeedURI == null) throw new IllegalArgumentException("otherNeedURI is not set");
        if (ownConnectionURI == null) throw new IllegalArgumentException("otherConnectionURI is not set");
        if (ownNeedURI.equals(otherNeedURI)) throw new IllegalArgumentException("needURI and otherNeedURI are the same");

        //Load need (throws exception if not found)
        Need need = DataAccessUtils.loadNeed(needRepository,ownNeedURI);
        if (! isNeedActive(need)) throw new IllegalMessageForNeedStateException(ownNeedURI, NeedMessage.CONNECTION_REQUESTED.name(), need.getState());
        //Create new connection object on our side

        //check if there already exists a connection between those two
        //we have multiple options:
        //a) no connection exists -> create new
        //b) a connection exists in state ESTABLISHED -> error message
        //c) a connection exists in state REQUEST_SENT. Our request was first, we won't accept a request
        //   from the other side. They have to accept/deny ours! -> error message
        //d) a connection exists in state REQUEST_RECEIVED. The remote side contacts us repeatedly, it seems.
        //   -> error message
        //e) a connection exists in state CLOSED -> create new
        List<Connection> existingConnections = connectionRepository.findByNeedURIAndRemoteNeedURI(ownNeedURI, otherNeedURI);
        if (existingConnections.size() > 0){
            for(Connection conn: existingConnections){
                if (ConnectionState.ESTABLISHED == conn.getState()
                        || ConnectionState.REQUEST_RECEIVED == conn.getState()
                        || ConnectionState.REQUEST_SENT == conn.getState()) {
                    throw new ConnectionAlreadyExistsException(conn.getConnectionURI(),ownNeedURI,otherNeedURI);
                }
            }
        }


        Connection con = new Connection();
        con.setNeedURI(ownNeedURI);
        con.setState(ConnectionState.REQUEST_RECEIVED);
        con.setRemoteNeedURI(otherNeedURI);
        con.setRemoteConnectionURI(otherNeedURI);
        //save connection (this creates a new URI)
        con = connectionRepository.saveAndFlush(con);
        //create and set new uri
        con.setConnectionURI(ownConnectionURI);
        con = connectionRepository.saveAndFlush(con);

        //TODO:  message handling
    }

    @Override
    public void accept(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
    {
        logger.info(MessageFormat.format("node-facing: ACCEPT called for connection {0}",connectionURI));
        if (connectionURI == null) throw new IllegalArgumentException("connectionURI is not set");
        //load connection, checking if it exists
        Connection con = DataAccessUtils.loadConnection(connectionRepository, connectionURI);
        //perform state transit
        ConnectionState nextState = performStateTransit(con, ConnectionMessage.OWNER_ACCEPT);
        //set new state and save in the db
        con.setState(nextState);
        //save in the db
        final Connection connectionForRunnable = connectionRepository.saveAndFlush(con);

    }

    @Override
    public void deny(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
    {
        logger.info(MessageFormat.format("node-facing: DENY called for connection {0}",connectionURI));
        if (connectionURI == null) throw new IllegalArgumentException("connectionURI is not set");
        //load connection, checking if it exists
        Connection con = DataAccessUtils.loadConnection(connectionRepository, connectionURI);
        //perform state transit
        ConnectionState nextState = performStateTransit(con, ConnectionMessage.OWNER_DENY);
        //set new state and save in the db
        con.setState(nextState);
        //save in the db
        final Connection connectionForRunnable = connectionRepository.saveAndFlush(con);
    }

    @Override
    public void close(final URI connectionURI) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
    {
        logger.info(MessageFormat.format("node-facing: CLOSE called for connection {0}",connectionURI));
        if (connectionURI == null) throw new IllegalArgumentException("connectionURI is not set");
        //load connection, checking if it exists
        Connection con = DataAccessUtils.loadConnection(connectionRepository, connectionURI);
        //perform state transit
        ConnectionState nextState = performStateTransit(con, ConnectionMessage.OWNER_CLOSE);
        //set new state and save in the db
        con.setState(nextState);
        //save in the db
        final Connection connectionForRunnable = connectionRepository.saveAndFlush(con);
    }

    @Override
    public void sendTextMessage(final URI connectionURI, final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
    {
        logger.info(MessageFormat.format("node-facing: SEND_TEXT_MESSAGE called for connection {0} with message {1}",connectionURI, message));
        if (connectionURI == null) throw new IllegalArgumentException("connectionURI is not set");
        if (message == null) throw new IllegalArgumentException("message is not set");
        //load connection, checking if it exists
        Connection con = DataAccessUtils.loadConnection(connectionRepository, connectionURI);
        //perform state transit (should not result in state change)
        ConnectionState nextState = performStateTransit(con, ConnectionMessage.OWNER_MESSAGE);
        //construct chatMessage object to store in the db
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setCreationDate(new Date());
        chatMessage.setLocalConnectionURI(con.getConnectionURI());
        chatMessage.setMessage(message);
        chatMessage.setOriginatorURI(con.getNeedURI());
        //save in the db
        chatMessageRepository.saveAndFlush(chatMessage);
        final URI remoteConnectionURI = con.getRemoteConnectionURI();
    }


    /**
     * Calculates the connectionState resulting from the message in the current connection state.
     * Checks if the specified message is allowed in the connection's state and throws an exception if not.
     * @param con
     * @param msg
     * @return
     * @throws won.protocol.exception.IllegalMessageForConnectionStateException if the message is not allowed in the connection's current state
     */
    private ConnectionState performStateTransit(Connection con, ConnectionMessage msg) throws IllegalMessageForConnectionStateException{
        if (!msg.isMessageAllowed(con.getState())){
            throw new IllegalMessageForConnectionStateException(con.getConnectionURI(), msg.name(),con.getState());
        }
        return con.getState().transit(msg);
    }

}
