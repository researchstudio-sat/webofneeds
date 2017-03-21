package won.matcher.solr.query.factory;

import org.apache.jena.query.Dataset;
import won.matcher.solr.index.NeedIndexer;
import won.protocol.model.NeedContentPropertyType;
import won.protocol.util.DefaultNeedModelWrapper;

/**
 * Created by hfriedrich on 22.08.2016.
 */
public class GeoDistFilterQueryFactory extends NeedDatasetQueryFactory
{
  private Float latitude;
  private Float longitude;
  private double distance;

  public GeoDistFilterQueryFactory(Dataset need, double distanceInKilometers) {
    super(need);

    DefaultNeedModelWrapper needModelWrapper = new DefaultNeedModelWrapper(need);
    latitude = needModelWrapper.getLocationLatitude(NeedContentPropertyType.ALL);
    longitude = needModelWrapper.getLocationLongitude(NeedContentPropertyType.ALL);
    distance = distanceInKilometers;
  }

  @Override
  protected String makeQueryString() {

    if (longitude == null || latitude == null) {
      return "";
    }

    // create a geographical distance filter with radius "distance"
    StringBuilder sb = new StringBuilder();
    sb.append("{!geofilt sfield=").append(NeedIndexer.SOLR_LOCATION_COORDINATES_FIELD).append(" pt=").append(latitude)
      .append(",").append(longitude).append(" d=").append(distance).append("}");
    return sb.toString();
  }

}
