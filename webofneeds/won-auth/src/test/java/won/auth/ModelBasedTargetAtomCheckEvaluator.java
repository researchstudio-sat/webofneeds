package won.auth;

import org.apache.jena.graph.Graph;
import org.apache.jena.shacl.Shapes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.auth.check.TargetAtomCheck;
import won.auth.check.TargetAtomCheckEvaluator;
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

public class ModelBasedTargetAtomCheckEvaluator implements TargetAtomCheckEvaluator {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    Shacl2JavaInstanceFactory instanceFactory;

    public ModelBasedTargetAtomCheckEvaluator(Shapes shapes, String packageName) {
        this.instanceFactory = new Shacl2JavaInstanceFactory(shapes, packageName);
    }

    public void loadData(Graph data) {
        this.instanceFactory.load(data);
    }

    @Override
    public boolean isRequestorAllowedTarget(TargetAtomCheck check) {
        Optional<Atom> atomOpt = instanceFactory.getInstanceOfType(check.getAtom().toString(), Atom.class);
        if (!atomOpt.isPresent()) {
            logger.debug("No data found for atom {} ", check.getAtom());
            return false;
        }
        Set<Socket> sockets = atomOpt.get().getSockets();
        if (logger.isDebugEnabled()){
            logger.debug("candidate sockets, initially: {} ", Arrays.asList(sockets.toArray()));
        }
        sockets = sockets
                        .parallelStream()
                        .filter(socket -> check.isSocketAllowed(URI.create(socket.get_node().toString())))
                        .collect(Collectors.toSet());
        if (logger.isDebugEnabled()){
            logger.debug("candidate sockets after filtering socket URIs: {} ", Arrays.asList(sockets.toArray()));
        }
        sockets = sockets
                        .parallelStream()
                        .filter(socket -> check.isSocketTypeAllowed(socket.getSocketDefinition()))
                        .collect(Collectors.toSet());
        if (logger.isDebugEnabled()){
            logger.debug("candidate sockets after filtering socket types: {} ", Arrays.asList(sockets.toArray()));
        }
        Set<Connection> connections = sockets
                        .stream()
                        .flatMap(s -> s.getConnections().getMembers().stream())
                        .filter(c -> check.isConnectionStateAllowed(c.getConnectionState()))
                        .collect(Collectors.toSet());
        if (logger.isDebugEnabled()){
            logger.debug("candidate connections after filtering by connection state: {} ", Arrays.asList(connections.toArray()));
        }
        boolean foundIt = connections
                        .parallelStream()
                        .anyMatch(c -> c
                                        .getTargetAtom()
                                        .get_node().toString()
                                        .equals(check.getRequestedTarget().toString()));
        if (logger.isDebugEnabled()){
            logger.debug("{} target atom {} in connections' targetAtoms", foundIt? "found":"did not find", check.getRequestedTarget());
        }
        return foundIt;
    }
}
