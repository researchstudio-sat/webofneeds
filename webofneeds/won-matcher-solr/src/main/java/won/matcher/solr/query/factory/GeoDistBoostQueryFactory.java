package won.matcher.solr.query.factory;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import won.matcher.solr.index.NeedIndexer;
import won.protocol.util.WonRdfUtils;

import java.net.URI;

/**
 * Created by hfriedrich on 22.08.2016.
 */
public class GeoDistBoostQueryFactory extends NeedDatasetQueryFactory
{
  private Float latitude;
  private Float longitude;

  public GeoDistBoostQueryFactory(Dataset need) {
    super(need);

    Model needModel = WonRdfUtils.NeedUtils.getNeedModelFromNeedDataset(needDataset);
    URI needUri = WonRdfUtils.NeedUtils.getNeedURI(needModel);
    latitude = WonRdfUtils.NeedUtils.getLocationLatitude(needModel, needUri);
    longitude = WonRdfUtils.NeedUtils.getLocationLongitude(needModel, needUri);
  }

  @Override
  protected String makeQueryString() {

    if (longitude == null || latitude == null) {
      return "";
    }

    // boost the query with a factor between 1 (far away) and 2 (close) according to the
    // geographical distance of the needs
    StringBuilder sb = new StringBuilder();
    sb.append("sum(1,recip(geodist(").append(NeedIndexer.SOLR_LOCATION_COORDINATES_FIELD).append(",").append(latitude)
      .append(",").append(longitude).append("),5,100,100))");

    return new MultiplicativeBoostQueryFactory(sb.toString()).createQuery();
  }
}
