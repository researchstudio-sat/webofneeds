package won.auth.socket;

import org.apache.jena.graph.Graph;

public interface SocketAuthorizationAclModifier {
    /**
     * Adds the socket's <code>localAuth</code> authorizations to the specified
     * graph, i.e., the authorizations that the socket of an atom specifies for the
     * atom's ACL.
     * <p>
     * This operation should be executed when the socket is added to the atom, ie.
     * upon atom creation or modification.
     * </p>
     *
     * @param authGraph
     * @param socketAuthorizations
     * @return
     */
    Graph addLocalAuth(Graph authGraph, SocketAuthorizations socketAuthorizations);

    /**
     * Adds the socket's <code>targetAuth</code> authorizations to the specified
     * graph, i.e., the authorizations the target socket of an atom's connection
     * specifies for the atom's ACL.
     * <p>
     * This operation should be executed when establishing a connection.
     * </p>
     *
     * @param authGraph
     * @param socketAuthorizations
     * @return
     */
    Graph addTargetAuth(Graph authGraph, SocketAuthorizations socketAuthorizations);

    /**
     * Removes the socket's <code>localAuth</code> authorizations to the specified
     * graph, i.e., the authorizations that the socket of an atom specifies for the
     * atom's ACL.
     * <p>
     * This operation should be executed when the socket is removed from the atom,
     * ie. upon modification.
     * </p>
     *
     * @param authGraph
     * @param socketAuthorizations
     * @return
     */
    Graph removeLocalAuth(Graph authGraph, SocketAuthorizations socketAuthorizations);

    /**
     * Removes the socket's <code>targetAuth</code> authorizations to the specified
     * graph, i.e., the authorizations the target socket of an atom's connection
     * specifies for the atom's ACL.
     * <p>
     * This operation should be executed when closing an established connection.
     * </p>
     *
     * @param authGraph
     * @param socketAuthorizations
     * @param socketHasMoreConnections
     * @return
     */
    Graph removeTargetAuth(Graph authGraph, SocketAuthorizations socketAuthorizations,
                    boolean socketHasMoreConnections);
}
