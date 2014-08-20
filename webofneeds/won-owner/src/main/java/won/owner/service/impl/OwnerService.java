package won.owner.service.impl;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import won.protocol.exception.MultipleQueryResultsFoundException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDecoder;
import won.protocol.message.WonMessageType;
import won.protocol.model.NeedState;
import won.protocol.owner.OwnerProtocolNeedServiceClientSide;
import won.protocol.util.RdfUtils;
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

  // ToDo (FS): add security layer

  public void handleMessageEventFromClient(Dataset wonMessage)
  {
    handleMessageEventFromClient(WonMessageDecoder.decodeFromDataset(wonMessage));
  }

  public void handleMessageEventFromClient(WonMessage wonMessage)
  {

    // ToDo (FS): implement

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

    // ToDo (FS): implement

  }

}
