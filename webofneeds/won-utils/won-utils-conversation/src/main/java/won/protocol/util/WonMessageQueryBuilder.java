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

public class WonMessageQueryBuilder<T> {

	private final SparqlSelectFunction<T> delegate;
	
	private WonMessageQueryBuilder(Function<QuerySolution, T> resultMapper){
		this.delegate = new SparqlSelectFunction<>("/conversation/message.rq", resultMapper);
	}
	
	public static <M> WonMessageQueryBuilder<M> getBuilder(Function<QuerySolution, M> resultMapper){
		return new WonMessageQueryBuilder<M>(resultMapper);
	}
	
	public static WonMessageQueryBuilder<URI> getBuilderForMessageUris(){
		return new WonMessageQueryBuilder<URI>(new SelectUriFunction());
	}
	
	public WonMessageQueryBuilder limit(int limit) {
		delegate.limit(limit);
		return this;
	}
	
	public WonMessageQueryBuilder offset(int offset) {
		delegate.offset(offset);
		return this;
	}
	
	public WonMessageQueryBuilder senderConnection(URI senderConnection) {
		QuerySolutionMap initialBinding = new QuerySolutionMap();
		initialBinding.add("senderConnection", new ResourceImpl(senderConnection.toString()));
		delegate.addInitialBindings(initialBinding);
		return this;
	}
	
	public WonMessageQueryBuilder senderNeed(URI senderNeed) {
		delegate.addInitialBinding("senderNeed", new ResourceImpl(senderNeed.toString()));
		return this;
	}
	
	public WonMessageQueryBuilder messageUri(URI messageUri) {
		delegate.addInitialBinding("msg", new ResourceImpl(messageUri.toString()));
		return this;
	}

	public WonMessageQueryBuilder isRetractsMessage() {
		delegate.having(SSE.parseExpr("(bound(?retracts))"));
		return this;
	}
	
	public WonMessageQueryBuilder isProposesMessage() {
		delegate.having(SSE.parseExpr("(bound(?proposes))"));
		return this;
	}

	public WonMessageQueryBuilder isProposesToCancelMessage() {
		delegate.having(SSE.parseExpr("(bound(?proposesToCancel))"));
		return this;
	}
	
	public WonMessageQueryBuilder isAcceptsMessage() {
		delegate.having(SSE.parseExpr("(bound(?accepts))"));
		return this;
	}
	
	public WonMessageQueryBuilder noResponses() {
		noFailureResponses();
		noSuccessResponses();
		return this;
	}
	
	public WonMessageQueryBuilder noFailureResponses() {
		delegate.having(SSE.parseExpr("(!= ?msgType msg:FailureResponse)", delegate.getPrefixMapping()));
		return this;
	}
	
	public WonMessageQueryBuilder noSuccessResponses() {
		delegate.having(SSE.parseExpr("(!= ?msgType msg:SuccessResponse)", delegate.getPrefixMapping()));
		return this;
	}
	
	
	public SparqlSelectFunction<T> build(){
		return delegate;
	}
	
	
	public static class SelectUriFunction implements Function<QuerySolution, URI> {

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
