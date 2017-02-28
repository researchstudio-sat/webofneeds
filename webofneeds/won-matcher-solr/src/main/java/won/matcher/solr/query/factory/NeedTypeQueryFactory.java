package won.matcher.solr.query.factory;

import org.apache.jena.query.Dataset;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.WON;

/**
 * Created by hfriedrich on 01.08.2016.
 */
public class NeedTypeQueryFactory extends NeedDatasetQueryFactory
{
  private static final String DEMAND = WON.BASIC_NEED_TYPE_DEMAND.toString();
  private static final String SUPPLY = WON.BASIC_NEED_TYPE_SUPPLY.toString();
  private static final String DOTOGETHER = WON.BASIC_NEED_TYPE_DO_TOGETHER.toString();
  private static final String CRITIQUE = WON.BASIC_NEED_TYPE_CRITIQUE.toString();

  private static final String NEED_TYPE_SOLR_FIELD = "_graph.http___purl.org_webofneeds_model_hasBasicNeedType._id";

  private String matchNeedType;

  public NeedTypeQueryFactory(final Dataset need) {
    super(need);

    String basicNeedType = RdfUtils.findOnePropertyFromResource(needDataset, null, WON.HAS_BASIC_NEED_TYPE).asResource().toString();
    matchNeedType = null;
    if (DEMAND.equalsIgnoreCase(basicNeedType)) {
      matchNeedType = SUPPLY;
    } else if (SUPPLY.equalsIgnoreCase(basicNeedType)) {
      matchNeedType = DEMAND;
    } else if (DOTOGETHER.equalsIgnoreCase(basicNeedType)) {
      matchNeedType = DOTOGETHER;
    } else if (CRITIQUE.equalsIgnoreCase(basicNeedType)) {
      matchNeedType = CRITIQUE;
    }
  }

  @Override
  protected String makeQueryString() {
    return new ExactMatchFieldQueryFactory(NEED_TYPE_SOLR_FIELD, matchNeedType).createQuery();
  }

}
