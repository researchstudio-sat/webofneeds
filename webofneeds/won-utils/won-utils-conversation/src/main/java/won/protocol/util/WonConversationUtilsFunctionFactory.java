package won.protocol.util;

import java.net.URI;
import java.util.List;
import java.util.function.Function;

import org.apache.jena.query.Dataset;

public class WonConversationUtilsFunctionFactory {
	
	private static DatasetToUriListBySparqlFunction allMessagesFunction;
	
	public static Function<Dataset, List<URI>> getAllMessagesFunction() {
		if (allMessagesFunction == null) {
			allMessagesFunction = new DatasetToUriListBySparqlFunction("/conversation/allmessages.rq");
		}
		return allMessagesFunction;
	}

}
