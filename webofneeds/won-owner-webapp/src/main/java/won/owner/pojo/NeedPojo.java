package won.owner.pojo;

import won.protocol.model.BasicNeedType;

/**
 * User: Gabriel
 * Date: 19.12.12
 * Time: 11:44
 */
public class NeedPojo
{
  private String needURI;

  private String title;
  private BasicNeedType basicNeedType;
  private boolean active;
  private boolean anonymize;
  private String wonNode;

  private String textDescription;

  // using objects to be able to check for null
  private Double upperPriceLimit;
  private Double lowerPriceLimit;
  private String currency;

  private Double latitude;
  private Double longitude;

  private String startTime;
  private String endTime;
  private Long recurIn;
  private Integer recurTimes;
  private boolean recurInfiniteTimes;

  public String getTitle()
  {
    return title;
  }

  public void setTitle(final String title)
  {
    this.title = title;
  }

  public BasicNeedType getBasicNeedType()
  {
    return basicNeedType;
  }

  public void setBasicNeedType(final BasicNeedType basicNeedType)
  {
    this.basicNeedType = basicNeedType;
  }

  public boolean isActive()
  {
    return active;
  }

  public void setActive(final boolean active)
  {
    this.active = active;
  }

  public boolean isAnonymize()
  {
    return anonymize;
  }

  public void setAnonymize(final boolean anonymize)
  {
    this.anonymize = anonymize;
  }

  public String getWonNode()
  {
    return wonNode;
  }

  public void setWonNode(final String wonNode)
  {
    this.wonNode = wonNode;
  }

  public String getTextDescription()
  {
    return textDescription;
  }

  public void setTextDescription(final String textDescription)
  {
    this.textDescription = textDescription;
  }

  public String getNeedURI()
  {
    return needURI;
  }

  public void setNeedURI(final String needURI)
  {
    this.needURI = needURI;
  }

    public Double getUpperPriceLimit() {
        return upperPriceLimit;
    }

    public void setUpperPriceLimit(Double upperPriceLimit) {
        this.upperPriceLimit = upperPriceLimit;
    }

    public Double getLowerPriceLimit() {
        return lowerPriceLimit;
    }

    public void setLowerPriceLimit(Double lowerPriceLimit) {
        this.lowerPriceLimit = lowerPriceLimit;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Long getRecurIn() {
        return recurIn;
    }

    public void setRecurIn(Long recurIn) {
        this.recurIn = recurIn;
    }

    public Integer getRecurTimes() {
        return recurTimes;
    }

    public void setRecurTimes(Integer recurTimes) {
        this.recurTimes = recurTimes;
    }

    public boolean getRecurInfiniteTimes() {
        return recurInfiniteTimes;
    }

    public void setRecurInfiniteTimes(boolean recurInfiniteTimes) {
        this.recurInfiniteTimes = recurInfiniteTimes;
    }
}