package won.matcher.solr.query.factory;

/**
 * Created by hfriedrich on 22.08.2016.
 */
public class GeoDistFilterQueryFactory extends SolrQueryFactory {

    private float latitude;
    private float longitude;
    private String solrLocationField;
    private double distance;

    public GeoDistFilterQueryFactory(String solrLocationField, float latitude, float longitude, double distanceInKilometers) {

        this.solrLocationField = solrLocationField;
        this.latitude = latitude;
        this.longitude = longitude;
        distance = distanceInKilometers;
    }

    @Override
    protected String makeQueryString() {

        // create a geographical distance filter with radius "distance" (in kilometers)
        StringBuilder sb = new StringBuilder();
        sb.append("{!geofilt sfield=").append(solrLocationField).append(" pt=")
                .append(latitude).append(",").append(longitude).append(" d=").append(distance).append("}");
        return sb.toString();
    }

}
