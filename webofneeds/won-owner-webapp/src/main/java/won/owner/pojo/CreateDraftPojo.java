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

package won.owner.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * User: LEIH-NB
 * Date: 09.10.2014
 */
@JsonIgnoreProperties(ignoreUnknown = true) public class CreateDraftPojo {
  private String draftURI;
  private String draft;

  public CreateDraftPojo() {

  }

  public CreateDraftPojo(String draftURI, String draft) {
    this.draftURI = draftURI;
    this.draft = draft;
  }

  /*public DraftPojo(URI draftURI, Model content, Draft draftState ){
    super(draftURI, content);
    this.setCurrentStep(draftState.getCurrentStep());
    this.setUserName(draftState.getUserName());
  }      */
  public String getDraftURI() {
    return draftURI;
  }

  public void setDraftURI(final String draftURI) {
    this.draftURI = draftURI;
  }

  public String getDraft() {
    return draft;
  }

  public void setDraft(final String draft) {
    this.draft = draft;
  }
}
