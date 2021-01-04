package won.node.springsecurity.acl;

import java.util.Objects;
import won.auth.AuthUtils;
import won.auth.WonAclEvaluator;
import won.auth.model.OperationRequest;

public class WonAclEvalContext {
    /**
     * Operational modes
     */
    public enum Mode {
        /** The decision to allow the operation has been made, allow everything **/
        ALLOW_ALL,
        /** Use the context to filter the result based on the ACLs **/
        FILTER
    };

    private OperationRequest operationRequest;
    private WonAclEvaluator wonAclEvaluator;
    private Mode mode;

    private WonAclEvalContext() {
        this.mode = Mode.ALLOW_ALL;
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

    public WonAclEvaluator getWonAclEvaluator() {
        return wonAclEvaluator;
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
}
