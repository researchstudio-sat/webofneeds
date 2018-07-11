package won.matcher.sparql.actor;

import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.SupervisorStrategy;
import akka.actor.UntypedActor;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import com.github.jsonldjava.core.JsonLdError;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.expr.*;
import org.apache.jena.sparql.expr.nodevalue.NodeValueBoolean;
import org.apache.jena.sparql.expr.nodevalue.NodeValueNode;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.path.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.Duration;
import won.matcher.service.common.event.BulkHintEvent;
import won.matcher.service.common.event.BulkNeedEvent;
import won.matcher.service.common.event.HintEvent;
import won.matcher.service.common.event.NeedEvent;
import won.matcher.sparql.config.SparqlMatcherConfig;
import won.protocol.util.NeedModelWrapper;
import won.protocol.vocabulary.WON;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Siren/Solr based abstract matcher with all implementations for querying as
 * well as indexing needs.
 */
@Component
@Scope("prototype")
public class SparqlMatcherActor extends UntypedActor {
	private LoggingAdapter log = Logging.getLogger(getContext().system(), this);


	private ActorRef pubSubMediator;

	@Autowired
	private SparqlMatcherConfig config;

	@Override
    public void preStart() throws IOException {

		// subscribe to need events
		pubSubMediator = DistributedPubSub.get(getContext().system()).mediator();
	}

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

	private static String hashFunction(Object input) {
		return Integer.toHexString(input.hashCode());
	}

	private static final Var resultName = Var.alloc("result");
	private static final Var hint1 = Var.alloc("hint1");
	private static final Var hint2 = Var.alloc("hint2");

	private static BasicPattern createDetailsQuery(Model model) {
		BasicPattern pattern = new BasicPattern();

		StreamSupport.stream(Spliterators.spliteratorUnknownSize(model.listStatements(), Spliterator.CONCURRENT), true)
		.map((statement) -> {
			Triple triple = statement.asTriple();
			RDFNode object = statement.getObject();

			Node newSubject = NodeFactory.createVariable(hashFunction(triple.getSubject()));

			Node newObject = triple.getObject();

			if(object.isAnon()) {
				newObject = NodeFactory.createVariable(hashFunction(newObject));
			}

			return new Triple(newSubject, triple.getPredicate(), newObject);
		}).filter(p -> p != null).forEach(pattern::add);

		return pattern;
	}

	public Op hasTag(Op op, Var variable, Node tag) {
		Var tmpVar = Var.alloc(hashFunction(variable));

		Triple matchTriple = new Triple(resultName.asNode(), NodeFactory.createURI("http://purl.org/webofneeds/model#hasFlag"), tmpVar);
		ExprList filter = new ExprList(new E_Equals(new ExprVar(tmpVar), new NodeValueNode(tag)));
		VarExprList variableAssignment = new VarExprList(variable, new E_Bound(new ExprVar(tmpVar)));
		return OpExtend.create(OpLeftJoin.create(op, new OpTriple(matchTriple), filter), variableAssignment);
	}

	public Op hasOverlappingContext(Op op, Set<String> contexts) {
		Var tmpVar = Var.alloc("overlappingContext");

		if(contexts.isEmpty()) {
			return op;
		}

		List<Expr> contextExpressions = contexts.stream().map(NodeValueString::new).collect(Collectors.toList());

		Triple matchTriple = new Triple(resultName.asNode(), NodeFactory.createURI("http://purl.org/webofneeds/model#hasMatchingContext"), tmpVar);
		ExprList filter = new ExprList(new E_OneOf(new ExprVar(tmpVar), new ExprList(contextExpressions)));

		return OpFilter.filter(filter, OpJoin.create(op, new OpTriple(matchTriple)));
	}


	public Op createHintQuery(Op op, boolean noHintForMe, boolean noHintForCounterpart) {
		Var remoteMe = Var.alloc("remoteMe");
		Var remoteCounterpart = Var.alloc("remoteCounterpart");
		VarExprList expressions = new VarExprList();
		expressions.add(hint1, new E_LogicalNot(new E_LogicalOr(new ExprVar(remoteCounterpart), new NodeValueBoolean(noHintForMe))));
		expressions.add(hint2, new E_LogicalNot(new E_LogicalOr(new ExprVar(remoteMe), new NodeValueBoolean(noHintForCounterpart))));

		Op remoteCounterpartOp = hasTag(op, remoteCounterpart, NodeFactory.createURI("http://purl.org/webofneeds/model#NoHintForCounterpart"));
		Op remoteMeOp = hasTag(remoteCounterpartOp, remoteMe, NodeFactory.createURI("http://purl.org/webofneeds/model#NoHintForMe"));

		ExprList filter = new ExprList(new E_LogicalOr(new ExprVar(hint1), new ExprVar(hint2)));
		Op variableOp = OpExtend.create(remoteMeOp, expressions);
		return OpFilter.filterBy(filter, variableOp);
	}

	private static Op createNeedQuery(Model model, Statement parentStatement, Node newPredicate) {
		StatementBoundary boundary = new StatementBoundaryBase() {
			public boolean stopAt(Statement s) {
				return parentStatement.getSubject().equals(s.getSubject());
			}
		};

		Model subModel = new ModelExtract(boundary).extract(parentStatement.getObject().asResource(), model);
		BasicPattern pattern = createDetailsQuery(subModel);

		pattern.add(new Triple(resultName.asNode(), newPredicate, NodeFactory.createVariable(hashFunction(parentStatement.getObject()))));

		return new OpBGP(pattern);
	}

	private static Op relevantTimeFilter(Op op) {
		Var time1 = Var.alloc("time1");
		Var time2 = Var.alloc("time2");

		Expr currentTime = new NodeValueNode(NodeFactory.createLiteral(Instant.now().toString(), XSDDatatype.XSDdateTime));
		Op noMatchBefore = OpLeftJoin.create(op, new OpTriple(new Triple(
				resultName.asNode(),
				NodeFactory.createURI("http://purl.org/webofneeds/model#doNotMatchBefore"),
				time1.asNode())),
				new ExprList(new E_LessThan(currentTime, new ExprVar(time1.asNode()))));

		Op noMatchAfter = OpLeftJoin.create(noMatchBefore, new OpTriple(new Triple(
				resultName.asNode(),
				NodeFactory.createURI("http://purl.org/webofneeds/model#doNotMatchAfter"),
				time2.asNode())),
				new ExprList(new E_GreaterThan(currentTime, new ExprVar(time2.asNode()))));

		return OpFilter.filter(new E_LogicalNot(new E_LogicalOr(new E_Bound(new ExprVar(time1)), new E_Bound(new ExprVar(time2)))), noMatchAfter);
	}

	private static Op createSearchQuery(String searchString) {

		Node blank = NodeFactory.createURI("");
		P_Link blankPath = new P_Link(blank);
		P_NegPropSet negation = new P_NegPropSet();
		negation.add(blankPath);
		P_Alt any = new P_Alt(blankPath, negation);

		P_Link isPath = new P_Link(NodeFactory.createURI("http://purl.org/webofneeds/model#is"));
		P_Link seeksPath = new P_Link(NodeFactory.createURI("http://purl.org/webofneeds/model#seeks"));

		Path searchPath = Collections.<Path>nCopies(5, new P_ZeroOrOne(any)).stream().reduce(new P_Alt(isPath,seeksPath), P_Seq::new);

		Var textSearchTarget = Var.alloc("textSearchTarget");

		Op pathOp = new OpPath(new TriplePath(
				resultName.asNode(),
				searchPath,
				textSearchTarget.asNode()));

		Expr filterExpression = Arrays.stream(searchString.toLowerCase().split(" "))
				.<Expr>map(searchPart ->
					new E_StrContains(
							new E_StrLowerCase(new ExprVar(textSearchTarget)),
							new NodeValueString(searchPart)
					)
				)
				.reduce((left, right) ->  new E_LogicalOr(left, right))
				.orElse(new NodeValueBoolean(true));



		return OpFilter.filterBy(
				new ExprList(
						filterExpression
				),
				pathOp
				);
	}

	protected void processActiveNeedEvent(NeedEvent needEvent) throws IOException, JsonLdError {
		log.info("Received active need.");

		String needURI = needEvent.getUri();


		NeedModelWrapper need = new NeedModelWrapper(needEvent.deserializeNeedDataset());

		Model model = need.getNeedModel();

		ArrayList<Op> queries = new ArrayList<>(2);

		Statement seeks = model.getProperty(model.createResource(needURI), model.createProperty("http://purl.org/webofneeds/model#seeks"));

		if(seeks != null) {
			Op seeksQuery = createNeedQuery(model, seeks, NodeFactory.createURI("http://purl.org/webofneeds/model#is"));

			queries.add(seeksQuery);
		}

		Statement is = model.getProperty(model.createResource(needURI), model.createProperty("http://purl.org/webofneeds/model#is"));

		if(is != null) {
			Op isQuery = createNeedQuery(model, is, NodeFactory.createURI("http://purl.org/webofneeds/model#seeks"));

			queries.add(isQuery);
		}

		Statement search = model.getProperty(model.createResource(needURI), model.createProperty("http://purl.org/webofneeds/model#hasSearchString"));

		if(search != null) {
			String searchString = search.getString();
			queries.add(createSearchQuery(searchString));
		}

		Set<String> contexts = model.listObjectsOfProperty(model.createResource(needURI),
				model.createProperty("http://purl.org/webofneeds/model#hasMatchingContext")).toList().stream().map(rdfNode -> rdfNode.asLiteral().getString()).collect(Collectors.toSet());

		queries.stream().reduce((left, right) -> new OpUnion(left, right))
		.ifPresent((union) -> {
			BasicPattern nodeUriBGP = new BasicPattern();
			Var wonNodeVar = Var.alloc("wonNode");
            nodeUriBGP.add(new Triple(resultName.asNode(), NodeFactory.createURI("http://purl.org/webofneeds/model#hasWonNode"), wonNodeVar.asNode()));

            Op query = new OpSlice(
                    new OpDistinct(
                            new OpProject(
                                    relevantTimeFilter(
                                            hasOverlappingContext(
                                                    createHintQuery(
                                                            OpJoin.create(
                                                                    new OpBGP(nodeUriBGP),
                                                                    union),
                                                            need.hasFlag(WON.NO_HINT_FOR_ME),
                                                            need.hasFlag(WON.NO_HINT_FOR_COUNTERPART)
                                                    ), contexts
                                            )),
                                    Arrays.asList(new Var[]{resultName, wonNodeVar, hint1, hint2})
                            )
                    ),
                    0,
					config.getLimitResults());
			QueryExecution execution = QueryExecutionFactory.sparqlService(config.getSparqlEndpoint(), OpAsQuery.asQuery(query));

			ResultSet result = execution.execSelect();

			BulkHintEvent bulkHintEvent = new BulkHintEvent();

			while(result.hasNext()) {
				QuerySolution solution = result.nextSolution();
				String foundNeedURI = solution.get(resultName.getName()).toString();
				String foundNeedNodeURI = solution.get(wonNodeVar.getName()).toString();
				boolean hintFor1 = solution.get(hint1.getName()).asLiteral().getBoolean();
				boolean hintFor2 = solution.get(hint2.getName()).asLiteral().getBoolean();

				if(hintFor1)
					bulkHintEvent.addHintEvent(new HintEvent(needEvent.getWonNodeUri(), needURI, foundNeedNodeURI, foundNeedURI, config.getMatcherUri(), 1));
				if(hintFor2)
					bulkHintEvent.addHintEvent(new HintEvent(foundNeedNodeURI, foundNeedURI, needEvent.getWonNodeUri(), needURI, config.getMatcherUri(), 1));
			}

			pubSubMediator.tell(new DistributedPubSubMediator.Publish(bulkHintEvent.getClass().getName(), bulkHintEvent), getSelf());

		});
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
