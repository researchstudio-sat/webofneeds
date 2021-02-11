package won.auth.socket.support;

import org.apache.jena.graph.Graph;
import won.auth.AuthUtils;
import won.auth.model.*;
import won.shacl2java.Shacl2JavaInstanceFactory;

import java.net.URI;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static won.shacl2java.runtime.DeepEqualsUtils.same;
import static won.shacl2java.runtime.DeepEqualsUtils.sameContent;

public class SocketAclAlgorithms {
    private static void replaceAuthorizationRequestorPlaceholder(Authorization auth, URI requestingAtom) {
        GeneralVisitor visitor = new DefaultGeneralVisitor() {
            @Override
            protected void onBeginVisit(AtomExpression host) {
                if (host.getAtomsRelativeAtomExpression()
                                .contains(RelativeAtomExpression.AUTHORIZATION_REQUESTOR)) {
                    host.setAtomsRelativeAtomExpression(
                                    host.getAtomsRelativeAtomExpression()
                                                    .stream()
                                                    .filter(e -> !e.equals(
                                                                    RelativeAtomExpression.AUTHORIZATION_REQUESTOR))
                                                    .collect(Collectors.toSet()));
                    host.addAtomsURI(requestingAtom);
                }
                host.detach();
            }
        };
        visitor.visit(auth);
    }

    private static Set<Authorization> getAuthorizations(Graph authGraph) {
        if (authGraph == null && authGraph.isEmpty()) {
            return new HashSet<>();
        }
        Shacl2JavaInstanceFactory factory = AuthUtils.instanceFactory();
        return factory.accessor(authGraph).getInstancesOfType(Authorization.class);
    }

    /**
     * Compares the two authorizations ignoring the requestedBy field.
     *
     * @param left
     * @param right
     * @return
     */
    private static boolean isSameAuthorization(Authorization left, Authorization right) {
        if (left.getGranteeGranteeWildcard() != null) {
            if (left.getGranteeGranteeWildcard() != right.getGranteeGranteeWildcard()) {
                return false;
            }
        }
        if (!sameContent(left.getGranteesAseRoot(), right.getGranteesAseRoot())) {
            return false;
        }
        if (!sameContent(left.getGranteesAtomExpression(), right.getGranteesAtomExpression())) {
            return false;
        }
        if (!sameContent(left.getBearers(), right.getBearers())) {
            return false;
        }
        if (!sameContent(left.getGrants(), right.getGrants())) {
            return false;
        }
        if (!same(left.getProvideAuthInfo(), right.getProvideAuthInfo())) {
            return false;
        }
        return true;
    }

    /**
     * Adds the specified <code>authsToAdd</code> to the <code>authGraph</code>,
     * assuming that the <code>localSocket</code> specifies that, either to install
     * its <code>localAuths</code> or to install the <code>targetAuths</code> of a
     * targetSocket that is being connected to.
     *
     * @param authGraph
     * @param authsToAdd
     * @param localSocket
     * @param requestingAtom either the local or the remote atom (depending on which
     * authorizations we are installing).
     * @return
     */
    public Graph addAuthorizationsForSocket(Graph authGraph, Set<Authorization> authsToAdd, URI localSocket,
                    URI requestingAtom) {
        Set<Authorization> existingAuths = getAuthorizations(authGraph);
        for (Authorization authToAdd : authsToAdd) {
            if (requestingAtom != null) {
                replaceAuthorizationRequestorPlaceholder(authToAdd, requestingAtom);
            }
            Optional<Authorization> existingOpt = existingAuths.stream().filter(e -> isSameAuthorization(e, authToAdd))
                            .findFirst();
            if (existingOpt.isPresent()) {
                Authorization existingAuth = existingOpt.get();
                existingAuth.addRequestedBy(localSocket);
            } else {
                authToAdd.addRequestedBy(localSocket);
                existingAuths.add(authToAdd);
            }
        }
        existingAuths.forEach(a -> a.detach());
        return RdfOutput.toGraph(existingAuths);
    }

    /**
     * In all authorizations found in the authGraph, remove <code>localSocket</code>
     * from the <code>requestedBy</code> entries if any of the following is true:
     * <ul>
     * <li><code>removeAsRequestingSocket == true</code></li>
     * <li>the <code>requestingAtom</code> is mentioned in the authorization.
     * </ul>
     * If an authorization has no <code>requestedBy</code> entries left, it is left
     * out in the result.
     *
     * @param authGraph the auth graph to manipulate
     * @param localSocket the URI of the atom's own socket
     * @param requestingAtom the URI of the atom that
     * @param removeAsRequestingSocket
     * @return
     */
    public Graph removeAuthorizationsForSocket(Graph authGraph, URI localSocket, URI requestingAtom,
                    boolean removeAsRequestingSocket) {
        Objects.requireNonNull(authGraph);
        Objects.requireNonNull(localSocket);
        Objects.requireNonNull(requestingAtom);
        Shacl2JavaInstanceFactory factory = AuthUtils.instanceFactory();
        Set<Authorization> existingAuths = factory.accessor(authGraph).getInstancesOfType(Authorization.class);
        Set<Authorization> modifiedAuths = existingAuths.stream()
                        .map(existingAuth -> {
                            if (removeAsRequestingSocket || mentionsAtom(existingAuth, requestingAtom)) {
                                removeFromRequestedBy(existingAuth, localSocket);
                            }
                            return existingAuth;
                        })
                        .filter(existingAuth -> !existingAuth.getRequestedBys().isEmpty())
                        .collect(Collectors.toSet());
        modifiedAuths.forEach(auth -> auth.detach());
        return RdfOutput.toGraph(modifiedAuths);
    }

    private boolean mentionsAtom(Authorization authorization, URI atom) {
        AtomicBoolean result = new AtomicBoolean(false);
        GeneralVisitor visitor = new DefaultGeneralVisitor() {
            @Override
            protected void onBeginVisit(AtomExpression host) {
                if (host.getAtomsURI().contains(atom)) {
                    result.set(true);
                }
            }
        };
        visitor.visit(authorization);
        return result.get();
    }

    private void removeFromRequestedBy(Authorization authorization, URI socket) {
        Set<URI> requestedBys = authorization.getRequestedBys().stream().filter(u -> !u.equals(socket))
                        .collect(
                                        Collectors.toSet());
        authorization.setRequestedBys(requestedBys);
    }
}
