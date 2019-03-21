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

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    Coordinate that = (Coordinate) o;

    if (Float.compare(that.latitude, latitude) != 0)
      return false;
    if (Float.compare(that.longitude, longitude) != 0)
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = (latitude != +0.0f ? Float.floatToIntBits(latitude) : 0);
    result = 31 * result + (longitude != +0.0f ? Float.floatToIntBits(longitude) : 0);
    return result;
  }
}
