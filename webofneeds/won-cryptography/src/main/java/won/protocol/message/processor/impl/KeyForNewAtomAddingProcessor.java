package won.protocol.message.processor.impl;

import java.security.PublicKey;

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

/**
 * This processor is intended for use in owners (bot or webapp). If the message
 * type is CREATE, the processor adds the appropriate public key to the atom's
 * RDF content. If the <code>fixedPrivateKeyAlias</code> property is set, the
 * processor generates at most one key pair with that alias and uses that key
 * pair for all atom it processes. If the <code>fixedPrivateKeyAlias</code>
 * property is not set, the processor generates one keypair per atom, using the
 * atom URI as the keypair's alias. This processor should be removed when
 * user's/atom's key management and signing happens in the Owner Client
 * (browser). User: ypanchenko Date: 10.04.2015
 */
public class KeyForNewAtomAddingProcessor implements WonMessageProcessor {
    private static final Logger logger = LoggerFactory.getLogger(KeyForNewAtomAddingProcessor.class);
    private CryptographyService cryptographyService;
    private KeyPairAliasDerivationStrategy keyPairAliasDerivationStrategy;

    public KeyForNewAtomAddingProcessor() {
    }

    public void setCryptographyService(final CryptographyService cryptoService) {
        this.cryptographyService = cryptoService;
    }

    @Override
    public WonMessage process(final WonMessage message) throws WonMessageProcessingException {
        try {
            if (message.getMessageType() == WonMessageType.CREATE_ATOM) {
                String atomUri = message.getSenderAtomURI().toString();
                Dataset msgDataset = WonMessageEncoder.encodeAsDataset(message);
                // generate and add atom's public key to the atom content
                String alias = keyPairAliasDerivationStrategy.getAliasForAtomUri(atomUri);
                if (cryptographyService.getPrivateKey(alias) == null) {
                    cryptographyService.createNewKeyPair(alias, alias);
                }
                PublicKey pubKey = cryptographyService.getPublicKey(alias);
                WonKeysReaderWriter keyWriter = new WonKeysReaderWriter();
                String contentName = message.getContentGraphURIs().get(0);
                Model contentModel = msgDataset.getNamedModel(contentName);
                keyWriter.writeToModel(contentModel, contentModel.createResource(atomUri), pubKey);
                return new WonMessage(msgDataset);
            } else if (message.getMessageType() == WonMessageType.REPLACE) {
                String atomUri = message.getSenderAtomURI().toString();
                Dataset msgDataset = WonMessageEncoder.encodeAsDataset(message);
                // we should already have the key. If not, that's a problem!
                String alias = keyPairAliasDerivationStrategy.getAliasForAtomUri(atomUri);
                if (cryptographyService.getPrivateKey(alias) == null) {
                    throw new IllegalStateException("Cannot replace atom " + atomUri + ": no key pair found");
                }
                PublicKey pubKey = cryptographyService.getPublicKey(alias);
                WonKeysReaderWriter keyWriter = new WonKeysReaderWriter();
                String contentName = message.getContentGraphURIs().get(0);
                Model contentModel = msgDataset.getNamedModel(contentName);
                keyWriter.writeToModel(contentModel, contentModel.createResource(atomUri), pubKey);
                return new WonMessage(msgDataset);
            }
        } catch (Exception e) {
            logger.error("Failed to add key", e);
            throw new WonMessageProcessingException(
                            "Failed to add key for atom in message " + message.getMessageURI().toString());
        }
        return message;
    }

    public void setKeyPairAliasDerivationStrategy(KeyPairAliasDerivationStrategy keyPairAliasDerivationStrategy) {
        this.keyPairAliasDerivationStrategy = keyPairAliasDerivationStrategy;
    }
}
