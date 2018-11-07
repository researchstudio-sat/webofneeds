package won.matcher.sparql.actor;

import java.io.IOException;
import java.net.URI;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelExtract;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StatementBoundary;
import org.apache.jena.rdf.model.StatementBoundaryBase;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpDistinct;
import org.apache.jena.sparql.algebra.op.OpFilter;
import org.apache.jena.sparql.algebra.op.OpPath;
import org.apache.jena.sparql.algebra.op.OpProject;
import org.apache.jena.sparql.algebra.op.OpUnion;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.expr.E_LogicalOr;
import org.apache.jena.sparql.expr.E_StrContains;
import org.apache.jena.sparql.expr.E_StrLowerCase;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.nodevalue.NodeValueBoolean;
import org.apache.jena.sparql.expr.nodevalue.NodeValueString;
import org.apache.jena.sparql.path.P_Alt;
import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.P_NegPropSet;
import org.apache.jena.sparql.path.P_Seq;
import org.apache.jena.sparql.path.P_ZeroOrOne;
import org.apache.jena.sparql.path.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.github.jsonldjava.core.JsonLdError;

import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.SupervisorStrategy;
import akka.actor.UntypedActor;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import scala.concurrent.duration.Duration;
import won.matcher.service.common.event.BulkHintEvent;
import won.matcher.service.common.event.BulkNeedEvent;
import won.matcher.service.common.event.HintEvent;
import won.matcher.service.common.event.NeedEvent;
import won.matcher.sparql.config.SparqlMatcherConfig;
import won.protocol.util.NeedModelWrapper;
import won.protocol.util.RdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.vocabulary.WON;

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

    @Autowired
    private LinkedDataSource linkedDataSource;

    @Override
    public void preStart() throws IOException {
        // subscribe to need events
        pubSubMediator = DistributedPubSub.get(getContext().system()).mediator();
    }

    @Override
    public void onReceive(final Object o) throws Exception {
        String eventTypeForLogging = "unknown";
        Optional<String> uriForLogging = Optional.empty();
        try {
            if (o instanceof NeedEvent) {
                eventTypeForLogging = "NeedEvent";
                NeedEvent needEvent = (NeedEvent) o;
                uriForLogging = Optional.ofNullable(needEvent.getUri());
                if (needEvent.getEventType().equals(NeedEvent.TYPE.ACTIVE)) {
                    processActiveNeedEvent(needEvent);
                } else if (needEvent.getEventType().equals(NeedEvent.TYPE.INACTIVE)) {
                    processInactiveNeedEvent(needEvent);
                } else {
                    unhandled(o);
                }
            } else if (o instanceof BulkNeedEvent) {
                eventTypeForLogging = "BulkNeedEvent";
                log.info("received bulk need event, processing {} need events ...",
                        ((BulkNeedEvent) o).getNeedEvents().size());
                for (NeedEvent event : ((BulkNeedEvent) o).getNeedEvents()) {
                    processActiveNeedEvent(event);
                }
            } else {
                eventTypeForLogging = "unhandled";
                unhandled(o);
            }
        } catch (Exception e) {
            log.info(String.format("Caught exception when processing %s event %s. More info on loglevel 'debug'",
                    eventTypeForLogging, uriForLogging.orElse("[no uri available]")));
            log.debug("caught exception", e);
        }
    }

    protected void processInactiveNeedEvent(NeedEvent needEvent) throws IOException, JsonLdError {
        log.info("Received inactive need.");
    }

    private static String hashFunction(Object input) {
        return Integer.toHexString(input.hashCode());
    }

    private static final Var resultName = Var.alloc("result");

    private static BasicPattern createDetailsQuery(Model model) {
        BasicPattern pattern = new BasicPattern();

        StreamSupport.stream(Spliterators.spliteratorUnknownSize(model.listStatements(), Spliterator.CONCURRENT), true)
                .map((statement) -> {
                    Triple triple = statement.asTriple();
                    RDFNode object = statement.getObject();

                    Node newSubject = Var.alloc(hashFunction(triple.getSubject()));

                    Node newObject = triple.getObject();

                    if (object.isAnon()) {
                        newObject = Var.alloc(hashFunction(newObject));
                    }

                    return new Triple(newSubject, triple.getPredicate(), newObject);
                }).filter(p -> p != null).forEach(pattern::add);

        return pattern;
    }

    private static Op createNeedQuery(Model model, Statement parentStatement, Node newPredicate) {
        StatementBoundary boundary = new StatementBoundaryBase() {
            public boolean stopAt(Statement s) {
                return parentStatement.getSubject().equals(s.getSubject());
            }
        };

        Model subModel = new ModelExtract(boundary).extract(parentStatement.getObject().asResource(), model);
        BasicPattern pattern = createDetailsQuery(subModel);

        if(pattern.isEmpty()) {
            return null;
        }

        pattern.add(new Triple(resultName, newPredicate, Var.alloc(hashFunction(parentStatement.getObject()))));

        return new OpBGP(pattern);
    }

    private static Op createSearchQuery(String searchString) {

        Node blank = NodeFactory.createURI("");
        P_Link blankPath = new P_Link(blank);
        P_NegPropSet negation = new P_NegPropSet();
        negation.add(blankPath);
        P_Alt any = new P_Alt(blankPath, negation);

        P_Link isPath = new P_Link(NodeFactory.createURI("http://purl.org/webofneeds/model#is"));
        P_Link seeksPath = new P_Link(NodeFactory.createURI("http://purl.org/webofneeds/model#seeks"));

        Path searchPath = Collections.<Path>nCopies(5, new P_ZeroOrOne(any)).stream().reduce(new P_Alt(isPath, seeksPath), P_Seq::new);

        Var textSearchTarget = Var.alloc("textSearchTarget");

        Op pathOp = new OpPath(new TriplePath(
                resultName,
                searchPath,
                textSearchTarget));

        Expr filterExpression = Arrays.stream(searchString.toLowerCase().split(" "))
                .<Expr>map(searchPart ->
                        new E_StrContains(
                                new E_StrLowerCase(new ExprVar(textSearchTarget)),
                                new NodeValueString(searchPart)
                        )
                )
                .reduce((left, right) -> new E_LogicalOr(left, right))
                .orElse(new NodeValueBoolean(true));


        return OpFilter.filterBy(
                new ExprList(
                        filterExpression
                ),
                pathOp
        );
    }

    protected void processActiveNeedEvent(NeedEvent needEvent) throws IOException {
        
        NeedModelWrapper need = new NeedModelWrapper(needEvent.deserializeNeedDataset());
        log.debug("starting sparql-based matching for need {}", need.getNeedUri());
        
        List<NeedModelWrapper> matches = queryNeed(need);
        log.debug("found {} match candidates", matches.size());
        Dataset needDataset = need.copyDataset();
        
        final boolean noHintForCounterpart = need.hasFlag(WON.NO_HINT_FOR_COUNTERPART);
        
        Map<NeedModelWrapper, List<NeedModelWrapper>> filteredNeeds = Stream.concat(
                Stream.of(new AbstractMap.SimpleEntry<>(need, matches)),
                //add the reverse match, if no flags forbid it
                matches.stream().map(matchedNeed -> {
                    boolean noHintForMe = matchedNeed.hasFlag(WON.NO_HINT_FOR_ME);
                    if (!noHintForCounterpart && !noHintForMe && !need.getNeedUri().equals(matchedNeed.getNeedUri())) {
                        // query for the matched need - but only in the dataset containing the original need. If we have a match, it means
                        // that the matched need should also get a hint, otherwise it should not.
                        if (log.isDebugEnabled()) {
                            log.debug("checking if match {} of {} should get a hint by inverse matching it in need's dataset: \n{}", 
                                    new Object[] {matchedNeed.getNeedUri(), need.getNeedUri(), RdfUtils.toString(needDataset) } );
                        }
                        List<NeedModelWrapper> matchForMatchedNeed = queryNeed(matchedNeed, Optional.of(new NeedModelWrapperAndDataset(need, needDataset)));
                        if (log.isDebugEnabled()) {
                            log.debug("match {} of {} is also getting a hint: {}", 
                                    new Object[] {matchedNeed.getNeedUri(), need.getNeedUri(), matchForMatchedNeed.size() > 0});
                        }
                        return new AbstractMap.SimpleEntry<>(matchedNeed, matchForMatchedNeed);
                    } else {
                        // the flags in the original or in the matched need forbid a hint. don't add one.
                        return new AbstractMap.SimpleEntry<>(matchedNeed, (List<NeedModelWrapper>) Collections.EMPTY_LIST);
                    }
                    
                }))
                .map(entry -> {
                    List<NeedModelWrapper> filteredMatches = entry.getValue().stream().filter(f -> postFilter(entry.getKey(), f)).collect(Collectors.toList());
                    return new AbstractMap.SimpleEntry<>(entry.getKey(), filteredMatches);
                }).collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

        BulkHintEvent bulkHintEvent = new BulkHintEvent();

        filteredNeeds.forEach((hintTarget, hints) -> {
            hints.stream().limit(config.getLimitResults()).forEach(hint -> {
                bulkHintEvent.addHintEvent(new HintEvent(hintTarget.getWonNodeUri(), hintTarget.getNeedUri(), hint.getWonNodeUri(), hint.getNeedUri(), config.getMatcherUri(), 1));
            });
        });
        
        pubSubMediator.tell(new DistributedPubSubMediator.Publish(bulkHintEvent.getClass().getName(), bulkHintEvent), getSelf());
        log.debug("finished sparql-based matching for need {} (found {} matches)", need.getNeedUri(), bulkHintEvent.getHintEvents().size());
    }

    private Optional<Op> clientSuppliedQuery(String queryString) {
        Query query =  QueryFactory.create(queryString);
        if(query.getQueryType() != Query.QueryTypeSelect) {
            return Optional.empty();
        }

        if(!query.getProjectVars().contains(resultName)) {
            return Optional.empty();
        }
        Op op = Algebra.compile(query);
        return Optional.of(new OpDistinct(op));
    }

    private Optional<Op> defaultQuery(NeedModelWrapper need) {
        Model model = need.getNeedModel();

        String needURI = need.getNeedUri();

        ArrayList<Op> queries = new ArrayList<>(3);

        Statement seeks = model.getProperty(model.createResource(needURI), model.createProperty("http://purl.org/webofneeds/model#seeks"));

        if (seeks != null) {
            Op seeksQuery = createNeedQuery(model, seeks, NodeFactory.createURI("http://purl.org/webofneeds/model#is"));
            if(seeksQuery != null)
                queries.add(seeksQuery);
        }

        Statement is = model.getProperty(model.createResource(needURI), model.createProperty("http://purl.org/webofneeds/model#is"));

        if (is != null) {
            Op isQuery = createNeedQuery(model, is, NodeFactory.createURI("http://purl.org/webofneeds/model#seeks"));
            if(isQuery != null)
                queries.add(isQuery);
        }

        Statement search = model.getProperty(model.createResource(needURI), model.createProperty("http://purl.org/webofneeds/model#hasSearchString"));

        if (search != null) {
            String searchString = search.getString();
            queries.add(createSearchQuery(searchString));
        }

        return queries.stream()
                .reduce((left, right) -> new OpUnion(left, right))
                .map((union) -> new OpDistinct(
                                new OpProject(
                                        union,
                                        Arrays.asList(new Var[]{resultName})
                                )
                        )
                );
    }

    private List<NeedModelWrapper> queryNeed(NeedModelWrapper need) {
      return queryNeed(need, Optional.empty());
    }
    
    /**
     * Query for matches to the need, optionally the needToCheck is used to search in. If needToCheck is passed,
     * it is used as the result data iff the needToCheck is a match for need. This saves us a linked data lookup for
     * data we already have.
     *   
     * @param need
     * @param needToCheck
     * @return
     */
    private List<NeedModelWrapper> queryNeed(NeedModelWrapper need, Optional<NeedModelWrapperAndDataset> needToCheck) {

        Optional<Op> query;

        Optional<String> userQuery = need.getQuery();

        if (userQuery.isPresent()) {
            query = clientSuppliedQuery(userQuery.get());
        } else {
            query = defaultQuery(need);
        }

        List<NeedModelWrapper> needs = query.map(q -> {
            if (log.isDebugEnabled()) {
                log.debug("transforming query, adding 'no hint for counterpart' restriction: {}", q);
            }
            Op noHintForCounterpartQuery = SparqlMatcherUtils.noHintForCounterpartQuery(q, resultName, config.getLimitResults()*5);
            if (log.isDebugEnabled()) {
                log.debug("transformed query: {}", noHintForCounterpartQuery);
                log.debug("transforming query, adding 'wihout no hint for counterpart' restriction: {}", q);
            }
            Op hintForCounterpartQuery = SparqlMatcherUtils.hintForCounterpartQuery(q, resultName, config.getLimitResults() * 5);
            if (log.isDebugEnabled()) {
                log.debug("transformed query: {}", hintForCounterpartQuery);
            }
            return Stream.concat(
                    executeQuery(noHintForCounterpartQuery,  needToCheck),
                    executeQuery(hintForCounterpartQuery,  needToCheck)
                    ).collect(Collectors.toList());
        })
                .orElse(Collections.EMPTY_LIST);

        return needs;
    }

   

    /**
     * Executes the query, optionally only searching in the datasetToQuery.
     * @param q
     * @param datasetToQuery
     * @return
     */
    private Stream<NeedModelWrapper> executeQuery(Op q, Optional<NeedModelWrapperAndDataset> needToCheck) {
            Query compiledQuery = OpAsQuery.asQuery(q);

            // if we were given a needToCheck, restrict the query result to that uri so that 
            // we get exactly one result if that uri is found for the need
            if (needToCheck.isPresent()) {
              Binding binding = BindingFactory.binding(resultName, new ResourceImpl(needToCheck.get().needModelWrapper.getNeedUri()).asNode());
              compiledQuery.setValuesDataBlock(Collections.singletonList(resultName),  Collections.singletonList(binding));
            }
            
            if (log.isDebugEnabled()) {
                log.debug("executeQuery query: {}, needToCheck: {}", new Object[] {compiledQuery, needToCheck});
            }
            List<String> foundUris = new LinkedList<String>();
            // process query results iteratively
            try (QueryExecution execution = QueryExecutionFactory
                                    .sparqlService(config.getSparqlEndpoint(), compiledQuery)
                            ) {

                ResultSet result = execution.execSelect();
                while(result.hasNext()) {
                    QuerySolution querySolution = result.next();
                    String foundNeedURI = querySolution.get(resultName.getName()).toString();
                    foundUris.add(foundNeedURI);
                }
            } catch (Exception e) {
                log.info("caught exception during sparql-based matching (more info on loglevel 'debug'): {} ", e.getMessage());
                if (log.isDebugEnabled()) {
                    e.printStackTrace();
                }
                return Stream.empty();
            }
            
            //load data in parallel
            return foundUris.parallelStream().map(foundNeedUri -> {
                    try {
                        //if we have a needToCheck, return it if the URI we found actually is its URI, otherwise null
                        if ((needToCheck.isPresent())) {
                            return needToCheck.get().needModelWrapper.getNeedUri().equals(foundNeedUri) ? needToCheck.get().needModelWrapper : null;
                        } else {
                            // no needToCheck, which happens when we first look for matches in the graph store: 
                            // download the linked data and return a new NeedModelWrapper
                            return new NeedModelWrapper(linkedDataSource.getDataForResource(URI.create(foundNeedUri)));
                        }
                    } catch (Exception e) {
                        log.info("caught exception trying to load need URI {} : {} (more on loglevel 'debug')" , foundNeedUri, e.getMessage() );
                        if (log.isDebugEnabled()) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                }).filter(foundNeed -> foundNeed != null);
    }

    private static Set<String> getMatchingContexts(NeedModelWrapper need) {
        Model model = need.getNeedModel();
        Resource needURI = model.createResource(need.getNeedUri());
        Property matchingContextProperty = model.createProperty("http://purl.org/webofneeds/model#hasMatchingContext");

        Stream<RDFNode> stream = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(model.listObjectsOfProperty(needURI, matchingContextProperty), Spliterator.CONCURRENT),
                false);

        return stream.map(node -> node.asLiteral().getString()).collect(Collectors.toSet());

    }

    private boolean postFilter(NeedModelWrapper need, NeedModelWrapper foundNeed) {
        try {
            if (need.getNeedUri().equals(foundNeed.getNeedUri())) {
                return false;
            }
            if (need.hasFlag(WON.NO_HINT_FOR_ME)) {
                return false;
            }
            if (foundNeed.hasFlag(WON.NO_HINT_FOR_COUNTERPART)) {
                return false;
            }

            Set<String> needContexts = getMatchingContexts(need);
            if (!needContexts.isEmpty()) {
                Set<String> foundNeedContexts = getMatchingContexts(foundNeed);
                foundNeedContexts.retainAll(needContexts);
                if (foundNeedContexts.isEmpty()) {
                    return false;
                }
            }

            Calendar now = Calendar.getInstance();
            if (now.after(foundNeed.getDoNotMatchAfter()))
                return false;
            if (now.before(foundNeed.getDoNotMatchBefore()))
                return false;

            return true;
        } catch (Exception e) {
            log.info("caught Exception during post-filtering, ignoring match", e);
        }
        return false;
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

    private class NeedModelWrapperAndDataset {
        private NeedModelWrapper needModelWrapper;
        private Dataset dataset;
        public NeedModelWrapperAndDataset(NeedModelWrapper needModelWrapper, Dataset dataset) {
            super();
            this.needModelWrapper = needModelWrapper;
            this.dataset = dataset;
        }
        
    }
    
}
