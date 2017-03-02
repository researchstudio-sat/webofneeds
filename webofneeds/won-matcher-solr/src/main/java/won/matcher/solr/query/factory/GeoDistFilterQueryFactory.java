package won.matcher.solr.query.factory;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import won.matcher.solr.index.NeedIndexer;
import won.protocol.util.WonRdfUtils;

import java.net.URI;

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

    Model needModel = WonRdfUtils.NeedUtils.getNeedModelFromNeedDataset(needDataset);
    URI needUri = WonRdfUtils.NeedUtils.getNeedURI(needModel);
    latitude = WonRdfUtils.NeedUtils.getLocationLatitude(needModel, needUri);
    longitude = WonRdfUtils.NeedUtils.getLocationLongitude(needModel, needUri);
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
