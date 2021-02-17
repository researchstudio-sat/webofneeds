package won.auth.support;

import won.auth.model.*;
import won.shacl2java.runtime.model.GraphEntity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Set;

import static won.auth.support.TreeExpressionVisitorUtils.isSameTreeNode;

public class AseMerger extends DefaultTreeExpressionVisitor {
    private AseRoot merged;
    private Set<SimpleOperationExpression> simpleOperationExpressions;
    private Set<MessageOperationExpression> messageOperationExpressions;
    private ArrayDeque<TreeExpression> targetPathFromRoot = new ArrayDeque();
    private TreeExpression currentTarget;

    public AseMerger() {
        this(new AseRoot());
    }

    public AseMerger(AseRoot aseRoot) {
        this.merged = (AseRoot) aseRoot.clone();
        this.merged.detach();
        this.currentTarget = merged;
    }

    public AseRoot getMerged() {
        return merged;
    }

    @Override
    protected void onBeforeRecursion(TreeExpression host, TreeExpression child) {
        targetPathFromRoot.push(currentTarget);
        TreeExpression nextTarget = null;
        try {
            nextTarget = findNextTarget(currentTarget, child);
            if (nextTarget == null) {
                nextTarget = (TreeExpression) ((GraphEntity) child).clone();
                addChild(currentTarget, nextTarget);
            }
            currentTarget = nextTarget;
        } catch (Exception e) {
            throw new RuntimeException("Error collecting grants", e);
        }
    }

    private void addChild(TreeExpression currentTarget, TreeExpression child)
                    throws InvocationTargetException, IllegalAccessException {
        Method[] methods = currentTarget.getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            if (m.getParameterCount() == 1) {
                if (m.getName().startsWith("set")) {
                    Class<?> pt = m.getParameterTypes()[0];
                    if (pt.equals(child.getClass())) {
                        TreeExpression te = (TreeExpression) m.invoke(currentTarget, child);
                        return;
                    }
                }
                if (m.getName().startsWith("add")) {
                    Type type = m.getParameterTypes()[0];
                    if (type.equals(child.getClass())) {
                        Collection<TreeExpression> candidates = (Collection<TreeExpression>) m
                                        .invoke(currentTarget, child);
                        return;
                    }
                }
            }
        }
        throw new IllegalStateException(
                        String.format("Cannot add child %s to %s: no setter/adder found", child.toString(),
                                        currentTarget.toString()));
    }

    private TreeExpression findNextTarget(TreeExpression currentTarget, TreeExpression child)
                    throws InvocationTargetException, IllegalAccessException {
        Method[] methods = currentTarget.getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            if (m.getParameterCount() == 0 && m.getName().startsWith("get")) {
                Class<?> rt = m.getReturnType();
                if (rt.equals(child.getClass())) {
                    TreeExpression te = (TreeExpression) m.invoke(currentTarget);
                    if (isSameTreeNode(te, child)) {
                        return te;
                    }
                    return null;
                }
                Type type = m.getGenericReturnType();
                if (!(type instanceof ParameterizedType)) {
                    continue;
                }
                ParameterizedType pt = (ParameterizedType) type;
                if (Collection.class.isAssignableFrom((Class<?>) pt.getRawType())) {
                    Type[] typeArgs = pt.getActualTypeArguments();
                    if (typeArgs.length == 1 && typeArgs[0].equals(child.getClass())) {
                        Collection<TreeExpression> candidates = (Collection<TreeExpression>) m.invoke(currentTarget);
                        for (TreeExpression candidate : candidates) {
                            if (isSameTreeNode(candidate, child)) {
                                return candidate;
                            }
                        }
                        return null;
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected void onAfterRecursion(TreeExpression host, TreeExpression child) {
        currentTarget = targetPathFromRoot.pop();
    }

    @Override
    protected void onBeginVisit(AtomMessageExpression host) {
        copyOperations(host, (OperationContainer) currentTarget);
    }

    @Override
    protected void onBeginVisit(ConnectionMessagesExpression host) {
        copyOperations(host, (OperationContainer) currentTarget);
    }

    @Override
    protected void onBeginVisit(AseRoot host) {
        copyOperations(host, (OperationContainer) currentTarget);
    }

    @Override
    protected void onBeginVisit(GraphExpression host) {
        copyOperations(host, (OperationContainer) currentTarget);
    }

    @Override
    protected void onBeginVisit(AtomMessagesExpression host) {
        copyOperations(host, (OperationContainer) currentTarget);
    }

    @Override
    protected void onBeginVisit(SocketExpression host) {
        copyOperations(host, (OperationContainer) currentTarget);
    }

    @Override
    protected void onBeginVisit(ConnectionMessageExpression host) {
        copyOperations(host, (OperationContainer) currentTarget);
    }

    @Override
    protected void onBeginVisit(ConnectionsExpression host) {
        copyOperations(host, (OperationContainer) currentTarget);
    }

    @Override
    protected void onBeginVisit(ConnectionExpression host) {
        copyOperations(host, (OperationContainer) currentTarget);
    }

    private void copyOperations(OperationContainer from, OperationContainer to) {
        from.getOperationsTokenOperationExpression().stream().forEach(o -> to.addOperationsTokenOperationExpression(o));
        from.getOperationsMessageOperationExpression().stream()
                        .forEach(o -> to.addOperationsMessageOperationExpression(o));
        from.getOperationsSimpleOperationExpression().stream()
                        .forEach(o -> to.addOperationsSimpleOperationExpression(o));
    }
}
