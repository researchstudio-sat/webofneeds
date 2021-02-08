package won.auth.support;

import org.apache.jena.graph.Graph;
import org.apache.jena.shacl.Shapes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.auth.WonAclEvaluator;
import won.auth.check.AtomNodeChecker;
import won.auth.check.TargetAtomCheckEvaluator;
import won.auth.model.Authorization;
import won.cryptography.rdfsign.WebIdKeyLoader;
import won.shacl2java.Shacl2JavaInstanceFactory;

import java.lang.invoke.MethodHandles;
import java.util.Set;

public class InternalWonAclEvaluatorFactory {
    public static final long DEFAULT_TOKEN_EXPIRES_AFTER_SECONDS = 3600;
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    protected Shacl2JavaInstanceFactory instanceFactory;
    protected Shapes shapes;
    protected TargetAtomCheckEvaluator targetAtomCheckEvaluator;
    protected AtomNodeChecker atomNodeChecker;
    protected WebIdKeyLoader webIdKeyLoader;

    public InternalWonAclEvaluatorFactory(
                    Shapes shapes,
                    TargetAtomCheckEvaluator targetAtomCheckEvaluator,
                    AtomNodeChecker atomNodeChecker,
                    WebIdKeyLoader webIdKeyLoader) {
        this.targetAtomCheckEvaluator = targetAtomCheckEvaluator;
        this.shapes = shapes;
        this.instanceFactory = new Shacl2JavaInstanceFactory(shapes, "won.auth.model");
        this.webIdKeyLoader = webIdKeyLoader;
        this.atomNodeChecker = atomNodeChecker;
    }

    public synchronized WonAclEvaluator create(Graph dataGraph) {
        return new WonAclEvaluator(
                        getAuthorizations(dataGraph),
                        targetAtomCheckEvaluator,
                        atomNodeChecker,
                        webIdKeyLoader);
    }

    private synchronized Set<Authorization> getAuthorizations(Graph dataGraph) {
        this.instanceFactory.load(dataGraph);
        Set<Authorization> auths = this.instanceFactory.getInstancesOfType(Authorization.class, true);
        this.instanceFactory.reset();
        return auths;
    }
}
