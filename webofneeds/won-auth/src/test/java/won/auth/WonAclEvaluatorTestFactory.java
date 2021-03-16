package won.auth;

import org.apache.jena.graph.Graph;
import won.auth.check.AtomNodeChecker;
import won.auth.check.ConnectionTargetCheckEvaluator;
import won.auth.model.Authorization;
import won.auth.model.OperationRequest;
import won.cryptography.rdfsign.WebIdKeyLoader;
import won.shacl2java.Shacl2JavaInstanceFactory;

import java.util.Set;

public class WonAclEvaluatorTestFactory extends WonAclEvaluatorFactory {
    private Shacl2JavaInstanceFactory.Accessor accessor = null;

    public WonAclEvaluatorTestFactory(
                    ConnectionTargetCheckEvaluator targetAtomCheckEvaluator,
                    AtomNodeChecker atomNodeChecker, WebIdKeyLoader webIdKeyLoader) {
        super(targetAtomCheckEvaluator, atomNodeChecker, webIdKeyLoader);
    }

    public synchronized void load(Graph aclGraph) {
        accessor = instanceFactory.accessor(aclGraph);
    }

    public synchronized Set<Authorization> getAuthorizations(boolean includeDefaultAuths) {
        checkLoaded();
        return this.accessor.getInstancesOfType(Authorization.class, includeDefaultAuths);
    }

    public synchronized Set<OperationRequest> getOperationRequests() {
        checkLoaded();
        return this.accessor.getInstancesOfType(OperationRequest.class);
    }

    public synchronized WonAclEvaluator create(boolean includeDefaultAuths) {
        checkLoaded();
        return new WonAclEvaluator(
                        getAuthorizations(includeDefaultAuths),
                        targetAtomCheckEvaluator,
                        atomNodeChecker,
                        webIdKeyLoader);
    }

    public void checkLoaded() {
        if (accessor == null) {
            throw new IllegalStateException("No ACL data loaded - call load(..) first!");
        }
    }

    /**
     * Access to the instance factory for tests.
     *
     * @return
     */
    Shacl2JavaInstanceFactory.Accessor getInstanceFactoryAccessor() {
        return accessor;
    }
}
