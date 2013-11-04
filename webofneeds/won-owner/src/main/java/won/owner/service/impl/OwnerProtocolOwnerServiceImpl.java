package won.owner.service.impl;

import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.owner.protocol.impl.OwnerProtocolNeedServiceClientJMSBased;
import won.protocol.jms.WonMessageListener;
import won.protocol.exception.*;
import won.protocol.model.*;

import won.protocol.owner.OwnerProtocolOwnerService;
import won.protocol.repository.ChatMessageRepository;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.MatchRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.util.DataAccessUtils;

import javax.jms.Message;
import java.net.URI;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gabriel
 * Date: 03.12.12
 * Time: 14:12
 */
//TODO copied from OwnerProtocolOwnerService... refactoring needed
public class OwnerProtocolOwnerServiceImpl implements OwnerProtocolOwnerService, WonMessageListener{

    final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void onMessage(Message message) {
       /* try{
            TextMessage msg = (TextMessage) message;
            message.
            logger.info("reading message " + msg.getText());
            message
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
            chatMessage.setMessage(msg.getText());
            chatMessage.setOriginatorURI(con.getRemoteNeedURI());
            //save in the db
            chatMessageRepository.saveAndFlush(chatMessage);
        }catch (JMSException e){
            e.printStackTrace();
        }
          */
    }

    @Override
    public void consume(URI connectionURI, Message msg) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Autowired
    private NeedRepository needRepository;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private OwnerProtocolNeedServiceClientJMSBased ownerService;

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
        Need need = DataAccessUtils.loadNeed(needRepository,ownNeedURI);
        if (! isNeedActive(need)) throw new IllegalMessageForNeedStateException(ownNeedURI, ConnectionEventType.PARTNER_OPEN.name(), need.getState());
        //Create new connection object on our side

        //set new uri
        List<Connection> existingConnections = connectionRepository.findByNeedURIAndRemoteNeedURI(ownNeedURI, otherNeedURI);
        if (existingConnections.size() > 0){
            for(Connection conn: existingConnections){
                if (ConnectionState.CONNECTED == conn.getState() ||
                        ConnectionState.REQUEST_RECEIVED == conn.getState()) {
                    throw new ConnectionAlreadyExistsException(conn.getConnectionURI(),ownNeedURI,otherNeedURI);
                } else {
                    conn.setState(conn.getState().transit(ConnectionEventType.OWNER_OPEN));
                    connectionRepository.saveAndFlush(conn);
                }
            }
        } else {
            Connection con = new Connection();
            con.setNeedURI(ownNeedURI);
            con.setState(ConnectionState.REQUEST_RECEIVED);
            con.setRemoteNeedURI(otherNeedURI);
            con.setConnectionURI(ownConnectionURI);
            connectionRepository.saveAndFlush(con);
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


    public void setOwnerService(OwnerProtocolNeedServiceClientJMSBased ownerService) {
        this.ownerService = ownerService;
    }



}
