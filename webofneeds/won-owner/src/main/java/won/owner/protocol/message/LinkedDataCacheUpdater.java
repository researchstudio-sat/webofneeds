package won.owner.protocol.message;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import won.protocol.message.WonMessage;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.message.processor.exception.WonMessageProcessingException;
import won.protocol.util.linkeddata.CachingLinkedDataSource;
import won.protocol.util.linkeddata.LinkedDataSource;

/**
 * Processor for incoming messages on the owner side. Will put incoming (hence complete) messages into the linked data
 * cache.
 * 
 * @author fkleedorfer
 *
 */
public class LinkedDataCacheUpdater implements WonMessageProcessor {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private LinkedDataSource linkedDataSourceOnBehalfOfNeed;

    @Override
    public WonMessage process(WonMessage message) throws WonMessageProcessingException {
        if (this.linkedDataSourceOnBehalfOfNeed != null
                && this.linkedDataSourceOnBehalfOfNeed instanceof CachingLinkedDataSource) {
            logger.debug("putting message {} into cache", message.getMessageURI());
            URI requester = message.getReceiverNeedURI();
            ((CachingLinkedDataSource) linkedDataSourceOnBehalfOfNeed).addToCache(message.getCompleteDataset(),
                    message.getMessageURI(), requester);
        }
        return message;
    }

    public void setLinkedDataSourceOnBehalfOfNeed(LinkedDataSource linkedDataSourceOnBehalfOfNeed) {
        this.linkedDataSourceOnBehalfOfNeed = linkedDataSourceOnBehalfOfNeed;
    }

}
