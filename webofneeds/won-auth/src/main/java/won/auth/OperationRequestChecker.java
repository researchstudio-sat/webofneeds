package won.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.auth.model.*;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.stream.Collectors;

import static won.auth.model.Individuals.ANY_OPERATION;

class OperationRequestChecker implements TreeExpressionVisitor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private Deque<Set<OperationExpression>> grantedOperations = new ArrayDeque<>();

    private static boolean isLowerThan(AsePosition lower, AsePosition higher) {
        return lowerBy(lower, higher, new HashSet()) > -1;
    }

    private static int lowerBy(AsePosition lower, AsePosition higher) {
        return lowerBy(lower, higher, new HashSet());
    }

    /**
     * Returns the level difference if <code>lower</code> is lower, otherwise -1;
     * 
     * @param lower
     * @param higher
     * @param visited
     * @return
     */
    private static int lowerBy(AsePosition lower, AsePosition higher, Set visited) {
        if (visited.contains(lower)) {
            return -1;
        }
        visited.add(lower);
        AsePosition parent = lower.getParentPosition();
        if (parent != null) {
            if (parent.equals(higher)) {
                return visited.size();
            }
            return lowerBy(parent, higher, visited);
        }
        return -1;
    }

    private class DecisionAtHigherLevel {
        private Boolean decision = null;
        private AsePosition position = null;

        public DecisionAtHigherLevel() {
        }

        public void updateIfLower(AsePosition position, boolean decision) {
            if (this.position == null) {
                this.position = position;
                this.decision = decision;
            } else if (isLowerThan(position, this.position)) {
                this.position = position;
                this.decision = decision;
            }
        }

        public Boolean getDecision() {
            return decision;
        }

        public AsePosition getPosition() {
            return position;
        }

        public boolean hasDecision() {
            return decision != null && position != null;
        }
    }

    // if the requested position is lower than the end of its branch in the ASE
    // tree,
    // the decision is made at that point.
    private DecisionAtHigherLevel decisionAtHigherLevel = new DecisionAtHigherLevel();
    private OperationRequest operationRequest;
    private Boolean finalDecision = null;
    private Set<OperationExpression> NOTHING_GRANTED = Collections.unmodifiableSet(new HashSet<>());

    public OperationRequestChecker(OperationRequest operationRequest) {
        this.operationRequest = operationRequest;
    }

    public Boolean getFinalDecision() {
        if (finalDecision != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Decision at requested level {} is {}", operationRequest.getReqPosition(),
                                finalDecision ? "positive" : "negative");
            }
            return finalDecision;
        }
        if (decisionAtHigherLevel.hasDecision()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Decision at requested level {} made at higher-than-requested level {} is {}",
                                new Object[] { operationRequest.getReqPosition(),
                                                decisionAtHigherLevel.getPosition(),
                                                decisionAtHigherLevel.getDecision() ? "positive" : "negative" });
            }
            return decisionAtHigherLevel.getDecision();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("No decision was made during analysis, defaulting to negative");
        }
        return false;
    }

    private boolean isFinalDecisionMade() {
        return this.finalDecision != null;
    }

    private boolean isOperationGranted(OperationExpression operation) {
        Set<OperationExpression> granted = grantedOperations.peek();
        if (granted.contains(ANY_OPERATION) || granted.contains(operation)) {
            return true;
        }
        Set<MessageType> mto = operation.getMessageTosMessageOperation().stream()
                        .flatMap(mo -> mo.getMembers().stream()).collect(Collectors.toSet());
        mto.addAll(operation.getMessageTosMessageType());
        Boolean firstResult = null;
        if (!mto.isEmpty()) {
            boolean result = granted.parallelStream()
                            .anyMatch(go -> mto.stream()
                                            .allMatch(msgType -> go.getMessageTosMessageType().contains(msgType)
                                                            || go.getMessageTosMessageOperation().stream()
                                                                            .anyMatch(msgOp -> msgOp.getMembers()
                                                                                            .contains(msgType))));
            if (!result) {
                return false;
            } else {
                firstResult = true;
            }
        }
        Set<MessageType> mob = operation.getMessageOnBehalfsMessageOperation().stream()
                        .flatMap(mo -> mo.getMembers().stream()).collect(Collectors.toSet());
        mob.addAll(operation.getMessageOnBehalfsMessageType());
        if (!mob.isEmpty()) {
            boolean result = granted.parallelStream()
                            .anyMatch(go -> mob.stream()
                                            .allMatch(msgType -> go.getMessageOnBehalfsMessageType().contains(msgType)
                                                            || go.getMessageOnBehalfsMessageOperation().stream()
                                                                            .anyMatch(msgOp -> msgOp.getMembers()
                                                                                            .contains(msgType))));
            if (!result) {
                return false;
            } else {
                if (firstResult == null || firstResult) {
                    return true;
                }
            }
        }
        if (firstResult != null) {
            return firstResult;
        }
        return false;
    }

    private void decideForPosition(AsePosition decisionPosition) {
        AsePosition requestedPos = operationRequest.getReqPosition();
        if (!isFinalDecisionMade()) {
            if (!isBranchMarkedNothingGranted()) {
                if (requestedPos.equals(decisionPosition)) {
                    finalDecision = isOperationGranted(operationRequest.getOperation());
                    if (logger.isDebugEnabled()) {
                        logger.debug("Making final decision at position {}: {}", decisionPosition, finalDecision);
                    }
                } else if (isLowerThan(requestedPos, decisionPosition)) {
                    boolean decisionAtCurrentPosition = isOperationGranted(operationRequest.getOperation());
                    if (logger.isDebugEnabled()) {
                        logger.debug("Recording decision at higher-than-requested position {}: {}",
                                        decisionPosition, decisionAtCurrentPosition);
                    }
                    this.decisionAtHigherLevel.updateIfLower(decisionPosition, decisionAtCurrentPosition);
                }
            }
        }
    }

    private void markBranchNothingGranted() {
        grantedOperations.pop();
        grantedOperations.push(NOTHING_GRANTED);
    }

    private boolean isBranchMarkedNothingGranted() {
        return grantedOperations.peek() == NOTHING_GRANTED;
    }

    private void collectOperations(OperationContainer node) {
        Set<OperationExpression> granted = grantedOperations.peek();
        if (granted != NOTHING_GRANTED) {
            granted.addAll(node.getOperations());
        }
    }

    private void inherit(TreeExpression node) {
        if (node instanceof Inheriting) {
            if (((Inheriting) node).getInherit() != null && ((Inheriting) node).getInherit() == false) {
                grantedOperations.push(new HashSet<>());
            } else {
                Set<OperationExpression> parentLevel = grantedOperations.peek();
                Set<OperationExpression> inherited = new HashSet<>(parentLevel);
                grantedOperations.push(inherited);
            }
        } else {
            grantedOperations.push(new HashSet<>());
        }
    }

    @Override
    public void onBeforeRecursion(Object parent, Object child) {
        // check if we are omitting ASEs. if so, check if they are the decision position
        // and if so, decide.
        AsePosition childPosition = ((TreeExpression) child).getAsePosition();
        AsePosition candidate = childPosition.getParentPosition();
        while (!candidate.equals(((TreeExpression) parent).getAsePosition()) && !isFinalDecisionMade()) {
            decideForPosition(candidate);
        }
    }

    @Override
    public void onAfterRecursion(Object parent, Object child) {
        if (logger.isDebugEnabled()) {
            logger.debug("allowed ops in {}: {}", child, Arrays.asList(grantedOperations.peek().toArray()));
        }
        // coming back from the recursion, we pop the stack
        // now, the top element is the one we pushed onto it at this level
        grantedOperations.pop();
    }

    @Override
    public void visit(ConnectionMessagesExpression other) {
        inherit(other);
        if (isBranchMarkedNothingGranted() || isFinalDecisionMade()) {
            return;
        }
        if (operationRequest.getReqConnectionMessages() == null) {
            markBranchNothingGranted();
        }
        collectOperations(other);
        decideForPosition(other.getAsePosition());
    }

    @Override
    public void visit(GraphExpression other) {
        inherit(other);
        if (isBranchMarkedNothingGranted() || isFinalDecisionMade()) {
            return;
        }
        collectOperations(other);
        if (!other.getGraphIris().isEmpty()) {
            if (!other.getGraphIris().containsAll(operationRequest.getReqGraphs())) {
                markBranchNothingGranted();
            }
        }
        if (!other.getGraphTypes().isEmpty()) {
            if (!other.getGraphTypes().containsAll(operationRequest.getReqGraphTypes())) {
                markBranchNothingGranted();
            }
        }
        decideForPosition(other.getAsePosition());
    }

    @Override
    public void visit(ConnectionMessageExpression other) {
        inherit(other);
        if (isBranchMarkedNothingGranted() || isFinalDecisionMade()) {
            return;
        }
        collectOperations(other);
        decideForPosition(other.getAsePosition());
    }

    @Override
    public void visit(AseRoot other) {
        inherit(other);
        if (isBranchMarkedNothingGranted() || isFinalDecisionMade()) {
            return;
        }
        if (!other.getAtomStates().isEmpty()) {
            if (!other.getAtomStates().contains(operationRequest.getReqAtomState())) {
                markBranchNothingGranted();
            }
        }
        collectOperations(other);
        decideForPosition(other.getAsePosition());
    }

    @Override
    public void visit(AtomMessagesExpression other) {
        inherit(other);
        if (isBranchMarkedNothingGranted() || isFinalDecisionMade()) {
            return;
        }
        collectOperations(other);
        decideForPosition(other.getAsePosition());
    }

    @Override
    public void visit(TokenRequestExpression other) {
        inherit(other);
        if (isBranchMarkedNothingGranted() || isFinalDecisionMade()) {
            return;
        }
        collectOperations(other);
        decideForPosition(other.getAsePosition());
    }

    @Override
    public void visit(AtomMessageExpression other) {
        inherit(other);
        if (isBranchMarkedNothingGranted() || isFinalDecisionMade()) {
            return;
        }
        collectOperations(other);
        decideForPosition(other.getAsePosition());
    }

    @Override
    public void visit(ConnectionsExpression other) {
        inherit(other);
        if (isBranchMarkedNothingGranted() || isFinalDecisionMade()) {
            return;
        }
        if (!other.getConnectionStates().isEmpty()) {
            if (!other.getConnectionStates().contains(operationRequest.getReqConnectionState())) {
                markBranchNothingGranted();
            }
        }
        collectOperations(other);
        decideForPosition(other.getAsePosition());
    }

    @Override
    public void visit(SocketExpression other) {
        inherit(other);
        if (isBranchMarkedNothingGranted() || isFinalDecisionMade()) {
            return;
        }
        if (!other.getSocketIris().isEmpty()) {
            if (!other.getSocketIris().contains(operationRequest.getReqSocket())) {
                markBranchNothingGranted();
            }
        }
        if (!other.getSocketTypes().isEmpty()) {
            if (!other.getSocketTypes().contains(operationRequest.getReqSocketType())) {
                markBranchNothingGranted();
            }
        }
        collectOperations(other);
        decideForPosition(other.getAsePosition());
    }

    @Override
    public void visit(ConnectionExpression other) {
        inherit(other);
        if (isBranchMarkedNothingGranted() || isFinalDecisionMade()) {
            return;
        }
        if (!other.getConnectionStates().isEmpty()) {
            if (!other.getConnectionStates().contains(operationRequest.getReqConnectionState())) {
                markBranchNothingGranted();
            }
        }
        collectOperations(other);
        decideForPosition(other.getAsePosition());
    }
}
