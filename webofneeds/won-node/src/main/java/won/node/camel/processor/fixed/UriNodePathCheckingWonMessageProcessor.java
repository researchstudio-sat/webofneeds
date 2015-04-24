package won.node.camel.processor.fixed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageType;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.message.processor.exception.UriAlreadyInUseException;
import won.protocol.message.processor.exception.UriNodePathException;
import won.protocol.util.WonRdfUtils;

import java.net.URI;

/**
 * Check if the event or need uri is well-formed according the node's
 * domain and its path conventions
 *
 * User: ypanchenko
 * Date: 23.04.2015
 */
public class UriNodePathCheckingWonMessageProcessor implements WonMessageProcessor
{

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private String needUriPrefix;
  private String eventUriPrefix;

  public UriNodePathCheckingWonMessageProcessor(String needUriPrefix, String eventUriPrefix) {
    this.needUriPrefix = needUriPrefix;
    this.eventUriPrefix = eventUriPrefix;
  }

  @Override
  public WonMessage process(final WonMessage message) throws UriAlreadyInUseException {

    checkEventURI(message);
    checkNeedURI(message);

    //TODO check graph uris? for those graphs that start with event uri?

    return message;
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

  private void checkEventURI(final WonMessage message) {
    URI eventURI = message.getMessageURI();
    String prefix = getPrefix(eventURI);
    if (!prefix.equals(eventUriPrefix)) {
      throw new UriNodePathException(eventURI);
    }
    return;
  }
}
