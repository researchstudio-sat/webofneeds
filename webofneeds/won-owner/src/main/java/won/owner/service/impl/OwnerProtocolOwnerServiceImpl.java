package won.owner.service.impl;

import com.hp.hpl.jena.rdf.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.node.facet.impl.WON_TX;
import won.owner.service.OwnerProtocolOwnerServiceCallback;
import won.protocol.exception.*;
import won.protocol.message.WonMessage;
import won.protocol.model.*;
import won.protocol.repository.ChatMessageRepository;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.MatchRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.util.DataAccessUtils;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.WON;

import java.net.URI;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gabriel
 * Date: 03.12.12
 * Time: 14:12
 */
    //TODO: refactor service interfaces.
public class OwnerProtocolOwnerServiceImpl {//implements OwnerProtocolOwnerService{

    final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private NeedRepository needRepository;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    //handler for incoming won protocol messages. The default handler does nothing.
    @Autowired(required = false)
    private OwnerProtocolOwnerServiceCallback ownerServiceCallback = new NopOwnerProtocolOwnerServiceCallback();

 // @Autowired
 // private OwnerApplicationService ownerService;

    //TODO: refactor this to use DataAccessService

    public void hint(final String ownNeedURI, final String otherNeedURI,
                     final String score, final String originatorURI,
                     final String content, final WonMessage wonMessage)
            throws NoSuchNeedException, IllegalMessageForNeedStateException {
        logger.debug("owner from need: HINT called for own need {}, other need {}, " +
                       "with score {} from originator {} and content {}",
                new Object[]{ownNeedURI, otherNeedURI, score, originatorURI, content});



        URI ownNeedUriConvert = wonMessage.getReceiverNeedURI();
        URI otherNeedUriConvert = URI.create(RdfUtils.findOnePropertyFromResource(
          wonMessage.getMessageContent(), wonMessage.getMessageURI(),
          WON.HAS_MATCH_COUNTERPART).asResource().getURI());
        double scoreConvert = RdfUtils.findOnePropertyFromResource(
          wonMessage.getMessageContent(), wonMessage.getMessageURI(),
          WON.HAS_MATCH_SCORE).asLiteral().getDouble();
        URI originatorUriConvert = wonMessage.getSenderNodeURI();

        Model contentConvert = ModelFactory.createDefaultModel();
        Iterator<String> i = wonMessage.getMessageContent().listNames();
        while (i.hasNext()) {
          contentConvert.add(wonMessage.getMessageContent().getNamedModel(i.next()));
        }

        if (scoreConvert < 0 || scoreConvert > 1) throw new IllegalArgumentException("score is not in [0,1]");

        Match match = new Match();
        match.setFromNeed(ownNeedUriConvert);
        match.setToNeed(otherNeedUriConvert);
        match.setOriginator(originatorUriConvert);
        match.setScore(scoreConvert);
        //TODO: save new connection or find existing one!

        //ownerServiceCallback.onHint(match, contentConvert, wonMessage);
    }

    private boolean isNeedActive(final Need need) {
        return NeedState.ACTIVE == need.getState();
    }

    public void connect(final String ownNeedURI, final String otherNeedURI, final String ownConnectionURI,
                        final String content, final WonMessage wonMessage)
            throws NoSuchNeedException, ConnectionAlreadyExistsException, IllegalMessageForNeedStateException
    {

      URI ownNeedURIConvert;
      URI otherNeedURIConvert;
      URI ownConnectionURIConvert;
      URI facetURI;
     // Model contentConvert = wonMessage.getMessageContent();

      ownNeedURIConvert = wonMessage.getReceiverNeedURI();
      otherNeedURIConvert = wonMessage.getSenderNeedURI();

      ownConnectionURIConvert = wonMessage.getReceiverURI();


      facetURI = URI.create(RdfUtils.findOnePropertyFromResource(
        wonMessage.getMessageContent(),
        wonMessage.getMessageURI(),
        WON.HAS_FACET)
      .asResource().getURI());

      if (ownNeedURIConvert == null) throw new IllegalArgumentException("needURI is not set");
      if (otherNeedURIConvert == null) throw new IllegalArgumentException("otherNeedURI is not set");
      if (ownConnectionURIConvert == null) throw new IllegalArgumentException("otherConnectionURI is not set");
      if (ownNeedURIConvert.equals(otherNeedURIConvert)) throw new IllegalArgumentException("needURI and otherNeedURI are" +
                                                                                        " the " +
                                                                                   "same");
      logger.debug("owner from need: CONNECT called for own need {}, other need {}, own connection {} and content {}",
                   new Object[]{ownNeedURIConvert, otherNeedURIConvert, ownConnectionURIConvert, content});
      Connection con = findOrCreateConnection(ownNeedURIConvert, otherNeedURIConvert, ownConnectionURIConvert,
        facetURI, ConnectionState.REQUEST_RECEIVED);


     // ownerServiceCallback.onConnect(con, contentConvert, wonMessage);
      //ownerService.handleConnectMessageEventFromWonNode(con, contentConvert);
    }

  public Connection findOrCreateConnection(final URI ownNeedURIConvert, final URI otherNeedURIConvert,
    final URI ownConnectionURIConvert, final URI facetURI, final ConnectionState connectionState)
    throws NoSuchNeedException, IllegalMessageForNeedStateException {
    //Load need (throws exception if not found)
    Need need = DataAccessUtils.loadNeed(needRepository, ownNeedURIConvert);
    if (!isNeedActive(need))
      throw new IllegalMessageForNeedStateException(ownNeedURIConvert, ConnectionEventType.PARTNER_OPEN.name(), need.getState());

    List<Connection> connections = connectionRepository.findByNeedURIAndRemoteNeedURI(ownNeedURIConvert, otherNeedURIConvert);
    Connection con = null;

    for(Connection c : connections) {
      //TODO: check remote need type as well or create GroupMemberFacet
      if (c.getTypeURI() == null || //TODO: should not happen but currently does in case of a hint
          facetURI.equals(c.getTypeURI()) //make sure the facet is also the same!
        ) {
        con = c;
        break;
      }
    }

    if (con != null) {
      con.setState(connectionState);
      if (con.getTypeURI() == null) {
        con.setTypeURI(facetURI); //just in case it was null until now
      }
      con = connectionRepository.save(con);

    } else {
      /* Create connection */
      con = new Connection();
      con.setNeedURI(ownNeedURIConvert);
      con.setState(connectionState);
      con.setRemoteNeedURI(otherNeedURIConvert);
      con.setConnectionURI(ownConnectionURIConvert);
      con.setTypeURI(facetURI);
      connectionRepository.save(con);
    }
    return con;
  }

    public void open(URI connectionURI, Model content, final WonMessage wonMessage)
            throws NoSuchConnectionException, IllegalMessageForConnectionStateException {
        URI connectionURIFromWonMessage = wonMessage.getReceiverURI();

        logger.debug("owner from need: OPEN called for connection {}.", connectionURIFromWonMessage);
        if (connectionURIFromWonMessage == null) throw new IllegalArgumentException("connectionURI not defined");

        //load connection, checking if it exists
        Connection con = DataAccessUtils.loadConnection(connectionRepository, connectionURIFromWonMessage);
        //set new state and save in the db
        con.setState(con.getState().transit(ConnectionEventType.PARTNER_OPEN));
        //save in the db
        connectionRepository.save(con);
    //    ownerServiceCallback.onOpen(con, content, wonMessage);
        //ownerService.handleOpenMessageEventFromWonNode(con, content);

    }

    public void close(final URI connectionURI, Model content, final WonMessage wonMessage)
            throws NoSuchConnectionException, IllegalMessageForConnectionStateException
    {

        URI connectionURIFromWonMessage = wonMessage.getReceiverURI();

        logger.debug("owner from need: CLOSE called for connection {}", connectionURIFromWonMessage);
        if (connectionURIFromWonMessage == null)
          throw new IllegalArgumentException("connectionURI is not set");

        //load connection, checking if it exists
        Connection con = DataAccessUtils.loadConnection(connectionRepository, connectionURIFromWonMessage);
        //set new state and save in the db
        con.setState(con.getState().transit(ConnectionEventType.PARTNER_CLOSE));
        //save in the db
        connectionRepository.save(con);
     //   ownerServiceCallback.onClose(con, content, wonMessage);
        //ownerService.handleCloseMessageEventFromWonNode(con, content);
    }

    public void sendMessage(final URI connectionURI, final Model message, final WonMessage wonMessage)
            throws NoSuchConnectionException, IllegalMessageForConnectionStateException
    {

        URI connectionURIFromWonMessage = wonMessage.getReceiverURI();
        Model messageFromWonMessage = ModelFactory.createDefaultModel();
        Iterator<String> i = wonMessage.getMessageContent().listNames();
        while (i.hasNext()) {
          messageFromWonMessage.add(wonMessage.getMessageContent().getNamedModel(i.next()));
        }

        logger
          .debug("owner from need: SEND_TEXT_MESSAGE called for connection {} with message {}",
                 connectionURIFromWonMessage, messageFromWonMessage);
        if (connectionURIFromWonMessage == null) throw new IllegalArgumentException("connectionURI is not set");
        if (messageFromWonMessage == null) throw new IllegalArgumentException("message is not set");
        //load connection, checking if it exists
        Connection con = DataAccessUtils.loadConnection(connectionRepository, connectionURIFromWonMessage);
        Resource baseRes = messageFromWonMessage.getResource(message.getNsPrefixURI(""));
        StmtIterator stmtIterator = null;
        boolean baFacetType = false;
        if (con.getTypeURI().equals(FacetType.BAPCCoordinatorFacet.getURI()) ||
          con.getTypeURI().equals(FacetType.BAPCParticipantFacet.getURI()) ||
          con.getTypeURI().equals(FacetType.BACCCoordinatorFacet.getURI()) ||
          con.getTypeURI().equals(FacetType.BACCParticipantFacet.getURI()) ||
          con.getTypeURI().equals(FacetType.BAAtomicPCCoordinatorFacet.getURI()) ||
          con.getTypeURI().equals(FacetType.BAAtomicCCCoordinatorFacet.getURI())) {
          baFacetType = true;
          stmtIterator = baseRes.listProperties(WON_TX.COORDINATION_MESSAGE);
        } else {
          stmtIterator = baseRes.listProperties(WON.HAS_TEXT_MESSAGE);
        }
        String textMessage = null;
        while (stmtIterator.hasNext()) {
          RDFNode obj = stmtIterator.nextStatement().getObject();
          if (obj.isLiteral()) {
            textMessage = obj.asLiteral().getLexicalForm();
            break;
          } else if (baFacetType)
            textMessage = this.getCoordinationMessage(obj.toString());
          else
            textMessage = null;
        }
        if (textMessage == null) {
          logger.debug("could not extract text message from RDF content of message");
          textMessage = "[could not extract text message]";
        }
        //perform state transit (should not result in state change)
        //ConnectionState nextState = performStateTransit(con, ConnectionEventType.OWNER_MESSAGE);
        //construct chatMessage object to store in the db
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setCreationDate(new Date());
        chatMessage.setLocalConnectionURI(con.getConnectionURI());
        chatMessage.setMessage(textMessage);
        chatMessage.setOriginatorURI(con.getRemoteNeedURI());
        //save in the db
        chatMessageRepository.save(chatMessage);
       // ownerServiceCallback.onTextMessage(con, chatMessage, messageFromWonMessage, wonMessage);
        //ownerService.handleTextMessageEventFromWonNode(con, chatMessage, message);

    }

    //url -> Message
    public String getCoordinationMessage(String s)
    {
        String msg = null;
        msg = s.substring(s.lastIndexOf("#")+1);
        msg = msg.toUpperCase();
        msg = msg.substring(0,7)+"_"+msg.substring(7);
        return msg;
    }
    public void processIncomingWonMessage(){

    }
}
