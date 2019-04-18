package won.matcher.solr;

import java.io.IOException;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.github.jsonldjava.core.JsonLdError;

import won.bot.framework.component.atomproducer.AtomProducer;
import won.bot.framework.component.atomproducer.impl.RoundRobinCompositeAtomProducer;
import won.matcher.solr.evaluation.SolrMatcherEvaluation;
import won.matcher.solr.index.AtomIndexer;
import won.matcher.solr.spring.SolrTestAppConfiguration;
import won.protocol.model.AtomGraphType;
import won.protocol.util.AtomModelWrapper;

/**
 * Created by hfriedrich on 11.09.2015.
 * <p>
 * Utility app to write atoms from mail directories into the Solr index for
 * testing queries directly on the index etc.
 */
public class SolrAtomIndexer {
    public static void main(String[] args) throws IOException, InterruptedException, JsonLdError {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(SolrTestAppConfiguration.class);
        AtomIndexer indexer = ctx.getBean(AtomIndexer.class);
        // set the options of the atom producer (e.g. if it should exhaust) in the
        // SolrAtomIndexerAppConfiguration file
        AtomProducer atomProducer = ctx.getBean(RoundRobinCompositeAtomProducer.class);
        Model atomModel = new AtomModelWrapper(atomProducer.create()).copyAtomModel(AtomGraphType.ATOM);
        int atoms = 0;
        while (!atomProducer.isExhausted()) {
            // indexer.indexAtomModel(atomModel, UUID.randomUUID().toString(), true);
            Dataset ds = DatasetFactory.createTxnMem();
            ds.addNamedModel("https://node.matchat.org/won/resource/atom/test#atom", atomModel);
            AtomModelWrapper atomModelWrapper = new AtomModelWrapper(atomModel, null);
            atomModel = atomModelWrapper.normalizeAtomModel();
            indexer.indexAtomModel(atomModel, SolrMatcherEvaluation.createAtomId(ds), true);
            atoms++;
            if (atoms % 100 == 0) {
                System.out.println("Indexed " + atoms + " atoms.");
            }
            atomModel = new AtomModelWrapper(atomProducer.create()).copyAtomModel(AtomGraphType.ATOM);
        }
        System.out.println("Indexed " + atoms + " atoms.");
        System.exit(0);
    }
}
