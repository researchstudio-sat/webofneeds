package won.matcher.solr.query.factory;

import org.apache.solr.common.params.SolrParams;

/**
 * Created by hfriedrich on 19.08.2016.
 */
public interface SolrParamsFactory
{
  public SolrParams createParams();
}
