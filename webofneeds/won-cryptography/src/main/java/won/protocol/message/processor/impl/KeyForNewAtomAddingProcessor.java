package won.protocol.message.processor.impl;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.cryptography.keymanagement.KeyPairAliasDerivationStrategy;
import won.cryptography.rdfsign.WonKeysReaderWriter;
import won.cryptography.service.CryptographyService;
import won.protocol.exception.WonMessageProcessingException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageType;
import won.protocol.message.processor.WonMessageProcessor;

import java.lang.invoke.MethodHandles;
import java.security.PublicKey;

/**
 * This processor is intended for use in owners (bot or webapp). If the message
 * type is CREATE, the processor adds the appropriate public key to the atom's
 * #key graph. If the <code>fixedPrivateKeyAlias</code> property is set, the
 * processor generates at most one key pair with that alias and uses that key
 * pair for all atom it processes. If the <code>fixedPrivateKeyAlias</code>
 * property is not set, the processor generates one keypair per atom, using the
 * atom URI as the keypair's alias. This processor should be removed when
 * user's/atom's key management and signing happens in the Owner Client
 * (browser). User: ypanchenko Date: 10.04.2015
 */
public class KeyForNewAtomAddingProcessor implements WonMessageProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    private CryptographyService cryptographyService;
    @Autowired
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
                String atomUri = message.getAtomURIRequired().toString();
                // generate and add atom's public key to the atom content
                String alias = keyPairAliasDerivationStrategy.getAliasForAtomUri(atomUri);
                if (cryptographyService.getPrivateKey(alias) == null) {
                    cryptographyService.createNewKeyPair(alias, alias);
                }
                PublicKey pubKey = cryptographyService.getPublicKey(alias);
                WonKeysReaderWriter keyWriter = new WonKeysReaderWriter();
                Model keyGraph = ModelFactory.createDefaultModel();
                keyWriter.writeToModel(keyGraph, keyGraph.createResource(atomUri), pubKey);
                message.addOrReplaceContentGraph(WonMessage.KEY_URI_SUFFIX, keyGraph);
            }
        } catch (Exception e) {
            logger.error("Failed to add key", e);
            throw new WonMessageProcessingException(
                            "Failed to add key for atom in message " + message.getMessageURI().toString(), e);
        }
        return message;
    }

    public void setKeyPairAliasDerivationStrategy(KeyPairAliasDerivationStrategy keyPairAliasDerivationStrategy) {
        this.keyPairAliasDerivationStrategy = keyPairAliasDerivationStrategy;
    }
}
