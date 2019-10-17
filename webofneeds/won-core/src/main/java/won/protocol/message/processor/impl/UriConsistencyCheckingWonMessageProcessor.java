package won.protocol.message.processor.impl;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;

import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageType;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.message.processor.exception.UriAlreadyInUseException;
import won.protocol.message.processor.exception.UriNodePathException;
import won.protocol.service.WonNodeInfo;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.WonRdfUtils;

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
        // Check that atom URI for create_atom message corresponds to local pattern
        checkCreateMsgAtomURI(message, senderNodeInfo);
        // Specified sender-recipientAtom/Connection must conform to
        // sender-recipientNode
        // URI pattern
        checkSenders(senderNodeInfo, message);
        checkReceivers(recipientNodeInfo, message);
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

    private void checkLocalEventURI(final WonMessage message, WonNodeInfo localNodeInfo) {
        checkNodeConformance(localNodeInfo, null, null, message.getMessageURI());
    }

    private void checkRemoteEventURI(final WonMessage message, final WonNodeInfo remoteNodeInfo) {
        checkNodeConformance(remoteNodeInfo, null, null, message.getCorrespondingRemoteMessageURI());
    }

    private void checkCreateMsgAtomURI(final WonMessage message, final WonNodeInfo nodeInfo) {
        // check only for create message
        if (message.getMessageType() == WonMessageType.CREATE_ATOM) {
            URI atomURI = WonRdfUtils.AtomUtils.getAtomURI(message.getCompleteDataset());
            checkNodeConformance(nodeInfo, atomURI, null, null);
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
