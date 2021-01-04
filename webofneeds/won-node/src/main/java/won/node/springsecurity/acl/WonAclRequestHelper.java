package won.node.springsecurity.acl;

import java.io.StringWriter;
import java.util.Base64;
import java.util.Collections;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.apache.http.client.cache.HeaderConstants;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.security.web.FilterInvocation;
import won.auth.model.AclEvalResult;
import won.auth.model.RdfOutput;

public class WonAclRequestHelper {
    public static final String ATT_ACL_EVALUATOR = "won.acl.evaluator";
    private static final String WWW_AUTHENTICATE_METHOD = "won-grantee";
    public static String ATT_OPERATION_REQUEST = "won.acl.operationRequest";
    public static String ATT_EVALUATION_CONTEXT = "won.acl.evaluationContext";
    public static String ATT_GRANTED_TOKENS = "won.acl.grantedTokens";
    private static final String PARAM_SCOPE = "scope";

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
        return request.getParameter(PARAM_SCOPE);
    }

    public static void setAuthInfoAsResponseHeader(FilterInvocation filterInvocation,
                    AclEvalResult finalResult) {
        Graph authInfo = RdfOutput.toGraph(finalResult.getProvideAuthInfo());
        StringWriter authInfoStringWriter = new StringWriter();
        RDFDataMgr.write(authInfoStringWriter, authInfo, Lang.TTL);
        String authInfoString = Base64.getEncoder().encodeToString(authInfoStringWriter.toString().getBytes());
        filterInvocation.getResponse()
                        .setHeader(HeaderConstants.WWW_AUTHENTICATE,
                                        WWW_AUTHENTICATE_METHOD + " " + authInfoString);
    }

    public static WonAclEvalContext getWonAclEvaluationContext(HttpServletRequest request) {
        return (WonAclEvalContext) request.getAttribute(ATT_EVALUATION_CONTEXT);
    }

    public static void setWonAclEvaluationContext(HttpServletRequest request, WonAclEvalContext context) {
        request.setAttribute(ATT_EVALUATION_CONTEXT, context);
    }
}
