package won.auth.support;

import won.auth.model.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

public class TreeExpressionVisitorUtils {
    public static boolean isAncestorPosition(AsePosition ancestorCandidate, AsePosition pos) {
        return isAncestorPosition(ancestorCandidate, pos, new HashSet<>());
    }

    public static boolean isAncestorPosition(AsePosition ancestorCandidate, AsePosition pos,
                    Set<AsePosition> visited) {
        visited.add(pos);
        if (pos.equals(ancestorCandidate)) {
            return false;
        }
        AsePosition parent = pos.getParentPosition();
        if (parent == null) {
            return false;
        }
        if (parent.equals(ancestorCandidate)) {
            return true;
        }
        if (visited.contains(parent)) {
            throw new IllegalStateException(String.format("ASE ancestor cycle detected via %s and %s", pos, parent));
        }
        return isAncestorPosition(ancestorCandidate, parent, visited);
    }

    public static boolean isSameTreeNode(TreeExpression left, TreeExpression right) {
        if (left == null || right == null) {
            return false;
        }
        if (!left.getClass().equals(right.getClass())) {
            return false;
        }
        if (left instanceof AseRoot) {
            // no filters in root
            return true;
        }
        if (left instanceof GraphExpression) {
            return graphExpressionEquality.apply((GraphExpression) left, (GraphExpression) right);
        }
        if (left instanceof SocketExpression) {
            return socketExpressionEquality.apply((SocketExpression) left, (SocketExpression) right);
        }
        if (left instanceof ConnectionsExpression) {
            return connectionsExpressionEquality.apply((ConnectionsExpression) left, (ConnectionsExpression) right);
        }
        if (left instanceof ConnectionExpression) {
            return connectionExpressionEquality.apply((ConnectionExpression) left, (ConnectionExpression) right);
        }
        if (left instanceof ConnectionMessagesExpression) {
            return inheritingExpressionEquality.apply((Inheriting) left, (Inheriting) right);
        }
        if (left instanceof ConnectionMessageExpression) {
            return inheritingExpressionEquality.apply((Inheriting) left, (Inheriting) right);
        }
        if (left instanceof AtomMessagesExpression) {
            return inheritingExpressionEquality.apply((Inheriting) left, (Inheriting) right);
        }
        if (left instanceof AtomMessageExpression) {
            return inheritingExpressionEquality.apply((Inheriting) left, (Inheriting) right);
        }
        throw new IllegalStateException(String.format("Cannot handle TreeExpression subclass %s", left.getClass()));
    }

    private static BiFunction<GraphExpression, GraphExpression, Boolean> graphExpressionEquality = (GraphExpression l,
                    GraphExpression r) -> Objects.deepEquals(l.getGraphTypes(), r.getGraphTypes())
                                    && Objects.deepEquals(l.getGraphIris(), r.getGraphIris())
                                    && Objects.deepEquals(l.getInherit(), r.getInherit());
    private static BiFunction<SocketExpression, SocketExpression, Boolean> socketExpressionEquality = (
                    SocketExpression l, SocketExpression r) -> Objects.deepEquals(l.getInherit(), r.getInherit())
                                    && Objects.deepEquals(l.getSocketIris(), r.getSocketIris())
                                    && Objects.deepEquals(l.getSocketTypes(), r.getSocketTypes());
    private static BiFunction<ConnectionsExpression, ConnectionsExpression, Boolean> connectionsExpressionEquality = (
                    ConnectionsExpression l,
                    ConnectionsExpression r) -> Objects.deepEquals(l.getInherit(), r.getInherit())
                                    && Objects.deepEquals(l.getConnectionStates(), r.getConnectionStates());
    private static BiFunction<ConnectionExpression, ConnectionExpression, Boolean> connectionExpressionEquality = (
                    ConnectionExpression l,
                    ConnectionExpression r) -> Objects.deepEquals(l.getInherit(), r.getInherit())
                                    && Objects.deepEquals(l.getConnectionStates(), r.getConnectionStates());
    private static BiFunction<Inheriting, Inheriting, Boolean> inheritingExpressionEquality = (Inheriting l,
                    Inheriting r) -> Objects
                                    .deepEquals(l.getInherit(), r.getInherit());
}
