package won.protocol.message.processor.impl;

import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageType;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.message.processor.exception.UriAlreadyInUseException;
import won.protocol.message.processor.exception.UriNodePathException;
import won.protocol.service.WonNodeInfo;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;

import java.net.URI;

/**
 * Checks if the event, graph or atom uri is well-formed according the node's
 * domain and its path conventions. Used on incoming messages. User: ypanchenko
 * Date: 23.04.2015
 */
public class UriConsistencyCheckingWonMessageProcessor implements WonMessageProcessor {
    @Autowired
    protected WonNodeInformationService wonNodeInformationService;

    @Override
    public WonMessage process(final WonMessage message) throws UriAlreadyInUseException {
        // extract info about local, remote and receiver nodes:
        URI senderNode = message.getSenderNodeURI();
        URI recipientNode = message.getRecipientNodeURI();
        WonNodeInfo senderNodeInfo = null;
        WonNodeInfo recipientNodeInfo = null;
        if (senderNode != null && !message.getMessageType().isHintMessage()) {
            // do not check the sender node for a hint
            // TODO: change this behaviour as soon as a matcher uses a WoN node
            senderNodeInfo = wonNodeInformationService.getWonNodeInformation(senderNode);
        }
        if (recipientNode != null) {
            recipientNodeInfo = wonNodeInformationService.getWonNodeInformation(recipientNode);
        }
        WonNodeInfo localNodeInfo = null;
        WonNodeInfo remoteNodeInfo = null;
        URI msgUri = message.getMessageURI();
        if (msgUri.getScheme().equals(senderNode.getScheme())
                        && msgUri.getAuthority().equals(senderNode.getAuthority())) {
            localNodeInfo = senderNodeInfo;
            remoteNodeInfo = recipientNodeInfo;
        } else if (msgUri.getScheme().equals(recipientNode.getScheme())
                        && msgUri.getAuthority().equals(recipientNode.getAuthority())) {
            localNodeInfo = recipientNodeInfo;
            remoteNodeInfo = senderNodeInfo;
        }
        URI localNode = URI.create(localNodeInfo.getWonNodeURI());
        // do checks for consistency between these nodes and message direction, as well
        // as atoms,
        // events and connection uris:
        // local node should be either receiver or sender node
        checkHasNode(message, localNode);
        // Any message URI used must conform to a URI pattern specified by the
        // respective publishing service:
        // Check that event URI corresponds to local pattern
        checkLocalEventURI(message, localNodeInfo);
        // Check that remote URI, if any, correspond to ?senderNode's event pattern
        checkRemoteEventURI(message, remoteNodeInfo);
        // Check that atom URI for create_atom message corresponds to local pattern
        checkCreateMsgAtomURI(message, localNodeInfo);
        // Specified sender-recipientAtom/Connection must conform to
        // sender-recipientNode
        // URI pattern
        checkSenders(senderNodeInfo, message);
        checkReceivers(recipientNodeInfo, message);
        // Check that local node is sender or receiver node URI, depending on the
        // message
        // direction
        checkDirection(message, localNode);
        return message;
    }

    private void checkHasNode(final WonMessage message, URI localNode) {
        if (!localNode.equals(message.getSenderNodeURI()) && !localNode.equals(message.getRecipientNodeURI())) {
            throw new UriNodePathException("neither sender nor receiver is " + localNode);
        }
    }

    private void checkReceivers(final WonNodeInfo recipientNodeInfo, final WonMessage message) {
        checkNodeConformance(recipientNodeInfo, message.getRecipientAtomURI(), message.getRecipientURI(), null);
    }

    private void checkSenders(final WonNodeInfo senderNodeInfo, final WonMessage message) {
        checkNodeConformance(senderNodeInfo, message.getSenderAtomURI(), message.getSenderURI(), null);
    }

    private void checkDirection(final WonMessage message, final URI localNode) {
        WonMessageDirection direction = message.getEnvelopeType();
        URI recipientNode = message.getRecipientNodeURI();
        URI senderNode = message.getSenderNodeURI();
        switch (direction) {
            case FROM_EXTERNAL:
                // local node should be a receiver node
                if (!localNode.equals(recipientNode)) {
                    throw new UriNodePathException(recipientNode + " is expected to be " + localNode);
                }
                break;
            case FROM_OWNER:
            case FROM_SYSTEM:
                // local node should be a sender node
                if (!localNode.equals(senderNode)) {
                    throw new UriNodePathException(senderNode + " is expected to be " + localNode);
                }
                break;
        }
    }

    private void checkLocalEventURI(final WonMessage message, WonNodeInfo localNodeInfo) {
        checkNodeConformance(localNodeInfo, null, null, message.getMessageURI());
    }

    private void checkRemoteEventURI(final WonMessage message, final WonNodeInfo remoteNodeInfo) {
        checkNodeConformance(remoteNodeInfo, null, null, message.getCorrespondingRemoteMessageURI());
    }

    private void checkCreateMsgAtomURI(final WonMessage message, final WonNodeInfo localNodeInfo) {
        // check only for create message
        if (message.getMessageType() == WonMessageType.CREATE_ATOM) {
            URI atomURI = WonRdfUtils.AtomUtils.getAtomURI(message.getCompleteDataset());
            checkNodeConformance(localNodeInfo, atomURI, null, null);
        }
    }

    private void checkNodeConformance(final WonNodeInfo info, final URI atomURI, final URI connURI,
                    final URI eventURI) {
        if (info == null) {
            return;
        }
        if (atomURI == null && connURI == null && eventURI == null) {
            return;
        }
        String atomPrefix = info.getAtomURIPrefix();
        String connPrefix = info.getConnectionURIPrefix();
        String eventPrefix = info.getEventURIPrefix();
        if (atomURI != null) {
            checkPrefix(atomURI, atomPrefix);
        }
        if (connURI != null) {
            checkPrefix(connURI, connPrefix);
        }
        if (eventURI != null) {
            checkPrefix(eventURI, eventPrefix);
        }
    }

    private String getPrefix(final URI atomURI) {
        return atomURI.toString().substring(0, atomURI.toString().lastIndexOf("/"));
    }

    private void checkPrefix(URI uri, String expectedPrefix) {
        String prefix = getPrefix(uri);
        if (!prefix.equals(expectedPrefix)) {
            throw new UriNodePathException(
                            "URI '" + uri + "' does not start with the expected prefix '" + expectedPrefix + "'");
        }
    }
}
