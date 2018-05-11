package won.matcher.sparql.query.factory;

import org.apache.jena.query.Dataset;

/**
 * Created by hfriedrich on 01.08.2016.
 */
public abstract class NeedDatasetQueryFactory extends SparqlQueryFactory
{
  protected Dataset needDataset;

  public NeedDatasetQueryFactory(Dataset need) {
    this.needDataset = need;
  }
}
