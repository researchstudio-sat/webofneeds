package won.matcher.sparql.actor;

import java.io.IOException;
import java.net.URI;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.jena.graph.Node;
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
import org.apache.jena.sparql.algebra.op.OpProject;
import org.apache.jena.sparql.algebra.op.OpUnion;
import org.apache.jena.sparql.core.BasicPattern;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
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
import won.matcher.service.common.event.BulkAtomEvent;
import won.matcher.service.common.event.HintEvent;
import won.matcher.service.common.event.AtomEvent;
import won.matcher.sparql.config.SparqlMatcherConfig;
import won.protocol.model.AtomState;
import won.protocol.util.AtomModelWrapper;
import won.protocol.util.RdfUtils;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.vocabulary.WON;

/**
 * Siren/Solr based abstract matcher with all implementations for querying as
 * well as indexing atoms.
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
        // subscribe to atom events
        pubSubMediator = DistributedPubSub.get(getContext().system()).mediator();
    }

    @Override
    public void onReceive(final Object o) throws Exception {
        String eventTypeForLogging = "unknown";
        Optional<String> uriForLogging = Optional.empty();
        try {
            if (o instanceof AtomEvent) {
                eventTypeForLogging = "AtomEvent";
                AtomEvent atomEvent = (AtomEvent) o;
                uriForLogging = Optional.ofNullable(atomEvent.getUri());
                if (atomEvent.getEventType().equals(AtomEvent.TYPE.ACTIVE)) {
                    processActiveAtomEvent(atomEvent);
                } else if (atomEvent.getEventType().equals(AtomEvent.TYPE.INACTIVE)) {
                    processInactiveAtomEvent(atomEvent);
                } else {
                    unhandled(o);
                }
            } else if (o instanceof BulkAtomEvent) {
                eventTypeForLogging = "BulkAtomEvent";
                log.info("received bulk atom event, processing {} atom events ...",
                                ((BulkAtomEvent) o).getAtomEvents().size());
                for (AtomEvent event : ((BulkAtomEvent) o).getAtomEvents()) {
                    processActiveAtomEvent(event);
                }
            } else {
                eventTypeForLogging = "unhandled";
                unhandled(o);
            }
        } catch (Exception e) {
            log.info(String.format("Caught exception when processing %s event %s. More info on loglevel 'debug'",
                            eventTypeForLogging, uriForLogging.orElse("[no uri available]")));
            log.debug("caught exception", e);
            if (log.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }

    protected void processInactiveAtomEvent(AtomEvent atomEvent) throws IOException, JsonLdError {
        log.info("Received inactive atom.");
    }

    private static String hashFunction(Object input) {
        return Integer.toHexString(input.hashCode());
    }

    private static final Var resultName = Var.alloc("result");
    private static final Var thisAtom = Var.alloc("thisAtom");
    private static final Var scoreName = Var.alloc("score");

    private static BasicPattern createDetailsQuery(Model model, Statement parentStatement) {
        BasicPattern pattern = new BasicPattern();
        StreamSupport.stream(Spliterators.spliteratorUnknownSize(model.listStatements(), Spliterator.CONCURRENT), true)
                        .map((statement) -> {
                            Triple triple = statement.asTriple();
                            RDFNode object = statement.getObject();
                            Node newSubject = Var.alloc(hashFunction(triple.getSubject()));
                            Node newObject = triple.getObject();
                            if (triple.getSubject().equals(parentStatement.getObject().asNode())) {
                                newSubject = resultName.asNode();
                            }
                            if (object.isAnon()) {
                                newObject = Var.alloc(hashFunction(newObject));
                            }
                            return new Triple(newSubject, triple.getPredicate(), newObject);
                        }).filter(p -> p != null).forEach(pattern::add);
        return pattern;
    }

    private static Op createAtomQuery(Model model, Statement parentStatement) {
        StatementBoundary boundary = new StatementBoundaryBase() {
            public boolean stopAt(Statement s) {
                return parentStatement.getSubject().equals(s.getSubject());
            }
        };
        Model subModel = new ModelExtract(boundary).extract(parentStatement.getObject().asResource(), model);
        BasicPattern pattern = createDetailsQuery(subModel, parentStatement);
        if (pattern.isEmpty()) {
            return null;
        }
        return new OpBGP(pattern);
    }

    /**
     * Produces hints for the atom and possibly also 'inverse' hints. Inverse hints
     * are hints sent to the atoms we find as matches for the original atom. The
     * score is calculated as a function of scores provided by the embedded sparql
     * queries: the range of those scores is projected on a range of 0-1. For
     * inverse matches, the score is always 100%, because there is only one possible
     * match - the original atom. Note: this could be improved by remembering
     * reported match scores in the matcher and using historic scores for
     * normalization, but that's a lot more work.
     */
    protected void processActiveAtomEvent(AtomEvent atomEvent) throws IOException {
        AtomModelWrapper atom = new AtomModelWrapper(atomEvent.deserializeAtomDataset());
        log.debug("starting sparql-based matching for atom {}", atom.getAtomUri());
        List<ScoredAtom> matches = queryAtom(atom);
        log.debug("found {} match candidates", matches.size());
        // produce hints after post-filtering the matches we found:
        Collection<HintEvent> hintEvents = produceHints(atom,
                        matches.stream().filter(foundAtom -> foundAtom.atom.getAtomState() == AtomState.ACTIVE) // we
                                                                                                                // may
                                                                                                                // not
                                                                                                                // have
                                                                                                                // updated
                                                                                                                // our
                                                                                                                // atom
                                                                                                                // state
                                                                                                                // in
                                                                                                                // the
                                                                                                                // database.
                                                                                                                // re-check!
                                        .filter(foundAtom -> postFilter(atom, foundAtom.atom))
                                        .collect(Collectors.toList()));
        publishHintEvents(hintEvents, atom.getAtomUri(), false);
        // but use the whole list of matches for inverse matching
        final boolean noHintForCounterpart = atom.flag(WON.NoHintForCounterpart);
        if (!noHintForCounterpart) {
            // we want do do inverse matching:
            // 1. check if the inverse match is appropriate
            // 2. do post-filtering
            // 3. produce hints
            // 4. collect all inverse hints and publish in one event
            Dataset atomDataset = atom.copyDataset();
            List<HintEvent> inverseHintEvents = matches.stream().filter(n -> n.atom.getAtomState() == AtomState.ACTIVE)
                            .filter(matchedAtom -> !matchedAtom.atom.flag(WON.NoHintForMe)
                                            && !atom.getAtomUri().equals(matchedAtom.atom.getAtomUri()))
                            .map(matchedAtom -> {
                                // query for the matched atom - but only in the dataset containing the original
                                // atom. If we have a match, it means
                                // that the matched atom should also get a hint, otherwise it should not.
                                if (log.isDebugEnabled()) {
                                    log.debug("checking if match {} of {} should get a hint by inverse matching it in atom's dataset: \n{}",
                                                    new Object[] { matchedAtom.atom.getAtomUri(), atom.getAtomUri(),
                                                                    RdfUtils.toString(atomDataset) });
                                }
                                List<ScoredAtom> matchesForMatchedAtom = queryAtom(matchedAtom.atom,
                                                Optional.of(new AtomModelWrapperAndDataset(atom, atomDataset)));
                                if (log.isDebugEnabled()) {
                                    log.debug("match {} of {} is also getting a hint: {}",
                                                    new Object[] { matchedAtom.atom.getAtomUri(), atom.getAtomUri(),
                                                                    matchesForMatchedAtom.size() > 0 });
                                }
                                return new AbstractMap.SimpleEntry<>(matchedAtom.atom, matchesForMatchedAtom.stream()
                                                .filter(n -> n.atom.getAtomState() == AtomState.ACTIVE)
                                                .filter(inverseMatch -> postFilter(matchedAtom.atom, inverseMatch.atom))
                                                .collect(Collectors.toList()));
                            }).map(entry -> produceHints(entry.getKey(), entry.getValue()))
                            .flatMap(hints -> hints.stream()).collect(Collectors.toList());
            // now that we've collected all inverse hints, publish them
            publishHintEvents(inverseHintEvents, atom.getAtomUri(), true);
        }
        log.debug("finished sparql-based matching for atom {}", atom.getAtomUri());
    }

    private Collection<HintEvent> produceHints(AtomModelWrapper atom, List<ScoredAtom> matches) {
        // find max score
        Optional<Double> maxScore = matches.stream().map(n -> n.score).max((x, y) -> (int) Math.signum(x - y));
        if (!maxScore.isPresent()) {
            // this should not happen
            return Collections.EMPTY_LIST;
        }
        // find min score
        Optional<Double> minScore = matches.stream().map(n -> n.score).min((x, y) -> (int) Math.signum(x - y));
        if (!maxScore.isPresent()) {
            // this should not happen
            return Collections.EMPTY_LIST;
        }
        double range = (maxScore.get() - minScore.get());
        return matches.stream().sorted((hint1, hint2) -> (int) Math.signum(hint2.score - hint1.score)) // sort
                                                                                                       // descending
                        .limit(config.getLimitResults()).map(hint -> {
                            double score = range == 0 ? 1.0 : (hint.score - minScore.get()) / range;
                            return new HintEvent(atom.getWonNodeUri(), atom.getAtomUri(), hint.atom.getWonNodeUri(),
                                            hint.atom.getAtomUri(), config.getMatcherUri(), score);
                        }).collect(Collectors.toList());
    }

    /**
     * publishes the specified hint events.
     * 
     * @param hintEvents the collection of HintEvent to publish
     * @param atomURI used for logging
     * @param inverse used for logging
     */
    private void publishHintEvents(Collection<HintEvent> hintEvents, String atomURI, boolean inverse) {
        BulkHintEvent bulkHintEvent = new BulkHintEvent();
        bulkHintEvent.addHintEvents(hintEvents);
        pubSubMediator.tell(new DistributedPubSubMediator.Publish(bulkHintEvent.getClass().getName(), bulkHintEvent),
                        getSelf());
        log.debug("sparql-based " + (inverse ? "inverse " : "") + "matching for atom {} (found {} matches)", atomURI,
                        bulkHintEvent.getHintEvents().size());
    }

    private Optional<Op> clientSuppliedQuery(String queryString) {
        Query query = QueryFactory.create(queryString);
        if (query.getQueryType() != Query.QueryTypeSelect) {
            return Optional.empty();
        }
        if (!query.getProjectVars().contains(resultName)) {
            return Optional.empty();
        }
        Op op = Algebra.compile(query);
        return Optional.of(op);
    }

    private Optional<Op> defaultQuery(AtomModelWrapper atom) {
        Model model = atom.getAtomModel();
        String atomURI = atom.getAtomUri();
        ArrayList<Op> queries = new ArrayList<>(3);
        Statement seeks = model.getProperty(model.createResource(atomURI),
                        model.createProperty("https://w3id.org/won/core#seeks"));
        if (seeks != null) {
            Op seeksQuery = createAtomQuery(model, seeks);
            if (seeksQuery != null)
                queries.add(seeksQuery);
        }
        Statement search = model.getProperty(model.createResource(atomURI),
                        model.createProperty("https://w3id.org/won/core#hasSearchString"));
        if (search != null) {
            String searchString = search.getString();
            queries.add(SparqlMatcherUtils.createSearchQuery(searchString, resultName, 2, true, true));
        }
        return queries.stream().reduce((left, right) -> new OpUnion(left, right))
                        .map((union) -> new OpDistinct(new OpProject(union, Arrays.asList(new Var[] { resultName }))));
    }

    private List<ScoredAtom> queryAtom(AtomModelWrapper atom) {
        return queryAtom(atom, Optional.empty());
    }

    /**
     * Query for matches to the atom, optionally the atomToCheck is used to search
     * in. If atomToCheck is passed, it is used as the result data iff the
     * atomToCheck is a match for atom. This saves us a linked data lookup for data
     * we already have.
     * 
     * @param atom
     * @param atomToCheck
     * @return
     */
    private List<ScoredAtom> queryAtom(AtomModelWrapper atom, Optional<AtomModelWrapperAndDataset> atomToCheck) {
        Optional<Op> query;
        Optional<String> userQuery = atom.getQuery();
        if (userQuery.isPresent()) {
            query = clientSuppliedQuery(userQuery.get());
        } else {
            query = defaultQuery(atom);
        }
        List<ScoredAtom> atoms = query.map(q -> {
            if (log.isDebugEnabled()) {
                log.debug("transforming query, adding 'no hint for counterpart' restriction: {}", q);
            }
            Op noHintForCounterpartQuery = SparqlMatcherUtils.noHintForCounterpartQuery(q, resultName);
            if (log.isDebugEnabled()) {
                log.debug("transformed query: {}", noHintForCounterpartQuery);
                log.debug("transforming query, adding 'wihout no hint for counterpart' restriction: {}", q);
            }
            Op hintForCounterpartQuery = SparqlMatcherUtils.hintForCounterpartQuery(q, resultName);
            if (log.isDebugEnabled()) {
                log.debug("transformed query: {}", hintForCounterpartQuery);
            }
            return Stream.concat(executeQuery(noHintForCounterpartQuery, atomToCheck, atom.getAtomUri()),
                            executeQuery(hintForCounterpartQuery, atomToCheck, atom.getAtomUri())).collect(Collectors.toList());
        }).orElse(Collections.emptyList());
        return atoms;
    }

    /**
     * Executes the query, optionally only searching in the atomToCheck.
     * 
     * @param q
     * @param atomToCheck 
     * @param atomURI - the URI of the atom we are matching for
     * @return
     */
    private Stream<ScoredAtom> executeQuery(Op q, Optional<AtomModelWrapperAndDataset> atomToCheck, String atomURI) {
        Query compiledQuery = OpAsQuery.asQuery(q);
        // if we were given an atomToCheck, restrict the query result to that uri so
        // that
        // we get exactly one result if that uri is found for the atom
        List<Binding> valuesBlockBindings = new ArrayList<>();
        List<Var> valuesBlockVariables = new ArrayList<>();
        //bind the ?thisAtom variable to the atom we are matching for
        valuesBlockBindings.add(BindingFactory.binding(thisAtom, new ResourceImpl(atomURI.toString()).asNode()));
        valuesBlockVariables.add(thisAtom);
        
        if (atomToCheck.isPresent()) {
            valuesBlockBindings.add(BindingFactory.binding(resultName,
                            new ResourceImpl(atomToCheck.get().atomModelWrapper.getAtomUri()).asNode()));
            valuesBlockVariables.add(resultName);
        }
        compiledQuery.setValuesDataBlock(valuesBlockVariables, valuesBlockBindings);
        // make sure we order by score, if present, and we limit the results
        if (compiledQuery.getProjectVars().contains(scoreName)) {
            compiledQuery.addOrderBy(scoreName, Query.ORDER_DESCENDING);
        }
        if (!compiledQuery.hasLimit() || compiledQuery.getLimit() > config.getLimitResults() * 5) {
            compiledQuery.setLimit(config.getLimitResults() * 5);
        }
        compiledQuery.setOffset(0);
        compiledQuery.setDistinct(true);
        if (log.isDebugEnabled()) {
            log.debug("executeQuery query: {}, atomToCheck: {}", new Object[] { compiledQuery, atomToCheck });
        }
        List<ScoredAtomUri> foundUris = new LinkedList<>();
        // process query results iteratively
        try (QueryExecution execution = QueryExecutionFactory.sparqlService(config.getSparqlEndpoint(),
                        compiledQuery)) {
            ResultSet result = execution.execSelect();
            while (result.hasNext()) {
                QuerySolution querySolution = result.next();
                RDFNode atomUriNode = querySolution.get(resultName.getName());
                if (atomUriNode == null || !atomUriNode.isURIResource()) {
                    continue;
                }
                String foundAtomURI = atomUriNode.asResource().getURI();
                double score = 1.0;
                if (querySolution.contains(scoreName.getName())) {
                    RDFNode scoreNode = querySolution.get(scoreName.getName());
                    if (scoreNode != null && scoreNode.isLiteral()) {
                        try {
                            score = scoreNode.asLiteral().getDouble();
                        } catch (NumberFormatException e) {
                            // if the score is not interpretable as double, ignore it
                        }
                    }
                }
                foundUris.add(new ScoredAtomUri(foundAtomURI, score));
            }
        } catch (Exception e) {
            log.info("caught exception during sparql-based matching (more info on loglevel 'debug'): {} ",
                            e.getMessage());
            if (log.isDebugEnabled()) {
                e.printStackTrace();
            }
            return Stream.empty();
        }
        // load data in parallel
        return foundUris.parallelStream().map(foundAtomUri -> {
            try {
                // if we have an atomToCheck, return it if the URI we found actually is its URI,
                // otherwise null
                if ((atomToCheck.isPresent())) {
                    AtomModelWrapper result = atomToCheck.get().atomModelWrapper.getAtomUri().equals(foundAtomUri.uri)
                                    ? atomToCheck.get().atomModelWrapper
                                    : null;
                    if (result == null) {
                        return null;
                    }
                    return new ScoredAtom(result, foundAtomUri.score);
                } else {
                    // no atomToCheck, which happens when we first look for matches in the graph
                    // store:
                    // download the linked data and return a new AtomModelWrapper
                    Dataset ds = linkedDataSource.getDataForResource(URI.create(foundAtomUri.uri));
                    // make sure we don't accidentally use empty or faulty results
                    if (!AtomModelWrapper.isAAtom(ds)) {
                        return null;
                    }
                    return new ScoredAtom(new AtomModelWrapper(ds), foundAtomUri.score);
                }
            } catch (Exception e) {
                log.info("caught exception trying to load atom URI {} : {} (more on loglevel 'debug')", foundAtomUri,
                                e.getMessage());
                if (log.isDebugEnabled()) {
                    e.printStackTrace();
                }
                return null;
            }
        }).filter(foundAtom -> foundAtom != null);
    }

    private static Set<String> getMatchingContexts(AtomModelWrapper atom) {
        Model model = atom.getAtomModel();
        Resource atomURI = model.createResource(atom.getAtomUri());
        Property matchingContextProperty = model.createProperty("https://w3id.org/won/core#matchingContext");
        Stream<RDFNode> stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                        model.listObjectsOfProperty(atomURI, matchingContextProperty), Spliterator.CONCURRENT), false);
        return stream.map(node -> node.asLiteral().getString()).collect(Collectors.toSet());
    }

    private boolean postFilter(AtomModelWrapper atom, AtomModelWrapper foundAtom) {
        try {
            if (atom.getAtomUri().equals(foundAtom.getAtomUri())) {
                return false;
            }
            if (atom.flag(WON.NoHintForMe)) {
                return false;
            }
            if (foundAtom.flag(WON.NoHintForCounterpart)) {
                return false;
            }
            Set<String> atomContexts = getMatchingContexts(atom);
            if (!atomContexts.isEmpty()) {
                Set<String> foundAtomContexts = getMatchingContexts(foundAtom);
                foundAtomContexts.retainAll(atomContexts);
                if (foundAtomContexts.isEmpty()) {
                    return false;
                }
            }
            Calendar now = Calendar.getInstance();
            if (now.after(foundAtom.getDoNotMatchAfter()))
                return false;
            if (now.before(foundAtom.getDoNotMatchBefore()))
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

    private class AtomModelWrapperAndDataset {
        private AtomModelWrapper atomModelWrapper;
        private Dataset dataset;

        public AtomModelWrapperAndDataset(AtomModelWrapper atomModelWrapper, Dataset dataset) {
            super();
            this.atomModelWrapper = atomModelWrapper;
            this.dataset = dataset;
        }
    }

    private class ScoredAtom {
        private AtomModelWrapper atom;
        private double score;

        public ScoredAtom(AtomModelWrapper atom, double score) {
            super();
            this.atom = atom;
            this.score = score;
        }
    }

    private class ScoredAtomUri {
        private String uri;
        private double score;

        public ScoredAtomUri(String uri, double score) {
            super();
            this.uri = uri;
            this.score = score;
        }
    }
}
