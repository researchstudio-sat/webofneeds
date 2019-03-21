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

package won.owner.web.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import won.protocol.service.WonNodeInformationService;

import java.net.URI;

@Controller @RequestMapping("/appConfig") public class AppConfigController {
  @Autowired private WonNodeInformationService wonNodeInformationService;

  @RequestMapping(value = "/getDefaultWonNodeUri", method = RequestMethod.GET) public ResponseEntity<URI> getDefaultWonNodeUri() {
    return new ResponseEntity(this.wonNodeInformationService.getDefaultWonNodeURI().toString(), HttpStatus.OK);
  }

  public void setWonNodeInformationService(final WonNodeInformationService wonNodeInformationService) {
    this.wonNodeInformationService = wonNodeInformationService;
  }
}
