package won.auth;

import org.apache.jena.graph.Graph;
import org.apache.jena.shacl.Shapes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.auth.check.AtomNodeChecker;
import won.shacl2java.Shacl2JavaInstanceFactory;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Optional;

public class ModelBasedAtomNodeChecker implements AtomNodeChecker {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    Shacl2JavaInstanceFactory instanceFactory;
    Shacl2JavaInstanceFactory.Accessor accessor = null;

    public ModelBasedAtomNodeChecker(Shapes shapes, String packageName) {
        this.instanceFactory = new Shacl2JavaInstanceFactory(shapes, packageName);
    }

    public void loadData(Graph data) {
        this.accessor = this.instanceFactory.accessor(data);
    }

    public Shacl2JavaInstanceFactory.Accessor getAccessor() {
        return accessor;
    }

    @Override
    public boolean isNodeOfAtom(URI atomUri, URI nodeUri) {
        Optional<won.auth.test.model.Atom> atom = accessor.getInstanceOfType(atomUri.toString(),
                        won.auth.test.model.Atom.class);
        if (!atom.isPresent()) {
            return false;
        }
        return nodeUri.equals(atom.get().getWonNode());
    }

    @Override
    public Optional<URI> getNodeOfAtom(URI atomUri) {
        Optional<won.auth.test.model.Atom> atom = accessor.getInstanceOfType(atomUri.toString(),
                        won.auth.test.model.Atom.class);
        if (!atom.isPresent()) {
            return Optional.empty();
        }
        return Optional.ofNullable(atom.get().getWonNode());
    }
}
