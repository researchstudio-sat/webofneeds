package won.matcher.solr.query.factory;

import com.hp.hpl.jena.query.Dataset;

/**
 * Created by hfriedrich on 01.08.2016.
 */
public abstract class NeedDatasetQueryFactory extends SolrQueryFactory
{
  protected Dataset needDataset;

  public NeedDatasetQueryFactory(Dataset need) {
    this.needDataset = need;
  }
}
