package won.matcher.solr;

import java.io.IOException;

import org.apache.jena.query.Dataset;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.github.jsonldjava.core.JsonLdError;

import won.bot.framework.component.atomproducer.AtomProducer;
import won.bot.framework.component.atomproducer.impl.RoundRobinCompositeAtomProducer;
import won.matcher.solr.hints.HintBuilder;
import won.matcher.solr.query.TestMatcherQueryExecutor;
import won.matcher.solr.query.factory.BasicAtomQueryFactory;
import won.matcher.solr.query.factory.TestAtomQueryFactory;
import won.matcher.solr.spring.SolrTestAppConfiguration;

/**
 * Created by hfriedrich on 03.08.2016.
 * <p>
 * Utility test app to query an Solr index and check what results it returns.
 */
public class SolrMatcherQueryTest {
    public static void main(String[] args) throws IOException, InterruptedException, JsonLdError, SolrServerException {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(SolrTestAppConfiguration.class);
        HintBuilder hintBuilder = ctx.getBean(HintBuilder.class);
        // DefaultMatcherQueryExecuter queryExecutor =
        // ctx.getBean(DefaultMatcherQueryExecuter.class);
        TestMatcherQueryExecutor queryExecutor = ctx.getBean(TestMatcherQueryExecutor.class);
        // set the options of the atom producer (e.g. if it should exhaust) in the
        // SolrAtomIndexerAppConfiguration file
        AtomProducer atomProducer = ctx.getBean(RoundRobinCompositeAtomProducer.class);
        while (!atomProducer.isExhausted()) { // && atoms < 20) {
            Dataset ds = atomProducer.create();
            try {
                TestAtomQueryFactory atomQuery = new TestAtomQueryFactory(ds);
                String query = atomQuery.createQuery();
                System.out.println("execute query: " + query);
                SolrDocumentList docs = queryExecutor.executeAtomQuery(query, 20, null,
                                new BasicAtomQueryFactory(ds).createQuery());
                SolrDocumentList matchedDocs = hintBuilder.calculateMatchingResults(docs);
                System.out.println("Found docs: " + ((docs != null) ? docs.size() : 0) + ", keep docs: "
                                + ((matchedDocs != null) ? matchedDocs.size() : 0));
                if (docs == null) {
                    continue;
                }
                System.out.println("Keep docs: ");
                System.out.println("======================");
                for (SolrDocument doc : matchedDocs) {
                    String score = doc.getFieldValue("score").toString();
                    String matchedAtomId = doc.getFieldValue("id").toString();
                    System.out.println("Score: " + score + ", Id: " + matchedAtomId);
                }
                System.out.println("All docs: ");
                System.out.println("======================");
                for (SolrDocument doc : docs) {
                    String score = doc.getFieldValue("score").toString();
                    String matchedAtomId = doc.getFieldValue("id").toString();
                    System.out.println("Score: " + score + ", Id: " + matchedAtomId);
                }
            } catch (SolrException e) {
                System.err.println(e);
            }
        }
        System.exit(0);
    }
}
