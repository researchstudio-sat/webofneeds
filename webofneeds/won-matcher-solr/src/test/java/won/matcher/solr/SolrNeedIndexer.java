package won.matcher.solr;

import com.github.jsonldjava.core.JsonLdError;
import com.hp.hpl.jena.rdf.model.Model;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import won.bot.framework.component.needproducer.NeedProducer;
import won.bot.framework.component.needproducer.impl.RoundRobinCompositeNeedProducer;
import won.matcher.solr.index.NeedIndexer;
import won.matcher.solr.spring.SolrTestAppConfiguration;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by hfriedrich on 11.09.2015.
 *
 * Utility app to write needs from mail directories into the Solr index for testing queries directly on the index etc.
 */
public class SolrNeedIndexer
{
  public static void main(String[] args) throws IOException, InterruptedException, JsonLdError {

    AnnotationConfigApplicationContext ctx =
      new AnnotationConfigApplicationContext(SolrTestAppConfiguration.class);

    NeedIndexer indexer = ctx.getBean(NeedIndexer.class);

    // set the options of the need producer (e.g. if it should exhaust) in the SolrNeedIndexerAppConfiguration file
    NeedProducer needProducer = ctx.getBean(RoundRobinCompositeNeedProducer.class);

    Model needModel = needProducer.create();
    int needs = 0;
    while (!needProducer.isExhausted()) {
      indexer.indexNeedModel(needModel, UUID.randomUUID().toString());
      needs++;

      if (needs % 100 == 0) {
        System.out.println("Indexed " + needs + " needs.");
      }
      needModel = needProducer.create();
    }
    System.out.println("Indexed " + needs + " needs.");
    System.exit(0);
  }
}
