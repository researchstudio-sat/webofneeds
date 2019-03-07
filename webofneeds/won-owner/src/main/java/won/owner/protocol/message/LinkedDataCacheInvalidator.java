package won.owner.protocol.message;

import java.net.URI;

import org.apache.jena.query.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageType;
import won.protocol.message.processor.WonMessageProcessor;
import won.protocol.message.processor.exception.WonMessageProcessingException;
import won.protocol.util.NeedModelWrapper;
import won.protocol.util.linkeddata.CachingLinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

/**
 * Removes elements from the linked data cache when certain messages are seen. 
 * User: ypanchenko Date: 27.10.2015
 */
public class LinkedDataCacheInvalidator implements WonMessageProcessor {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public void setLinkedDataSource(final CachingLinkedDataSource linkedDataSource) {
		this.linkedDataSource = linkedDataSource;
	}

	public void setLinkedDataSourceOnBehalfOfNeed(CachingLinkedDataSource linkedDataSourceOnBehalfOfNeed) {
		this.linkedDataSourceOnBehalfOfNeed = linkedDataSourceOnBehalfOfNeed;
	}

	private CachingLinkedDataSource linkedDataSource;

	private CachingLinkedDataSource linkedDataSourceOnBehalfOfNeed;

	@Override
	public WonMessage process(final WonMessage message) throws WonMessageProcessingException {
		
		WonMessageType type = message.getMessageType();
		if (type == WonMessageType.SUCCESS_RESPONSE) {
			type = message.getIsResponseToMessageType();
		}
		
		URI webId = message.getReceiverNeedURI();
		
		if (message.getReceiverURI() != null) {
			// the cached list of events of the receiver need for the involved connection
			// should be invalidated, since one more
			// message was created
			
			logger.debug("invalidating events list for need " + message.getReceiverNeedURI() + " for connection "
					+ message.getReceiverURI());
			URI eventContainerUri = WonLinkedDataUtils.getEventContainerURIforConnectionURI(message.getReceiverURI(),
					linkedDataSource);
			invalidate(eventContainerUri, webId);
			
			if (type.causesConnectionStateChange()) {
				invalidate(message.getReceiverURI(), webId);
			}
		}
		
		if (type.causesNewConnection()) {
			// the list of connections of the receiver need should be invalidated, since
			// these type
			// of messages mean that the new connection has been created recently
			logger.debug("invalidating connections list for need " + message.getReceiverNeedURI());
			Dataset need = linkedDataSource.getDataForResource(message.getReceiverNeedURI());
			NeedModelWrapper wrapper = new NeedModelWrapper(need);
			URI connectionsListUri = URI.create(wrapper.getConnectionContainerUri());
			invalidate(connectionsListUri, webId);
		}
		
		if (type.causesNeedStateChange()) {
			invalidate(message.getReceiverNeedURI(), webId);
		}
		
		return message;
	}
	
	private void invalidate(URI uri, URI webId) {
		if (uri == null) return;
		linkedDataSource.invalidate(uri);
		linkedDataSource.invalidate(uri, webId);
		if (linkedDataSourceOnBehalfOfNeed != linkedDataSource) {
			linkedDataSourceOnBehalfOfNeed.invalidate(uri);
			linkedDataSourceOnBehalfOfNeed.invalidate(uri, webId);
		}
	}
}
