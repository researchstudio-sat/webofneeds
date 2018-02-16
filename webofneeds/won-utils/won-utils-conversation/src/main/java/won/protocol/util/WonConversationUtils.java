package won.protocol.util;

import java.net.URI;
import java.util.List;
import java.util.function.Function;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

public class WonConversationUtils {
	
	/**
	 * Get all message URIs (excluding those of responses) from the conversation, oldest first.
	 * @param conversationDataset
	 * @return
	 */
	public static List<URI> getAllMessageURIs(Dataset conversationDataset){
		return WonConversationQueryBuilder
				.getBuilderForMessageUris().oldestFirst().build().apply(conversationDataset);
	}
	
	private static <T> T getFirstOrNull(Dataset dataset, Function<Dataset, List<T>> function) {
		//RDFDataMgr.write(System.err, dataset, Lang.TRIG);
		List<T> results = function.apply(dataset);
		if (results.size() > 0) return results.get(0);
		return null;
	}
	
	public static URI getLatestMessageOfNeed(Dataset conversationDataset, URI senderNeed){
		return (URI) getFirstOrNull(conversationDataset,
				WonConversationQueryBuilder.getBuilderForMessageUris()
					.noResponses()
					.newestFirst()
					.limit(1)
					.senderNeed(senderNeed)
					.build());
		/*
		return (URI) getFirstOrNull(conversationDataset, WonLatestMessageQueryBuilder.getBuilderForMessageUris()
				.senderNeed(senderNeed)
				.build());
				*/
	}
	
	public static URI getLatestAcceptsMessageOfNeed(Dataset conversationDataset, URI senderNeed){
		return (URI) getFirstOrNull(conversationDataset,
				WonConversationQueryBuilder.getBuilderForMessageUris()
					.noResponses()
					.newestFirst()
					.limit(1)
					.senderNeed(senderNeed)
					.isAcceptsMessage()
					.build());
	}
	
	public static URI getLatestAcceptsMessage(Dataset conversationDataset){
		return (URI) getFirstOrNull(conversationDataset,
				WonConversationQueryBuilder.getBuilderForMessageUris()
					.noResponses()
					.newestFirst()
					.limit(1)
					.isAcceptsMessage()
					.build());
	}
	
	public static URI getLatestRetractsMessageOfNeed(Dataset conversationDataset, URI senderNeed){
		return (URI) getFirstOrNull(conversationDataset,
				WonConversationQueryBuilder.getBuilderForMessageUris()
				.noResponses()
				.newestFirst()
				.limit(1)
				.senderNeed(senderNeed)
				.isRetractsMessage()
				.build());
	}
	
	public static URI getLatestProposesMessageOfNeed(Dataset conversationDataset, URI senderNeed){
		return (URI) getFirstOrNull(conversationDataset,
				WonConversationQueryBuilder.getBuilderForMessageUris()
				.noResponses()
				.newestFirst()
				.limit(1)
				.senderNeed(senderNeed)
				.isProposesMessage()
				.build());
	}
	
	public static URI getLatestProposesToCancelMessageOfNeed(Dataset conversationDataset, URI senderNeed){
		return (URI) getFirstOrNull(conversationDataset,
				WonConversationQueryBuilder.getBuilderForMessageUris()
				.noResponses()
				.newestFirst()
				.limit(1)
				.senderNeed(senderNeed)
				.isProposesToCancelMessage()
				.build());
	}
	
	
	
	
	public static String getTextMessage(Dataset conversationDataset, URI messageUri) {
		return (String) getFirstOrNull(conversationDataset,
				WonMessageQueryBuilder.getBuilder(x -> x.get("text").asLiteral().toString())
				.messageUri(messageUri).build());
	}
	
	
}
