package won.auth;

import org.apache.jena.graph.Graph;
import org.apache.jena.shacl.Shapes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.auth.check.TargetAtomCheck;
import won.auth.check.TargetAtomCheckEvaluator;
import won.auth.check.TokenValidator;
import won.auth.model.*;
import won.shacl2java.Shacl2JavaEntityFactory;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.*;

import static won.auth.model.PredefinedAtomExpression.ANY_ATOM;
import static won.auth.model.PredefinedGranteeExpression.ANYONE;


public class WonAclEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private TargetAtomCheckEvaluator targetAtomCheckEvaluator;
    private TokenValidator tokenValidator;
    private Shacl2JavaEntityFactory entityFactory;

    public WonAclEvaluator(TargetAtomCheckEvaluator targetAtomCheckEvaluator, Shapes shapes) {
        this.targetAtomCheckEvaluator = targetAtomCheckEvaluator;
        this.entityFactory = new Shacl2JavaEntityFactory(shapes, "won.auth.model");
    }

    public void loadData(Graph dataGraph) {
        long start = System.currentTimeMillis();
        this.entityFactory.load(dataGraph);
        if (logger.isDebugEnabled()){
            logger.debug("loaded entity factory in {} ms", System.currentTimeMillis() - start);
        }
    }

    public Set<Authorization> getAutorizations() {
        return this.entityFactory
                .getEntitiesOfType(Authorization.class);
    }

    public Set<OperationRequest> getOperationRequests() {
        return this.entityFactory.getEntitiesOfType(OperationRequest.class);
    }

    private static AclEvalResult accessControlDecision(boolean decision){
        AclEvalResult acd = new AclEvalResult();
        if (decision){
            acd.setDecision(DecisionValue.ACCESS_GRANTED);
        } else {
            acd.setDecision(DecisionValue.ACCESS_DENIED);
        }
        return acd;
    }

    public AclEvalResult decide(Authorization authorization, OperationRequest request) {
        long start = System.currentTimeMillis();
        try {
            //determine if the requestor is in the set of grantees
            if (!isRequestorAGrantee(authorization, request)) {
                debug("requestor {} is not grantee", authorization, request, request.getRequestor());
                return accessControlDecision(false);
            }

            //determine if the operation is granted
            if (isOperationGranted(authorization, request)) {
                debug("operation is granted", authorization, request);
                return accessControlDecision(true);
            }
            debug("operation is not granted", authorization, request);
            return accessControlDecision(false);
        } finally {
            if (logger.isDebugEnabled()){
                debug("decision took {} ms", authorization, request, System.currentTimeMillis() - start);
            }
        }
    }


    private static boolean isOperationGranted(Authorization authorization, OperationRequest request) {

        for (AseRoot root : authorization.getGrants()) {
            OperationRequestChecker operationRequestChecker = new OperationRequestChecker(request);
            root.acceptRecursively(operationRequestChecker, false);
            boolean finalDecision = operationRequestChecker.getFinalDecision();
            debug("operation granted: {}", authorization, request, finalDecision);
            if (finalDecision){
                return true;
            }
        }

        return false;
    }

    public boolean isRequestorAGrantee(Authorization authorization, OperationRequest request) {
        //fast check: check for direct referenct
        if (ANYONE.equals(authorization.getGranteePredefinedGranteeExpression())) {
            return true;
        }
        URI requestor = request.getRequestor();
        for (AtomExpression grantee : authorization.getGranteesAtomExpression()) {
            debug("looking for grantee in atom expressions", authorization, request);
            if (ANY_ATOM.equals(grantee.getAtomPredefinedAtomExpression())) {
                debug("anyone is a grantee", authorization, request);
                return true;
            }
            if (grantee.getAtomsURI().contains(requestor)) {
                debug("requestor {} is listed explicitly as grantee", authorization, request, requestor);
                return true;
            }
        }
        debug("looking for grantee in atom structure expressions", authorization, request);
        for (AseRoot root : authorization.getGranteesAseRoot()) {
            TargetAtomCheckGenerator v = new TargetAtomCheckGenerator();
            root.acceptRecursively(v, false);
            for (TargetAtomCheck check : v.getTargetAtomChecks()) {
                if (this.targetAtomCheckEvaluator.isAllowedTargetAtom(requestor, check)) {
                    debug("requestor {} is grantee of Atom Structure Expression", authorization, request, requestor);
                    return true;
                }
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



}
