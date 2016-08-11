package won.matcher.solr;

import com.github.jsonldjava.core.JsonLdError;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import won.bot.framework.component.needproducer.NeedProducer;
import won.bot.framework.component.needproducer.impl.RoundRobinCompositeNeedProducer;
import won.matcher.solr.config.SolrMatcherConfig;
import won.matcher.solr.evaluation.SolrMatcherEvaluation;
import won.matcher.solr.hints.HintBuilder;
import won.matcher.solr.query.TestNeedQueryFactory;
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

    SolrMatcherConfig config = ctx.getBean(SolrMatcherConfig.class);
    HintBuilder hintBuilder = ctx.getBean(HintBuilder.class);

    SolrClient solrClient = new HttpSolrClient.Builder(config.getSolrEndpointUri(true)).build();

    // set the options of the need producer (e.g. if it should exhaust) in the SolrNeedIndexerAppConfiguration file
    NeedProducer needProducer = ctx.getBean(RoundRobinCompositeNeedProducer.class);

    int needs = 0;
    while (!needProducer.isExhausted()) { //&& needs < 20) {

      Dataset ds = DatasetFactory.create(needProducer.create());
      TestNeedQueryFactory needQuery = new TestNeedQueryFactory(ds);
      needQuery.addTermsToTitleQuery(needQuery.getTitleTerms(), 4);
      needQuery.addTermsToTitleQuery(needQuery.getTagTerms(), 2);
      needQuery.addTermsToTagQuery(needQuery.getTagTerms(), 4);
      needQuery.addTermsToTagQuery(needQuery.getTitleTerms(), 2);
      needQuery.addTermsToDescriptionQuery(needQuery.getTitleTerms(), 2);
      needQuery.addTermsToDescriptionQuery(needQuery.getTagTerms(), 2);
      needQuery.addTermsToDescriptionQuery(needQuery.getDescriptionTerms(), 1);

      // compute a hash value from the title and description of the needs and use it as an identifier
      String needId = SolrMatcherEvaluation.createNeedId(ds);
      //tensorMatchingData.addNeedAttribute(needId, needQuery.getTitleTerms(), TensorMatchingData.SliceType.TITLE);

      System.out.println("\nExecute Query: \n" + needQuery.createQuery());

      SolrQuery query = new SolrQuery();
      query.setQuery(needQuery.createQuery());
      query.setFields("id", "score",
                      "_graph.http___purl.org_webofneeds_model_hasContent.http___purl.org_dc_elements_1.1_title",
                      HintBuilder.WON_NODE_SOLR_FIELD);
      query.setRows(config.getMaxHints());

      try {
        QueryResponse response = solrClient.query(query);
        SolrDocumentList docs = response.getResults();

        SolrDocumentList newDocs = hintBuilder.calculateMatchingResults(docs);
        System.out.println("Found docs: " + docs.size() + ", keep docs: " + newDocs.size());

        System.out.println("Keep docs: ");
        System.out.println("======================");
        for (SolrDocument doc : newDocs) {
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

      needs++;
    }

    System.exit(0);
  }
}
