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
 * Processor for incoming messages on the owner side. Will put incoming (hence
 * complete) messages into the linked data cache.
 * 
 * @author fkleedorfer
 */
public class LinkedDataCacheUpdater implements WonMessageProcessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private LinkedDataSource linkedDataSourceOnBehalfOfAtom;

    @Override
    public WonMessage process(WonMessage message) throws WonMessageProcessingException {
        if (this.linkedDataSourceOnBehalfOfAtom != null
                        && this.linkedDataSourceOnBehalfOfAtom instanceof CachingLinkedDataSource) {
            logger.debug("putting message {} into cache", message.getMessageURI());
            URI requester = message.getRecipientAtomURI();
            ((CachingLinkedDataSource) linkedDataSourceOnBehalfOfAtom).addToCache(message.getCompleteDataset(),
                            message.getMessageURI(), requester);
        }
        return message;
    }

    public void setLinkedDataSourceOnBehalfOfAtom(LinkedDataSource linkedDataSourceOnBehalfOfAtom) {
        this.linkedDataSourceOnBehalfOfAtom = linkedDataSourceOnBehalfOfAtom;
    }
}
