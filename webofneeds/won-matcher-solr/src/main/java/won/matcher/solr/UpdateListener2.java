package won.matcher.solr;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrEventListener;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.update.processor.UpdateRequestProcessorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.matcher.Matcher;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 11.07.13
 * Time: 17:12
 * To change this template use File | Settings | File Templates.
 */
public class UpdateListener2  extends UpdateRequestProcessorFactory implements SolrEventListener {

    private Logger logger = LoggerFactory.getLogger(UpdateListener.class);
    private Matcher matcher;
    private Queue<SolrInputDocument> documentStorage;

    @Override
    public void init(NamedList namedList) {
        logger.info("Initiating Matcher.");

        try {
            matcher = new Matcher(null);
        } catch (Exception e) {
            logger.error("Failed to initialize core container in Matcher. Aborting.", e);
        }

        documentStorage = new ConcurrentLinkedQueue<SolrInputDocument>();
    }

    @Override
    public void postCommit() {
        logger.debug("Post Commit received!!");

        for(SolrInputDocument doc : documentStorage) {
           try {
              matcher.processDocument(doc);
           } catch (IOException e) {
               logger.error("Failed to process document.", e);
           }
        }

        try {
            matcher.finish();
        } catch (IOException e) {
            logger.error("Failed to finish matcher.", e);
        }
    }

    @Override
    public void newSearcher(SolrIndexSearcher solrIndexSearcher, SolrIndexSearcher solrIndexSearcher2) {
        logger.debug("new Searcher Called");
    }

    @Override
    public UpdateRequestProcessor getInstance(final SolrQueryRequest req, final SolrQueryResponse rsp, final UpdateRequestProcessor next) {
        return new MatcherUpdateRequestProcessor(next);
    }

    private class MatcherUpdateRequestProcessor extends UpdateRequestProcessor {

        public MatcherUpdateRequestProcessor(UpdateRequestProcessor next) {
            super(next);
        }

        @Override
        public void processAdd(final AddUpdateCommand cmd) throws IOException {
            super.processAdd(cmd);
            documentStorage.add(cmd.getSolrInputDocument());
        }

    }
}
