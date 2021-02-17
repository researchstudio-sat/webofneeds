package won.auth.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.auth.AuthUtils;
import won.auth.model.*;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static won.auth.WonAclEvaluator.DEFAULT_TOKEN_EXPIRES_AFTER_SECONDS;
import static won.auth.model.Individuals.ANY_OPERATION;
import static won.auth.model.MessageWildcard.ANY_MESSAGE_TYPE;
import static won.auth.support.TreeExpressionVisitorUtils.isAncestorPosition;

public class OperationRequestChecker extends DefaultTreeExpressionVisitor {
    private enum MaybeBoolean {
        TRUE(true), FALSE(false), NULL(null);
        private Boolean value;

        private MaybeBoolean(Boolean value) {
            this.value = value;
        }

        public static MaybeBoolean forValue(Boolean value) {
            if (value == null) {
                return NULL;
            } else if (value.equals(Boolean.TRUE)) {
                return TRUE;
            } else {
                return FALSE;
            }
        }

        private boolean getBoolean(boolean defaultValue) {
            if (isNull()) {
                return defaultValue;
            }
            return value;
        }

        public MaybeBoolean and(MaybeBoolean other) {
            if (isNull()) {
                if (other.isNull()) {
                    return NULL;
                } else {
                    return other;
                }
            } else {
                if (other.isNull()) {
                    return this;
                } else {
                    return forValue(this.value && other.value);
                }
            }
        }

        public MaybeBoolean or(MaybeBoolean other) {
            if (isNull()) {
                if (other.isNull()) {
                    return NULL;
                } else {
                    return other;
                }
            } else {
                if (other.isNull()) {
                    return this;
                } else {
                    return forValue(this.value || other.value);
                }
            }
        }

        public boolean isTrue() {
            return this.value != null && this.value;
        }

        public boolean isFalse() {
            return this.value != null && !this.value;
        }

        public boolean isNull() {
            return this.value == null;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private Deque<Set<OperationExpression>> grantedOperations = new ArrayDeque<>();
    private Deque<MaybeBoolean> decision = new ArrayDeque<>();
    private OperationRequest operationRequest;

    public OperationRequestChecker(OperationRequest operationRequest) {
        this.operationRequest = operationRequest;
        this.grantedOperations.push(new HashSet<>());
        this.decision.push(MaybeBoolean.NULL);
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

    public Boolean getFinalDecision() {
        return this.decision.pop().getBoolean(false);
    }

    private boolean isOperationGranted(OperationExpression operation) {
        Set<OperationExpression> granted = grantedOperations.peek();
        OperationGrantsRequested check = new OperationGrantsRequested(operation);
        return granted.stream().anyMatch(grantedOperation -> grantedOperation.when(check));
    }

    private boolean isOperationGrantedAtPosition(AsePosition decisionPosition) {
        Objects.requireNonNull(decisionPosition);
        return isOperationGranted(operationRequest.getOperationsUnion().stream().findFirst().get());
    }

    private void checkOperationForCurrentPosition(TreeExpression currentExpression) {
        if (logger.isDebugEnabled()) {
            logger.debug("checking operation for expression {}", currentExpression, currentExpression.getAsePosition());
        }
        if (currentExpression instanceof OperationContainer) {
            collectOperations((OperationContainer) currentExpression);
        }
        AsePosition currentPosition = currentExpression.getAsePosition();
        boolean allowed = isOperationGrantedAtPosition(currentPosition);
        if (currentPosition.equals(operationRequest.getReqPosition())) {
            if (logger.isDebugEnabled()) {
                logger.debug("decision at requested position {}: {}", currentExpression.getAsePosition(), allowed);
            }
            if (allowed) {
                grant();
                preventRecursion(); // will not recurse any deeper from this node
                abort(); // will return from any subsequent visit() immediately
            } else {
                deny();
                // we've found a negative result on the way to the child
                // nothing we can find there will change that result, so don't recurse
                preventNextRecursion();
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("decision at other than requested position {}: {}", currentExpression.getAsePosition(),
                                allowed);
            }
            if (allowed) {
                grant();
            } else {
                deny();
            }
        }
    }

    private void collectOperations(OperationContainer node) {
        Set<OperationExpression> granted = grantedOperations.peek();
        granted.addAll(node.getOperationsUnion());
    }

    private void inheritAllowedOperations(TreeExpression node) {
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
        if (logger.isDebugEnabled()) {
            logger.debug("before recursion into {}", child);
        }
        inheritDecision();
        inheritAllowedOperations(child);
        if (!(child.getAsePosition().equals(operationRequest.getReqPosition())
                        || isAncestorPosition(child.getAsePosition(),
                                        operationRequest.getReqPosition()))) {
            // we don't recurse into subtrees that cannot contain the operation request's
            // position
            // don't decide here, just omit the branch
            if (logger.isDebugEnabled()) {
                logger.debug("preventing recursion into {}", child);
            }
            preventNextRecursion();
            return;
        }
    }

    @Override
    protected void onAfterRecursion(TreeExpression parent, TreeExpression child) {
        if (logger.isDebugEnabled()) {
            logger.debug("returning from {}", child);
            logger.debug("allowed ops in {}: {}", child, Arrays.asList(grantedOperations.peek().toArray()));
        }
        // coming back from the recursion, we pop the stack
        // now, the top element is the one we pushed onto it at this level
        grantedOperations.pop();
        MaybeBoolean decisionFromLowerLevel = decision.pop();
        decision.pop(); // supersede this one by the one from the lower level
        decision.push(decisionFromLowerLevel);
    }

    @Override
    protected void onBeginVisit(ConnectionMessagesExpression other) {
        checkOperationForCurrentPosition(other);
    }

    /**
     * Takes the decision from the parent recursion level, defaulting to unknown at
     * the root.
     */
    private void inheritDecision() {
        MaybeBoolean inherited = this.decision.peek();
        this.decision.push(inherited);
    }

    @Override
    protected void onBeginVisit(GraphExpression other) {
        if (!other.getGraphIris().isEmpty()) {
            if (!other.getGraphIris().containsAll(operationRequest.getReqGraphs())) {
                preventRecursion();
                return;
            }
        }
        if (!other.getGraphTypes().isEmpty()) {
            if (!other.getGraphTypes().containsAll(operationRequest.getReqGraphTypes())) {
                preventRecursion();
                return;
            }
        }
        checkOperationForCurrentPosition(other);
    }

    private void deny() {
        this.decision.pop();
        this.decision.push(MaybeBoolean.FALSE);
    }

    private void grant() {
        this.decision.pop();
        this.decision.push(MaybeBoolean.TRUE);
    }

    private boolean isGranted() {
        return this.decision.peek().isTrue();
    }

    private boolean isDenied() {
        return this.decision.peek().isFalse();
    }

    @Override
    protected void onBeginVisit(ConnectionMessageExpression other) {
        checkOperationForCurrentPosition(other);
    }

    @Override
    protected void onBeginVisit(AseRoot other) {
        if (!other.getAtomStates().isEmpty()) {
            if (!other.getAtomStates().contains(operationRequest.getReqAtomState())) {
                preventRecursion();
                return;
            }
        }
        if (other.getTargetAtom() != null) {
            if (other.getTargetAtom().getAtomsRelativeAtomExpression()
                            .contains(RelativeAtomExpression.OPERATION_REQUESTOR)) {
                if (!operationRequest.getRequestor().equals(operationRequest.getReqConnectionTargetAtom())) {
                    preventRecursion();
                    return;
                }
            }
        }
        checkOperationForCurrentPosition(other);
    }

    @Override
    protected void onBeginVisit(AtomMessagesExpression other) {
        checkOperationForCurrentPosition(other);
    }

    @Override
    protected void onBeginVisit(AtomMessageExpression other) {
        checkOperationForCurrentPosition(other);
    }

    @Override
    protected void onBeginVisit(ConnectionsExpression other) {
        if (!other.getConnectionStates().isEmpty()) {
            if (!other.getConnectionStates().contains(operationRequest.getReqConnectionState())) {
                preventRecursion();
                return;
            }
        }
        if (other.getTargetAtom() != null) {
            if (other.getTargetAtom().getAtomsRelativeAtomExpression()
                            .contains(RelativeAtomExpression.OPERATION_REQUESTOR)) {
                if (!operationRequest.getRequestor().equals(operationRequest.getReqConnectionTargetAtom())) {
                    preventRecursion();
                    return;
                }
            }
        }
        checkOperationForCurrentPosition(other);
    }

    @Override
    protected void onBeginVisit(SocketExpression other) {
        if (!other.getSocketIris().isEmpty()) {
            if (!other.getSocketIris().contains(operationRequest.getReqSocket())) {
                preventRecursion();
                return;
            }
        }
        if (!other.getSocketTypes().isEmpty()) {
            if (!other.getSocketTypes().contains(operationRequest.getReqSocketType())) {
                preventRecursion();
                return;
            }
        }
        if (other.getTargetAtom() != null) {
            if (other.getTargetAtom().getAtomsRelativeAtomExpression()
                            .contains(RelativeAtomExpression.OPERATION_REQUESTOR)) {
                if (!operationRequest.getRequestor().equals(operationRequest.getReqConnectionTargetAtom())) {
                    preventRecursion();
                    return;
                }
            }
        }
        checkOperationForCurrentPosition(other);
    }

    @Override
    protected void onBeginVisit(ConnectionExpression other) {
        if (!other.getConnectionStates().isEmpty()) {
            if (!other.getConnectionStates().contains(operationRequest.getReqConnectionState())) {
                preventRecursion();
                return;
            }
        }
        if (other.getTargetAtom() != null) {
            if (other.getTargetAtom().getAtomsRelativeAtomExpression()
                            .contains(RelativeAtomExpression.OPERATION_REQUESTOR)) {
                // if the connection filters by requestor and we cannot be sure of the
                // requestor
                // (which is the case if we process a token-based auth), we must abort.
                if (operationRequest.getRequestor() == null
                                || !operationRequest.getRequestor()
                                                .equals(operationRequest.getReqConnectionTargetAtom())) {
                    deny();
                    preventRecursion();
                    return;
                }
            }
        }
        checkOperationForCurrentPosition(other);
    }

    private static class FalseUnless implements OperationExpression.Cases<Boolean> {
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
        public Boolean is(TokenOperationExpression granted) {
            String grantedScope = granted.getRequestToken().getTokenScopeString();
            URI grantedURI = granted.getRequestToken().getTokenScopeURI();
            long grantedValidity = AuthUtils.getExpiresAfterSecondsLong(granted.getRequestToken())
                            .orElse(DEFAULT_TOKEN_EXPIRES_AFTER_SECONDS);
            return requested.when(new FalseUnless() {
                @Override
                public Boolean is(TokenOperationExpression option) {
                    // by not specifiying a scope, you implicitly request all tokens
                    boolean scopeFits = option.getRequestToken().getTokenScopesUnion().isEmpty();
                    if (!scopeFits && grantedScope != null) {
                        String reqScope = option.getRequestToken().getTokenScopeString();
                        if (grantedScope.equals(reqScope)) {
                            scopeFits = true;
                        }
                    }
                    if (!scopeFits && grantedURI != null) {
                        if (grantedURI.equals(option.getRequestToken().getTokenScopeURI())) {
                            scopeFits = true;
                        }
                    }
                    if (!scopeFits) {
                        return false;
                    }
                    Optional<Long> requestedValidity = AuthUtils.getExpiresAfterSecondsLong(option.getRequestToken());
                    if (requestedValidity.isPresent()) {
                        return requestedValidity.get() <= grantedValidity;
                    }
                    return true;
                }
            });
        }

        @Override
        public Boolean is(SimpleOperationExpression granted) {
            if (granted.equals(ANY_OPERATION)) {
                return true;
            }
            return requested.when(new FalseUnless() {
                @Override
                public Boolean is(SimpleOperationExpression requestedOption) {
                    return granted.getNode().equals(requestedOption.getNode());
                }
            });
        }

        @Override
        public Boolean is(MessageOperationExpression granted) {
            return requested.when(new FalseUnless() {
                @Override
                public Boolean is(MessageOperationExpression requestedOption) {
                    if (ANY_MESSAGE_TYPE.equals(granted.getMessageToMessageWildcard())
                                    && !requestedOption.getMessageTosUnion().isEmpty()
                                    && (requestedOption.getMessageOnBehalfsUnion().isEmpty()
                                                    || ANY_MESSAGE_TYPE.equals(
                                                                    granted.getMessageOnBehalfMessageWildcard()))) {
                        return true;
                    }
                    if (ANY_MESSAGE_TYPE.equals(granted.getMessageOnBehalfMessageWildcard())
                                    && !requestedOption.getMessageOnBehalfsUnion().isEmpty()
                                    && (requestedOption.getMessageTosUnion().isEmpty()
                                                    || ANY_MESSAGE_TYPE.equals(
                                                                    granted.getMessageToMessageWildcard()))) {
                        return true;
                    }
                    Set<MessageType> requestedTos = collectMessageTypes(requestedOption.getMessageTosUnion());
                    if (!requestedTos.isEmpty()) {
                        Set<MessageType> grantedTos = collectMessageTypes(granted.getMessageTosUnion());
                        if (grantedTos.containsAll(requestedTos)) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                    Set<MessageType> requestedOnBehalfs = collectMessageTypes(
                                    requestedOption.getMessageOnBehalfsUnion());
                    if (!requestedOnBehalfs.isEmpty()) {
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
}
