package won.protocol.message.processor.impl;

import java.security.PrivateKey;
import java.security.PublicKey;

import org.apache.jena.riot.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.cryptography.keymanagement.KeyPairAliasDerivationStrategy;
import won.cryptography.service.CryptographyService;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageEncoder;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.message.processor.exception.WonMessageProcessingException;

/**
 * User: ypanchenko Date: 03.04.2015
 */
public class SignatureAddingWonMessageProcessor implements WonMessageProcessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private CryptographyService cryptographyService;
    private KeyPairAliasDerivationStrategy keyPairAliasDerivationStrategy;

    public SignatureAddingWonMessageProcessor() {
    }

    @Override
    public WonMessage process(final WonMessage message) throws WonMessageProcessingException {
        // use default key for signing
        PrivateKey privateKey = cryptographyService.getDefaultPrivateKey();
        String webId = cryptographyService.getDefaultPrivateKeyAlias();
        PublicKey publicKey = cryptographyService.getPublicKey(webId);
        try {
            return processWithKey(message, webId, privateKey, publicKey);
        } catch (Exception e) {
            logger.error("Failed to sign", e);
            throw new WonMessageProcessingException("Failed to sign message " + message.getMessageURI().toString());
        }
    }

    public WonMessage processOnBehalfOfNeed(final WonMessage message) throws WonMessageProcessingException {
        // use senderNeed key for signing
        String alias = keyPairAliasDerivationStrategy.getAliasForNeedUri(message.getSenderNeedURI().toString());
        PrivateKey privateKey = cryptographyService.getPrivateKey(alias);
        PublicKey publicKey = cryptographyService.getPublicKey(alias);
        try {
            return processWithKey(message, message.getSenderNeedURI().toString(), privateKey, publicKey);
        } catch (Exception e) {
            logger.error("Failed to sign", e);
            throw new WonMessageProcessingException("Failed to sign message " + message.getMessageURI().toString());
        }
    }

    private WonMessage processWithKey(final WonMessage wonMessage, final String privateKeyUri,
            final PrivateKey privateKey, final PublicKey publicKey) throws Exception {
        WonMessage signed = WonMessageSignerVerifier.sign(privateKey, publicKey, privateKeyUri, wonMessage);
        logger.debug("SIGNED with key " + privateKeyUri + ":\n" + WonMessageEncoder.encode(signed, Lang.TRIG));
        return signed;
    }

    public void setCryptographyService(final CryptographyService cryptoService) {
        this.cryptographyService = cryptoService;
    }

    public void setKeyPairAliasDerivationStrategy(KeyPairAliasDerivationStrategy keyPairAliasDerivationStrategy) {
        this.keyPairAliasDerivationStrategy = keyPairAliasDerivationStrategy;
    }
}
