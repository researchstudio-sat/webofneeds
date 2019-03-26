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

package won.protocol.model;

import java.net.URI;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * User: sbyim Date: 12.11.13
 */
@Entity
@Table(name = "wonNode")
public class WonNode {

  @Id
  @GeneratedValue
  @Column(name = "id")
  private Long id;

  @Column(name = "wonNodeURI", unique = true)
  @Convert(converter = URIConverter.class)
  private URI wonNodeURI;

  @Column(name = "brokerURI")
  @Convert(converter = URIConverter.class)
  private URI brokerURI;

  @Column(name = "ownerProtocolEndpoint")
  private String ownerProtocolEndpoint;

  @Column(name = "startingComponent")
  private String startingComponent;

  @Column(name = "brokerComponent")
  private String brokerComponent;

  private String ownerApplicationID;

  public String getOwnerProtocolEndpoint() {
    return ownerProtocolEndpoint;
  }

  public void setOwnerProtocolEndpoint(String ownerProtocolEndpoint) {
    this.ownerProtocolEndpoint = ownerProtocolEndpoint;
  }

  public URI getWonNodeURI() {
    return wonNodeURI;
  }

  public void setWonNodeURI(URI wonNodeURI) {
    this.wonNodeURI = wonNodeURI;
  }

  public String getOwnerApplicationID() {
    return ownerApplicationID;
  }

  public void setOwnerApplicationID(String ownerApplicationID) {
    this.ownerApplicationID = ownerApplicationID;
  }

  public URI getBrokerURI() {
    return brokerURI;
  }

  public void setBrokerURI(URI brokerURI) {
    this.brokerURI = brokerURI;
  }

  public String getStartingComponent() {
    return startingComponent;
  }

  public void setStartingComponent(String startingComponent) {
    this.startingComponent = startingComponent;
  }

  public String getBrokerComponent() {
    return brokerComponent;
  }

  public void setBrokerComponent(String brokerComponent) {
    this.brokerComponent = brokerComponent;
  }
}
