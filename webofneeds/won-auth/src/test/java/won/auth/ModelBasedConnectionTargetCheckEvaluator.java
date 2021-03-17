package won.auth;

import org.apache.jena.graph.Graph;
import org.apache.jena.shacl.Shapes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.auth.check.ConnectionTargetCheck;
import won.auth.check.ConnectionTargetCheckEvaluator;
import won.auth.test.model.Atom;
import won.auth.test.model.Connection;
import won.auth.test.model.Socket;
import won.shacl2java.Shacl2JavaInstanceFactory;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ModelBasedConnectionTargetCheckEvaluator implements ConnectionTargetCheckEvaluator {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    Shacl2JavaInstanceFactory instanceFactory;
    Shacl2JavaInstanceFactory.Accessor accessor = null;

    public ModelBasedConnectionTargetCheckEvaluator(Shapes shapes, String packageName) {
        this.instanceFactory = new Shacl2JavaInstanceFactory(shapes, packageName);
    }

    public void loadData(Graph data) {
        this.accessor = this.instanceFactory.accessor(data);
    }

    public Shacl2JavaInstanceFactory getInstanceFactory() {
        return instanceFactory;
    }

    @Override
    public boolean isRequestorAllowedTarget(ConnectionTargetCheck check) {
        Optional<Atom> atomOpt = accessor.getInstanceOfType(check.getAtom().toString(), Atom.class);
        if (!atomOpt.isPresent()) {
            logger.debug("No data found for atom {} ", check.getAtom());
            return false;
        }
        Set<Socket> sockets = atomOpt.get().getSockets();
        if (logger.isDebugEnabled()) {
            logger.debug("candidate sockets, initially: {} ", Arrays.asList(sockets.toArray()));
        }
        sockets = sockets
                        .parallelStream()
                        .filter(socket -> check.isSocketAllowed(URI.create(socket.getNode().toString())))
                        .collect(Collectors.toSet());
        if (logger.isDebugEnabled()) {
            logger.debug("candidate sockets after filtering socket URIs: {} ", Arrays.asList(sockets.toArray()));
        }
        sockets = sockets
                        .parallelStream()
                        .filter(socket -> check.isSocketTypeAllowed(socket.getSocketDefinition()))
                        .collect(Collectors.toSet());
        if (logger.isDebugEnabled()) {
            logger.debug("candidate sockets after filtering socket types: {} ", Arrays.asList(sockets.toArray()));
        }
        Set<Connection> connections = sockets
                        .stream()
                        .filter(s -> s.getConnections() != null)
                        .flatMap(s -> s.getConnections().getMembers().stream())
                        .filter(c -> check.isConnectionStateAllowed(c.getConnectionState()))
                        .collect(Collectors.toSet());
        if (logger.isDebugEnabled()) {
            logger.debug("candidate connections after filtering by connection state: {} ",
                            Arrays.asList(connections.toArray()));
        }
        boolean foundIt = connections
                        .parallelStream()
                        .anyMatch(c -> c
                                        .getTargetAtom()
                                        .getNode().toString()
                                        .equals(check.getRequestedTarget().toString())
                                        || (check.isWonNodeCheck()
                                                        && check.getRequestedTarget()
                                                                        .equals(c.getTargetAtom().getWonNode())));
        if (logger.isDebugEnabled()) {
            logger.debug("{} target atom {} in connections' targetAtoms", foundIt ? "found" : "did not find",
                            check.getRequestedTarget());
        }
        return foundIt;
    }
}
