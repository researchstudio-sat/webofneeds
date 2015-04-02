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

import com.hp.hpl.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.cryptography.rdfsign.SignatureVerificationResult;
import won.cryptography.rdfsign.WonKeysExtractor;
import won.cryptography.rdfsign.WonVerifier;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageEncoder;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.message.processor.exception.WonMessageProcessingException;
import won.protocol.util.linkeddata.LinkedDataSource;

import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;
import java.util.Set;

/**
 * Checks all signatures found in a WonMessage. It is assumed that the message is well-formed.
 */
public class SignatureCheckingWonMessageProcessor implements WonMessageProcessor
{
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private LinkedDataSource linkedDataSource;

  @Override
  public WonMessage process(final WonMessage message) throws WonMessageProcessingException {

    Dataset dataset = WonMessageEncoder.encodeAsDataset(message);
    SignatureVerificationResult result = null;
    try {
      // obtain public keys
      Map<String,PublicKey> keys = getRequiredPublicKeys(dataset);
      // verify with those public keys
      WonVerifier verifier = new WonVerifier(dataset);
      verifier.verify(keys);
      result = verifier.getVerificationResult();
    } catch (Exception e) {
      throw new WonMessageProcessingException(e);
    }
    // throw exception if the verification fails:
    if (!result.isVerificationPassed()) {
      throw new WonMessageProcessingException(new SignatureException(result.getMessage()));
    }
    // TODO find a way to pass information about verified unreferenced signatures to the
    // following processors. This is useful because the values of such signatures should
    // be referenced if this message is wrapped by another envelope.
    return message;
  }


  // TODO If public key extracted from the message itself, there is a security flaw -
  // no guarantee that that public key really belongs to original generator.
  // in this case integration with webid-tls when the certificate has to be provided via tls might solve the problem
  // TODO what about owner key?
  // TODO maybe already known public keys can be taken from own cache/store?
  // TODO proper exceptions
  /**
   * Extract public keys from content and from referenced in signature key uris data
   * @param msgDataset
   * @return
   * @throws NoSuchAlgorithmException
   * @throws NoSuchProviderException
   * @throws InvalidKeySpecException
   */
  private Map<String, PublicKey> getRequiredPublicKeys(final Dataset msgDataset)
    throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {

    WonKeysExtractor extractor = new WonKeysExtractor();
    // extract keys if directly provided in the message content:
    Map<String,PublicKey> keys = extractor.fromDataset(msgDataset);
    // extract referenced key by dereferencing a (kind of) webid of a signer
    Set<String> refKeys = extractor.getKeyRefs(msgDataset);
    for (String refKey : refKeys) {
      Dataset keyDataset = linkedDataSource.getDataForResource(URI.create(refKey));
      // TODO replace the extractor with WonRDFUtils method and use the extractor itself internally
      // in that method
      Set<PublicKey> resolvedKeys = extractor.fromDataset(keyDataset, refKey);
      for (PublicKey resolvedKey : resolvedKeys) {
        keys.put(refKey, resolvedKey);
        // now we only expect one key but in future there could be several keys for one WebID
        break;
      }
    }
    return keys;
  }
}
