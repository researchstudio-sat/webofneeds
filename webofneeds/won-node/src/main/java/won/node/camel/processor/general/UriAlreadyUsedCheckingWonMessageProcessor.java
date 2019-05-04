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
package won.node.camel.processor.general;

import java.net.URI;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.util.IsoMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageType;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.message.processor.exception.EventAlreadyProcessedException;
import won.protocol.message.processor.exception.UriAlreadyInUseException;
import won.protocol.model.MessageEventPlaceholder;
import won.protocol.model.Atom;
import won.protocol.repository.MessageEventRepository;
import won.protocol.repository.AtomRepository;
import won.protocol.util.RdfUtils;
import won.protocol.util.WonRdfUtils;

/**
 * Checks whether the event or atom URI is already used. It is possible that
 * while this check succeeds for the uri, when the time comes to save this uri
 * into the repository, this uri by that time will be used. TODO Therefore, the
 * UriAlreadyInUseException or EventAlreadyProcessedException has to also be
 * thrown from there. Nevertheless, to have such a separate check for the
 * uri-is-use problems is useful, because it can detect and react to the problem
 * on the early stage, before the whole message processing logic is at work.
 * User: ypanchenko Date: 23.04.2015
 */
public class UriAlreadyUsedCheckingWonMessageProcessor implements WonMessageProcessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private MessageEventRepository messageEventRepository;
    @Autowired
    protected AtomRepository atomRepository;

    @Override
    public WonMessage process(final WonMessage message) throws UriAlreadyInUseException {
        checkEventURI(message);
        checkAtomURI(message);
        return message;
    }

    private void checkAtomURI(final WonMessage message) {
        if (message.getMessageType() == WonMessageType.CREATE_ATOM) {
            URI atomURI = WonRdfUtils.AtomUtils.getAtomURI(message.getCompleteDataset());
            Atom atom = atomRepository.findOneByAtomURI(atomURI);
            if (atom == null) {
                return;
            } else {
                throw new UriAlreadyInUseException(message.getSenderAtomURI().toString());
            }
        }
        return;
    }

    private void checkEventURI(final WonMessage message) {
        MessageEventPlaceholder event = messageEventRepository.findOneByMessageURI(message.getMessageURI());
        if (event == null) {
            return;
        } else {
            if (hasResponse(event) && isDuplicateMessage(message, event)) {
                // the same massage as the one already processed is received
                throw new EventAlreadyProcessedException(message.getMessageURI().toString());
            } else {
                throw new UriAlreadyInUseException(message.getMessageURI().toString());
            }
        }
    }

    private boolean hasResponse(final MessageEventPlaceholder event) {
        return event.getResponseMessageURI() != null;
    }

    private boolean isDuplicateMessage(final WonMessage message, MessageEventPlaceholder event) {
        // retrieve already processed message
        Dataset processedDataset = event.getDatasetHolder().getDataset();
        // compare with received message
        // TODO ideally, here, only signatures of the corresponding envelopes have to be
        // compared.
        // But as of now, before saving, the receiving side can add any number of
        // envelopes.
        // Therefore, temporarily, before we know better how to retrieve the right
        // envelope,
        // we compare here the main envelope data and the contents, without envelope
        // signatures.
        WonMessage processedMessage = new WonMessage(processedDataset);
        boolean sameEnvelope = hasSameEnvelopeData(processedMessage, message);
        boolean sameContent = hasSameContent(processedMessage, message);
        if (sameEnvelope && sameContent) {
            return true;
        }
        return false;
    }

    private boolean hasSameContent(final WonMessage processedMessage, final WonMessage message) {
        Dataset messageContent = message.getMessageContent();
        Dataset processedContent = processedMessage.getMessageContent();
        for (String name : RdfUtils.getModelNames(processedContent)) {
            Model model = processedContent.getNamedModel(name);
            if (WonRdfUtils.SignatureUtils.isSignatureGraph(name, model)
                            && !RdfUtils.getModelNames(messageContent).contains(name)) {
                processedContent.removeNamedModel(name);
            }
        }
        if (IsoMatcher.isomorphic(processedContent.asDatasetGraph(), messageContent.asDatasetGraph())) {
            return true;
        }
        return false;
    }

    private boolean hasSameEnvelopeData(final WonMessage processedMessage, final WonMessage message) {
        if (equalsOrBothNull(processedMessage.getSenderAtomURI(), message.getSenderAtomURI())
                        && equalsOrBothNull(processedMessage.getRecipientAtomURI(), message.getRecipientAtomURI())
                        // the receiving side can add this info
                        // &&
                        // equalsOrBothNull(processedMessage.getSenderURI(), message.getSenderURI())
                        && equalsOrBothNull(processedMessage.getRecipientURI(), message.getRecipientURI())
                        && equalsOrBothNull(processedMessage.getSenderNodeURI(), message.getSenderNodeURI())
                        && equalsOrBothNull(processedMessage.getRecipientNodeURI(), message.getRecipientNodeURI())
                        // the receiving side can add this info
                        // &&
                        // equalsOrBothNull(processedMessage.getCorrespondingRemoteMessageURI(),
                        // message.getCorrespondingRemoteMessageURI())
                        && equalsOrBothNull(processedMessage.getIsResponseToMessageURI(),
                                        message.getIsResponseToMessageURI())
                        && processedMessage.getContentGraphURIs().containsAll(message.getContentGraphURIs())
                        && equalsOrBothNull(processedMessage.getMessageType(), message.getMessageType())
                        && processedMessage.getRefersTo().containsAll(message.getRefersTo())
                        && equalsOrBothNull(processedMessage.getEnvelopeType(), message.getEnvelopeType())) {
            return true;
        }
        return false;
    }

    private boolean equalsOrBothNull(final Object uri1, final Object uri2) {
        if ((uri1 == null && uri2 == null) || (uri1.equals(uri2))) {
            return true;
        }
        return false;
    }
}
