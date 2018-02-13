package won.protocol.util;

import java.net.URI;
import java.util.List;

import org.apache.jena.query.Dataset;

public class WonConversationUtils {
	
	/**
	 * Get all message URIs (excluding those of responses) from the conversation, oldest first.
	 * @param conversationDataset
	 * @return
	 */
	public static List<URI> getAllMessageURIs(Dataset conversationDataset){
		return WonConversationUtilsFunctionFactory.getAllMessagesFunction().apply(conversationDataset);
	}

	
	/**
	 * Returns the create message of the need that this connection belongs to.
	 * @param connectionURI
	 * @return
	 */
	public static URI getCreateMessageURI(URI connectionURI, Dataset conversationDataset) {
		throw new UnsupportedOperationException("not yet implemented");
	}
}
