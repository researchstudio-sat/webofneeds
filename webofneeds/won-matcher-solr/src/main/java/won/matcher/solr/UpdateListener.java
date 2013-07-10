package won.matcher.solr;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrEventListener;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.matcher.Matcher;

import java.io.IOException;

/**
 * User: gabriel
 * Date: 21.02.13
 * Time: 13:53
 */
public class UpdateListener implements SolrEventListener
{
  private Logger logger = LoggerFactory.getLogger(UpdateListener.class);
  private Matcher matcher;

  private DocumentStorage documentStorage;

  @Override
  public void init(NamedList namedList)
  {
    logger.info("Initiating Matcher.");

    try {
      matcher = new Matcher();
    } catch (Exception e) {
      logger.error("Failed to initialize core container in Matcher. Aborting.", e);
    }

    documentStorage = DocumentStorage.getInstance();
  }

  @Override
  public void postCommit()
  {
    logger.debug("Post Commit received!!");
    while (documentStorage.hasNext())
      try {
        matcher.processDocument(documentStorage.pop());
      } catch (IOException e) {
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      }

    try {
      matcher.finish();
    } catch (IOException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  @Override
  public void newSearcher(SolrIndexSearcher solrIndexSearcher, SolrIndexSearcher solrIndexSearcher2)
  {
    logger.debug("new Searcher Called");
  }

}
