package won.matcher.sparql.query.factory;

import org.apache.solr.common.params.SolrParams;

/**
 * Created by hfriedrich on 19.08.2016.
 */
public interface SparqlParamsFactory
{
  public SolrParams createParams();
}
