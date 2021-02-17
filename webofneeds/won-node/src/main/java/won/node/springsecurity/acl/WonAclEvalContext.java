package won.node.springsecurity.acl;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.compose.Difference;
import won.auth.AuthUtils;
import won.auth.WonAclEvaluator;
import won.auth.model.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class WonAclEvalContext {
    private OperationRequest operationRequest;
    private WonAclEvaluator wonAclEvaluator;
    private Mode mode;
    private Map<OperationRequest, AclEvalResult> aclEvalResults = new HashMap();

    private WonAclEvalContext() {
        this.mode = Mode.ALLOW_ALL;
        this.operationRequest = new OperationRequest(); // dummy
    }

    public WonAclEvalContext(OperationRequest operationRequest,
                    WonAclEvaluator wonAclEvaluator, Mode mode) {
        this.operationRequest = operationRequest;
        this.wonAclEvaluator = wonAclEvaluator;
        this.mode = mode;
    }

    public static WonAclEvalContext allowAll() {
        return new WonAclEvalContext();
    }

    public static WonAclEvalContext contentFilter(OperationRequest operationRequest, WonAclEvaluator evaluator) {
        return new WonAclEvalContext(operationRequest, evaluator, Mode.FILTER);
    }

    /**
     * Returns a shallow clone of the <code>OperationRequest</code> in the context.
     * The <code>set*()</code> methods are safe to use on the result.
     *
     * @return the cloned operation request
     */
    public OperationRequest getOperationRequest() {
        return AuthUtils.cloneShallow(operationRequest);
    }

    public AclEvalResult decideAndRemember(OperationRequest request) {
        AclEvalResult result = null;
        if (isModeAllowAll()) {
            result = new AclEvalResult();
            DecisionValue decisionValue = DecisionValue.ACCESS_GRANTED;
            result.setDecision(decisionValue);
        } else {
            result = wonAclEvaluator.decide(request);
        }
        this.aclEvalResults.put(request, result);
        return result;
    }

    /**
     * Returns an rdf graph with an ASE containing all operations allowed for the
     * presented credentials.
     *
     * @return null if none are allowed.
     */
    public Graph getGrants() {
        if (isModeAllowAll()) {
            throw new IllegalStateException("Did not expect this call in mode ALLOW_ALL");
        }
        Optional<AseRoot> grantedOperations = wonAclEvaluator.getGrants(getOperationRequest());
        Graph actualResult = null;
        if (grantedOperations.isPresent()) {
            actualResult = RdfOutput.toGraph(grantedOperations.get(), false);
            actualResult = new Difference(actualResult, AuthUtils.shapes().getGraph());
        }
        return actualResult;
    }

    public Optional<AclEvalResult> getCombinedResults() {
        return this.aclEvalResults.values().stream().reduce(WonAclEvaluator::mergeAclEvalResults);
    }

    public Mode getMode() {
        return mode;
    }

    public boolean isModeFilter() {
        return this.mode == Mode.FILTER;
    }

    public boolean isModeAllowAll() {
        return this.mode == Mode.ALLOW_ALL;
    }

    /**
     * Operational modes
     */
    public enum Mode {
        /**
         * The decision to allow the operation has been made, allow everything
         **/
        ALLOW_ALL,
        /**
         * Use the context to filter the result based on the ACLs
         **/
        FILTER
    }
}
