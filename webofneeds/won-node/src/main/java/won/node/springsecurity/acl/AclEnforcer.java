package won.node.springsecurity.acl;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import won.auth.AuthUtils;
import won.auth.model.AclEvalResult;
import won.auth.model.DecisionValue;
import won.auth.model.OperationRequest;
import won.protocol.model.Connection;
import won.protocol.model.DataWithEtag;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

public class AclEnforcer implements MethodInterceptor {
    public AclEnforcer() {
    }

    private static OperationRequest populateOperationRequest(WonAclEvalContext wonAclEvalContext, Connection con) {
        OperationRequest or = wonAclEvalContext.getOperationRequest();
        or.setReqConnection(con.getConnectionURI());
        or.setReqSocket(con.getSocketURI());
        or.setReqSocketType(con.getTypeURI());
        or.setReqConnectionState(AuthUtils.toAuthConnectionState(con.getState()));
        return or;
    }

    private static boolean isAccessGranted(WonAclEvalContext wonAclEvalContext, Connection con) {
        OperationRequest or = populateOperationRequest(wonAclEvalContext, con);
        AclEvalResult result = wonAclEvalContext.decideAndRemember(or);
        return DecisionValue.ACCESS_GRANTED.equals(result.getDecision());
    }

    private static Collection<Connection> filterByAcl(WonAclEvalContext wonAclEvalContext,
                    Collection<Connection> connections) {
        return connections.stream().filter(con -> isAccessGranted(wonAclEvalContext, con)).collect(Collectors.toList());
    }

    private static Optional<Connection> filterByAcl(WonAclEvalContext wonAclEvalContext,
                    Optional<Connection> connections) {
        return connections.filter(con -> isAccessGranted(wonAclEvalContext, con));
    }

    private static Connection filterByAcl(WonAclEvalContext wonAclEvalContext, Connection con) {
        if (isAccessGranted(wonAclEvalContext, con)) {
            return con;
        }
        return null;
    }

    private static DataWithEtag<Connection> filterByAcl(WonAclEvalContext wonAclEvalContext,
                    DataWithEtag<Connection> dwe) {
        if (dwe.getData() == null) {
            // if we have no data, assume not found or deleted (which we don't forbid)
            return dwe;
        }
        if (isAccessGranted(wonAclEvalContext, dwe.getData())) {
            return dwe;
        }
        return DataWithEtag.accessDenied();
    }

    private static Slice<Connection> filterByAcl(WonAclEvalContext wonAclEvalContext, Slice<Connection> slice) {
        Collection<Connection> filtered = filterByAcl(wonAclEvalContext, slice.getContent());
        return new SliceImpl(Arrays.asList(filtered.toArray()), slice.getPageable(), slice.hasNext());
    }

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        Type type = methodInvocation.getMethod().getGenericReturnType();
        if (type.equals(Connection.class)) {
            return filterByAcl(
                            getContextFromThreadLocal(),
                            (Connection) methodInvocation.proceed());
        } else if (isGenericTypeWithTypeParam(type, Collection.class, Connection.class)) {
            return filterByAcl(
                            getContextFromThreadLocal(),
                            (Collection<Connection>) methodInvocation.proceed());
        } else if (isGenericTypeWithTypeParam(type, Optional.class, Connection.class)) {
            return filterByAcl(
                            getContextFromThreadLocal(),
                            (Optional<Connection>) methodInvocation.proceed());
        } else if (isGenericTypeWithTypeParam(type, DataWithEtag.class, Connection.class)) {
            return filterByAcl(
                            getContextFromThreadLocal(),
                            (DataWithEtag<Connection>) methodInvocation.proceed());
        } else if (isGenericTypeWithTypeParam(type, Slice.class, Connection.class)) {
            return filterByAcl(
                            getContextFromThreadLocal(),
                            (Slice<Connection>) methodInvocation.proceed());
        }
        return methodInvocation.proceed();
    }

    private boolean isGenericTypeWithTypeParam(Type type, Class<?> rawType, Class<?> typeParam) {
        if (type instanceof ParameterizedType) {
            ParameterizedType ptype = (ParameterizedType) type;
            if (rawType.isAssignableFrom((Class<?>) ptype.getRawType())) {
                if (ptype.getActualTypeArguments().length == 1) {
                    return typeParam.isAssignableFrom((Class<?>) ptype.getActualTypeArguments()[0]);
                }
            }
        }
        return false;
    }

    public WonAclEvalContext getContextFromThreadLocal() {
        WonAclEvalContext ctx = WonAclRequestHelper.getWonAclEvaluationContextFromThreadLocal();
        if (ctx == null) {
            throw new IllegalStateException("Cannot enforce ACL: no WonAclEvalContext found in threadLocal.");
        }
        return ctx;
    }
}
