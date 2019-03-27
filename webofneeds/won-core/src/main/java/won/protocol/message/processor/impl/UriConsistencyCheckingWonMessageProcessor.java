package won.protocol.message.processor.impl;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * Check if the event, graph or need uri is well-formed according the node's
 * domain and its path conventions User: ypanchenko Date: 23.04.2015
 */
public class UriConsistencyCheckingWonMessageProcessor implements WonMessageProcessor {
    @Autowired
    protected WonNodeInformationService wonNodeInformationService;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public WonMessage process(final WonMessage message) throws UriAlreadyInUseException {
        // extract info about own, sender and receiver nodes:
        URI senderNode = message.getSenderNodeURI();
        URI receiverNode = message.getReceiverNodeURI();
        WonNodeInfo senderNodeInfo = null;
        WonNodeInfo receiverNodeInfo = null;
        if (senderNode != null && !WonMessageType.HINT_MESSAGE.equals(message.getMessageType())) {
            // do not check the sender node for a hint
            // TODO: change this behaviour as soon as a matcher uses a WoN node
            senderNodeInfo = wonNodeInformationService.getWonNodeInformation(senderNode);
        }
        if (receiverNode != null) {
            receiverNodeInfo = wonNodeInformationService.getWonNodeInformation(receiverNode);
        }
        WonNodeInfo ownNodeInfo = null;
        URI msgUri = message.getMessageURI();
        if (msgUri.getScheme().equals(senderNode.getScheme())
                        && msgUri.getAuthority().equals(senderNode.getAuthority())) {
            ownNodeInfo = senderNodeInfo;
        } else if (msgUri.getScheme().equals(receiverNode.getScheme())
                        && msgUri.getAuthority().equals(receiverNode.getAuthority())) {
            ownNodeInfo = receiverNodeInfo;
        }
        URI ownNode = URI.create(ownNodeInfo.getWonNodeURI());
        // do checks for consistency between these nodes and message direction, as well
        // as needs,
        // events and connection uris:
        // my node should be either receiver or sender node
        checkHasMyNode(message, ownNode);
        // Any message URI used must conform to a URI pattern specified by the
        // respective publishing service:
        // Check that event URI corresponds to my pattern
        checkLocalEventURI(message, ownNodeInfo);
        // Check that remote URI, if any, correspond to ?senderNode's event pattern
        checkRemoteEventURI(message, senderNodeInfo);
        // Check that need URI for create_need message corresponds to my pattern
        checkCreateMsgNeedURI(message, ownNodeInfo);
        // Specified sender-receiverNeed/Connection must conform to sender-receiverNode
        // URI pattern
        checkSenders(senderNodeInfo, message);
        checkReceivers(receiverNodeInfo, message);
        // Check that my node is sender or receiver node URI, depending on the message
        // direction
        checkDirection(message, ownNode);
        return message;
    }

    private void checkHasMyNode(final WonMessage message, URI ownNode) {
        if (!ownNode.equals(message.getSenderNodeURI()) && !ownNode.equals(message.getReceiverNodeURI())) {
            throw new UriNodePathException("neither sender nor receiver is " + ownNode);
        }
    }

    private void checkReceivers(final WonNodeInfo receiverNodeInfo, final WonMessage message) {
        checkNodeConformance(receiverNodeInfo, message.getReceiverNeedURI(), message.getReceiverURI(), null);
    }

    private void checkSenders(final WonNodeInfo senderNodeInfo, final WonMessage message) {
        checkNodeConformance(senderNodeInfo, message.getSenderNeedURI(), message.getSenderURI(), null);
    }

    private void checkDirection(final WonMessage message, final URI ownNode) {
        WonMessageDirection direction = message.getEnvelopeType();
        URI receiverNode = message.getReceiverNodeURI();
        URI senderNode = message.getSenderNodeURI();
        URI node;
        switch (direction) {
            case FROM_EXTERNAL:
                // my node should be a receiver node
                if (!ownNode.equals(receiverNode)) {
                    throw new UriNodePathException(receiverNode + " is expected to be " + ownNode);
                }
                break;
            case FROM_OWNER:
                // my node should be a sender node
                if (!ownNode.equals(senderNode)) {
                    throw new UriNodePathException(senderNode + " is expected to be " + ownNode);
                }
                break;
            case FROM_SYSTEM:
                // my node should be a sender node
                if (!ownNode.equals(senderNode)) {
                    throw new UriNodePathException(senderNode + " is expected to be " + ownNode);
                }
                break;
        }
    }

    private void checkLocalEventURI(final WonMessage message, WonNodeInfo ownNodeInfo) {
        checkNodeConformance(ownNodeInfo, null, null, message.getMessageURI());
    }

    private void checkRemoteEventURI(final WonMessage message, final WonNodeInfo senderNodeInfo) {
        checkNodeConformance(senderNodeInfo, null, null, message.getCorrespondingRemoteMessageURI());
    }

    private void checkCreateMsgNeedURI(final WonMessage message, final WonNodeInfo ownNodeInfo) {
        // check only for create message
        if (message.getMessageType() == WonMessageType.CREATE_NEED) {
            URI needURI = WonRdfUtils.NeedUtils.getNeedURI(message.getCompleteDataset());
            checkNodeConformance(ownNodeInfo, needURI, null, null);
        }
        return;
    }

    private void checkNodeConformance(final WonNodeInfo info, final URI needURI, final URI connURI,
                    final URI eventURI) {
        if (info == null) {
            return;
        }
        if (needURI == null && connURI == null && eventURI == null) {
            return;
        }
        String needPrefix = info.getNeedURIPrefix();
        String connPrefix = info.getConnectionURIPrefix();
        String eventPrefix = info.getEventURIPrefix();
        if (needURI != null) {
            checkPrefix(needURI, needPrefix);
        }
        if (connURI != null) {
            checkPrefix(connURI, connPrefix);
        }
        if (eventURI != null) {
            checkPrefix(eventURI, eventPrefix);
        }
    }

    private String getPrefix(final URI needURI) {
        return needURI.toString().substring(0, needURI.toString().lastIndexOf("/"));
    }

    private void checkPrefix(URI uri, String expectedPrefix) {
        String prefix = getPrefix(uri);
        if (!prefix.equals(expectedPrefix)) {
            throw new UriNodePathException(
                            "URI '" + uri + "' does not start with the expected prefix '" + expectedPrefix + "'");
        }
        return;
    }
}
