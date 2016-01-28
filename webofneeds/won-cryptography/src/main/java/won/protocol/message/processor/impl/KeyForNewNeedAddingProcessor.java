package won.protocol.message.processor.impl;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.cryptography.rdfsign.WonKeysReaderWriter;
import won.cryptography.service.CryptographyService;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageEncoder;
import won.protocol.message.WonMessageType;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.message.processor.exception.WonMessageProcessingException;

import java.security.PublicKey;

/**
 *  This processor is intended for use in Owner Server. If the message type is create_need
 *  message, it generates a key pair for this need, stores it in the key store, and adds the
 *  public keys of this key pair into the need's RDF content.
 *
 *  This processor should be removed when user's/need's key management and signing happens in
 *  the Owner Client (browser).
 *
 * User: ypanchenko
 * Date: 10.04.2015
 */
public class KeyForNewNeedAddingProcessor implements WonMessageProcessor {
  private static final Logger logger = LoggerFactory.getLogger(KeyForNewNeedAddingProcessor.class);

  private CryptographyService cryptographyService;

  public KeyForNewNeedAddingProcessor() {
  }

  public void setCryptographyService(final CryptographyService cryptoService) {
    this.cryptographyService = cryptoService;
  }

  @Override
  public WonMessage process(final WonMessage message) throws WonMessageProcessingException {

    try {
      if (message.getMessageType() == WonMessageType.CREATE_NEED) {
        String needUri = message.getSenderNeedURI().toString();
        Dataset msgDataset =  WonMessageEncoder.encodeAsDataset(message);
        // generate and add need's public key to the need content
        if (cryptographyService.getPrivateKey(needUri) == null) {
          cryptographyService.createNewKeyPair(needUri, needUri);
        }
        PublicKey pubKey = cryptographyService.getPublicKey(needUri);
        WonKeysReaderWriter keyWriter = new WonKeysReaderWriter();
        String contentName = message.getContentGraphURIs().get(0);
        Model contentModel = msgDataset.getNamedModel(contentName);
        keyWriter.writeToModel(contentModel, contentModel.createResource(needUri), pubKey);
      }
    } catch (Exception e) {
      logger.error("Failed to add key", e);
      throw new WonMessageProcessingException("Failed to add key for need in message " + message.getMessageURI().toString());
    }
    return message;
  }


}
