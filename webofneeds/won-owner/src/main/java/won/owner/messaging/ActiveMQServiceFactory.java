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

package won.owner.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.exception.NoSuchConnectionException;
import won.protocol.jms.MessageBrokerService;
import won.protocol.model.Connection;
import won.protocol.model.Need;
import won.protocol.repository.ConnectionRepository;
import won.protocol.repository.NeedRepository;
import won.protocol.util.DataAccessUtils;

import java.net.URI;

/**
 * User: LEIH-NB
 * Date: 28.01.14
 */
public class ActiveMQServiceFactory {
    @Autowired
    ConnectionRepository connectionRepository;
    @Autowired
    NeedRepository needRepository;

    public OwnerProtocolActiveMQServiceImplRefactoring createActiveMQService(String methodName,URI uri) throws NoSuchConnectionException {
        OwnerProtocolActiveMQServiceImplRefactoring messageBrokerService = null;
        URI wonNodeURI=null;

        if (methodName.equals("connect")||methodName.equals("deactivate")||methodName.equals("activate")){
            Need need = needRepository.findByNeedURI(uri).get(0);
            wonNodeURI = need.getWonNodeURI();
        } else if (methodName.equals("register")||methodName.equals("createNeed")){
            wonNodeURI = uri;
        } else if (methodName.equals("textMessage")||methodName.equals("close")||methodName.equals("open")){
            Connection con = DataAccessUtils.loadConnection(connectionRepository, uri);
            URI needURI = con.getNeedURI();
            Need need = needRepository.findByNeedURI(needURI).get(0);
            wonNodeURI = need.getWonNodeURI();

        }
        return new OwnerProtocolActiveMQServiceImplRefactoring(wonNodeURI);
    }



}
