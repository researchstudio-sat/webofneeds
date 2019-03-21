package won.matcher.solr.query.factory;

/**
 * Created by hfriedrich on 22.08.2016.
 */
public class GeoDistBoostQueryFactory extends SolrQueryFactory {
  private float latitude;
  private float longitude;
  private String solrLocationField;

  public GeoDistBoostQueryFactory(String solrLocationField, float latitude, float longitude) {

    this.latitude = latitude;
    this.longitude = longitude;
    this.solrLocationField = solrLocationField;
  }

  @Override protected String makeQueryString() {

    // calculate the inverse of the distance as a distance measure
    StringBuilder sb = new StringBuilder();
    sb.append("recip(geodist(").append(solrLocationField).append(",").append(latitude).append(",").append(longitude)
        .append("),5,100,100)");

    return sb.toString();
  }
}
