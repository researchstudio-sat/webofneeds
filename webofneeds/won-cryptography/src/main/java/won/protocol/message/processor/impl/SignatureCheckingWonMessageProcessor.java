/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.protocol.message.processor.impl;

import java.lang.invoke.MethodHandles;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import won.cryptography.rdfsign.SignatureVerificationState;
import won.cryptography.rdfsign.WonKeysReaderWriter;
import won.protocol.exception.WonMessageProcessingException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageType;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.rest.LinkedDataFetchingException;
import won.protocol.util.Prefixer;
import won.protocol.util.RdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;

/**
 * Checks all signatures found in a WonMessage. It is assumed that the message
 * is well-formed.
 */
public class SignatureCheckingWonMessageProcessor implements WonMessageProcessor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public SignatureCheckingWonMessageProcessor() {
    }

    public void setLinkedDataSource(final LinkedDataSource linkedDataSource) {
        this.linkedDataSource = linkedDataSource;
    }

    @Autowired
    private LinkedDataSource linkedDataSource;

    @Override
    public WonMessage process(final WonMessage message) throws WonMessageProcessingException {
        SignatureVerificationState result;
        /*
         * If the message is a successResponse to a delete Message then we can't check
         * the signature as it is stored in the deleted Atom, so we just accept the
         * message as valid and return it.
         */
        if (message.getRespondingToMessageType() == WonMessageType.DELETE
                        && message.getMessageType() == WonMessageType.SUCCESS_RESPONSE) {
            return message;
        }
        for (WonMessage toCheck : message.getAllMessages()) {
            try {
                // obtain public keys
                Map<String, PublicKey> keys = getRequiredPublicKeys(toCheck.getCompleteDataset());
                // verify with those public keys
                result = WonMessageSignerVerifier.verify(keys, toCheck);
                if (logger.isDebugEnabled()) {
                    logger.debug("VERIFIED=" + result.isVerificationPassed()
                                    + " with keys: " + keys.values()
                                    + " for\n"
                                    + RdfUtils.writeDatasetToString(
                                                    Prefixer.setPrefixes(toCheck.getCompleteDataset()),
                                                    Lang.TRIG));
                }
            } catch (LinkedDataFetchingException e) {
                /*
                 * If a delete message could not be validated because the atom was already
                 * deleted, we assume that this message is just mirrored back to the owner and
                 * is to be accepteed
                 */
                if (WonMessageType.DELETE.equals(toCheck.getMessageType())) {
                    if (e.getCause() instanceof HttpClientErrorException
                                    && HttpStatus.GONE.equals(
                                                    ((HttpClientErrorException) e.getCause()).getStatusCode())) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Failure during processing signature check of message"
                                            + toCheck.getMessageURI()
                                            + " (messageType was DELETE, but atom is already deleted, accept message anyway)");
                        }
                        return toCheck;
                    }
                }
                // TODO SignatureProcessingException?
                throw new WonMessageProcessingException("Could not verify message " + toCheck.getMessageURI(), e);
            } catch (Exception e) {
                // TODO SignatureProcessingException?
                throw new WonMessageProcessingException("Could not verify message " + toCheck.getMessageURI(), e);
            }
            // throw exception if the verification fails:
            if (!result.isVerificationPassed()) {
                String errormessage = "Message verification failed. Message:"
                                + toCheck.toStringForDebug(false)
                                + ", Problem:"
                                + result.getMessage();
                if (logger.isDebugEnabled()) {
                    logger.debug(errormessage + ". Offending message:\n"
                                    + RdfUtils.toString(Prefixer.setPrefixes(toCheck.getCompleteDataset())));
                }
                // TODO SignatureProcessingException?
                throw new WonMessageProcessingException(new SignatureException(
                                errormessage + ". To log the offending message, set Loglevel to DEBUG for logger '"
                                                + this.getClass().getName() + "'"));
            }
        }
        return message;
    }

    private boolean appendIfPresent(URI uri, String label, StringBuilder sb) {
        if (uri != null) {
            sb.append(label).append(": ").append(uri);
            return true;
        }
        return false;
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
