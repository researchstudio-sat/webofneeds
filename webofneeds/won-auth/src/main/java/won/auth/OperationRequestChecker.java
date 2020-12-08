package won.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.auth.model.*;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static won.auth.model.Individuals.ANY_OPERATION;
import static won.auth.model.MessageWildcard.ANY_MESSAGE_TYPE;

class OperationRequestChecker extends DefaultTreeExpressionVisitor {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private Deque<Set<OperationExpression>> grantedOperations = new ArrayDeque<>();

    private static boolean isLowerThan(AsePosition lower, AsePosition higher) {
        return lowerBy(lower, higher, new HashSet()) > -1;
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
        OperationGrantsRequested check = new OperationGrantsRequested(operation);
        return granted.stream().anyMatch(grantedOperation -> grantedOperation.when(check));
    }

    private void decideForPosition(AsePosition decisionPosition) {
        AsePosition requestedPos = operationRequest.getReqPosition();
        if (!isFinalDecisionMade()) {
            if (!isBranchMarkedNothingGranted()) {
                if (requestedPos.equals(decisionPosition)) {
                    // TODO: implement sh:xone handling to get just one operation here
                    finalDecision = isOperationGranted(
                                    operationRequest.getOperationsUnion().stream().findFirst().get());
                    if (logger.isDebugEnabled()) {
                        logger.debug("Making final decision at position {}: {}", decisionPosition, finalDecision);
                    }
                } else if (isLowerThan(requestedPos, decisionPosition)) {
                    boolean decisionAtCurrentPosition = isOperationGranted(
                                    operationRequest.getOperationsUnion().stream().findFirst().get());
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
            granted.addAll(node.getOperationsUnion());
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
    protected void onBeforeRecursion(TreeExpression parent, TreeExpression child) {
        // check if we are omitting ASEs. if so, check if they are the decision position
        // and if so, decide.
        AsePosition childPosition = ((TreeExpression) child).getAsePosition();
        AsePosition candidate = childPosition.getParentPosition();
        while (!candidate.equals(((TreeExpression) parent).getAsePosition()) && !isFinalDecisionMade()) {
            decideForPosition(candidate);
        }
    }

    @Override
    protected void onAfterRecursion(TreeExpression parent, TreeExpression child) {
        if (logger.isDebugEnabled()) {
            logger.debug("allowed ops in {}: {}", child, Arrays.asList(grantedOperations.peek().toArray()));
        }
        // coming back from the recursion, we pop the stack
        // now, the top element is the one we pushed onto it at this level
        grantedOperations.pop();
    }

    @Override
    protected void onBeginVisit(ConnectionMessagesExpression other) {
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
    protected void onBeginVisit(GraphExpression other) {
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
    protected void onBeginVisit(ConnectionMessageExpression other) {
        inherit(other);
        if (isBranchMarkedNothingGranted() || isFinalDecisionMade()) {
            return;
        }
        collectOperations(other);
        decideForPosition(other.getAsePosition());
    }

    @Override
    protected void onBeginVisit(AseRoot other) {
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
    protected void onBeginVisit(AtomMessagesExpression other) {
        inherit(other);
        if (isBranchMarkedNothingGranted() || isFinalDecisionMade()) {
            return;
        }
        collectOperations(other);
        decideForPosition(other.getAsePosition());
    }

    @Override
    protected void onBeginVisit(AtomMessageExpression other) {
        inherit(other);
        if (isBranchMarkedNothingGranted() || isFinalDecisionMade()) {
            return;
        }
        collectOperations(other);
        decideForPosition(other.getAsePosition());
    }

    @Override
    protected void onBeginVisit(ConnectionsExpression other) {
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
    protected void onBeginVisit(SocketExpression other) {
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
    protected void onBeginVisit(ConnectionExpression other) {
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

    private static class FalseUnless implements OperationExpression.Cases<Boolean> {
        @Override
        public Boolean is(AuthInfoOperationExpression option) {
            return false;
        }

        @Override
        public Boolean is(TokenOperationExpression option) {
            return false;
        }

        @Override
        public Boolean is(SimpleOperationExpression option) {
            return false;
        }

        @Override
        public Boolean is(MessageOperationExpression option) {
            return false;
        }
    }

    private static class OperationGrantsRequested implements OperationExpression.Cases<Boolean> {
        private OperationExpression requested;

        public OperationGrantsRequested(OperationExpression requested) {
            this.requested = requested;
        }

        @Override
        public Boolean is(AuthInfoOperationExpression granted) {
            return false;
        }

        @Override
        public Boolean is(TokenOperationExpression granted) {
            return false;
        }

        @Override
        public Boolean is(SimpleOperationExpression granted) {
            if (granted.equals(ANY_OPERATION)) {
                return true;
            }
            return requested.when(new FalseUnless() {
                @Override
                public Boolean is(SimpleOperationExpression requestedOption) {
                    return granted.get_node().equals(requestedOption.get_node());
                }
            });
        }

        @Override
        public Boolean is(MessageOperationExpression granted) {
            return requested.when(new FalseUnless() {
                @Override
                public Boolean is(MessageOperationExpression requestedOption) {
                    if (ANY_MESSAGE_TYPE.equals(granted.getMessageTosMessageWildcard())
                                    && !requestedOption.getMessageTosUnion().isEmpty()
                                    && (requestedOption.getMessageOnBehalfsUnion().isEmpty()
                                                    || ANY_MESSAGE_TYPE.equals(
                                                                    granted.getMessageOnBehalfsMessageWildcard()))) {
                        return true;
                    }
                    if (ANY_MESSAGE_TYPE.equals(granted.getMessageOnBehalfsMessageWildcard())
                                    && !requestedOption.getMessageOnBehalfsUnion().isEmpty()
                                    && (requestedOption.getMessageTosUnion().isEmpty()
                                                    || ANY_MESSAGE_TYPE.equals(
                                                                    granted.getMessageOnBehalfsMessageWildcard()))) {
                        return true;
                    }
                    Set<MessageType> requestedTos = collectMessageTypes(requestedOption.getMessageTosUnion());
                    if (!requestedTos.isEmpty()){
                        Set<MessageType> grantedTos = collectMessageTypes(granted.getMessageTosUnion());
                        if (grantedTos.containsAll(requestedTos)) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                    Set<MessageType> requestedOnBehalfs = collectMessageTypes(
                                    requestedOption.getMessageOnBehalfsUnion());
                    if (!requestedOnBehalfs.isEmpty()){
                        Set<MessageType> grantedOnBehalfs = collectMessageTypes(granted.getMessageOnBehalfsUnion());
                        if (grantedOnBehalfs.containsAll(requestedOnBehalfs)) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                    return false;
                }
            });
        }
    }

    private static Set<MessageType> collectMessageTypes(Set<MessageTypeSpecification> msgTypeSpecs) {
        return msgTypeSpecs.stream()
                        .flatMap(s -> s.when(new MessageTypeSpecification.Cases<Stream<MessageType>>() {
                            @Override
                            public Stream<MessageType> is(MessageWildcard option) {
                                return new HashSet<MessageType>().stream();
                            }

                            @Override
                            public Stream<MessageType> is(MessageTypesExpression option) {
                                return option.getMembers().stream();
                            }

                            @Override
                            public Stream<MessageType> is(MessageType option) {
                                Set<MessageType> ret = new HashSet<>();
                                ret.add(option);
                                return ret.stream();
                            }
                        }))
                        .collect(Collectors.toSet());
    }
}
