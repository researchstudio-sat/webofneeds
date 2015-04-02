package won.owner.service.impl;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import de.uni_koblenz.aggrimm.icp.crypto.sign.algorithm.algorithm.SignatureAlgorithmFisteus2010;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import won.cryptography.rdfsign.WonKeysExtractor;
import won.cryptography.rdfsign.WonSigner;
import won.cryptography.service.KeyStoreService;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDecoder;
import won.protocol.message.WonMessageEncoder;
import won.protocol.message.WonMessageType;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.message.sender.WonMessageSender;

import java.security.PublicKey;

/**
 * Service that connects client-side logic (e.g. the WonWebSocketHandler in won-owner-webapp)
 * with facilities for sending and receiving messages.
 */
public class OwnerApplicationService implements WonMessageProcessor, WonMessageSender
{

  private static final Logger logger = LoggerFactory.getLogger(OwnerApplicationService.class);

  @Autowired
  @Qualifier("default")
  private WonMessageSender wonMessageSenderDelegate;

  @Autowired
  private KeyStoreService keyStoreService;

  //when the callback is a bean in a child context, it sets itself as a dependency here
  @Autowired(required = false)
  private WonMessageProcessor messageProcessorDelegate =
    new NopOwnerApplicationServiceCallback();

  /**
   * Sends a message to the won node.
   * @param wonMessage
   */
  public void sendWonMessage(WonMessage wonMessage) {
    try {
      //TODO: adding public keys and signing can be removed when it happens in the browser
      String needUri = wonMessage.getSenderNeedURI().toString();
      Dataset msgDataset =  WonMessageEncoder.encodeAsDataset(wonMessage);
      if (wonMessage.getMessageType() == WonMessageType.CREATE_NEED) {
        // add need's public key to the need content
        PublicKey pubKey = keyStoreService.getPublicKey(needUri);
        WonKeysExtractor extractor = new WonKeysExtractor();
        String contentName = wonMessage.getContentGraphURIs().get(0);
        Model contentModel = msgDataset.getNamedModel(contentName);
        extractor.addToModel(contentModel, contentModel.createResource(contentName), pubKey);
      }
      // add signature:
      WonSigner signer = new WonSigner(msgDataset, new SignatureAlgorithmFisteus2010());
      signer.sign(keyStoreService.getPrivateKey(needUri), needUri, wonMessage.getContentGraphURIs());
      WonMessage wonMessageSigned = WonMessageDecoder.decodeFromDataset(msgDataset);
      // send to node:
      wonMessageSenderDelegate.sendWonMessage(wonMessageSigned);
    } catch (Exception e) {
      //TODO: send error message back to client!
      logger.info("could not send WonMessage", e);
    }
  }

  /**
   * Sends a message to the owner.
   * @param wonMessage
   */
  @Override
  public WonMessage process(final WonMessage wonMessage){
    //todo: check signatures
    return messageProcessorDelegate.process(wonMessage);
  }

  public void setMessageProcessorDelegate(final WonMessageProcessor messageProcessorDelegate) {
    this.messageProcessorDelegate = messageProcessorDelegate;
  }

}
