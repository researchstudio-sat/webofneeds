package won.protocol.model;

/**
 * Created by hfriedrich on 12.04.2017.
 */
public class Coordinate {

    private float latitude;
    private float longitude;

    public Coordinate(float latitude, float longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public float getLatitude() {
        return latitude;
    }
}
