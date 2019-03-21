package won.protocol.message.processor.impl;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.cryptography.keymanagement.KeyPairAliasDerivationStrategy;
import won.cryptography.rdfsign.WonKeysReaderWriter;
import won.cryptography.service.CryptographyService;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageEncoder;
import won.protocol.message.WonMessageType;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.message.processor.exception.WonMessageProcessingException;

import java.security.PublicKey;

/**
 * This processor is intended for use in owners (bot or webapp).
 * <p>
 * If the message type is CREATE, the processor adds the appropriate public key
 * to the need's RDF content.
 * <p>
 * If the <code>fixedPrivateKeyAlias</code> property is set, the processor
 * generates at most one key pair with that alias and uses that key pair for all
 * need it processes.
 * <p>
 * If the <code>fixedPrivateKeyAlias</code> property is not set, the processor
 * generates one keypair per need, using the need URI as the keypair's alias.
 * <p>
 * This processor should be removed when user's/need's key management and
 * signing happens in the Owner Client (browser).
 * <p>
 * User: ypanchenko Date: 10.04.2015
 */
public class KeyForNewNeedAddingProcessor implements WonMessageProcessor {
  private static final Logger logger = LoggerFactory.getLogger(KeyForNewNeedAddingProcessor.class);

  private CryptographyService cryptographyService;

  private KeyPairAliasDerivationStrategy keyPairAliasDerivationStrategy;

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
        Dataset msgDataset = WonMessageEncoder.encodeAsDataset(message);
        // generate and add need's public key to the need content

        String alias = keyPairAliasDerivationStrategy.getAliasForNeedUri(needUri);
        if (cryptographyService.getPrivateKey(alias) == null) {
          cryptographyService.createNewKeyPair(alias, alias);
        }

        PublicKey pubKey = cryptographyService.getPublicKey(alias);
        WonKeysReaderWriter keyWriter = new WonKeysReaderWriter();
        String contentName = message.getContentGraphURIs().get(0);
        Model contentModel = msgDataset.getNamedModel(contentName);
        keyWriter.writeToModel(contentModel, contentModel.createResource(needUri), pubKey);
        return new WonMessage(msgDataset);
      }
    } catch (Exception e) {
      logger.error("Failed to add key", e);
      throw new WonMessageProcessingException(
          "Failed to add key for need in message " + message.getMessageURI().toString());
    }
    return message;
  }

  public void setKeyPairAliasDerivationStrategy(KeyPairAliasDerivationStrategy keyPairAliasDerivationStrategy) {
    this.keyPairAliasDerivationStrategy = keyPairAliasDerivationStrategy;
  }

}
