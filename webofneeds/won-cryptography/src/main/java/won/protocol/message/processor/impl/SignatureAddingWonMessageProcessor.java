package won.protocol.message.processor.impl;

import org.apache.jena.riot.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.cryptography.service.CryptographyService;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageEncoder;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.message.processor.exception.WonMessageProcessingException;

import java.security.PrivateKey;

/**
 * User: ypanchenko
 * Date: 03.04.2015
 */
public class SignatureAddingWonMessageProcessor implements WonMessageProcessor
{
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private String defaultKeyUri;

  @Autowired
  private CryptographyService cryptoService;

  public SignatureAddingWonMessageProcessor() {
  }

  public SignatureAddingWonMessageProcessor(String defaultKeyUri) {
    this.defaultKeyUri = defaultKeyUri;
  }

  @Override
  public WonMessage process(final WonMessage message) throws WonMessageProcessingException {
    // use default key for signing
    PrivateKey privateKey = cryptoService.getPrivateKey(defaultKeyUri);
    try {
      return processWithKey(message, defaultKeyUri, privateKey);
    } catch (Exception e) {
      //TODO proper exceptions
      e.printStackTrace();
      throw new WonMessageProcessingException("Failed to sign message " + message.getMessageURI().toString());
    }
  }

  public WonMessage processOnBehalfOfNeed(final WonMessage message) throws WonMessageProcessingException {
    // use senderNeed key for signing
    PrivateKey privateKey = cryptoService.getPrivateKey(message.getSenderNeedURI().toString());
    try {
      return processWithKey(message, message.getSenderNeedURI().toString(), privateKey);
    } catch (Exception e) {
      //TODO proper exceptions
      e.printStackTrace();
      throw new WonMessageProcessingException("Failed to sign message " + message.getMessageURI().toString());
    }
  }

  private WonMessage processWithKey(final WonMessage wonMessage, final String privateKeyUri,
                                    final PrivateKey privateKey) throws Exception {
    WonMessage signed = WonMessageSignerVerifier.sign(privateKey, privateKeyUri, wonMessage);
    logger.debug("SIGNED with key " + privateKeyUri + ":\n" + WonMessageEncoder.encode(signed, Lang.TRIG));
    return signed;
  }

  public void setCryptoService(final CryptographyService cryptoService) {
    this.cryptoService = cryptoService;
  }
}
