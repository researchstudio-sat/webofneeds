package won.protocol.util;

import java.net.URI;
import java.util.function.Function;

import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.sparql.expr.E_Bound;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.sse.SSE;

public class WonConversationQueryBuilder<T> {

	private final SparqlSelectFunction<T> delegate;
	
	private WonConversationQueryBuilder(SparqlFunctionResultMapper<T> resultMapper){
		this.delegate = new SparqlSelectFunction<>("/conversation/messages.rq", resultMapper);
	}
	
	public static <T> WonConversationQueryBuilder<T> getBuilder(SparqlFunctionResultMapper<T> resultMapper){
		return new WonConversationQueryBuilder<T>(resultMapper);
	}
	
	public static WonConversationQueryBuilder<URI> getBuilderForMessageUris(){
		return new WonConversationQueryBuilder<URI>(new SelectUriFunction());
	}
	
	public WonConversationQueryBuilder limit(int limit) {
		delegate.limit(limit);
		return this;
	}
	
	public WonConversationQueryBuilder offset(int offset) {
		delegate.offset(offset);
		return this;
	}
	
	public WonConversationQueryBuilder newestFirst() {
		delegate.addOrderBy("timestamp", Query.ORDER_DESCENDING );
		return this;
	}

	public WonConversationQueryBuilder oldestFirst() {
		delegate.addOrderBy("timestamp", Query.ORDER_ASCENDING);
		return this;
	}
	
	public WonConversationQueryBuilder senderNeed(URI senderNeed) {
		delegate.addInitialBinding("senderNeed", new ResourceImpl(senderNeed.toString()));
		return this;
	}

	public WonConversationQueryBuilder isRetractsMessage() {
		delegate.having(SSE.parseExpr("(bound(?retracts))"));
		return this;
	}
	
	public WonConversationQueryBuilder isProposesMessage() {
		delegate.having(SSE.parseExpr("(bound(?proposes))"));
		return this;
	}

	public WonConversationQueryBuilder isProposesToCancelMessage() {
		delegate.having(SSE.parseExpr("(bound(?proposesToCancel))"));
		return this;
	}
	
	public WonConversationQueryBuilder isAcceptsMessage() {
		delegate.having(SSE.parseExpr("(bound(?accepts))"));
		return this;
	}
	
	public WonConversationQueryBuilder noResponses() {
		noFailureResponses();
		noSuccessResponses();
		return this;
	}
	
	public WonConversationQueryBuilder noFailureResponses() {
		delegate.having(SSE.parseExpr("(!= ?msgType msg:FailureResponse)", delegate.getPrefixMapping()));
		return this;
	}
	
	public WonConversationQueryBuilder noSuccessResponses() {
		delegate.having(SSE.parseExpr("(!= ?msgType msg:SuccessResponse)", delegate.getPrefixMapping()));
		return this;
	}
	
	
	public SparqlSelectFunction<T> build(){
		return delegate;
	}
	
	
	public static class SelectUriFunction implements SparqlFunctionResultMapper<URI> {

		@Override
		public URI apply(QuerySolution solution) {
			RDFNode uriNode = solution.get("uri");
			if (uriNode == null) {
				throw new IllegalStateException("Query has no variable named 'uri'");
			}
			if (!uriNode.isURIResource()) {
				throw new IllegalStateException("Value of result variable 'uri' is not a resource");
			}
			return URI.create(uriNode.asResource().getURI().toString());
		}

	}

}
