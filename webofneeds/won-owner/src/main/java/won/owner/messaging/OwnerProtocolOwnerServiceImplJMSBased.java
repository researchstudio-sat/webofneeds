/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.owner.messaging;

import com.hp.hpl.jena.rdf.model.Model;
import org.apache.camel.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.owner.messaging.OwnerProtocolNeedServiceClientJMSBased;
import won.protocol.exception.*;
import won.protocol.model.*;
import won.protocol.owner.OwnerProtocolOwnerService;
import won.protocol.repository.*;
import won.protocol.util.DataAccessUtils;
import won.protocol.util.RdfUtils;

import java.net.URI;
import java.util.List;


/**
 * Created with IntelliJ IDEA.
 * User: Gabriel
 * Date: 03.12.12
 * Time: 14:12
 */
//TODO copied from OwnerProtocolOwnerService... refactoring needed
    //TODO: refactor service interfaces.
public class OwnerProtocolOwnerServiceImplJMSBased {//implements OwnerProtocolOwnerService {

    final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private WonNodeRepository wonNodeRepository;

    @Autowired
    OwnerProtocolOwnerService delegate;

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


   // @Override
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

  //  @Override
    public void connect(@Header("ownNeedURI") final String ownNeedURI, @Header("otherNeedURI")final String otherNeedURI, @Header("ownConnectionURI")final String ownConnectionURI,
                        @Header("content")final String content) throws NoSuchNeedException, ConnectionAlreadyExistsException, IllegalMessageForNeedStateException
    {

        logger.info("node-facing: CONNECTION_REQUESTED called for own need {}, other need {}, own connection {} and content ''{}''", new Object[]{ownNeedURI,otherNeedURI,ownConnectionURI, content});
        if (ownNeedURI == null) throw new IllegalArgumentException("needURI is not set");
        if (otherNeedURI == null) throw new IllegalArgumentException("otherNeedURI is not set");
        if (ownConnectionURI == null) throw new IllegalArgumentException("otherConnectionURI is not set");
        if (ownNeedURI.equals(otherNeedURI)) throw new IllegalArgumentException("needURI and otherNeedURI are the same");

        delegate.connect(ownNeedURI,otherNeedURI,ownConnectionURI,content);
        //Load need (throws exception if not found)
       /* Need need = DataAccessUtils.loadNeed(needRepository,ownNeedURIConvert);
        if (! isNeedActive(need)) throw new IllegalMessageForNeedStateException(ownNeedURIConvert, ConnectionEventType.PARTNER_OPEN.name(), need.getState()); */
        //Create new connection object on our side

        //set new uri
        /*List<Connection> existingConnections = connectionRepository.findByNeedURIAndRemoteNeedURI(ownNeedURIConvert, otherNeedURIConvert);
        if (existingConnections.size() > 0){
            for(Connection conn: existingConnections){
                if (ConnectionState.CONNECTED == conn.getState() ||
                        ConnectionState.REQUEST_RECEIVED == conn.getState()) {
                    throw new ConnectionAlreadyExistsException(conn.getConnectionURI(),ownNeedURIConvert,otherNeedURIConvert);
                } else {
                    conn.setState(conn.getState().transit(ConnectionEventType.OWNER_OPEN));
                    connectionRepository.saveAndFlush(conn);
                }
            }
        } else {
            Connection con = new Connection();
            con.setNeedURI(ownNeedURIConvert);
            con.setState(ConnectionState.REQUEST_RECEIVED);
            con.setRemoteNeedURI(otherNeedURIConvert);
            con.setConnectionURI(ownConnectionURIConvert);
            connectionRepository.saveAndFlush(con);

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

        for(Connection c : connections) { */
          //TODO: check remote need type as well or create GroupMemberFacet
       /*   if (facetURI.equals(c.getTypeURI()))
            con = c;
        }        */

        //TODO: impose unique constraint on connections
       /* if(con != null) {
          if(ConnectionEventType.PARTNER_OPEN.isMessageAllowed(con.getState())) {
            //TODO: Move this to the transition() - Method in ConnectionState
            con.setState(con.getState().transit(ConnectionEventType.PARTNER_OPEN));
            con = connectionRepository.saveAndFlush(con);
          } else {
            throw new ConnectionAlreadyExistsException(con.getConnectionURI(), con.getNeedURI(), con.getRemoteNeedURI());
          }
        }

        if (con == null) {  */
          /* Create connection */
       /*   con = new Connection();
          con.setNeedURI(ownNeedURI);
          con.setState(ConnectionState.REQUEST_RECEIVED);
          con.setRemoteNeedURI(otherNeedURI);
          con.setConnectionURI(ownConnectionURI);
          con.setTypeURI(facetURI);
          connectionRepository.saveAndFlush(con);
                                           */
          //TODO: do we save the connection content? where? as a chat content?

       // }
    }

    //@Override
    public void open(@Header("connectionURI")String connectionURI, @Header("content")String content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        logger.info("node-facing: OPEN called for connection {} with content {}.", connectionURI, RdfUtils.toModel(content));
        if (connectionURI == null) throw new IllegalArgumentException("connectionURI is not set");
        delegate.open(URI.create(connectionURI), RdfUtils.toModel(content));
        //load connection, checking if it exists
       // Connection con = DataAccessUtils.loadConnection(connectionRepository, URI.create(connectionURI));
        //set new state and save in the db
       /* logger.info("CONNECTION STATE: "+con.getState().toString());
        con.setState(con.getState().transit(ConnectionEventType.OWNER_OPEN));
        logger.info("Connection State after Transit: "+con.getState().toString());  */
        //save in the db
        //connectionRepository.saveAndFlush(con);
    }

  //  @Override
    public void close(@Header("connectionURI")final String connectionURI, @Header("content")String content) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
    {
        logger.info("node-facing: CLOSE called for connection {}", connectionURI);

        if (connectionURI == null) throw new IllegalArgumentException("connectionURI is not set");
        delegate.close(URI.create(connectionURI),RdfUtils.toModel(content));
        //load connection, checking if it exists
        //Connection con = DataAccessUtils.loadConnection(connectionRepository, URI.create(connectionURI));
        //set new state and save in the db
       // con.setState(con.getState().transit(ConnectionEventType.OWNER_CLOSE));
        //save in the db
       // connectionRepository.saveAndFlush(con);
    }


  //  @Override
    public void textMessage(@Header("connectionURI")final String connectionURI, @Header("message")final String message) throws NoSuchConnectionException, IllegalMessageForConnectionStateException
    {

        logger.info("node-facing: SEND_TEXT_MESSAGE called for connection {} with message {}", connectionURI, message);
        if (connectionURI == null) throw new IllegalArgumentException("connectionURI is not set");
        if (message == null) throw new IllegalArgumentException("message is not set");

        delegate.textMessage(URI.create(connectionURI),message);

       // URI connectionURIConvert = URI.create(connectionURI);
        //load connection, checking if it exists
       // Connection con = DataAccessUtils.loadConnection(connectionRepository, connectionURIConvert);

        //perform state transit (should not result in state change)
        //ConnectionState nextState = performStateTransit(con, ConnectionEventType.OWNER_MESSAGE);
        //construct chatMessage object to store in the db
       /* ChatMessage chatMessage = new ChatMessage();
        chatMessage.setCreationDate(new Date());
        chatMessage.setLocalConnectionURI(con.getConnectionURI());
        chatMessage.setMessage(message);
        chatMessage.setOriginatorURI(con.getRemoteNeedURI());   */
        //save in the db
       //chatMessageRepository.saveAndFlush(chatMessage);
    }


    public void setOwnerService(OwnerProtocolNeedServiceClientJMSBased ownerService) {
        this.ownerService = ownerService;
    }
}
