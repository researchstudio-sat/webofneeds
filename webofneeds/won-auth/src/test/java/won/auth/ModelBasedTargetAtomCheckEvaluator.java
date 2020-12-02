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
import java.util.Set;
import java.util.stream.Collectors;

public class ModelBasedTargetAtomCheckEvaluator implements TargetAtomCheckEvaluator {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    Shacl2JavaInstanceFactory entityFactory;

    public ModelBasedTargetAtomCheckEvaluator(Shapes shapes, String packageName) {
        this.entityFactory = new Shacl2JavaInstanceFactory(shapes, packageName);
    }

    public void loadData(Graph data) {
        this.entityFactory.load(data);
    }

    @Override
    public boolean isAllowedTargetAtom(URI atomUri, TargetAtomCheck check) {
        Atom atom = (Atom) entityFactory.getInstances(atomUri.toString());
        if (atom == null) {
            logger.debug("No data found for atom {} ", atomUri);
            return false;
        }
        Set<Socket> sockets = atom.getSockets();
        if (!check.getAllowedSockets().isEmpty()) {
            sockets = sockets
                            .parallelStream()
                            .filter(socket -> check.getAllowedSockets()
                                            .parallelStream()
                                            .map(Object::toString)
                                            .anyMatch(s -> s.equals(socket.get_node().getURI())))
                            .collect(Collectors.toSet());
        }
        if (!check.getAllowedSocketTypes().isEmpty()) {
            sockets = sockets
                            .parallelStream()
                            .filter(socket -> check
                                            .getAllowedSocketTypes()
                                            .contains(socket.getSocketDefinition()))
                            .collect(Collectors.toSet());
        }
        Set<Connection> connections = sockets
                        .stream()
                        .flatMap(s -> s.getConnections().getMembers().stream())
                        .filter(c -> check
                                        .getAllowedConnectionStates()
                                        .contains(c.getConnectionState()))
                        .collect(Collectors.toSet());
        return connections
                        .parallelStream()
                        .anyMatch(c -> c
                                        .getTargetAtom()
                                        .get_node().toString()
                                        .equals(check.getRequestorAtom().toString()));
    }
}
