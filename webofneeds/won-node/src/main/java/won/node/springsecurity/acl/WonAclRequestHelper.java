package won.node.springsecurity.acl;

import org.apache.http.client.cache.HeaderConstants;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.FilterInvocation;
import won.auth.model.AclEvalResult;
import won.auth.model.DecisionValue;
import won.auth.model.RdfOutput;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.StringWriter;
import java.util.Base64;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

public class WonAclRequestHelper {
    public static final String ATT_ACL_EVALUATOR = "won.acl.evaluator";
    public static final String WWW_AUTHENTICATE_METHOD = "won-grantee";
    public static final String PARAM_SCOPES = "scopes";
    static final ThreadLocal<WonAclEvalContext> wonAclEvalContextThreadLocal = new ThreadLocal();
    // pattern for 'token68' according to
    // https://tools.ietf.org/html/rfc7235#appendix-C
    private static final Pattern BEARER_TOKEN_PATTERN = Pattern
                    .compile("Bearer\\s+([a-zA-Z0-9-._~+/]+=*)", Pattern.CASE_INSENSITIVE);
    public static String ATT_OPERATION_REQUEST = "won.acl.operationRequest";
    public static String ATT_EVALUATION_CONTEXT = "won.acl.evaluationContext";
    public static String ATT_GRANTED_TOKENS = "won.acl.grantedTokens";

    public static void putContextInThreadLocal(HttpServletRequest request) {
        WonAclEvalContext ctx = getWonAclEvaluationContext(request);
        if (ctx == null) {
            ctx = WonAclEvalContext.allowAll();
        }
        wonAclEvalContextThreadLocal.set(ctx);
    }

    public static void removeContextFromThreadLocal() {
        wonAclEvalContextThreadLocal.remove();
    }

    public static Set<String> getGrantedTokens(HttpServletRequest request) {
        Set<String> tokens = (Set<String>) request.getAttribute(ATT_GRANTED_TOKENS);
        if (tokens != null) {
            return tokens;
        }
        return Collections.emptySet();
    }

    public static void setGrantedTokens(HttpServletRequest request, Set<String> tokens) {
        if (tokens != null) {
            request.setAttribute(ATT_GRANTED_TOKENS, tokens);
        }
    }

    public static String getRequestParamScope(HttpServletRequest request) {
        return request.getParameter(PARAM_SCOPES);
    }

    public static void setAuthInfoAsResponseHeader(FilterInvocation filterInvocation,
                    AclEvalResult finalResult) {
        setAuthInfoAsResponseHeader(filterInvocation.getResponse(), finalResult);
    }

    public static void setAuthInfoAsResponseHeader(HttpServletResponse response,
                    AclEvalResult finalResult) {
        if (finalResult.getProvideAuthInfo() == null) {
            return;
        }
        Graph authInfo = RdfOutput.toGraph(finalResult.getProvideAuthInfo());
        StringWriter authInfoStringWriter = new StringWriter();
        RDFDataMgr.write(authInfoStringWriter, authInfo, Lang.TTL);
        String authInfoString = Base64.getEncoder().encodeToString(authInfoStringWriter.toString().getBytes());
        response.setHeader(HeaderConstants.WWW_AUTHENTICATE,
                        WWW_AUTHENTICATE_METHOD + " " + authInfoString);
    }

    public static WonAclEvalContext getWonAclEvaluationContext(HttpServletRequest request) {
        return (WonAclEvalContext) request.getAttribute(ATT_EVALUATION_CONTEXT);
    }

    public static WonAclEvalContext getWonAclEvaluationContextFromThreadLocal() {
        return wonAclEvalContextThreadLocal.get();
    }

    public static void setWonAclEvaluationContext(HttpServletRequest request, WonAclEvalContext context) {
        request.setAttribute(ATT_EVALUATION_CONTEXT, context);
    }

    public static Optional<String> getBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION);
        if (authHeader == null) {
            return Optional.empty();
        }
        Matcher m = BEARER_TOKEN_PATTERN.matcher(authHeader);
        if (!m.matches()) {
            return Optional.empty();
        }
        return Optional.of(m.group(1));
    }

    public static int getHttpStatusCodeForAclEvaluationResult(AclEvalResult result) {
        // implementing return values roughly as per RFC6750, Sec. 3.1
        if (DecisionValue.ACCESS_GRANTED.equals(result.getDecision())) {
            return HttpStatus.OK.value();
        }
        if (DecisionValue.ACCESS_DENIED.equals(result.getDecision())) {
            if (result.getInvalidToken()) {
                return HttpStatus.UNAUTHORIZED.value();
            }
            if (result.getInsufficientScope()) {
                return HttpStatus.FORBIDDEN.value();
            } else {
                return HttpStatus.FORBIDDEN.value();
            }
        }
        return HttpStatus.BAD_REQUEST.value();
    }
}
