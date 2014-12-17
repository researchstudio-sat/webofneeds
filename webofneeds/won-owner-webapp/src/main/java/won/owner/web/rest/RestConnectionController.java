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
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import won.owner.pojo.ConnectionPojo;
import won.owner.pojo.NeedPojo;
import won.owner.pojo.TextMessagePojo;
import won.protocol.model.ChatMessage;
import won.protocol.model.Connection;
import won.protocol.repository.ChatMessageRepository;
import won.protocol.repository.ConnectionRepository;
import won.protocol.rest.LinkedDataRestClient;

import javax.ws.rs.core.MediaType;
import java.util.LinkedList;
import java.util.List;

/**
 * User: LEIH-NB
 * Date: 23.07.14
 */
@Controller
@RequestMapping("/rest/connections")
@Deprecated
public class RestConnectionController
{
  @Autowired
  private ConnectionRepository connectionRepository;

  @Autowired
  private ChatMessageRepository chatMessageRepository;



  @ResponseBody
  @RequestMapping(
    value = "/{connectionId}",
    method = RequestMethod.GET,
    produces = MediaType.APPLICATION_JSON
  )
  public ConnectionPojo getConnection(@PathVariable("connectionId") long connectionId){
    LinkedDataRestClient linkedDataRestClient = new LinkedDataRestClient();

    Connection connection = connectionRepository.findOne(connectionId);
    ConnectionPojo connectionPojo = new ConnectionPojo(connection.getConnectionURI(),
                                                       linkedDataRestClient.readResourceData(connection
                                                                                               .getConnectionURI()).getDefaultModel());
    return connectionPojo;

  }

  @ResponseBody
  @RequestMapping(
    value = "/{conId}/messages",
    method = RequestMethod.GET,
    produces = MediaType.APPLICATION_JSON
  )
  public List<ChatMessage> listMessages(@PathVariable String conId) {
    Connection con = connectionRepository.findOne(Long.valueOf(conId));
    List<TextMessagePojo> textMessages = new LinkedList<TextMessagePojo>();
    return chatMessageRepository.findByLocalConnectionURI(con.getConnectionURI());
  }


  @ResponseBody
  @RequestMapping(
    value = "/{connectionId}",
    consumes = MediaType.APPLICATION_JSON,
    produces = MediaType.APPLICATION_JSON,
    method = RequestMethod.POST
  )
  //TODO: move transactionality annotation into the service layer
  @Transactional(propagation = Propagation.SUPPORTS)
  public NeedPojo connect(@RequestBody ConnectionPojo connectionPojo) {
    return null;
  }



}
