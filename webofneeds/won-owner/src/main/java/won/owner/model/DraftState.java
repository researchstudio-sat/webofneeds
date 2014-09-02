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

/*
 * This file is subject to the terms and conditions defined in file 'LICENSE.txt', which is part of this source code package.
 */

package won.owner.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.net.URI;

/**
 * I used wonuser as table name because user is Postgres keyword - http://www.postgresql.org/message-id/Pine.NEB.4.10.10008291649550.4357-100000@scimitar.caravan.com
 *
 */
@Entity
@Table(
		name = "needDraftState",
		uniqueConstraints = @UniqueConstraint(columnNames = {"id"})
)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DraftState{

	@Id
	@GeneratedValue
	@Column(name = "id")
	private Long id;


  @Column(name ="userName")
  private String userName;

  @Column( name = "draftURI", unique = true)
  private URI draftURI;

  private int currentStep;

  public DraftState(){
  }

  public DraftState(URI draftURI, int currentStep, String userName){
    this.draftURI = draftURI;
    this.currentStep = currentStep;
    this.userName = userName;
  }

  public Long getId() {
    return id;
  }

  public int getCurrentStep() {
    return currentStep;
  }

  public void setCurrentStep(final int currentStep) {
    this.currentStep = currentStep;
  }

  public URI getDraftURI() {
    return draftURI;
  }

  public void setDraftURI(final URI draftURI) {
    this.draftURI = draftURI;
  }

  public String getUserName() {
    return userName;
  }
  public void setUserName(final String userName) {
    this.userName = userName;
  }



}
