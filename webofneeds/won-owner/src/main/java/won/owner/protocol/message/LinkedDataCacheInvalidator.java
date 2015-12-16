package won.owner.protocol.message;

import com.hp.hpl.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageType;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.message.processor.exception.WonMessageProcessingException;
import won.protocol.util.WonRdfUtils;
import won.protocol.util.linkeddata.CachingLinkedDataSource;

import java.net.URI;

/**
 * User: ypanchenko
 * Date: 27.10.2015
 */
public class LinkedDataCacheInvalidator implements WonMessageProcessor
{

  private final Logger logger = LoggerFactory.getLogger(getClass());


  public void setLinkedDataSource(final CachingLinkedDataSource linkedDataSource) {
    this.linkedDataSource = linkedDataSource;
  }

  private CachingLinkedDataSource linkedDataSource;


  @Override
  public WonMessage process(final WonMessage message) throws WonMessageProcessingException {

    if (message.getReceiverURI() != null) {
      // the cached list of events of the receiver need for the involved connection should be invalidated, since one more
      // message was created
      logger.debug("invalidating events list for need " + message.getReceiverNeedURI() + " for connection " + message
        .getReceiverURI());
      linkedDataSource.invalidate(message.getReceiverURI());
    }


    if (message.getMessageType().equals(WonMessageType.CONNECT) || message.getMessageType().equals(WonMessageType
                                                                                                    .HINT_MESSAGE)) {
      // the list of connections of the receiver need should be invalidated, since these type
      // of messages mean that the new connection has been created recently
      logger.debug("invalidating connections list for need " + message.getReceiverNeedURI());
      Dataset need = linkedDataSource.getDataForResource(message.getReceiverNeedURI());
      URI connectionsListUri = WonRdfUtils.NeedUtils.queryConnectionContainer(need, message.getReceiverNeedURI());
      linkedDataSource.invalidate(connectionsListUri);

    }

    return message;
  }
}
