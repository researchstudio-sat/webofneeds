package won.auth;

import org.apache.jena.graph.Graph;
import org.apache.jena.shacl.Shapes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.auth.check.AtomNodeChecker;
import won.auth.check.TargetAtomCheckEvaluator;
import won.auth.model.Authorization;
import won.cryptography.rdfsign.WebIdKeyLoader;
import won.shacl2java.Shacl2JavaInstanceFactory;

import java.lang.invoke.MethodHandles;
import java.util.Set;

public class WonAclEvaluatorFactory {
    public static final long DEFAULT_TOKEN_EXPIRES_AFTER_SECONDS = 3600;
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    protected Shacl2JavaInstanceFactory instanceFactory = AuthUtils.instanceFactory();
    protected Shapes shapes;
    @Autowired
    protected TargetAtomCheckEvaluator targetAtomCheckEvaluator;
    @Autowired
    protected AtomNodeChecker atomNodeChecker;
    @Autowired
    protected WebIdKeyLoader webIdKeyLoader;

    public WonAclEvaluatorFactory() {
    }

    public WonAclEvaluatorFactory(
                    TargetAtomCheckEvaluator targetAtomCheckEvaluator,
                    AtomNodeChecker atomNodeChecker,
                    WebIdKeyLoader webIdKeyLoader) {
        this.targetAtomCheckEvaluator = targetAtomCheckEvaluator;
        this.shapes = AuthUtils.shapes();
        this.webIdKeyLoader = webIdKeyLoader;
        this.atomNodeChecker = atomNodeChecker;
    }

    public WonAclEvaluator create(Graph dataGraph) {
        return new WonAclEvaluator(
                        getAuthorizations(dataGraph),
                        targetAtomCheckEvaluator,
                        atomNodeChecker,
                        webIdKeyLoader);
    }

    public Set<Authorization> getAuthorizations(Graph dataGraph) {
        Shacl2JavaInstanceFactory.Accessor accessor = this.instanceFactory.accessor(dataGraph);
        Set<Authorization> auths = accessor.getInstancesOfType(Authorization.class, true);
        return auths;
    }
}
