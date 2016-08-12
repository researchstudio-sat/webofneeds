package won.matcher.solr;

import com.github.jsonldjava.core.JsonLdError;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import won.bot.framework.component.needproducer.NeedProducer;
import won.bot.framework.component.needproducer.impl.RoundRobinCompositeNeedProducer;
import won.matcher.solr.hints.HintBuilder;
import won.matcher.solr.query.TestMatcherQueryExecutor;
import won.matcher.solr.query.factory.NeedTypeQueryFactory;
import won.matcher.solr.query.factory.TestNeedQueryFactory;
import won.matcher.solr.spring.SolrTestAppConfiguration;

import java.io.IOException;

/**
 * Created by hfriedrich on 03.08.2016.
 *
 * Utility test app to query an Solr index and check what results it returns.
 */
public class SolrMatcherQueryTest
{
  public static void main(String[] args) throws IOException, InterruptedException, JsonLdError, SolrServerException {

    AnnotationConfigApplicationContext ctx =
      new AnnotationConfigApplicationContext(SolrTestAppConfiguration.class);

    HintBuilder hintBuilder = ctx.getBean(HintBuilder.class);
    //DefaultMatcherQueryExecuter queryExecutor = ctx.getBean(DefaultMatcherQueryExecuter.class);
    TestMatcherQueryExecutor queryExecutor = ctx.getBean(TestMatcherQueryExecutor.class);

    // set the options of the need producer (e.g. if it should exhaust) in the SolrNeedIndexerAppConfiguration file
    NeedProducer needProducer = ctx.getBean(RoundRobinCompositeNeedProducer.class);

    while (!needProducer.isExhausted()) { //&& needs < 20) {

      Dataset ds = DatasetFactory.create(needProducer.create());

      try {

        TestNeedQueryFactory needQuery = new TestNeedQueryFactory(ds);

        String query = needQuery.createQuery();
        System.out.println("execute query: " + query);

        SolrDocumentList docs = queryExecutor.executeNeedQuery(query, new NeedTypeQueryFactory(ds).createQuery());
        SolrDocumentList matchedDocs = hintBuilder.calculateMatchingResults(docs);

        System.out.println("Found docs: " + docs.size() + ", keep docs: " + matchedDocs.size());

        System.out.println("Keep docs: ");
        System.out.println("======================");
        for (SolrDocument doc : matchedDocs) {
          String title = doc.getFieldValue("_graph.http___purl.org_webofneeds_model_hasContent.http___purl" +
                                       ".org_dc_elements_1.1_title").toString();
          String score = doc.getFieldValue("score").toString();
          String matchedNeedId = doc.getFieldValue("id").toString();
          System.out.println("Score: " + score + ", Title: " + title + ", Id: " + matchedNeedId);
        }

        System.out.println("All docs: ");
        System.out.println("======================");
        for (SolrDocument doc : docs) {
          String title = doc.getFieldValue("_graph.http___purl.org_webofneeds_model_hasContent.http___purl" +
                                             ".org_dc_elements_1.1_title").toString();
          String score = doc.getFieldValue("score").toString();
          System.out.println("Score: " + score + ", Title: " + title);
        }


      } catch (SolrException e) {
        System.err.println(e);
      }
    }

    System.exit(0);
  }
}
