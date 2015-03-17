package won.owner.service.impl;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import won.cryptography.service.SecureRandomNumberServiceImpl;
import won.owner.service.OwnerApplicationServiceCallback;
import won.owner.service.OwnerProtocolOwnerServiceCallback;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.*;
import won.protocol.owner.OwnerProtocolNeedServiceClientSide;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * User: fsalcher
 * Date: 18.08.2014
 */
public class OwnerApplicationService implements OwnerProtocolOwnerServiceCallback
{

  private static final Logger logger = LoggerFactory.getLogger(OwnerApplicationService.class);

  @Autowired
  @Qualifier("default")
  private OwnerProtocolNeedServiceClientSide ownerProtocolService;

  //when the callback is a bean in a child context, it sets itself as a dependency here
  @Autowired(required = false)
  private OwnerApplicationServiceCallback ownerApplicationServiceCallbackToClient =
    new NopOwnerApplicationServiceCallback();


  @Autowired
  private Executor executor;

  @Autowired
  private SecureRandomNumberServiceImpl randomNumberService;

  final private Map<URI, WonMessage> wonMessageMap = new HashMap<>();

  // ToDo (FS): add security layer

  public void handleMessageEventFromClient(Dataset wonMessage) {
    handleMessageEventFromClient(WonMessageDecoder.decodeFromDataset(wonMessage));
  }

  public void handleMessageEventFromClient(WonMessage wonMessage) {

    try {
      ownerProtocolService.sendWonMessage(wonMessage);
    } catch (Exception e) {
      //TODO: send error message back to client!
      logger.info("could not send WonMessage", e);
    }
  }

  public void handleMessageEventFromWonNode(Dataset wonMessage) {
    handleMessageEventFromWonNode(WonMessageDecoder.decodeFromDataset(wonMessage));
  }

  public void handleMessageEventFromWonNode(WonMessage wonMessage) {
    ownerApplicationServiceCallbackToClient.onMessage(wonMessage);
  }

  // ToDo (FS): most (all?) of the response messages should be send back from the WON node (this is only temporary)
  // this is only a CREATE RESPONSE
  private void sendBackResponseMessageToClient(WonMessage wonMessage, Resource responseType) {

    try {
      URI responseMessageURI = URI.create(wonMessage.getSenderNeedURI().toString() +
                                            "/event/" +
                                            randomNumberService
                                              .generateRandomString(9));

      WonMessageBuilder wonMessageBuilder = new WonMessageBuilder();
      WonMessage responseWonMessage = wonMessageBuilder
        .setWonMessageType(WonMessageType.CREATE_RESPONSE)
        .setMessageURI(responseMessageURI)
        .setSenderNodeURI(wonMessage.getReceiverNodeURI())
        .setReceiverNeedURI(wonMessage.getSenderNeedURI())
        .setResponseMessageState(responseType)
        .addRefersToURI(wonMessage.getMessageURI())
        .setWonMessageDirection(WonMessageDirection.FROM_EXTERNAL)
        .build();

      ownerApplicationServiceCallbackToClient.onMessage(responseWonMessage);
    } catch (WonMessageBuilderException e) {
      logger.warn("caught WonMessageBuilderException:", e);
    }
  }

  @Override
  public void onWonMessage(final WonMessage wonMessage){
    ownerApplicationServiceCallbackToClient.onMessage(wonMessage);
  }
  // ToDo (FS): methods only used until the messaging system is completely refactored then only one callback method will be used


  public void setOwnerApplicationServiceCallbackToClient(final OwnerApplicationServiceCallback ownerApplicationServiceCallbackToClient) {
    this.ownerApplicationServiceCallbackToClient = ownerApplicationServiceCallbackToClient;
  }



}
