package won.node.springsecurity.acl;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.sparql.graph.GraphFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.util.StopWatch;
import won.auth.AuthUtils;
import won.auth.WonAclEvaluator;
import won.auth.check.AtomNodeChecker;
import won.auth.check.TargetAtomCheckEvaluator;
import won.auth.model.AclEvalResult;
import won.auth.model.DecisionValue;
import won.auth.model.OperationRequest;
import won.auth.model.TokenOperationExpression;
import won.auth.model.TokenSpecification;
import won.cryptography.rdfsign.WebIdKeyLoader;
import won.cryptography.service.CryptographyService;
import won.node.service.nodeconfig.URIService;
import won.node.service.persistence.AtomService;
import won.node.springsecurity.WebIdUserDetails;
import won.node.springsecurity.WonDefaultAccessControlRules;
import won.protocol.model.Atom;
import won.protocol.model.Connection;
import won.protocol.model.DatasetHolder;
import won.protocol.model.MessageEvent;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.DatasetHolderRepository;
import won.protocol.repository.MessageEventRepository;
import won.protocol.util.WonMessageUriHelper;

import static won.auth.AuthUtils.cloneShallow;
import static won.auth.AuthUtils.toAuthAtomState;
import static won.auth.AuthUtils.toAuthConnectionState;
import static won.auth.model.Individuals.OP_READ;
import static won.auth.model.Individuals.POSITION_ATOM_MESSAGE;
import static won.auth.model.Individuals.POSITION_ATOM_MESSAGES;
import static won.auth.model.Individuals.POSITION_CONNECTION;
import static won.auth.model.Individuals.POSITION_CONNECTIONS;
import static won.auth.model.Individuals.POSITION_CONNECTION_MESSAGE;
import static won.auth.model.Individuals.POSITION_CONNECTION_MESSAGES;
import static won.auth.model.Individuals.POSITION_ROOT;

public class WonAclAccessDecisionVoter implements AccessDecisionVoter, InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    // the aclEvaluator is not thread-safe, each thread needs its own.
    private ThreadLocal<WonAclEvaluator> aclEvaluator;
    @Autowired
    private TargetAtomCheckEvaluator targetAtomCheckEvaluator;
    @Autowired
    private AtomNodeChecker atomNodeChecker;
    @Autowired
    private WebIdKeyLoader webIdKeyLoader;
    @Autowired
    private DatasetHolderRepository datasetHolderRepository;
    @Autowired
    private URIService uriService;
    @Autowired
    private MessageEventRepository messageEventRepository;
    @Autowired
    private ConnectionRepository connectionRepository;
    @Autowired
    private AtomService atomService;
    @Autowired
    private WonDefaultAccessControlRules defaultAccessControlRules;
    @Autowired
    private CryptographyService cryptographyService;
    private Shapes aclShapes;
    private Graph aclShapesGraph;

    public WonAclAccessDecisionVoter() {
    }

    @Override
    public boolean supports(final ConfigAttribute attribute) {
        return true;
    }

    @Override
    public boolean supports(final Class clazz) {
        return FilterInvocation.class.equals(clazz);
    }

    @Override
    @Transactional
    public int vote(final Authentication authentication, final Object object,
                    final Collection collection) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        if (!(authentication instanceof PreAuthenticatedAuthenticationToken)) {
            return ACCESS_ABSTAIN;
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof WebIdUserDetails)) {
            return ACCESS_ABSTAIN;
        }
        WebIdUserDetails userDetails = (WebIdUserDetails) principal;
        if (!(object instanceof FilterInvocation)) {
            return ACCESS_ABSTAIN;
        }
        String webId = userDetails.getUsername();
        FilterInvocation filterInvocation = (FilterInvocation) object;
        String resource = filterInvocation.getRequest().getRequestURL().toString();
        URI resourceUri = null;
        try {
            resourceUri = uriService.toResourceURIIfPossible(new URI(resource));
        } catch (URISyntaxException e) {
            logger.debug("Cannot process ACL for resource {}", resource);
            return ACCESS_DENIED;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Processing WoN ACL for request to resource {}", resourceUri);
        }
        int result = ACCESS_DENIED;
        if (authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                        .filter(r -> "ROLE_WEBID".equals(r)).findAny().isPresent()) {
            // perform our hard coded access control checks
            List<String> webIDs = new ArrayList<>(1);
            webIDs.add(webId);
            // prepare the legacy implementation in case the target atom(s) have no acl
            // graph
            Supplier<Integer> legacyImpl = () -> {
                if (defaultAccessControlRules.isAccessPermitted(resource, webIDs)) {
                    return ACCESS_GRANTED;
                }
                return ACCESS_DENIED;
            };
            if (WonMessageUriHelper.isLocalMessageURI(resourceUri,
                            uriService.getMessageResourceURIPrefix())) {
                // handle request for message
                result = voteForMessageRequest(webId, resourceUri, filterInvocation, legacyImpl);
            } else {
                // handle other requests
                result = voteForNonMessageRequest(webId, resourceUri, filterInvocation, legacyImpl);
            }
        }
        stopWatch.stop();
        if (logger.isDebugEnabled()) {
            logger.debug("access control check took {} millis, result: {} ",
                            stopWatch.getLastTaskTimeMillis(),
                            (result == ACCESS_GRANTED ? "granted"
                                            : (result == ACCESS_DENIED ? "denied"
                                                            : (result == ACCESS_ABSTAIN ? "abstain" : result))));
        }
        return result;
    }

    public Graph getAclGraphOfAtom(Atom atom) {
        DatasetHolder atomDatasetHolder = atom.getDatatsetHolder();
        URI aclGraphUri = uriService.createAclGraphURIForAtomURI(atom.getAtomURI());
        // caution: at least some dataset implementations will create a new model
        // in the dataset if getNamedModel(name) is called and there is no model for
        // 'name' yet.
        if (!atomDatasetHolder.getDataset().containsNamedModel(aclGraphUri.toString())) {
            return null;
        }
        Model model = atomDatasetHolder.getDataset()
                        .getNamedModel(aclGraphUri.toString());
        if (model == null) {
            return null;
        }
        return model.getGraph();
    }

    public int voteForMessageRequest(String webId, URI resourceUri, FilterInvocation filterInvocation,
                    Supplier<Integer> legacyImpl) {
        // if we're requesting a message, we have to check access for each message
        // container
        // that it is in
        Map<URI, Set<OperationRequest>> opReqs = new HashMap<>();
        Map<URI, Graph> aclGraphs = new HashMap<>();
        Map<URI, Integer> legacyResults = new HashMap<>();
        URI messageUri = WonMessageUriHelper.toGenericMessageURI(resourceUri,
                        uriService.getMessageResourceURIPrefix());
        List<MessageEvent> msgs = messageEventRepository.findByMessageURI(messageUri);
        for (MessageEvent msg : msgs) {
            URI parent = msg.getParentURI();
            URI atomUri = uriService.getAtomURIofSubURI(parent);
            Optional<Atom> atom = atomService.getAtom(atomUri);
            if (!atom.isPresent()) {
                return ACCESS_DENIED;
            }
            if (!aclGraphs.containsKey(atomUri)) {
                Graph aclGraph = getAclGraphOfAtom(atom.get());
                if (aclGraph == null) {
                    legacyResults.put(atomUri, legacyImpl.get());
                    continue;
                }
                aclGraphs.put(atomUri, aclGraph);
            }
            if (!atom.isPresent()) {
                continue;
            }
            OperationRequest operationRequest = new OperationRequest();
            operationRequest.setReqAtomState(toAuthAtomState(atom.get().getState()));
            operationRequest.setRequestor(URI.create(webId));
            operationRequest.setReqAtom(atomUri);
            operationRequest.setOperationSimpleOperationExpression(OP_READ);
            if (uriService.isConnectionURI(parent)) {
                Optional<Connection> con = connectionRepository.findOneByConnectionURI(parent);
                if (con == null) {
                    continue;
                }
                operationRequest.setReqPosition(POSITION_CONNECTION_MESSAGE);
                operationRequest.setReqConnection(con.get().getConnectionURI());
                operationRequest.setReqSocket(con.get().getSocketURI());
                operationRequest.setReqSocketType(con.get().getTypeURI());
                operationRequest.setReqConnectionMessage(msg.getMessageURI());
                operationRequest.setReqConnectionState(toAuthConnectionState(con.get().getState()));
            } else if (uriService.isAtomURI(parent)) {
                operationRequest.setReqPosition(POSITION_ATOM_MESSAGE);
            } else {
                legacyResults.put(atomUri, legacyImpl.get());
                continue;
            }
            if (!opReqs.containsKey(atomUri)) {
                Set<OperationRequest> ors = new HashSet<>();
                ors.add(operationRequest);
                opReqs.put(atomUri, ors);
            } else {
                opReqs.get(atomUri).add(operationRequest);
            }
        }
        Set<AclEvalResult> aclEvalResults = new HashSet<>();
        for (URI atomUri : aclGraphs.keySet()) {
            Graph aclGraph = aclGraphs.get(atomUri);
            WonAclEvaluator evaluator = getWonAclEvaluator();
            evaluator.loadData(aclGraph);
            for (OperationRequest opReq : opReqs.get(atomUri)) {
                aclEvalResults.add(evaluator.decide(opReq));
            }
        }
        Optional<AclEvalResult> aclEvalResult = aclEvalResults.stream().reduce(WonAclEvaluator::mergeAclEvalResults);
        Integer legacyResult = legacyResults.values().stream().reduce((left, right) -> {
            if (left.equals(right)) {
                return left;
            }
            if (left.equals(ACCESS_GRANTED) || right.equals(ACCESS_GRANTED)) {
                return ACCESS_GRANTED;
            } else if (left.equals(ACCESS_ABSTAIN) || right.equals(ACCESS_ABSTAIN)) {
                return ACCESS_ABSTAIN;
            }
            return ACCESS_DENIED;
        }).orElse(ACCESS_ABSTAIN);
        if (legacyResult.equals(ACCESS_GRANTED)
                        || (aclEvalResult.isPresent()
                                        && aclEvalResult.get().getDecision().equals(DecisionValue.ACCESS_GRANTED))) {
            return ACCESS_GRANTED;
        } else {
            return ACCESS_DENIED;
        }
    }

    public int voteForNonMessageRequest(String webId, URI resourceUri,
                    FilterInvocation filterInvocation, Supplier<Integer> legacyImpl) {
        URI atomUri = uriService.getAtomURIofSubURI(resourceUri);
        if (atomUri == null) {
            logger.debug("Cannot process ACL for resource {}", resourceUri);
            return ACCESS_DENIED;
        }
        if (!uriService.isAtomURI(atomUri)) {
            logger.debug("Not an atom URI: {}", atomUri);
            return ACCESS_DENIED;
        }
        Optional<Atom> atom = atomService.getAtom(atomUri);
        if (!atom.isPresent()) {
            return ACCESS_DENIED;
        }
        Graph aclGraph = getAclGraphOfAtom(atom.get());
        if (aclGraph == null) {
            // fall back to legacy implementation
            WonAclRequestHelper.setWonAclEvaluationContext(
                            filterInvocation.getRequest(),
                            WonAclEvalContext.allowAll());
            return legacyImpl.get();
        }
        if (!atom.isPresent()) {
            return ACCESS_DENIED;
        }
        // set up request object
        OperationRequest request = new OperationRequest();
        request.setReqAtom(atomUri);
        request.setReqAtomState(toAuthAtomState(atom.get().getState()));
        request.setRequestor(URI.create(webId));
        request.setOperationSimpleOperationExpression(OP_READ);
        if (uriService.isAtomURI(resourceUri)) {
            request.setReqPosition(POSITION_ROOT);
            // we are going to need the acl evaluator to choose the graphs we can show
            // this can only be done by the logic that assembles the response dataset
            // approach: allow the request for now, then check acl for each subgraph
            WonAclRequestHelper.setWonAclEvaluationContext(
                            filterInvocation.getRequest(),
                            WonAclEvalContext.contentFilter(request, detachWonAclEvaluator()));
            return ACCESS_GRANTED;
        } else if (uriService.isAtomMessagesURI(resourceUri)) {
            request.setReqPosition(POSITION_ATOM_MESSAGES);
        } else if (uriService.isConnectionMessagesURI(resourceUri)) {
            request.setReqPosition(POSITION_CONNECTION_MESSAGES);
            Optional<Connection> con = connectionRepository.findOneByConnectionURI(
                            uriService.getConnectionURIofConnectionMessagesURI(
                                            resourceUri));
            if (!con.isPresent()) {
                return ACCESS_DENIED;
            }
            request.setReqSocketType(con.get().getTypeURI());
            request.setReqSocket(con.get().getSocketURI());
            request.setReqConnection(con.get().getConnectionURI());
            request.setReqConnectionState(toAuthConnectionState(con.get().getState()));
        } else if (uriService.isConnectionURI(resourceUri)) {
            request.setReqPosition(POSITION_CONNECTION);
            Optional<Connection> con = connectionRepository.findOneByConnectionURI(
                            uriService.getConnectionURIofConnectionMessagesURI(
                                            resourceUri));
            if (!con.isPresent()) {
                return ACCESS_DENIED;
            }
            request.setReqSocketType(con.get().getTypeURI());
            request.setReqSocket(con.get().getSocketURI());
            request.setReqConnection(con.get().getConnectionURI());
            request.setReqConnectionState(toAuthConnectionState(con.get().getState()));
        } else if (uriService.isConnectionContainerURI(resourceUri)) {
            // We want to enable requesting connections per socket but haven't got
            // the LD service implementation for that, yet. Moreover, a lot of code relies
            // on the legacy implementation. So, we'll process requests
            // to the atom's connection container as a special case: prepare the
            // operationRequest now but evaluate it only when we are processing the
            // results. Then, we have to group the connections by socket/socket type,
            // generate the corresponding operationRequests, evaluate them and
            // remove all connections the requestor is not allowed to read.
            request.setReqPosition(POSITION_CONNECTIONS);
            // consequence: allow now, check later
            WonAclRequestHelper.setWonAclEvaluationContext(
                            filterInvocation.getRequest(),
                            WonAclEvalContext.contentFilter(request, detachWonAclEvaluator()));
            return ACCESS_GRANTED;
        } else if (uriService.isTokenEndpointURI(resourceUri)) {
            // token request
            request.setReqPosition(POSITION_ROOT);
            String scopeParam = WonAclRequestHelper.getRequestParamScope(filterInvocation.getRequest());
            Set<OperationRequest> opReqs = new HashSet<>();
            if (scopeParam == null || scopeParam.trim().length() == 0) {
                TokenSpecification tokenSpec = new TokenSpecification();
                TokenOperationExpression opex = new TokenOperationExpression();
                opex.setRequestToken(tokenSpec);
                request.setOperationSimpleOperationExpression(null);
                request.setOperationTokenOperationExpression(opex);
                opReqs.add(request);
            } else {
                String[] scopes = scopeParam.split("\\s+");
                for (int i = 0; i < scopes.length; i++) {
                    TokenSpecification tokenSpec = new TokenSpecification();
                    URI tokenScopeUri = null;
                    try {
                        tokenScopeUri = new URI(scopes[i]);
                    } catch (URISyntaxException e) {
                    }
                    if (tokenScopeUri != null) {
                        tokenSpec.setTokenScopeURI(tokenScopeUri);
                    } else {
                        tokenSpec.setTokenScopeString(scopes[i]);
                    }
                    TokenOperationExpression opex = new TokenOperationExpression();
                    opex.setRequestToken(tokenSpec);
                    OperationRequest clonedRequest = cloneShallow(request);
                    clonedRequest.setOperationTokenOperationExpression(opex);
                    clonedRequest.setOperationSimpleOperationExpression(null);
                    opReqs.add(clonedRequest);
                }
                getWonAclEvaluator().loadData(aclGraph);
                Set<AclEvalResult> evalResults = new HashSet<>();
                for (OperationRequest req : opReqs) {
                    AclEvalResult result = getWonAclEvaluator().decide(req);
                    evalResults.add(result);
                }
                AclEvalResult finalResult = evalResults.stream().reduce(WonAclEvaluator::mergeAclEvalResults).get();
                if (DecisionValue.ACCESS_GRANTED.equals(finalResult.getDecision())) {
                    Set<String> tokens = finalResult
                                    .getIssueTokens()
                                    .stream()
                                    .map(token -> AuthUtils.toJWT(token,
                                                    cryptographyService.getDefaultPrivateKey(),
                                                    cryptographyService.getDefaultPrivateKeyAlias()))
                                    .collect(Collectors.toSet());
                    WonAclRequestHelper.setGrantedTokens(filterInvocation.getRequest(), tokens);
                }
                setAuthInfoIfDenied(filterInvocation, finalResult);
                return toAccessDecisionVote(finalResult);
            }
        } else {
            // default
            request.setReqPosition(POSITION_ROOT);
        }
        WonAclEvaluator evaluator = getWonAclEvaluator();
        evaluator.loadData(aclGraph);
        AclEvalResult result = evaluator.decide(request);
        setAuthInfoIfDenied(filterInvocation, result);
        return toAccessDecisionVote(result);
    }

    public void setAuthInfoIfDenied(FilterInvocation filterInvocation, AclEvalResult finalResult) {
        if (DecisionValue.ACCESS_DENIED.equals(finalResult.getDecision())
                        && finalResult.getProvideAuthInfo() != null) {
            WonAclRequestHelper.setAuthInfoAsResponseHeader(filterInvocation, finalResult);
        }
    }

    public int toAccessDecisionVote(AclEvalResult result) {
        if (DecisionValue.ACCESS_GRANTED.equals(result.getDecision())) {
            return ACCESS_GRANTED;
        } else {
            return ACCESS_DENIED;
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Resource res = new ClassPathResource("shacl/won-auth-shapes.ttl");
        aclShapesGraph = GraphFactory.createGraphMem();
        RDFDataMgr.read(aclShapesGraph, res.getInputStream(), Lang.TTL);
        aclShapes = Shapes.parse(aclShapesGraph);
    }

    private WonAclEvaluator getWonAclEvaluator() {
        WonAclEvaluator evaluator = this.aclEvaluator.get();
        if (evaluator == null) {
            evaluator = new WonAclEvaluator(
                            this.aclShapes,
                            this.targetAtomCheckEvaluator,
                            this.atomNodeChecker,
                            webIdKeyLoader);
        }
        return evaluator;
    }

    /**
     * Obtains the current thread-local instance of the WonAclEvaluator and removes
     * it from the threadLocal. In some cases, the evaluator is needed at a later
     * stage of request processing. It makes sense to use the instance already
     * prepared for this usage instead of re-creating one later, this is how to
     * obtain it in this case.
     *
     * @return
     */
    private WonAclEvaluator detachWonAclEvaluator() {
        WonAclEvaluator evaluator = getWonAclEvaluator();
        this.aclEvaluator.remove();
        return evaluator;
    }
}
