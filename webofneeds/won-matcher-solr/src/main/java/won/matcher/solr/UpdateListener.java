package won.matcher.solr;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrEventListener;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 21.02.13
 * Time: 13:53
 * To change this template use File | Settings | File Templates.
 */
public class UpdateListener implements SolrEventListener {
    Logger logger;

    @Override
    public void postCommit() {
        log.info("Post Commit received!!");
    }

    @Override
    public void postSoftCommit() {
        log.info("Post Soft Commit received!!");
    }

    @Override
    public void newSearcher(SolrIndexSearcher solrIndexSearcher, SolrIndexSearcher solrIndexSearcher2) {
        log.info("new Searcher Called");
    }

    @Override
    public void init(NamedList namedList) {
        logger = LoggerFactory.getLogger(UpdateListener.class);
    }
}
