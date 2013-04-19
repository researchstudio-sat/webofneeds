package won.matcher.solr;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrEventListener;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.plugin.SolrCoreAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.matcher.query.TextDescriptionMatcher;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 21.02.13
 * Time: 13:53
 * To change this template use File | Settings | File Templates.
 */
public class UpdateListener implements SolrEventListener {//, SolrCoreAware {
    private Logger logger;
    private TextDescriptionMatcher matcher;

    @Override
    public void postCommit() {
        log.info("Post Commit asdf asdf received!!");
        matcher.executeQuery();
    }

    @Override
    public void newSearcher(SolrIndexSearcher solrIndexSearcher, SolrIndexSearcher solrIndexSearcher2) {
        log.info("new Searcher Called");
    }

    @Override
    public void init(NamedList namedList) {
        logger = LoggerFactory.getLogger(UpdateListener.class);
        logger.info("init!!");
        matcher = new TextDescriptionMatcher();//SolrCore.openHandles.keySet().iterator().next());
    }

    /*
    @Override
    public void inform(SolrCore solrCore) {
        logger = LoggerFactory.getLogger(UpdateListener.class);
        logger.info("informed!!");
        matcher = new TextDescriptionMatcher(solrCore);
    }
    */
}
