package won.auth;

import org.apache.thrift.TException;
import won.auth.check.TargetAtomCheck;
import won.auth.model.DefaultTreeExpressionVisitor;
import won.auth.model.*;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

class TargetAtomCheckGenerator extends DefaultTreeExpressionVisitor {
    private Deque<TargetAtomCheck> checks = new ArrayDeque<>();
    private Set<TargetAtomCheck> targetAtomChecks = new HashSet<>();

    public TargetAtomCheckGenerator(URI atom, URI candidateAtom) {
        TargetAtomCheck rootCheck = new TargetAtomCheck(atom, candidateAtom);
        checks.push(rootCheck);
    }

    @Override
    protected void onBeforeRecursion(TreeExpression host, TreeExpression child) {
        if (child instanceof TargetAtomContainer) {
            checks.push(checks.peek().clone());
        }
    }

    @Override
    protected void onAfterRecursion(TreeExpression parent, TreeExpression child) {
        // pop the top off our stack if we just recursed into a
        // child for which we pushed an element onto it
        if (child instanceof TargetAtomContainer) {
            checks.pop();
        }
    }

    public Set<TargetAtomCheck> getTargetAtomChecks() {
        return targetAtomChecks;
    }

    public void processPossibleTargetAtom(TreeExpression other) {
        if (other instanceof TargetAtomContainer) {
            TargetAtomContainer tac = (TargetAtomContainer) other;
            if (tac.getTargetAtom() != null) {
                collectTargetAtomCheck(tac);
            }
        }
    }

    public void collectTargetAtomCheck(TargetAtomContainer node) {
        TargetAtomCheck collected = checks.peek().clone();
        if (collected.getAllowedConnectionStates().isEmpty()) {
            collected.setAllowedConnectionStatesCS(Collections.singleton(ConnectionState.CONNECTED));
        }
        this.targetAtomChecks.add(collected);
    }

    @Override
    protected void onBeginVisit(ConnectionsExpression other) {
        TargetAtomCheck check = checks.peek();
        check.setAllowedConnectionStatesCS(other.getConnectionStates());
        processPossibleTargetAtom(other);
    }

    @Override
    protected void onBeginVisit(SocketExpression other) {
        TargetAtomCheck check = checks.peek();
        check.setAllowedSockets(other.getSocketIris());
        check.setAllowedSocketTypes(other.getSocketTypes());
        processPossibleTargetAtom(other);
    }

    @Override
    protected void onBeginVisit(AseRoot other) {
        processPossibleTargetAtom(other);
    }

    @Override
    protected void onBeginVisit(ConnectionExpression other) {
        TargetAtomCheck check = checks.peek();
        Set<URI> inherited = check.getAllowedConnectionStates();
        if (inherited.isEmpty()) {
            check.setAllowedConnectionStatesCS(other.getConnectionStates());
        } else {
            check.intersectAllowedConnectionStatesCS(other.getConnectionStates());
        }
        processPossibleTargetAtom(other);
    }
}
