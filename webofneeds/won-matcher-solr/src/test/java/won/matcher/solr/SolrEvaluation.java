package won.matcher.solr;

import com.github.jsonldjava.core.JsonLdError;
import org.apache.solr.client.solrj.SolrServerException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import won.matcher.solr.evaluation.SolrMatcherEvaluation;
import won.matcher.solr.spring.SolrTestAppConfiguration;

import java.io.IOException;

/**
 * Created by hfriedrich on 08.08.2016.
 */
public class SolrEvaluation {
  public static void main(String[] args) throws IOException, InterruptedException, JsonLdError, SolrServerException {

    AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(SolrTestAppConfiguration.class);

    System.out.println("initialize ...");
    SolrMatcherEvaluation eval = ctx.getBean(SolrMatcherEvaluation.class);

    System.out.println("build connection tensor ...");
    eval.buildConnectionTensor();

    System.out.println("build prediction tensor ...");
    eval.buildPredictionTensor();

    System.exit(0);
  }
}
