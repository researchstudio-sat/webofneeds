package won.owner.service.impl;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import won.owner.messaging.OwnerClientOut;
import won.protocol.exception.MultipleQueryResultsFoundException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDecoder;
import won.protocol.message.WonMessageType;
import won.protocol.model.ChatMessage;
import won.protocol.model.Connection;
import won.protocol.model.Match;
import won.protocol.owner.OwnerProtocolNeedServiceClientSide;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.util.WonRdfUtils;

import java.net.URI;
import java.util.List;

/**
 * User: fsalcher
 * Date: 18.08.2014
 */
public class OwnerService
{

  private static final Logger logger = LoggerFactory.getLogger(OwnerService.class);

  @Autowired
  @Qualifier("default")
  private OwnerProtocolNeedServiceClientSide ownerProtocolService;

  @Autowired
  private OwnerClientOut ownerClientOut;

  @Autowired
  private ConnectionRepository connectionRepository;

  @Autowired
  private NeedRepository needRepository;


  // ToDo (FS): add security layer

  public void handleMessageEventFromClient(Dataset wonMessage)
  {
    handleMessageEventFromClient(WonMessageDecoder.decodeFromDataset(wonMessage));
  }

  public void handleMessageEventFromClient(WonMessage wonMessage)
  {

    // ToDo (FS): don't convert messages to the old protocol interfaces instead use the new message format

    WonMessageType wonMessageType = wonMessage.getMessageEvent().getMessageType();

    switch (wonMessageType) {
      case CREATE_NEED:

        Dataset messageContent = wonMessage.getMessageContent();

        URI senderURI = wonMessage.getMessageEvent().getSenderURI();

        URI ownerURI = null;
        try {
          ownerURI = WonRdfUtils.NeedUtils.queryOwner(messageContent);
        } catch (MultipleQueryResultsFoundException e) {
          logger.warn("caught MultipleOwnersFoundException:", e);
        }


        // ToDo (FS): maybe sender should be included in each message to retrieve the needURI

        // get the core graph of the message for the need model
        String coreModelURIString = senderURI.toString() + "#core";
        Model content = wonMessage.getMessageContent(coreModelURIString);

        // get the active status
        Boolean active = null;
        try {
          active = WonRdfUtils.NeedUtils.queryActiveStatus(messageContent);
        } catch (MultipleQueryResultsFoundException e) {
          logger.warn("caught MultipleOwnersFoundException:", e);
        }

        // get the wonNodeURI
        URI wonNodeURI = null;
        try {
          wonNodeURI = WonRdfUtils.NeedUtils.queryWonNode(messageContent);
        } catch (MultipleQueryResultsFoundException e) {
          logger.warn("caught MultipleOwnersFoundException:", e);
        }

        try {
          ownerProtocolService.createNeed(ownerURI, content, active, wonNodeURI, null);
        } catch (Exception e) {
          logger.warn("caught Exception:", e);
        }
        break;

      case CONNECT:
        try {
          URI needURI;
          URI otherNeedURI;

          needURI = wonMessage.getMessageEvent().getSenderURI();
          otherNeedURI = wonMessage.getMessageEvent().getReceiverURI();

          content = wonMessage.getMessageEvent().getModel();

          // ToDo (FS): change connect code such that the connectionID of the messageEvent will be used
          ownerProtocolService.connect(needURI, otherNeedURI, content, null);
        } catch (Exception e) {
          logger.warn("caught Exception", e);
        }
        break;

      case NEED_STATE:
        try {
          URI needURI;
          needURI = wonMessage.getMessageEvent().getSenderURI();

          switch (wonMessage.getMessageEvent().getNewNeedState()) {
            case ACTIVE:
              ownerProtocolService.activate(needURI, null);
              break;
            case INACTIVE:
              ownerProtocolService.deactivate(needURI, null);
          }
        } catch (Exception e) {
          logger.warn("caught Exception", e);
        }
        break;

      case OPEN:
        try {

          senderURI = wonMessage.getMessageEvent().getSenderURI();
          URI receiverURI = wonMessage.getMessageEvent().getReceiverURI();

          List<Connection> connections =
              connectionRepository.findByNeedURIAndRemoteNeedURI(senderURI, receiverURI);

          URI connectionURI = connections.get(0).getConnectionURI();

          content = wonMessage.getMessageEvent().getModel();

          ownerProtocolService.open(connectionURI, content, null);
        } catch (Exception e) {
          logger.warn("caught Exception", e);
        }
        break;

      case CLOSE:
        try {

          senderURI = wonMessage.getMessageEvent().getSenderURI();
          URI receiverURI = wonMessage.getMessageEvent().getReceiverURI();

          List<Connection> connections =
              connectionRepository.findByNeedURIAndRemoteNeedURI(senderURI, receiverURI);

          URI connectionURI = connections.get(0).getConnectionURI();

          content = wonMessage.getMessageEvent().getModel();

          ownerProtocolService.close(connectionURI, content, null);
        } catch (Exception e) {
          logger.warn("caught Exception", e);
        }
        break;

      case CONNECTION_MESSAGE:
        try {

          senderURI = wonMessage.getMessageEvent().getSenderURI();
          URI receiverURI = wonMessage.getMessageEvent().getReceiverURI();

          List<Connection> connections =
              connectionRepository.findByNeedURIAndRemoteNeedURI(senderURI, receiverURI);

          URI connectionURI = connections.get(0).getConnectionURI();

          content = wonMessage.getMessageEvent().getModel();

          ownerProtocolService.sendMessage(connectionURI, content, null);
        } catch (Exception e) {
          logger.warn("caught Exception", e);
        }
        break;

      default:
        break;
    }
  }

  public void handleMessageEventFromWonNode(Dataset wonMessage)
  {
    handleMessageEventFromWonNode(WonMessageDecoder.decodeFromDataset(wonMessage));
  }

  public void handleMessageEventFromWonNode(WonMessage wonMessage)
  {

    // ToDo (FS): handle messages

    ownerClientOut.sendMessage(wonMessage);

  }

  // ToDo (FS): methods only used until the messaging system is completely refactored
  public void handleHintMessageEventFromWonNode (Match match, final Model content)
  {
    // ToDo (FS): implement
  }

  public void handleConnectMessageEventFromWonNode (Connection con, final Model content)
  {
    // ToDo (FS): implement
  }

  public void handleOpenMessageEventFromWonNode (Connection con, final Model content)
  {
    // ToDo (FS): implement
  }

  public void handleCloseMessageEventFromWonNode (Connection con, final Model content)
  {
    // ToDo (FS): implement
  }

  public void handleTextMessageEventFromWonNode (Connection con, ChatMessage message, final Model content)
  {
    // ToDo (FS): implement
  }


}
