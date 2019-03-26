/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.protocol.message.processor.impl;

import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.apache.jena.query.Dataset;
import org.apache.jena.riot.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import won.cryptography.rdfsign.SignatureVerificationState;
import won.cryptography.rdfsign.WonKeysReaderWriter;
import won.protocol.message.WonMessage;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.message.processor.exception.WonMessageProcessingException;
import won.protocol.util.RdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;

/**
 * Checks all signatures found in a WonMessage. It is assumed that the message
 * is well-formed.
 */
public class SignatureCheckingWonMessageProcessor implements WonMessageProcessor {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  public SignatureCheckingWonMessageProcessor() {
  }

  public void setLinkedDataSource(final LinkedDataSource linkedDataSource) {
    this.linkedDataSource = linkedDataSource;
  }

  @Autowired
  private LinkedDataSource linkedDataSource;

  @Override
  public WonMessage process(final WonMessage message) throws WonMessageProcessingException {

    SignatureVerificationState result = null;
    try {
      // obtain public keys
      Map<String, PublicKey> keys = getRequiredPublicKeys(message.getCompleteDataset());
      // verify with those public keys
      result = WonMessageSignerVerifier.verify(keys, message);

      logger.debug("VERIFIED=" + result.isVerificationPassed() + " with keys: " + keys.values() + " for\n"
          + RdfUtils.writeDatasetToString(message.getCompleteDataset(), Lang.TRIG));

    } catch (Exception e) {
      // TODO SignatureProcessingException?
      throw new WonMessageProcessingException("Could not verify message " + message.getMessageURI(), e);
    }
    // throw exception if the verification fails:
    if (!result.isVerificationPassed()) {
      // TODO SignatureProcessingException?
      throw new WonMessageProcessingException(
          new SignatureException("Could not verify message " + message.getMessageURI() + ": " + result.getMessage()));
    }
    return message;
  }

  // TODO If public key extracted from the message itself, there is a security
  // flaw -
  // no guarantee that that public key really belongs to original generator.
  // in this case integration with webid-tls when the certificate has to be
  // provided via tls might solve the problem
  // TODO what about owner key?
  // TODO maybe already known public keys can be taken from own cache/store?
  // TODO proper exceptions
  /**
   * Extract public keys from content and from referenced in signature key uris
   * data
   * 
   * @param msgDataset
   * @return
   * @throws NoSuchAlgorithmException
   * @throws NoSuchProviderException
   * @throws InvalidKeySpecException
   */
  private Map<String, PublicKey> getRequiredPublicKeys(final Dataset msgDataset)
      throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {

    // extracted and then
    WonKeysReaderWriter keyReader = new WonKeysReaderWriter();
    // extract keys if directly provided in the message content:
    Map<String, PublicKey> keys = keyReader.readFromDataset(msgDataset);
    // extract referenced key by dereferencing a (kind of) webid of a signer
    Set<String> refKeys = keyReader.readKeyReferences(msgDataset);
    if (logger.isDebugEnabled()) {
      logger.debug("referenced keys: " + Arrays.toString(refKeys.toArray()));
    }
    for (String refKey : refKeys) {
      if (!keys.containsKey(refKey)) {
        Dataset keyDataset = linkedDataSource.getDataForResource(URI.create(refKey));
        // TODO replace the WonKeysReaderWriter methods with WonRDFUtils methods and use
        // the WonKeysReaderWriter
        // itself internally there in those methods
        Set<PublicKey> resolvedKeys = keyReader.readFromDataset(keyDataset, refKey);
        for (PublicKey resolvedKey : resolvedKeys) {
          keys.put(refKey, resolvedKey);
          // TODO now we only expect one key but in future there could be several keys for
          // one WebID
          break;
        }
      }
    }
    if (logger.isDebugEnabled()) {
      logger.debug("retrieved public keys of these webids: " + Arrays.toString(keys.keySet().toArray()));
    }
    return keys;
  }

}
