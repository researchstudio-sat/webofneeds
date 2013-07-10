package won.matcher.solr;

import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;

import java.io.IOException;

/**
 * User: atus
 * Date: 10.07.13
 */
public class MatcherUpdateRequestProcessor extends UpdateRequestProcessor
{
  private DocumentStorage storage;

  public MatcherUpdateRequestProcessor(UpdateRequestProcessor next)
  {
    super(next);
    storage = DocumentStorage.getInstance();
  }

  @Override
  public void processAdd(final AddUpdateCommand cmd) throws IOException
  {
    super.processAdd(cmd);    //To change body of overridden methods use File | Settings | File Templates.
    storage.push(cmd.getSolrInputDocument());
  }

}
