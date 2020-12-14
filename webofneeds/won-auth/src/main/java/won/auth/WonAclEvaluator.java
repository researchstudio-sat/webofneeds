package won.auth;

import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.graph.Graph;
import org.apache.jena.shacl.Shapes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.auth.check.TargetAtomCheck;
import won.auth.check.TargetAtomCheckEvaluator;
import won.auth.model.*;
import won.shacl2java.Shacl2JavaInstanceFactory;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static won.auth.model.DecisionValue.ACCESS_DENIED;
import static won.auth.model.DecisionValue.ACCESS_GRANTED;
import static won.auth.model.GranteeWildcard.ANYONE;
import static won.auth.model.RelativeAtomExpression.ANY_ATOM;
import static won.auth.model.RelativeAtomExpression.SELF;

public class WonAclEvaluator {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public static final long DEFAULT_TOKEN_EXPIRES_AFTER_SECONDS = 3600;
    private TargetAtomCheckEvaluator targetAtomCheckEvaluator;
    private Shacl2JavaInstanceFactory instanceFactory;

    public WonAclEvaluator(Shapes shapes, TargetAtomCheckEvaluator targetAtomCheckEvaluator) {
        this.targetAtomCheckEvaluator = targetAtomCheckEvaluator;
        this.instanceFactory = new Shacl2JavaInstanceFactory(shapes, "won.auth.model");
    }

    public void loadData(Graph dataGraph) {
        this.instanceFactory.load(dataGraph);
    }

    public Set<Authorization> getAuthorizations() {
        return this.instanceFactory
                        .getInstancesOfType(Authorization.class);
    }

    public Set<OperationRequest> getOperationRequests() {
        return this.instanceFactory.getInstancesOfType(OperationRequest.class);
    }

    public AclEvalResult decide(Set<Authorization> authorizations, OperationRequest request) {
        Set<Authorization> requestorIsGranteeOf = new HashSet<>();
        boolean grantAuthInfo = canProvideAuthInfo(authorizations, request,
                        requestorIsGranteeOf);
        Optional<AclEvalResult> result = authorizations.stream()
                        .map(auth -> decide(auth, request, grantAuthInfo, requestorIsGranteeOf))
                        .reduce((left, right) -> {
                            AclEvalResult merged = new AclEvalResult();
                            if (ACCESS_GRANTED.equals(left.getDecision())
                                            || ACCESS_GRANTED.equals(right.getDecision())) {
                                merged.setDecision(ACCESS_GRANTED);
                            } else {
                                merged.setDecision(ACCESS_DENIED);
                            }
                            HashSet<AuthToken> tokens = new HashSet<>();
                            tokens.addAll(left.getIssueTokens());
                            tokens.addAll(right.getIssueTokens());
                            merged.setIssueTokens(tokens);
                            if (ACCESS_DENIED.equals(merged.getDecision())) {
                                merged.setProvideAuthInfo(
                                                merge(left.getProvideAuthInfo(),
                                                                right.getProvideAuthInfo()));
                            }
                            return merged;
                        });
        return result.orElse(accessControlDecision(false, request));
    }

    private AuthInfo merge(AuthInfo left, AuthInfo right) {
        AuthInfo merged = new AuthInfo();
        copyAuthInfo(left, merged);
        copyAuthInfo(right, merged);
        return merged;
    }

    private void copyAuthInfo(AuthInfo from, AuthInfo to) {
        if (from != null) {
            for (TokenShape bearer : from.getBearers()) {
                to.addBearer(bearer);
            }
            for (AseRoot grantee : from.getGranteesAseRoot()) {
                to.addGranteesAseRoot(grantee);
            }
            if (from.getGranteeGranteeWildcard() != null) {
                to.setGranteeGranteeWildcard(from.getGranteeGranteeWildcard());
            }
        }
    }

    private AclEvalResult accessControlDecision(boolean accessGranted, OperationRequest request) {
        AclEvalResult result = new AclEvalResult();
        result.setDecision(accessGranted ? ACCESS_GRANTED : ACCESS_DENIED);
        result.setRequestedOperation(request);
        return result;
    }

    public boolean provideAuthInfo(Set<Authorization> authorizations,
                    OperationRequest request) {
        return canProvideAuthInfo(authorizations, request, Collections.emptySet());
    }

    private boolean canProvideAuthInfo(Set<Authorization> authorizations, OperationRequest request,
                    Set<Authorization> requestorIsGranteeOf) {
        return authorizations.stream()
                        .filter(authorization -> authorization.getProvideAuthInfo() != null)
                        .filter(authorization -> isRequestorAGrantee(authorization, request)
                                        || isRequestorBearerOfAcceptedToken(authorization, request))
                        .map(authorization -> {
                            if (logger.isDebugEnabled()) {
                                logger.debug("evaluating provideAuthInfo for {}", authorization);
                            }
                            requestorIsGranteeOf.add(authorization); // side effect!
                            return authorization;
                        })
                        .anyMatch(authorization -> {
                            boolean ret = canProvideAuthInfo(authorization, request);
                            if (logger.isDebugEnabled() && ret) {
                                logger.debug("providing auth info for {} ", authorization);
                            }
                            return ret;
                        });
    }

    private static boolean canProvideAuthInfo(Authorization authorization, OperationRequest request) {
        AseRoot root = authorization.getProvideAuthInfo();
        OperationRequestChecker operationRequestChecker = new OperationRequestChecker(request);
        root.accept(operationRequestChecker);
        boolean finalDecision = operationRequestChecker.getFinalDecision();
        debug("providing auth info : {}", authorization, request, finalDecision);
        if (finalDecision) {
            return true;
        }
        return false;
    }

    public AclEvalResult decide(Authorization authorization, OperationRequest request,
                    boolean provideAuthInfo) {
        return decide(authorization, request, provideAuthInfo, Collections.emptySet());
    }

    private AclEvalResult decide(Authorization authorization, OperationRequest request,
                    boolean provideAuthInfo, Set<Authorization> requestorIsGranteeOf) {
        // determine if the requestor is in the set of grantees
        boolean decisionSoFar = false;
        if (requestorIsGranteeOf.contains(authorization)) {
            decisionSoFar = true;
        } else {
            if (isRequestorAGrantee(authorization, request)) {
                debug("requestor {} is grantee", authorization, request, request.getRequestor());
                decisionSoFar = true;
            } else {
                debug("requestor {} is not grantee", authorization, request, request.getRequestor());
                if (isRequestorBearerOfAcceptedToken(authorization, request)) {
                    debug("requestor {} has an accepted token", authorization, request, request.getRequestor());
                    decisionSoFar = true;
                } else {
                    debug("requestor {} does not have an accepted token", authorization, request,
                                    request.getRequestor());
                    decisionSoFar = false;
                }
            }
        }
        AuthInfo authInfo = null;
        if (decisionSoFar || provideAuthInfo) {
            // determine if the operation is granted
            if (isOperationGranted(authorization, request)) {
                debug("operation is granted", authorization, request);
                if (provideAuthInfo) {
                    authInfo = new AuthInfo();
                    authInfo.setBearers(authorization.getBearers());
                    authInfo.setGranteesAseRoot(authorization.getGranteesAseRoot());
                    authInfo.setGranteeGranteeWildcard(authorization.getGranteeGranteeWildcard());
                }
            } else {
                decisionSoFar = false;
            }
        }
        debug("operation is not granted", authorization, request);
        return accessControlDecision(decisionSoFar, authorization, request, authInfo);
    }

    private static AclEvalResult accessControlDecision(boolean accessGranted, Authorization authorization,
                    OperationRequest request, AuthInfo authInfo) {
        AclEvalResult acd = new AclEvalResult();
        if (!accessGranted) {
            acd.setDecision(DecisionValue.ACCESS_DENIED);
        } else {
            acd.setDecision(ACCESS_GRANTED);
        }
        if (!accessGranted && authInfo != null) {
            acd.setProvideAuthInfo(authInfo);
        }
        if (accessGranted) {
            for (TokenOperationExpression op : authorization.getGrants().stream()
                            .flatMap(root -> root.getOperationsTokenOperationExpression().stream())
                            .collect(Collectors.toSet())) {
                TokenSpecification tokenSpec = op.getRequestToken();
                AuthToken token = new AuthToken();
                Calendar cal = Calendar.getInstance();
                cal.setTime(new Date());
                XSDDateTime iat = new XSDDateTime(cal);
                if (tokenSpec.getExpiresAfterBigInteger() != null) {
                    cal.add(Calendar.SECOND, tokenSpec.getExpiresAfterBigInteger().intValueExact());
                } else if (tokenSpec.getExpiresAfterLong() != null) {
                    cal.add(Calendar.SECOND, tokenSpec.getExpiresAfterLong().intValue());
                } else if (tokenSpec.getExpiresAfterInteger() != null) {
                    cal.add(Calendar.SECOND, tokenSpec.getExpiresAfterInteger());
                }
                XSDDateTime exp = new XSDDateTime(cal);
                token.setTokenExp(exp);
                token.setTokenIat(iat);
                token.setTokenIss(request.getReqAtom());
                token.setTokenSub(request.getRequestor());
                if (tokenSpec.getTokenScopeURI() != null) {
                    token.setTokenScopeURI(tokenSpec.getTokenScopeURI());
                } else if (tokenSpec.getTokenScopeString() != null) {
                    token.setTokenScopeString(tokenSpec.getTokenScopeString());
                }
                acd.addIssueToken(token);
            }
        }
        return acd;
    }

    private static boolean isOperationGranted(Authorization authorization, OperationRequest request) {
        for (AseRoot root : authorization.getGrants()) {
            OperationRequestChecker operationRequestChecker = new OperationRequestChecker(request);
            root.accept(operationRequestChecker);
            boolean finalDecision = operationRequestChecker.getFinalDecision();
            debug("operation granted: {}", authorization, request, finalDecision);
            if (finalDecision) {
                return true;
            }
        }
        return false;
    }

    private boolean isRequestorAGrantee(Authorization authorization, OperationRequest request) {
        // fast check: check for direct referenct
        if (ANYONE.equals(authorization.getGranteeGranteeWildcard())) {
            debug("anyone is a grantee", authorization, request);
            return true;
        }
        URI requestor = request.getRequestor();
        for (AtomExpression grantee : authorization.getGranteesAtomExpression()) {
            debug("looking for grantee in atom expressions", authorization, request);
            if (grantee.getAtomsRelativeAtomExpression().contains(ANY_ATOM)) {
                debug("any atom is a grantee", authorization, request);
                return true;
            }
            if (grantee.getAtomsURI().contains(requestor)) {
                debug("requestor {} is listed explicitly as grantee", authorization, request, requestor);
                return true;
            }
        }
        debug("looking for grantee in atom structure expressions", authorization, request);
        for (AseRoot root : authorization.getGranteesAseRoot()) {
            URI baseAtom = request.getReqAtom();
            if (isTargetAtom(requestor, baseAtom, root))
                return true;
        }
        return false;
    }

    private boolean isTargetAtom(URI candidate, URI baseAtom, AseRoot aseRoot) {
        TargetAtomCheckGenerator v = new TargetAtomCheckGenerator(baseAtom, candidate);
        aseRoot.accept(v);
        for (TargetAtomCheck check : v.getTargetAtomChecks()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Evaluating targetAtomCheck: {}", check);
            }
            if (this.targetAtomCheckEvaluator.isRequestorAllowedTarget(check)) {
                return true;
            }
        }
        return false;
    }

    private boolean isRequestorBearerOfAcceptedToken(Authorization authorization, OperationRequest request) {
        Set<AuthToken> decoded = new HashSet<>();
        decoded.addAll(request.getBearsTokens());
        Set<String> encodedTokens = request.getBearsEncodedTokens();
        decoded.addAll(decodeAuthTokens(encodedTokens));
        decoded = decoded.stream()
                        .filter(token -> isTokenValid(token))
                        .collect(Collectors.toSet());
        debug("valid tokens: {}", authorization, request, decoded.size());
        if (decoded.size() > 0) {
            for (TokenShape tokenShape : authorization.getBearers()) {
                Set<AseRoot> aseRoots = tokenShape.getIssuersAseRoot();
                Set<AtomExpression> atomExpressions = tokenShape.getIssuersAtomExpression();
                Set<AuthToken> elegibleTokens = filterTokensByScope(decoded, tokenShape);
                debug("tokens with correct scope: {}", authorization, request, elegibleTokens.size());
                if (isIssuerAccepted(elegibleTokens, aseRoots, atomExpressions, request.getReqAtom())) {
                    return true;
                }
            }
        }
        return false;
    }

    private Set<AuthToken> filterTokensByScope(Set<AuthToken> decoded, TokenShape tokenShape) {
        Set<String> requiredScopeStrings = tokenShape.getTokenScopesString();
        Set<AuthToken> elegibleTokens = new HashSet<>();
        if (!requiredScopeStrings.isEmpty()) {
            Set<AuthToken> tokensWithcorrectScope = decoded.stream()
                            .filter(token -> token.getTokenScopeString() != null
                                            && requiredScopeStrings.contains(token.getTokenScopeString()))
                            .collect(Collectors.toSet());
            elegibleTokens.addAll(tokensWithcorrectScope);
        }
        Set<URI> requiredScopeIris = tokenShape.getTokenScopesURI();
        if (!requiredScopeIris.isEmpty()) {
            Set<AuthToken> tokensWithcorrectScope = decoded.stream()
                            .filter(token -> token.getTokenScopeURI() != null
                                            && requiredScopeIris.contains(token.getTokenScopeURI()))
                            .collect(Collectors.toSet());
            elegibleTokens.addAll(tokensWithcorrectScope);
        }
        return elegibleTokens;
    }

    private boolean isIssuerAccepted(Set<AuthToken> elegibleTokens, Set<AseRoot> aseRoots,
                    Set<AtomExpression> atomExpressions, URI baseAtom) {
        return elegibleTokens.stream().anyMatch(token -> {
            URI issuer = token.getTokenIss();
            if (isIssuerInAtomExpressions(baseAtom, atomExpressions, issuer)) {
                return true;
            }
            if (isIssuerATargetAtom(baseAtom, aseRoots, issuer)) {
                return true;
            }
            return false;
        });
    }

    private boolean isIssuerATargetAtom(URI baseAtom, Set<AseRoot> aseRoots, URI issuer) {
        return aseRoots.stream().anyMatch(root -> isTargetAtom(issuer, baseAtom, root));
    }

    private boolean isIssuerInAtomExpressions(URI baseAtom, Set<AtomExpression> atomExpressions, URI issuer) {
        return atomExpressions.stream().anyMatch(ae -> {
            if (ae.getAtomsRelativeAtomExpression().contains(ANY_ATOM)) {
                return true;
            }
            if (ae.getAtomsRelativeAtomExpression().contains(SELF)) {
                return baseAtom.equals(issuer);
            }
            if (ae.getAtomsURI().contains(issuer)) {
                return true;
            }
            return false;
        });
    }

    private boolean isTokenValid(AuthToken token) {
        return token.getTokenExp() != null
                        && token.getTokenExp().asCalendar().toInstant().compareTo(Instant.now()) > 0
                        && token.getTokenIss() != null
                        && token.getTokenSub() != null
                        && token.getTokenIat() != null;
    }

    private Collection<? extends AuthToken> decodeAuthTokens(Set<String> encodedTokens) {
        if (encodedTokens.isEmpty()) {
            return Collections.emptySet();
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private static void debug(String message, Authorization auth, OperationRequest request, Object... params) {
        if (logger.isDebugEnabled()) {
            Object[] logParams = new Object[params.length + 2];
            logParams[logParams.length - 2] = request;
            logParams[logParams.length - 1] = auth;
            System.arraycopy(params, 0, logParams, 0, params.length);
            logger.debug("AuthCheck: " + message + " ({} against {})", logParams);
        }
    }

    /**
     * Access to the instance factory for tests.
     *
     * @return
     */
    Shacl2JavaInstanceFactory getInstanceFactory() {
        return instanceFactory;
    }
}
