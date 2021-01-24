package won.auth;

import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.auth.check.AtomNodeChecker;
import won.auth.check.TargetAtomCheck;
import won.auth.check.TargetAtomCheckEvaluator;
import won.auth.model.*;
import won.cryptography.rdfsign.WebIdKeyLoader;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static won.auth.model.DecisionValue.ACCESS_DENIED;
import static won.auth.model.DecisionValue.ACCESS_GRANTED;
import static won.auth.model.GranteeWildcard.ANYONE;
import static won.auth.model.RelativeAtomExpression.ANY_ATOM;
import static won.auth.model.RelativeAtomExpression.SELF;

public class WonAclEvaluator {
    public static final long DEFAULT_TOKEN_EXPIRES_AFTER_SECONDS = 3600;
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final URI OWNER_TOKEN_SCOPE = URI.create("https://w3id.org/won/auth#ownerToken");
    private final Set<Authorization> authorizations;
    private final TargetAtomCheckEvaluator targetAtomCheckEvaluator;
    private final AtomNodeChecker atomNodeChecker;
    private final WebIdKeyLoader webIdKeyLoader;
    private final Authorization implicitOwnerTokenAuthorization = createImplicitOwnerTokenAuthorization();
    private final Authorization implicitGrantOwnerTokenAuthorization = createImplicitGrantOwnerTokenAuthorization();

    public WonAclEvaluator(Set<Authorization> authorizations,
                    TargetAtomCheckEvaluator targetAtomCheckEvaluator,
                    AtomNodeChecker atomNodeChecker,
                    WebIdKeyLoader webIdKeyLoader) {
        this.authorizations = authorizations;
        this.targetAtomCheckEvaluator = targetAtomCheckEvaluator;
        this.webIdKeyLoader = webIdKeyLoader;
        this.atomNodeChecker = atomNodeChecker;
    }

    public static AclEvalResult mergeAclEvalResults(AclEvalResult left, AclEvalResult right) {
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
    }

    public static AuthInfo merge(AuthInfo left, AuthInfo right) {
        AuthInfo merged = new AuthInfo();
        copyAuthInfo(left, merged);
        copyAuthInfo(right, merged);
        return merged;
    }

    private static void copyAuthInfo(AuthInfo from, AuthInfo to) {
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

    private static Predicate<TokenOperationExpression> isRequestedToken(OperationRequest request,
                    boolean allRequested) {
        return op -> allRequested
                        || (!op.getRequestToken().getTokenScopesUnion().isEmpty()
                                        && op.getRequestToken().getTokenScopesUnion().containsAll(
                                                        request.getOperationTokenOperationExpression()
                                                                        .getRequestToken()
                                                                        .getTokenScopesUnion()));
    }

    private static boolean isOperationGranted(Authorization authorization, OperationRequest request) {
        if (request.getReqAtom().equals(request.getRequestor())) {
            // requestor is owner - allow anything
            return true;
        }
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
     * Create an Authorization that must be signed by the atom's node on behalf of
     * the owner and grants any operation. The node will only issue such a token to
     * the authenticated owner.
     *
     * @return
     */
    private static Authorization createImplicitOwnerTokenAuthorization() {
        Authorization auth = new Authorization();
        AseRoot grants = new AseRoot();
        grants.addOperationsSimpleOperationExpression(Individuals.ANY_OPERATION);
        auth.addGrant(grants);
        TokenShape token = new TokenShape();
        AtomExpression atomExpression = new AtomExpression();
        atomExpression.addAtomsRelativeAtomExpression(SELF);
        token.addIssuersAtomExpression(atomExpression);
        token.addTokenScopesURI(OWNER_TOKEN_SCOPE);
        token.setNodeSigned(true);
        auth.addBearer(token);
        return auth;
    }

    /**
     * Create an Authorization that grants the owner of the atom an 'owner token',
     * (see <a href=
     * "#createImplicitOwnerTokenAuthorization"><code>createImplicitOwnerTokenAuthorization</code></a>).
     *
     * @return
     */
    private static Authorization createImplicitGrantOwnerTokenAuthorization() {
        Authorization auth = new Authorization();
        AseRoot grants = new AseRoot();
        TokenSpecification ts = new TokenSpecification();
        ts.setTokenScopeURI(OWNER_TOKEN_SCOPE);
        ts.setNodeSigned(true);
        TokenOperationExpression toe = new TokenOperationExpression();
        toe.setRequestToken(ts);
        grants.addOperationsTokenOperationExpression(toe);
        auth.addGrant(grants);
        AtomExpression ae = new AtomExpression();
        ae.addAtomsRelativeAtomExpression(SELF);
        auth.addGranteesAtomExpression(ae);
        return auth;
    }

    private AclEvalResult decide(OperationRequest request) {
        return this.decide(this.authorizations, request);
    }

    public AclEvalResult decide(Set<Authorization> authorizations, OperationRequest request) {
        Set<Authorization> requestorIsGranteeOf = new HashSet<>();
        Set<Authorization> explicitAndImplicitAuthorizations = new HashSet<>();
        explicitAndImplicitAuthorizations.addAll(authorizations);
        explicitAndImplicitAuthorizations.add(this.implicitOwnerTokenAuthorization);
        explicitAndImplicitAuthorizations.add(this.implicitGrantOwnerTokenAuthorization);
        boolean grantAuthInfo = canProvideAuthInfo(explicitAndImplicitAuthorizations, request,
                        requestorIsGranteeOf);
        Optional<AclEvalResult> result = explicitAndImplicitAuthorizations.stream()
                        .map(auth -> decide(auth, request, grantAuthInfo, requestorIsGranteeOf))
                        .reduce((left, right) -> mergeAclEvalResults(left, right));
        return result.orElse(accessControlDecision(false, request));
    }

    private AclEvalResult accessControlDecision(boolean accessGranted, OperationRequest request) {
        AclEvalResult result = new AclEvalResult();
        result.setDecision(accessGranted ? ACCESS_GRANTED : ACCESS_DENIED);
        result.setRequestedOperation(request);
        return result;
    }

    private boolean provideAuthInfo(Set<Authorization> authorizations,
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

    private AclEvalResult decide(Authorization authorization, OperationRequest request,
                    boolean provideAuthInfo) {
        return decide(authorization, request, provideAuthInfo, Collections.emptySet());
    }

    private AclEvalResult decide(Authorization authorization, OperationRequest request,
                    boolean provideAuthInfo, Set<Authorization> requestorIsGranteeOf) {
        // determine if the requestor is in the set of grantees
        boolean decisionSoFar = false;
        if (request.getReqAtom().equals(request.getRequestor())) {
            // owner of atom is requestor - allow anything
            decisionSoFar = true;
        } else if (ANYONE.equals(authorization.getGranteeGranteeWildcard())) {
            // wildcard access
            decisionSoFar = true;
        } else if (requestorIsGranteeOf.contains(authorization)) {
            // requestor is grantee, we have established that earlier
            decisionSoFar = true;
        } else {
            Set<TokenShape> acceptableTokens = new HashSet<>(); // we will collect acceptable tokens
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
                debug("operation is not granted", authorization, request);
                decisionSoFar = false;
            }
        }
        return accessControlDecision(decisionSoFar, authorization, request, authInfo);
    }

    private AclEvalResult accessControlDecision(boolean accessGranted, Authorization authorization,
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
            acd.setIssueTokens(getRequestedTokens(authorization, request));
        }
        return acd;
    }

    private Set<AuthToken> getRequestedTokens(Authorization authorization, OperationRequest request) {
        TokenOperationExpression tokenOperationExpression = request.getOperationTokenOperationExpression();
        if (tokenOperationExpression == null) {
            return Collections.emptySet();
        }
        Set<AuthToken> authTokens = new HashSet<>();
        boolean allRequested = tokenOperationExpression
                        .getRequestToken().getTokenScopesUnion().isEmpty();
        for (TokenOperationExpression op : authorization.getGrants().stream()
                        .flatMap(root -> root.getOperationsTokenOperationExpression().stream())
                        .filter(isRequestedToken(request, allRequested))
                        .collect(Collectors.toSet())) {
            TokenSpecification tokenSpec = op.getRequestToken();
            AuthToken token = new AuthToken();
            token.setTokenIss(request.getReqAtom());
            token.setTokenSub(request.getRequestor());
            if (tokenSpec.getNodeSigned() == null || tokenSpec.getNodeSigned()) {
                Optional<URI> nodeUri = this.atomNodeChecker.getNodeOfAtom(request.getReqAtom());
                if (!nodeUri.isPresent()) {
                    throw new IllegalStateException(
                                    "Cannot issue token, nodeUri of " + request.getReqAtom() + " not found");
                }
                token.setTokenSig(nodeUri.get());
            } else {
                // don't set the sig field
            }
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            XSDDateTime iat = new XSDDateTime(cal);
            if (tokenSpec.getExpiresAfterBigInteger() != null) {
                cal.add(Calendar.SECOND, tokenSpec.getExpiresAfterBigInteger().intValueExact());
            } else if (tokenSpec.getExpiresAfterLong() != null) {
                cal.add(Calendar.SECOND, tokenSpec.getExpiresAfterLong().intValue());
            } else if (tokenSpec.getExpiresAfterInteger() != null) {
                cal.add(Calendar.SECOND, tokenSpec.getExpiresAfterInteger());
            } else {
                cal.add(Calendar.SECOND, (int) DEFAULT_TOKEN_EXPIRES_AFTER_SECONDS);
            }
            XSDDateTime exp = new XSDDateTime(cal);
            token.setTokenExp(exp);
            token.setTokenIat(iat);
            if (tokenSpec.getTokenScopeURI() != null) {
                token.setTokenScopeURI(tokenSpec.getTokenScopeURI());
            } else if (tokenSpec.getTokenScopeString() != null) {
                token.setTokenScopeString(tokenSpec.getTokenScopeString());
            }
            authTokens.add(token);
        }
        return authTokens;
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
            if (isTargetAtom(requestor, baseAtom, root)) {
                return true;
            }
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
        Set<AuthToken> finalDecoded = decoded;
        if (decoded.size() > 0) {
            return authorization.getBearers().stream().anyMatch(tokenShape -> {
                Set<AseRoot> aseRoots = tokenShape.getIssuersAseRoot();
                Set<AtomExpression> atomExpressions = tokenShape.getIssuersAtomExpression();
                Set<AuthToken> elegibleTokens = filterTokensByScope(finalDecoded, tokenShape);
                debug("tokens with correct scope: {}", authorization, request, elegibleTokens.size());
                return elegibleTokens.stream()
                                .filter(isIssuerAccepted(aseRoots, atomExpressions, request.getReqAtom()))
                                .filter(isSignerAccepted(tokenShape))
                                .findFirst().isPresent();
            });
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

    private Predicate<AuthToken> isIssuerAccepted(Set<AseRoot> aseRoots,
                    Set<AtomExpression> atomExpressions, URI baseAtom) {
        return token -> {
            URI issuer = token.getTokenIss();
            if (isIssuerInAtomExpressions(baseAtom, atomExpressions, issuer)) {
                return true;
            }
            if (isIssuerATargetAtom(baseAtom, aseRoots, issuer)) {
                return true;
            }
            return false;
        };
    }

    private Predicate<AuthToken> isSignerAccepted(TokenShape tokenShape) {
        return token -> {
            // by default, tokens are node-signed.
            if (tokenShape.getNodeSigned() == null || tokenShape.getNodeSigned()) {
                URI signer = token.getTokenSig();
                if (signer == null) {
                    // token should be node-signed but no sig field present - don't accept
                    return false;
                }
                // accept signer if it is the atom's ndoe
                return atomNodeChecker.isNodeOfAtom(token.getTokenIss(), token.getTokenSig());
            } else {
                if (token.getTokenSig() != null) {
                    // token is not node-signed but a sig field is present - don't accept
                    return false;
                }
                return true;
            }
        };
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
        return encodedTokens
                        .parallelStream()
                        .map(tokenStr -> AuthUtils.parseToken(tokenStr, webIdKeyLoader))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toSet());
    }
}
