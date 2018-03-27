package won.protocol.util;

import org.apache.jena.query.Dataset;
import won.protocol.agreement.AgreementProtocolState;

import java.net.URI;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;


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
	
	public static URI getNthLatestMessage(Dataset conversationDataset, Predicate predicate, int n){
		return AgreementProtocolState.of(conversationDataset).getNthLatestMessage(predicate, n);
	}
	
	public static URI getLatestMessageOfNeed(Dataset conversationDataset, URI senderNeed){
		return AgreementProtocolState.of(conversationDataset).getLatestMessageSentByNeed(senderNeed);
	}
	
	public static URI getNthLatestMessageOfNeed(Dataset conversationDataset, URI senderNeed, int n){
		return AgreementProtocolState.of(conversationDataset).getNthLatestMessageSentByNeed(senderNeed, n);
	}
	
	
	public static URI getLatestAcceptsMessageOfNeed(Dataset conversationDataset, URI senderNeed){
		return AgreementProtocolState.of(conversationDataset).getLatestAcceptsMessageSentByNeed(senderNeed);
	}
	
	public static URI getLatestRetractsMessageOfNeed(Dataset conversationDataset, URI senderNeed){
		return AgreementProtocolState.of(conversationDataset).getLatestRetractsMessageSentByNeed(senderNeed);
	}
	
	public static URI getLatestProposesMessageOfNeed(Dataset conversationDataset, URI senderNeed){
		return AgreementProtocolState.of(conversationDataset).getLatestProposeMessageSentByNeed(senderNeed);
	}
	
	public static URI getLatestProposesToCancelMessageOfNeed(Dataset conversationDataset, URI senderNeed){
		return AgreementProtocolState.of(conversationDataset).getLatestProposesToCancelMessageSentByNeed(senderNeed);
	}
	
	public static URI getLatestRejectsMessageOfNeed(Dataset conversationDataset, URI senderNeed, int n){
		return AgreementProtocolState.of(conversationDataset).getLatestRejectsMessageSentByNeed(senderNeed);
	}
	
	public static String getTextMessage(Dataset conversationDataset, URI messageUri) {
		return (String) getFirstOrNull(conversationDataset,
				WonMessageQueryBuilder.getBuilder(x -> x.get("text").asLiteral().toString())
				.messageUri(messageUri).build());
	}
	
}
