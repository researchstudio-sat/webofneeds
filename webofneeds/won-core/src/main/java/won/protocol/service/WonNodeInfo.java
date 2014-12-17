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

/**
* User: fkleedorfer
* Date: 27.10.2014
*/
public class WonNodeInfo
{

  public WonNodeInfo() {
  }

  public WonNodeInfo(
    String eventURIPrefix,
    String connectionURIPrefix,
    String needURIPrefix) {

    this.eventURIPrefix = eventURIPrefix;
    this.connectionURIPattern = connectionURIPrefix;
    this.needURIPattern = needURIPrefix;
  }

  private String eventURIPrefix;
  private String connectionURIPattern;
  private String needURIPattern;

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
