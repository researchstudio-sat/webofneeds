package won.node.camel.processor.fixed;

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

import java.net.URI;

/**
 * Check if the event, graph or need uri is well-formed according the node's
 * domain and its path conventions
 *
 * User: ypanchenko
 * Date: 23.04.2015
 */
public class UriNodePathCheckingWonMessageProcessor implements WonMessageProcessor
{

  @Autowired
  protected WonNodeInformationService wonNodeInformationService;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private String needUriPrefix;
  private String eventUriPrefix;
  private String connUriPrefix;

  public UriNodePathCheckingWonMessageProcessor(String needUriPrefix, String eventUriPrefix, String connUriPrefix) {
    this.needUriPrefix = needUriPrefix;
    this.eventUriPrefix = eventUriPrefix;
    this.connUriPrefix = connUriPrefix;
  }

  @Override
  public WonMessage process(final WonMessage message) throws UriAlreadyInUseException {

    // Any message URI used must conform to a URI pattern specified by the respective publishing service:
    // Check that event URI corresponds to my pattern
    checkLocalEventURI(message);
    // Check that remote URI, if any, correspond to ?senderNode's event pattern
    checkRemoteEventURI(message);
    // Check that need URI for create_need message corresponds to my pattern
    checkNeedURI(message);

    // Specified sender-receiverNeed/Connection must conform to sender-receiverNode URI pattern
    checkSenders(message);
    checkReceivers(message);

    // Check that my node is sender or receiver node URI, depending on the message direction
    checkDirection(message);

    return message;
  }

  private void checkReceivers(final WonMessage message) {
    checkNodeConformance(message.getReceiverNodeURI(), message.getReceiverNeedURI(), message.getReceiverURI(), null);
  }

  private void checkSenders(final WonMessage message) {

    // special case for e.g. create_message that has only sender need and receiver node
    if (message.getSenderNodeURI() == null) {
      checkNodeConformance(message.getReceiverNodeURI(), message.getSenderNeedURI(), null, null);
    } else { // common case
      checkNodeConformance(message.getSenderNodeURI(), message.getSenderNeedURI(), message.getSenderURI(), null);
    }
  }

  private void checkNodeConformance(final URI nodeURI, final URI needURI, final URI connURI, final URI eventURI) {

    if (needURI == null && connURI == null && eventURI == null) {
      return;
    }

    String needPrefix = null;
    String connPrefix = null;
    String eventPrefix = null;
    if (nodeURI.equals(wonNodeInformationService.getDefaultWonNodeURI())) {
      needPrefix = needUriPrefix;
      connPrefix = connUriPrefix;
      eventPrefix = eventUriPrefix;
    } else {
      WonNodeInfo info = wonNodeInformationService.getWonNodeInformation(nodeURI);
      needPrefix = info.getNeedURIPrefix();
      connPrefix = info.getConnectionURIPrefix();
      eventPrefix = info.getEventURIPrefix();
    }

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

  private void checkDirection(final WonMessage message) {

    WonMessageDirection direction = message.getEnvelopeType();
    URI receiver = message.getReceiverNodeURI();
    URI sender = message.getSenderNodeURI();
    URI node;
    switch (direction) {
      case FROM_EXTERNAL:
        // my node should be a receiver node
        node = message.getReceiverNodeURI();
        if (!wonNodeInformationService.getDefaultWonNodeURI().equals(node)) {
          throw new UriNodePathException(node);
        }
        break;
      case FROM_OWNER:
        // my node should be a sender node; if sender node is not specified - then the receiver node
        node = message.getSenderNodeURI();
        if (node == null) {
          node = message.getReceiverNodeURI();
        }
        if (!wonNodeInformationService.getDefaultWonNodeURI().equals(node)) {
          throw new UriNodePathException(node);
        }
        break;
      case FROM_SYSTEM:
        // my node should be a sender node
        node = message.getSenderNodeURI();
        if (!wonNodeInformationService.getDefaultWonNodeURI().equals(node)) {
          throw new UriNodePathException(node);
        }
        break;
    }

  }

  private void checkRemoteEventURI(final WonMessage message) {
    URI remoteEventURI = message.getCorrespondingRemoteMessageURI();
    URI senderNodeURI = message.getSenderNodeURI();
    if (remoteEventURI != null) {
      checkNodeConformance(senderNodeURI, null, null, remoteEventURI);
    }
  }

  private String getPrefix(final URI needURI) {
    return needURI.toString().substring(0, needURI.toString().lastIndexOf("/"));
  }

  private void checkNeedURI(final WonMessage message) {
    // check only for create message
    if (message.getMessageType() == WonMessageType.CREATE_NEED) {
      URI needURI = WonRdfUtils.NeedUtils.getNeedURI(message.getCompleteDataset());
      String prefix = getPrefix(needURI);
      if (!prefix.equals(needUriPrefix)) {
        throw new UriNodePathException(needURI);
      }
    }
    return;
  }

  private void checkLocalEventURI(final WonMessage message) {
    URI eventURI = message.getMessageURI();
    String prefix = getPrefix(eventURI);
    if (!prefix.equals(eventUriPrefix)) {
      throw new UriNodePathException(eventURI);
    }
    return;
  }

  private void checkPrefix(URI uri, String expectedPrefix) {
    String prefix = getPrefix(uri);
    if (!prefix.equals(expectedPrefix)) {
      throw new UriNodePathException(uri);
    }
    return;
  }

}
