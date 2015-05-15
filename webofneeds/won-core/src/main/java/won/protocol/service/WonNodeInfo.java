/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.protocol.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
* User: fkleedorfer
* Date: 27.10.2014
*/
public class WonNodeInfo
{

  public WonNodeInfo() {
    supportedProtocolImpl = new HashMap<>();
  }

  public WonNodeInfo(String eventURIPrefix, String connectionURIPrefix, String needURIPrefix) {

    this.eventURIPrefix = eventURIPrefix;
    this.connectionURIPattern = connectionURIPrefix;
    this.needURIPattern = needURIPrefix;
    supportedProtocolImpl = new HashMap<>();
  }

  private String eventURIPrefix;
  private String connectionURIPattern;
  private String needURIPattern;
  private Map<String, Map <String, String>> supportedProtocolImpl;

  public void setSupportedProtocolImplParamValue(String protocol, String paramName, String paramValue) {
    Map<String,String> protocolMap = supportedProtocolImpl.get(protocol);
    if (protocolMap == null) {
      protocolMap = new HashMap<>();
      supportedProtocolImpl.put(protocol, protocolMap);
    }
    protocolMap.put(paramName, paramValue);
  }

  public String getSupportedProtocolImplParamValue(String protocol, String paramName) {
    Map<String,String> protocolMap = supportedProtocolImpl.get(protocol);
    if (protocolMap != null) {
      return protocolMap.get(paramName);
    }
    return null;
  }

  public Set<String> getSupportedProtocolImpls() {
    Set<String> protocols = new HashSet<>();
    protocols.addAll(supportedProtocolImpl.keySet());
    return protocols;
  }

  public Set<String> getSupportedProtocolImplParams(String protocol) {

    Set<String> protocols = new HashSet<>();
    Map<String,String> protocolMap = supportedProtocolImpl.get(protocol);
    if (protocolMap != null) {
      protocols.addAll(protocolMap.keySet());
    }
    return protocolMap.keySet();
  }

  public String getEventURIPrefix() {
    return eventURIPrefix;
  }

  public String getConnectionURIPrefix() {
    return connectionURIPattern;
  }

  public String getNeedURIPrefix() {
    return needURIPattern;
  }

  public void setEventURIPrefix(final String needMessageEventURIPattern) {
    this.eventURIPrefix = needMessageEventURIPattern;
  }

  public void setConnectionURIPrefix(final String connectionURIPattern) {
    this.connectionURIPattern = connectionURIPattern;
  }

  public void setNeedURIPrefix(final String needURIPattern) {
    this.needURIPattern = needURIPattern;
  }

}
