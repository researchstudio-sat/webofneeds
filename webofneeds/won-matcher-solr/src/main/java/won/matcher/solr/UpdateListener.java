package won.matcher.solr;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrEventListener;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.matcher.Matcher;

import java.io.IOException;

/**
 * User: atus
 * Date: 21.02.13
 * Time: 13:53
 */
public class UpdateListener implements SolrEventListener
{
  private Logger logger = LoggerFactory.getLogger(getClass());

  private DocumentStorage documentStorage;

  @Override
  public void init(NamedList namedList)
  {
    logger.info("initiating matcher");
    documentStorage = DocumentStorage.getInstance();
  }

  @Override
  public void postCommit()
  {
    logger.info("postCommit called");
  }

  @Override
  public void newSearcher(SolrIndexSearcher solrIndexSearcher, SolrIndexSearcher solrIndexSearcher2)
  {
    logger.info("newSearcher called");

    Matcher matcher = new Matcher(solrIndexSearcher);

    while (documentStorage.hasNext())
      try {
        matcher.processDocument(documentStorage.pop());
      } catch (IOException e) {
        logger.error("Exception while processing document", e);
      }

    try {
      matcher.finish();
    } catch (IOException e) {
      logger.error("Exception while sending matches", e);
    }
  }

}
