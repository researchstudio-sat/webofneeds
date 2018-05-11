package won.matcher.sparql;

import com.github.jsonldjava.core.JsonLdError;

import won.matcher.sparql.evaluation.SparqlMatcherEvaluation;
import won.matcher.sparql.spring.SparqlTestAppConfiguration;

import org.apache.solr.client.solrj.SolrServerException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;

/**
 * Created by hfriedrich on 08.08.2016.
 */
public class SparqlEvaluation
{
  public static void main(String[] args) throws IOException, InterruptedException, JsonLdError, SolrServerException {

    AnnotationConfigApplicationContext ctx =
      new AnnotationConfigApplicationContext(SparqlTestAppConfiguration.class);

    System.out.println("initialize ...");
    SparqlMatcherEvaluation eval = ctx.getBean(SparqlMatcherEvaluation.class);

    System.out.println("build connection tensor ...");
    eval.buildConnectionTensor();

    System.out.println("build prediction tensor ...");
    eval.buildPredictionTensor();

    System.exit(0);
  }
}
