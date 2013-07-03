package won.matcher.solr;

import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.update.processor.UpdateRequestProcessorFactory;

/**
 * User: atus
 * Date: 03.07.13
 */
public class MatcherUpdateProcessorFactory extends UpdateRequestProcessorFactory
{
  @Override
  public UpdateRequestProcessor getInstance(final SolrQueryRequest req, final SolrQueryResponse rsp, final UpdateRequestProcessor next)
  {
    return new MatcherRequestProcessor(next);
  }
}
