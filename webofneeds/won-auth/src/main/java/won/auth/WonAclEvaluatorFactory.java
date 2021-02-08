package won.auth;

import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.sparql.graph.GraphFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import won.auth.check.AtomNodeChecker;
import won.auth.check.TargetAtomCheckEvaluator;
import won.auth.support.InternalWonAclEvaluatorFactory;
import won.cryptography.rdfsign.WebIdKeyLoader;

import java.lang.invoke.MethodHandles;

@Component
public class WonAclEvaluatorFactory implements InitializingBean, DisposableBean {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    protected Shapes shapes;
    @Autowired
    protected TargetAtomCheckEvaluator targetAtomCheckEvaluator;
    @Autowired
    protected AtomNodeChecker atomNodeChecker;
    @Autowired
    protected WebIdKeyLoader webIdKeyLoader;
    private ThreadLocal<InternalWonAclEvaluatorFactory> wonAclEvaluatorFactoryThreadLocal = new ThreadLocal<>();
    private Shapes aclShapes;
    private Graph aclShapesGraph;

    public WonAclEvaluatorFactory() {
    }

    public WonAclEvaluator getWonAclEvaluator(Graph aclGraph) {
        InternalWonAclEvaluatorFactory evaluatorFactory = this.wonAclEvaluatorFactoryThreadLocal.get();
        // note: because of the ThreadLocal, we don't need to synchronize. Eeach thread
        // does this
        // on its own data (and the WAEF's dependencies are thread-safe)
        if (evaluatorFactory == null) {
            evaluatorFactory = new InternalWonAclEvaluatorFactory(
                            this.aclShapes,
                            this.targetAtomCheckEvaluator,
                            this.atomNodeChecker,
                            webIdKeyLoader);
            this.wonAclEvaluatorFactoryThreadLocal.set(evaluatorFactory);
        }
        return evaluatorFactory.create(aclGraph);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Resource res = new ClassPathResource("shacl/won-auth-shapes.ttl");
        aclShapesGraph = GraphFactory.createGraphMem();
        RDFDataMgr.read(aclShapesGraph, res.getInputStream(), Lang.TTL);
        aclShapes = Shapes.parse(aclShapesGraph);
    }

    @Override
    public void destroy() throws Exception {
        this.wonAclEvaluatorFactoryThreadLocal = null;
    }
}
