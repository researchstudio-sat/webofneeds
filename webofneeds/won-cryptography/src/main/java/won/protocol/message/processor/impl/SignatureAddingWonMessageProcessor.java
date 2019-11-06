package won.protocol.message.processor.impl;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import won.cryptography.keymanagement.KeyPairAliasDerivationStrategy;
import won.cryptography.service.CryptographyService;
import won.protocol.exception.WonMessageProcessingException;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.service.MessageRoutingInfoService;
import won.protocol.util.Prefixer;
import won.protocol.util.RdfUtils;

/**
 * User: ypanchenko Date: 03.04.2015
 */
public class SignatureAddingWonMessageProcessor implements WonMessageProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    private CryptographyService cryptographyService;
    @Autowired(required = false) // in production, there is a factory setting this value, for testing, we use
                                 // Autowired
    private KeyPairAliasDerivationStrategy keyPairAliasDerivationStrategy;
    @Autowired
    private MessageRoutingInfoService messageRoutingInfoService;

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

    public WonMessage processOnBehalfOfAtom(final WonMessage wonMessage) throws WonMessageProcessingException {
        List<WonMessage> ret = new ArrayList();
        for (WonMessage message : wonMessage.getAllMessages()) {
            // use senderAtom key for signing
            Optional<URI> senderAtomURI = Optional.of(message.getSenderAtomURI());
            // messageRoutingInfoService.senderAtom(message);
            if (!senderAtomURI.isPresent()) {
                throw new WonMessageProcessingException(
                                "Cannot determine sender atom of message " + message.getMessageURI());
            }
            String alias = keyPairAliasDerivationStrategy.getAliasForAtomUri(senderAtomURI.get().toString());
            PrivateKey privateKey = cryptographyService.getPrivateKey(alias);
            PublicKey publicKey = cryptographyService.getPublicKey(alias);
            try {
                ret.add(processWithKey(message, senderAtomURI.get().toString(), privateKey, publicKey));
            } catch (Exception e) {
                logger.error("Failed to sign", e);
                throw new WonMessageProcessingException("Failed to sign message "
                                + message.getMessageURI().toString(), e);
            }
        }
        return WonMessage.of(ret);
    }

    private WonMessage processWithKey(final WonMessage wonMessage, final String privateKeyUri,
                    final PrivateKey privateKey, final PublicKey publicKey) throws Exception {
        WonMessage signed = WonMessageSignerVerifier.sign(privateKey, publicKey, privateKeyUri, wonMessage);
        if (logger.isDebugEnabled()) {
            logger.debug("SIGNED with key " + privateKeyUri + ":\n"
                            + RdfUtils.toString(Prefixer.setPrefixes(signed.getCompleteDataset())));
        }
        return signed;
    }

    public void setCryptographyService(final CryptographyService cryptoService) {
        this.cryptographyService = cryptoService;
    }

    public void setKeyPairAliasDerivationStrategy(KeyPairAliasDerivationStrategy keyPairAliasDerivationStrategy) {
        this.keyPairAliasDerivationStrategy = keyPairAliasDerivationStrategy;
    }
}
