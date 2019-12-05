package won.protocol.message.processor.impl;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StopWatch;

import won.protocol.exception.UriNodePathException;
import won.protocol.exception.WonMessageNotWellFormedException;
import won.protocol.exception.WonMessageProcessingException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessage.EnvelopePropertyCheckResult;
import won.protocol.message.WonMessageDirection;
import won.protocol.message.WonMessageType;
import won.protocol.message.WonMessageUtils;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.service.MessageRoutingInfoService;
import won.protocol.service.WonNodeInfo;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.LogMarkers;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.WonUriCheckHelper;

/**
 * Checks if the event, graph or atom uri is well-formed according the node's
 * domain and its path conventions. Used on incoming messages. User: ypanchenko
 * Date: 23.04.2015
 */
public class UriConsistencyCheckingWonMessageProcessor implements WonMessageProcessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    protected WonNodeInformationService wonNodeInformationService;
    @Autowired
    MessageRoutingInfoService messageRoutingInfoService;

    @Override
    public WonMessage process(WonMessage message) {
        StopWatch sw = new StopWatch();
        if (message == null) {
            throw new WonMessageProcessingException("No WonMessage object found in exchange");
        }
        sw.start("validMessageURI");
        if (!WonMessageUtils.isValidMessageUri(message.getMessageURIRequired())) {
            throw new WonMessageNotWellFormedException("Not a valid message URI: " + message.getMessageURI());
        }
        sw.stop();
        sw.start("envelopePropertyCheck");
        EnvelopePropertyCheckResult result = message.checkEnvelopeProperties();
        if (!result.isValid()) {
            throw new WonMessageNotWellFormedException(result.getMessage());
        }
        sw.stop();
        sw.start("getNodeInfo");
        Optional<URI> senderNode = messageRoutingInfoService.senderNode(message);
        Optional<URI> recipientNode = messageRoutingInfoService.recipientNode(message);
        if (!senderNode.isPresent() && !message.getMessageTypeRequired().isHintMessage()) {
            throw new WonMessageProcessingException(
                            "Cannot determine sender node for " + message.toShortStringForDebug());
        }
        if (!recipientNode.isPresent()) {
            throw new WonMessageProcessingException(
                            "Cannot determine recipient node for " + message.toShortStringForDebug());
        }
        WonNodeInfo senderNodeInfo = null;
        WonNodeInfo recipientNodeInfo = null;
        if (senderNode.isPresent() && !message.getMessageType().isHintMessage()) {
            // do not check the sender node for a hint
            // TODO: change this behaviour as soon as a matcher uses a WoN node
            senderNodeInfo = wonNodeInformationService.getWonNodeInformation(senderNode.get());
        }
        if (recipientNode != null) {
            recipientNodeInfo = wonNodeInformationService.getWonNodeInformation(recipientNode.get());
        }
        if (senderNodeInfo == null && !message.getMessageType().isHintMessage()) {
            throw new WonMessageProcessingException(
                            "Could not load sender WonNodeInfo (won node " + senderNode.get() + ")");
        }
        if (recipientNodeInfo == null) {
            throw new WonMessageProcessingException(
                            "Could not load recipient WonNodeInfo (won node " + recipientNode.get() + ")");
        }
        sw.stop();
        sw.start("senderAtomUriCheck");
        checkAtomUri(message.getSenderAtomURI(), senderNodeInfo);
        sw.stop();
        sw.start("senderSocketUriCheck");
        checkSocketUri(message.getSenderSocketURI(), senderNodeInfo);
        sw.stop();
        sw.start("recipientAtomUriCheck");
        checkAtomUri(message.getRecipientAtomURI(), recipientNodeInfo);
        sw.stop();
        sw.start("recipientSocketUriCheck");
        checkSocketUri(message.getRecipientSocketURI(), recipientNodeInfo);
        // there is no way atom or connection uri can be on the recipient node and the
        // recipient node is different from the sender node
        sw.stop();
        sw.start("atomUriCheck");
        checkAtomUri(message.getAtomURI(), senderNodeInfo);
        sw.stop();
        sw.start("connectionUriCheck");
        checkConnectionUri(message.getConnectionURI(), senderNodeInfo);
        // Check that atom URI for create_atom message corresponds to local pattern
        sw.stop();
        sw.start("createMsgAtomUriCheck");
        checkCreateMsgAtomURI(message, senderNodeInfo);
        sw.stop();
        sw.start("signerCheck");
        WonMessageDirection statedDirection = message.getEnvelopeType();
        if (statedDirection.isFromOwner()) {
            if (!Objects.equals(message.getSenderAtomURIRequired(), message.getSignerURIRequired())) {
                RDFDataMgr.write(System.out, message.getCompleteDataset(), Lang.TRIG);
                throw new WonMessageNotWellFormedException("WonMessage " + message.toShortStringForDebug()
                                + " is FROM_OWNER but not signed by its atom");
            }
        }
        if (statedDirection.isFromSystem()) {
            if (!Objects.equals(message.getSenderNodeURIRequired(), message.getSignerURIRequired())) {
                RDFDataMgr.write(System.out, message.getCompleteDataset(), Lang.TRIG);
                throw new WonMessageNotWellFormedException("WonMessage " + message.toShortStringForDebug()
                                + " is FROM_SYSTEM but not signed by its node");
            }
        }
        sw.stop();
        if (logger.isDebugEnabled()) {
            logger.debug(LogMarkers.TIMING, "URI Consistency check timing for message {}:\n{}",
                            message.getMessageURIRequired(), sw.prettyPrint());
        }
        return message;
    }

    private void checkHasNode(final WonMessage message, URI localNode) {
        if (!localNode.equals(message.getSenderNodeURI()) && !localNode.equals(message.getRecipientNodeURI())) {
            throw new UriNodePathException("neither sender nor receiver is " + localNode);
        }
    }

    private void checkCreateMsgAtomURI(final WonMessage message, final WonNodeInfo nodeInfo) {
        // check only for create message
        if (message.getMessageType() == WonMessageType.CREATE_ATOM) {
            URI atomURI = WonRdfUtils.AtomUtils.getAtomURI(message.getCompleteDataset());
            checkAtomUri(atomURI, nodeInfo);
        }
    }

    private void checkAtomUri(URI atom, WonNodeInfo info) {
        if (atom == null) {
            return;
        }
        if (!atom.toString().startsWith(info.getAtomURIPrefix())) {
            throw new WonMessageNotWellFormedException(
                            atom + " is not a valid atom URI on node " + info.getWonNodeURI());
        }
    }

    private void checkSocketUri(URI socket, WonNodeInfo info) {
        if (socket == null) {
            return;
        }
        if (!WonUriCheckHelper.isValidSocketURI(info.getAtomURIPrefix(), socket.toString())) {
            throw new WonMessageNotWellFormedException(
                            socket + " is not a valid socket URI on node " + info.getWonNodeURI());
        }
    }

    private void checkConnectionUri(URI connection, WonNodeInfo info) {
        if (connection == null) {
            return;
        }
        if (!WonUriCheckHelper.isValidConnectionURI(info.getAtomURIPrefix(), connection.toString())) {
            throw new WonMessageNotWellFormedException(
                            connection + " is not a valid connection URI on node " + info.getWonNodeURI());
        }
    }
}
