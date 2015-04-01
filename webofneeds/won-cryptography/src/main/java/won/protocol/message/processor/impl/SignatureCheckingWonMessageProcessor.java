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
import won.protocol.service.LinkedDataService;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
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
    //TODO for the node's public key - probably here it is better to use keystore directly
    //to get node's cerificate and get public key from there
  LinkedDataService linkedDataService;

  @Override
  public WonMessage process(final WonMessage message) throws WonMessageProcessingException {

    Dataset dataset = WonMessageEncoder.encodeAsDataset(message);
    SignatureVerificationResult result = null;
    try {
      // extract public keys
      // TODO replace with proper implementation
      Map<String,PublicKey> keys = getRequiredPublicKeys(dataset);
      // verify with those public keys
      WonVerifier verifier = new WonVerifier(dataset);
      verifier.verify(keys);
      result = verifier.getVerificationResult();
    } catch (Exception e) {
      throw new WonMessageProcessingException(e);
    }
    if (!result.isVerificationPassed()) {
      throw new WonMessageProcessingException(result.getMessage());
    }
    // TODO use adapter (WonMessageProcessorCamelAdapter) to add information about verified
    // unreferenced signatures that should be referenced if this message is wrapped inside other
    // message
    return message;
  }

  // TODO what is the right approach here to store/get/verify public keys of
  // the signatures in message?
  private Map<String, PublicKey> getRequiredPublicKeys(final Dataset dataset)
    throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
    // extract public keys from content and from node data
    // TODO what about owner key?
    // TODO maybe already known public keys can be taken from own cache/store?
    // also, if public key extracted from the message itself, there is a security flaw -
    // no guarantee that that public key really belongs to original generator.
    WonKeysExtractor extractor = new WonKeysExtractor();
    Map<String,PublicKey> keys = extractor.fromDataset(dataset);
    Set<String> refKeys = extractor.getKeyRefs(dataset);
    for (String refKey : refKeys) {
      Dataset nodeDataset = linkedDataService.getNodeDataset();
      Map<String,PublicKey> nodeKeys = extractor.fromDataset(nodeDataset, refKey);
      keys.putAll(nodeKeys);
    }
    return keys;
  }
}
