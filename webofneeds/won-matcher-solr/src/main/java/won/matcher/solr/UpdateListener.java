package won.matcher.solr;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrEventListener;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.matcher.Matcher;
import won.matcher.processor.HintSender;

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
    logger.debug("initiating matcher");
    documentStorage = DocumentStorage.getInstance();
  }

  @Override
  public void postCommit()
  {
    logger.debug("postCommit called");
  }

  @Override
  public void newSearcher(SolrIndexSearcher solrIndexSearcher, SolrIndexSearcher solrIndexSearcher2)
  {
    logger.debug("newSearcher called");

    Matcher matcher = new Matcher(solrIndexSearcher);
    matcher.addMatchProcessor(new HintSender());

    while (documentStorage.hasNext())
      try {
        matcher.processDocument(documentStorage.pop());
      } catch (IOException e) {
        logger.error(e.getMessage(), e);
      }

    matcher.processMatches();
  }

}
