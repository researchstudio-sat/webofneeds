package won.node.springsecurity.acl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.auth.check.ConnectionTargetCheck;
import won.auth.check.TargetAtomCheckEvaluator;
import won.protocol.model.ConnectionState;
import won.protocol.repository.ConnectionRepository;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class WonTargetAtomCheckEvaluator implements TargetAtomCheckEvaluator {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final int RESTRICTS_SOCKETS = 1;
    private static final int RESTRICTS_SOCKET_TYPES = 2;
    private static final int RESTRICTS_CONNECTION_STATES = 4;
    private static final int WON_NODE_CHECK = 8;
    @Autowired
    private ConnectionRepository connectionRepository;
    private TargetAtomCheckEvaluator[] evaluators = new TargetAtomCheckEvaluator[16];

    public WonTargetAtomCheckEvaluator() {
        evaluators[WON_NODE_CHECK
                        | RESTRICTS_CONNECTION_STATES
                        | RESTRICTS_SOCKET_TYPES
                        | RESTRICTS_SOCKETS] = check -> connectionRepository
                                        .existsWithAtomAndTargetAtomPrefixAndStatesAndSocketTypesAndSockets(
                                                        check.getAtom(),
                                                        check.getRequestedTarget().toString(),
                                                        check.getAllowedConnectionStates().stream()
                                                                        .map(ConnectionState::fromURI).collect(toSet()),
                                                        check.getAllowedSocketTypes(),
                                                        check.getAllowedSockets());
        evaluators[WON_NODE_CHECK
                        | RESTRICTS_CONNECTION_STATES
                        | RESTRICTS_SOCKET_TYPES] = check -> connectionRepository
                                        .existsWithAtomAndTargetAtomPrefixAndStatesAndSocketTypes(
                                                        check.getAtom(),
                                                        check.getRequestedTarget().toString(),
                                                        check.getAllowedConnectionStates().stream()
                                                                        .map(ConnectionState::fromURI).collect(toSet()),
                                                        check.getAllowedSocketTypes());
        evaluators[WON_NODE_CHECK
                        | RESTRICTS_CONNECTION_STATES
                        | RESTRICTS_SOCKETS] = check -> connectionRepository
                                        .existsWithAtomAndTargetAtomPrefixAndStatesAndSockets(
                                                        check.getAtom(),
                                                        check.getRequestedTarget().toString(),
                                                        check.getAllowedConnectionStates().stream()
                                                                        .map(ConnectionState::fromURI).collect(toSet()),
                                                        check.getAllowedSockets());
        evaluators[WON_NODE_CHECK
                        | RESTRICTS_CONNECTION_STATES] = check -> connectionRepository
                                        .existsWithAtomAndTargetAtomPrefixAndStates(
                                                        check.getAtom(),
                                                        check.getRequestedTarget().toString(),
                                                        check.getAllowedConnectionStates().stream()
                                                                        .map(ConnectionState::fromURI)
                                                                        .collect(toSet()));
        evaluators[WON_NODE_CHECK
                        | RESTRICTS_SOCKET_TYPES
                        | RESTRICTS_SOCKETS] = check -> connectionRepository
                                        .existsWithAtomAndTargetAtomPrefixAndSocketTypesAndSockets(
                                                        check.getAtom(),
                                                        check.getRequestedTarget().toString(),
                                                        check.getAllowedSocketTypes(),
                                                        check.getAllowedSockets());
        evaluators[WON_NODE_CHECK
                        | RESTRICTS_SOCKET_TYPES] = check -> connectionRepository
                                        .existsWithAtomAndTargetAtomPrefixAndSocketTypes(
                                                        check.getAtom(),
                                                        check.getRequestedTarget().toString(),
                                                        check.getAllowedSocketTypes());
        evaluators[WON_NODE_CHECK
                        | RESTRICTS_SOCKETS] = check -> connectionRepository
                                        .existsWithAtomAndTargetAtomPrefixAndSockets(
                                                        check.getAtom(),
                                                        check.getRequestedTarget().toString(),
                                                        check.getAllowedSockets());
        evaluators[RESTRICTS_CONNECTION_STATES
                        | RESTRICTS_SOCKET_TYPES
                        | RESTRICTS_SOCKETS] = check -> connectionRepository
                                        .existsWithAtomAndTargetAtomAndStatesAndSocketTypesAndSockets(
                                                        check.getAtom(),
                                                        check.getRequestedTarget(),
                                                        check.getAllowedConnectionStates().stream()
                                                                        .map(ConnectionState::fromURI).collect(toSet()),
                                                        check.getAllowedSocketTypes(),
                                                        check.getAllowedSockets());
        evaluators[RESTRICTS_CONNECTION_STATES | RESTRICTS_SOCKET_TYPES] = check -> connectionRepository
                        .existsWithAtomAndTargetAtomAndStatesAndSocketTypes(
                                        check.getAtom(),
                                        check.getRequestedTarget(),
                                        check.getAllowedConnectionStates().stream()
                                                        .map(ConnectionState::fromURI).collect(toSet()),
                                        check.getAllowedSocketTypes());
        evaluators[RESTRICTS_CONNECTION_STATES | RESTRICTS_SOCKETS] = check -> connectionRepository
                        .existsWithAtomAndTargetAtomAndStatesAndSockets(
                                        check.getAtom(),
                                        check.getRequestedTarget(),
                                        check.getAllowedConnectionStates().stream()
                                                        .map(ConnectionState::fromURI).collect(toSet()),
                                        check.getAllowedSockets());
        evaluators[RESTRICTS_CONNECTION_STATES] = check -> connectionRepository.existsWithAtomAndTargetAtomAndStates(
                        check.getAtom(),
                        check.getRequestedTarget(),
                        check.getAllowedConnectionStates().stream()
                                        .map(ConnectionState::fromURI).collect(toSet()));
        evaluators[RESTRICTS_SOCKET_TYPES | RESTRICTS_SOCKETS] = check -> connectionRepository
                        .existsWithAtomAndTargetAtomAndSocketTypesAndSockets(
                                        check.getAtom(),
                                        check.getRequestedTarget(),
                                        check.getAllowedSocketTypes(),
                                        check.getAllowedSockets());
        evaluators[RESTRICTS_SOCKET_TYPES] = check -> connectionRepository.existsWithAtomAndTargetAtomAndSocketTypes(
                        check.getAtom(),
                        check.getRequestedTarget(),
                        check.getAllowedSocketTypes());
        evaluators[RESTRICTS_SOCKETS] = check -> connectionRepository.existsWithAtomAndTargetAtomAndSockets(
                        check.getAtom(),
                        check.getRequestedTarget(),
                        check.getAllowedSockets());
        evaluators[0] = check -> connectionRepository.existsWithAtomAndTargetAtom(
                        check.getAtom(),
                        check.getRequestedTarget());
    }

    @Override
    public boolean isRequestorAllowedTarget(ConnectionTargetCheck check) {
        boolean restrictsConnectionStates = isPresent(check.getAllowedConnectionStates());
        boolean restrictsSocketTypes = isPresent(check.getAllowedSocketTypes());
        boolean restrictsSockets = isPresent(check.getAllowedSockets());
        int rcs = restrictsConnectionStates ? RESTRICTS_CONNECTION_STATES : 0;
        int rst = restrictsSocketTypes ? RESTRICTS_SOCKET_TYPES : 0;
        int rs = restrictsSockets ? RESTRICTS_SOCKETS : 0;
        int wnc = check.isWonNodeCheck() ? WON_NODE_CHECK : 0;
        int evaluatorIndex = wnc | rcs | rst | rs;
        TargetAtomCheckEvaluator evaluator = evaluators[evaluatorIndex];
        if (logger.isDebugEnabled()) {
            logger.debug("Using evaluator with index {} (rcs:{}, rst:{}, rs:{}) ",
                            new Object[] { evaluatorIndex, restrictsConnectionStates, restrictsSocketTypes,
                                            restrictsSockets });
        }
        return evaluator.isRequestorAllowedTarget(check);
    }

    public boolean isPresent(Set collection) {
        return collection != null && !collection.isEmpty();
    }
}
