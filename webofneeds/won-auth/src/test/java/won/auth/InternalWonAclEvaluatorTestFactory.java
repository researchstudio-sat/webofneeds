package won.auth;

import org.apache.jena.graph.Graph;
import org.apache.jena.shacl.Shapes;
import won.auth.check.AtomNodeChecker;
import won.auth.check.TargetAtomCheckEvaluator;
import won.auth.model.Authorization;
import won.auth.model.OperationRequest;
import won.auth.support.InternalWonAclEvaluatorFactory;
import won.cryptography.rdfsign.WebIdKeyLoader;
import won.shacl2java.Shacl2JavaInstanceFactory;

import java.util.Set;

public class InternalWonAclEvaluatorTestFactory extends InternalWonAclEvaluatorFactory {
    public InternalWonAclEvaluatorTestFactory(Shapes shapes,
                    TargetAtomCheckEvaluator targetAtomCheckEvaluator,
                    AtomNodeChecker atomNodeChecker,
                    WebIdKeyLoader webIdKeyLoader) {
        super(shapes, targetAtomCheckEvaluator, atomNodeChecker, webIdKeyLoader);
    }

    public synchronized void load(Graph aclGraph) {
        instanceFactory.load(aclGraph);
    }

    public synchronized Set<Authorization> getAuthorizations() {
        checkLoaded();
        return this.instanceFactory.getInstancesOfType(Authorization.class, true);
    }

    public synchronized Set<OperationRequest> getOperationRequests() {
        checkLoaded();
        return this.instanceFactory.getInstancesOfType(OperationRequest.class);
    }

    public synchronized WonAclEvaluator create() {
        checkLoaded();
        return new WonAclEvaluator(
                        getAuthorizations(),
                        targetAtomCheckEvaluator,
                        atomNodeChecker,
                        webIdKeyLoader);
    }

    public void checkLoaded() {
        if (!instanceFactory.isLoaded()) {
            throw new IllegalStateException("No ACL data loaded - call load(..) first!");
        }
    }

    /**
     * Access to the instance factory for tests.
     *
     * @return
     */
    Shacl2JavaInstanceFactory getInstanceFactory() {
        return instanceFactory;
    }
}
