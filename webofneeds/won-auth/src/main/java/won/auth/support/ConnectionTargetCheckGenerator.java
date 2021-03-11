package won.auth.support;

import won.auth.check.ConnectionTargetCheck;
import won.auth.model.*;

import java.net.URI;
import java.util.*;

public class ConnectionTargetCheckGenerator extends DefaultTreeExpressionVisitor {
    private Deque<ConnectionTargetCheck> checks = new ArrayDeque<>();
    private Set<ConnectionTargetCheck> connectionTargetChecks = new HashSet<>();

    public ConnectionTargetCheckGenerator(URI atom, URI candidateAtom) {
        ConnectionTargetCheck rootCheck = new ConnectionTargetCheck(atom, candidateAtom);
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

    public Set<ConnectionTargetCheck> getConnectionTargetChecks() {
        return connectionTargetChecks;
    }

    public void processPossibleTargetAtom(TreeExpression other) {
        if (other instanceof TargetAtomContainer) {
            TargetAtomContainer tac = (TargetAtomContainer) other;
            if (tac.getTargetAtom() != null) {
                collectTargetAtomCheck(tac);
            }
        }
    }

    public void processPossibleTargetWonNode(TreeExpression other) {
        if (other instanceof TargetAtomContainer) {
            TargetAtomContainer tac = (TargetAtomContainer) other;
            if (tac.getTargetWonNode() != null) {
                checks.peek().setWonNodeCheck(true);
                collectTargetAtomCheck(tac);
            }
        }
    }

    public void collectTargetAtomCheck(TargetAtomContainer node) {
        ConnectionTargetCheck collected = checks.peek().clone();
        if (collected.getAllowedConnectionStates().isEmpty()) {
            collected.setAllowedConnectionStatesCS(Collections.singleton(ConnectionState.CONNECTED));
        }
        this.connectionTargetChecks.add(collected);
    }

    @Override
    protected void onBeginVisit(ConnectionsExpression other) {
        ConnectionTargetCheck check = checks.peek();
        check.setAllowedConnectionStatesCS(other.getConnectionStates());
        processPossibleTargetAtom(other);
        processPossibleTargetWonNode(other);
    }

    @Override
    protected void onBeginVisit(SocketExpression other) {
        ConnectionTargetCheck check = checks.peek();
        check.setAllowedSockets(other.getSocketIris());
        check.setAllowedSocketTypes(other.getSocketTypes());
        processPossibleTargetAtom(other);
        processPossibleTargetWonNode(other);
    }

    @Override
    protected void onBeginVisit(AseRoot other) {
        processPossibleTargetAtom(other);
        processPossibleTargetWonNode(other);
    }

    @Override
    protected void onBeginVisit(ConnectionExpression other) {
        ConnectionTargetCheck check = checks.peek();
        Set<URI> inherited = check.getAllowedConnectionStates();
        if (inherited.isEmpty()) {
            check.setAllowedConnectionStatesCS(other.getConnectionStates());
        } else {
            check.intersectAllowedConnectionStatesCS(other.getConnectionStates());
        }
        processPossibleTargetAtom(other);
        processPossibleTargetWonNode(other);
    }
}
