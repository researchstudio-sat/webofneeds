package won.protocol.jms;

/**
 * User: LEIH-NB Date: 18.02.14
 */
public class CamelConfiguration {

  private String endpoint;

  private String brokerComponentName;

  public String getBrokerComponentName() {
    return brokerComponentName;
  }

  public void setBrokerComponentName(String brokerComponentName) {
    this.brokerComponentName = brokerComponentName;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

}
