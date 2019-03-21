package won.matcher.solr;

import com.github.jsonldjava.core.JsonLdError;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import won.bot.framework.component.needproducer.NeedProducer;
import won.bot.framework.component.needproducer.impl.RoundRobinCompositeNeedProducer;
import won.matcher.solr.evaluation.SolrMatcherEvaluation;
import won.matcher.solr.index.NeedIndexer;
import won.matcher.solr.spring.SolrTestAppConfiguration;
import won.protocol.model.NeedGraphType;
import won.protocol.util.NeedModelWrapper;

import java.io.IOException;

/**
 * Created by hfriedrich on 11.09.2015.
 * <p>
 * Utility app to write needs from mail directories into the Solr index for
 * testing queries directly on the index etc.
 */
public class SolrNeedIndexer {
  public static void main(String[] args) throws IOException, InterruptedException, JsonLdError {

    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(SolrTestAppConfiguration.class);

    NeedIndexer indexer = ctx.getBean(NeedIndexer.class);

    // set the options of the need producer (e.g. if it should exhaust) in the
    // SolrNeedIndexerAppConfiguration file
    NeedProducer needProducer = ctx.getBean(RoundRobinCompositeNeedProducer.class);
    Model needModel = new NeedModelWrapper(needProducer.create()).copyNeedModel(NeedGraphType.NEED);

    int needs = 0;
    while (!needProducer.isExhausted()) {
      // indexer.indexNeedModel(needModel, UUID.randomUUID().toString(), true);
      Dataset ds = DatasetFactory.createTxnMem();
      ds.addNamedModel("https://node.matchat.org/won/resource/need/test#need", needModel);

      NeedModelWrapper needModelWrapper = new NeedModelWrapper(needModel, null);
      needModel = needModelWrapper.normalizeNeedModel();

      indexer.indexNeedModel(needModel, SolrMatcherEvaluation.createNeedId(ds), true);
      needs++;

      if (needs % 100 == 0) {
        System.out.println("Indexed " + needs + " needs.");
      }
      needModel = new NeedModelWrapper(needProducer.create()).copyNeedModel(NeedGraphType.NEED);
    }
    System.out.println("Indexed " + needs + " needs.");
    System.exit(0);
  }
}
