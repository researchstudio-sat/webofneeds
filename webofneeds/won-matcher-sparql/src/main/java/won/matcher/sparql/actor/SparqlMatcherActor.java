package won.matcher.sparql.actor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelExtract;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StatementBoundary;
import org.apache.jena.rdf.model.StatementBoundaryBase;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpProject;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.github.jsonldjava.core.JsonLdError;

import akka.actor.OneForOneStrategy;
import akka.actor.SupervisorStrategy;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import scala.concurrent.duration.Duration;
import won.matcher.service.common.event.BulkNeedEvent;
import won.matcher.service.common.event.NeedEvent;
import won.matcher.sparql.config.SparqlMatcherConfig;

/**
 * Siren/Solr based abstract matcher with all implementations for querying as
 * well as indexing needs.
 */
@Component
@Scope("prototype")
public class SparqlMatcherActor extends UntypedActor {
	private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	@Autowired
	private SparqlMatcherConfig config;

	@Override
	public void onReceive(final Object o) throws Exception {

		if (o instanceof NeedEvent) {
			NeedEvent needEvent = (NeedEvent) o;
			if (needEvent.getEventType().equals(NeedEvent.TYPE.ACTIVE)) {
				processActiveNeedEvent(needEvent);
			} else if (needEvent.getEventType().equals(NeedEvent.TYPE.INACTIVE)) {
				processInactiveNeedEvent(needEvent);
			} else {
				unhandled(o);
			}
		} else if (o instanceof BulkNeedEvent) {
			log.info("received bulk need event, processing {} need events ...",
					((BulkNeedEvent) o).getNeedEvents().size());
			for (NeedEvent event : ((BulkNeedEvent) o).getNeedEvents()) {
				processActiveNeedEvent(event);
			}
		} else {
			unhandled(o);
		}
	}

	protected void processInactiveNeedEvent(NeedEvent needEvent) throws IOException, JsonLdError {
		log.info("Received inactive need.");
	}

	private static String hashFunction(String input) {
		return Integer.toHexString(input.hashCode());
	}

	protected void processActiveNeedEvent(NeedEvent needEvent) throws IOException, JsonLdError {
		log.info("Received active need.");

		String needURI = needEvent.getUri();

		Model model = needEvent.deserializeNeedDataset().getUnionModel();

		StatementBoundary boundary = new StatementBoundaryBase() {
			public boolean stopAt(Statement s) {
				String subjectURI = s.getSubject().getURI();
				String predicate = s.getPredicate().getLocalName();

				if (needURI.equals(subjectURI)) {
					if (predicate.equals("is") || predicate.equals("seeks")) {
						return false;
					} else {
						return true;
					}
				} else {
					return false;
				}
			}
		};

		model = new ModelExtract(boundary).extract(model.createResource(needURI), model);

		BasicPattern base = new BasicPattern();

		StreamSupport.stream(Spliterators.spliteratorUnknownSize(model.listStatements(), Spliterator.CONCURRENT), true)
				.map((statement) -> {
					Resource subject = statement.getSubject();
					String predicate = statement.getPredicate().getURI();
					RDFNode object = statement.getObject();

					if (needURI.equals(subject.getURI())) {
						if (predicate.equals("http://purl.org/webofneeds/model#is")) {
							predicate = "http://purl.org/webofneeds/model#seeks";
						} else if (predicate.equals("http://purl.org/webofneeds/model#seeks")) {
							predicate = "http://purl.org/webofneeds/model#is";
						} else {
							return null;
						}
					}

					Node subjectNode = subject.isAnon() ? NodeFactory.createVariable(hashFunction(subject.getId().toString())) : NodeFactory.createVariable(hashFunction(subject.getURI().toString()));

					if (object.isLiteral()) {
						return new Triple(subjectNode, NodeFactory.createURI(predicate), NodeFactory.createVariable(hashFunction(statement.toString())));
					} else {
						Resource objectResource = object.asResource();
						Node objectNode = objectResource.isAnon() ? NodeFactory.createVariable(hashFunction(objectResource.getId().toString()))
								: NodeFactory.createURI(objectResource.getURI());
						return new Triple(subjectNode,
								NodeFactory.createURI(predicate), objectNode);
					}

				}).filter((elem) -> elem != null).forEach(base::add);
		
		Op projection = new OpProject(new OpBGP(base), Arrays.asList(new Var[]{Var.alloc(hashFunction(needURI))}));
		
	}

	@Override
	public SupervisorStrategy supervisorStrategy() {

		SupervisorStrategy supervisorStrategy = new OneForOneStrategy(0, Duration.Zero(),
				new Function<Throwable, SupervisorStrategy.Directive>() {

					@Override
					public SupervisorStrategy.Directive apply(Throwable t) throws Exception {

						log.warning("Actor encountered error: {}", t);
						// default behaviour
						return SupervisorStrategy.escalate();
					}
				});

		return supervisorStrategy;
	}

}
