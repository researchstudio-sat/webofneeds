package won.protocol.service;

import java.util.HashMap;
import java.util.Map;

/**
 * Builder so that {@link WonNodeInfo} can be kept immutable
 *
 * Created by hfriedrich on 21.02.2017.
 */
public class WonNodeInfoBuilder {
  private String wonNodeURI;
  private String eventURIPrefix;
  private String connectionURIPrefix;
  private String needURIPrefix;
  private String needListURI;
  private Map<String, Map<String, String>> supportedProtocolImpl;

  public WonNodeInfoBuilder() {
    supportedProtocolImpl = new HashMap<>();
  }

  public WonNodeInfoBuilder setWonNodeURI(final String wonNodeURI) {
    this.wonNodeURI = wonNodeURI;
    return this;
  }

  public WonNodeInfoBuilder setEventURIPrefix(final String eventURIPrefix) {
    this.eventURIPrefix = eventURIPrefix;
    return this;
  }

  public WonNodeInfoBuilder setConnectionURIPrefix(final String connectionURIPrefix) {
    this.connectionURIPrefix = connectionURIPrefix;
    return this;
  }

  public WonNodeInfoBuilder setNeedURIPrefix(final String needURIPrefix) {
    this.needURIPrefix = needURIPrefix;
    return this;
  }

  public WonNodeInfoBuilder setNeedListURI(final String needListURI) {
    this.needListURI = needListURI;
    return this;
  }

  public WonNodeInfoBuilder addSupportedProtocolImplParamValue(String protocol, String paramName, String paramValue) {
    Map<String, String> protocolMap = supportedProtocolImpl.get(protocol);
    if (protocolMap == null) {
      protocolMap = new HashMap<>();
      supportedProtocolImpl.put(protocol, protocolMap);
    }
    protocolMap.put(paramName, paramValue);
    return this;
  }

  public WonNodeInfo build() {
    return new WonNodeInfo(wonNodeURI, eventURIPrefix, connectionURIPrefix, needURIPrefix, needListURI,
        supportedProtocolImpl);
  }
}
