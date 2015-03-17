package won.owner.messaging.processor;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.owner.service.OwnerProtocolOwnerServiceCallback;
import won.owner.service.impl.NopOwnerProtocolOwnerServiceCallback;
import won.protocol.message.WonMessageProcessor;
import won.protocol.repository.ChatMessageRepository;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.MatchRepository;
import won.protocol.repository.NeedRepository;

/**
 * User: syim
 * Date: 17.03.2015
 */
public abstract class AbstractMessageProcessor implements WonMessageProcessor
{
  final Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private NeedRepository needRepository;

  @Autowired
  private ConnectionRepository connectionRepository;

  @Autowired
  private MatchRepository matchRepository;

  @Autowired
  private ChatMessageRepository chatMessageRepository;

  //handler for incoming won protocol messages. The default handler does nothing.
  @Autowired(required = false)
  private OwnerProtocolOwnerServiceCallback ownerServiceCallback = new NopOwnerProtocolOwnerServiceCallback();

  @Override
  public abstract void process(final Exchange exchange) throws Exception;
}
