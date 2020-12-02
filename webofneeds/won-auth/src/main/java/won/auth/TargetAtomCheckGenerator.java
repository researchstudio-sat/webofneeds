package won.auth;

import won.auth.check.TargetAtomCheck;
import won.auth.model.*;

import java.net.URI;
import java.util.*;

class TargetAtomCheckGenerator implements TreeExpressionVisitor {
    private Deque<TreeExpression> pathFromRoot = new ArrayDeque<>();
    private Set<TargetAtomCheck> targetAtomChecks = new HashSet<>();
    private URI atom;
    private URI requestorAtom;

    public TargetAtomCheckGenerator(URI atom, URI requestorAtom) {
        this.atom = atom;
        this.requestorAtom = requestorAtom;
    }

    @Override
    public void onAfterRecursion(Object parent, Object child) {
        // pop the top off our stack if we just recursed into a
        // child for which we pushed an element onto it
        if (child instanceof ConnectionsExpression
                        || child instanceof ConnectionExpression
                        || child instanceof SocketExpression) {
            pathFromRoot.pop();
        }
    }

    public Set<TargetAtomCheck> getTargetAtomChecks() {
        return targetAtomChecks;
    }

    @Override
    public void visit(ConnectionExpression other) {
        pathFromRoot.push(other);
    }

    public void collectTargetAtomCheck(TargetAtomContainer node) {
        TargetAtomCheck check = TargetAtomCheck.of(atom, requestorAtom,
                        Collections.unmodifiableCollection(pathFromRoot));
        this.targetAtomChecks.add(check);
    }

    @Override
    public void visit(ConnectionsExpression other) {
        pathFromRoot.push(other);
    }

    @Override
    public void visit(SocketExpression other) {
        pathFromRoot.push(other);
    }
}
