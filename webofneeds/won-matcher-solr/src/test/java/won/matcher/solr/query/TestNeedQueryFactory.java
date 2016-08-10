package won.matcher.solr.query;

import com.hp.hpl.jena.query.Dataset;

/**
 * Created by hfriedrich on 03.08.2016.
 */
public class TestNeedQueryFactory extends BasicNeedQueryFactory
{
  public TestNeedQueryFactory(final Dataset need) {
    super(need);
  }

  @Override
  protected String makeQueryString() {

    SolrQueryFactory[] factoryArray = new SolrQueryFactory[contentFactories.size()];
    BooleanQueryFactory contentShouldQuery = new BooleanQueryFactory(BooleanQueryFactory.BooleanOperator.OR, contentFactories
      .toArray(factoryArray));

    BooleanQueryFactory mustQuery = new BooleanQueryFactory(BooleanQueryFactory.BooleanOperator.AND, new
      NeedTypeQueryFactory(needDataset));

    BooleanQueryFactory topQuery = new BooleanQueryFactory(BooleanQueryFactory.BooleanOperator.AND, mustQuery,
                                                           contentShouldQuery);

    return topQuery.createQuery();
  }
}
