package won.node.camel.processor.fixed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageType;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.message.processor.exception.UriAlreadyInUseException;
import won.protocol.model.MessageEventPlaceholder;
import won.protocol.model.Need;
import won.protocol.repository.MessageEventRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.util.WonRdfUtils;

import java.net.URI;

/**
 * TODO actually, it is possible that while this check succeeds for the uri,
 * when the time comes to save this uri into the repository, this uri by that
 * time will be taken. Therefore the UriInUseException has to be thrown from
 * there and not from such a s separate checker.
 *
 * User: ypanchenko
 * Date: 23.04.2015
 */
public class UriAlreadyUsedCheckingWonMessageProcessor implements WonMessageProcessor
{

  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private MessageEventRepository messageEventRepository;
  @Autowired
  protected NeedRepository needRepository;

  @Override
  public WonMessage process(final WonMessage message) throws UriAlreadyInUseException {
    logger.warn("uri in use check not yet completely implemented!");

    checkEventURI(message);

    checkNeedURI(message);

    // TODO how and where the same massage received twice should be handled? we could check by the
    // message signature if it's the same message as we already received...

    return message;
  }

  private void checkNeedURI(final WonMessage message) {
    if (message.getMessageType() == WonMessageType.CREATE_NEED) {
      URI needURI = WonRdfUtils.NeedUtils.getNeedURI(message.getCompleteDataset());
      Need need = needRepository.findOneByNeedURI(needURI);
      if (need == null) {
        return;
      } else {
        throw new UriAlreadyInUseException(message.getSenderNeedURI().toString());
      }
    }
    return;
  }

  private void checkEventURI(final WonMessage message) {
    MessageEventPlaceholder event = messageEventRepository.findOneByMessageURI(message.getMessageURI());
    if (event == null) {
      return;
    } else {
      throw new UriAlreadyInUseException(message.getMessageURI().toString());
    }
  }
}
