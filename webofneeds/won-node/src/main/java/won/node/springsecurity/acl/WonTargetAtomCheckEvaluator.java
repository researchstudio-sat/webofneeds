package won.node.springsecurity.acl;

import java.lang.invoke.MethodHandles;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import won.auth.check.TargetAtomCheck;
import won.auth.check.TargetAtomCheckEvaluator;
import won.protocol.repository.ConnectionRepository;

public class WonTargetAtomCheckEvaluator implements TargetAtomCheckEvaluator {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final int RESTRICTS_SOCKETS = 1;
    private static final int RESTRICTS_SOCKET_TYPES = 2;
    private static final int RESTRICTS_CONNECTION_STATES = 4;
    @Autowired
    private ConnectionRepository connectionRepository;
    private TargetAtomCheckEvaluator[] evaluators = new TargetAtomCheckEvaluator[8];

    public WonTargetAtomCheckEvaluator() {
        evaluators[RESTRICTS_CONNECTION_STATES | RESTRICTS_SOCKET_TYPES
                        | RESTRICTS_SOCKETS] = check -> connectionRepository
                                        .existsWithAtomAndTargetAtomAndStatesAndSocketTypesAndSockets(
                                                        check.getAtom(),
                                                        check.getRequestedTarget(),
                                                        check.getAllowedConnectionStates(),
                                                        check.getAllowedSocketTypes(),
                                                        check.getAllowedSockets());
        evaluators[RESTRICTS_CONNECTION_STATES | RESTRICTS_SOCKET_TYPES] = check -> connectionRepository
                        .existsWithAtomAndTargetAtomAndStatesAndSocketTypes(
                                        check.getAtom(),
                                        check.getRequestedTarget(),
                                        check.getAllowedConnectionStates(),
                                        check.getAllowedSocketTypes());
        evaluators[RESTRICTS_CONNECTION_STATES | RESTRICTS_SOCKETS] = check -> connectionRepository
                        .existsWithAtomAndTargetAtomAndStatesAndSockets(
                                        check.getAtom(),
                                        check.getRequestedTarget(),
                                        check.getAllowedConnectionStates(),
                                        check.getAllowedSockets());
        evaluators[RESTRICTS_CONNECTION_STATES] = check -> connectionRepository.existsWithAtomAndTargetAtomAndStates(
                        check.getAtom(),
                        check.getRequestedTarget(),
                        check.getAllowedConnectionStates());
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
    public boolean isRequestorAllowedTarget(TargetAtomCheck check) {
        boolean restrictsConnectionStates = isPresent(check.getAllowedConnectionStates());
        boolean restrictsSocketTypes = isPresent(check.getAllowedSocketTypes());
        boolean restrictsSockets = isPresent(check.getAllowedSockets());
        int rcs = restrictsConnectionStates ? RESTRICTS_CONNECTION_STATES : 0;
        int rst = restrictsSocketTypes ? RESTRICTS_SOCKET_TYPES : 0;
        int rs = restrictsSockets ? RESTRICTS_SOCKETS : 0;
        int evaluatorIndex = rcs | rst | rs;
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
