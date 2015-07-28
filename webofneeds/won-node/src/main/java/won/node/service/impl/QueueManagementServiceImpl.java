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

package won.node.service.impl;

import org.apache.camel.Exchange;
import org.apache.camel.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import won.protocol.model.OwnerApplication;
import won.protocol.repository.OwnerApplicationRepository;
import won.protocol.service.QueueManagementService;

import java.util.ArrayList;
import java.util.List;

/**
 * User: LEIH-NB
 * Date: 13.11.13
 */
public class QueueManagementServiceImpl implements QueueManagementService {

    @Autowired
    private OwnerApplicationRepository ownerApplicationRepository;

    final Logger logger = LoggerFactory.getLogger(getClass());
    private String defaultUriScheme;

   private String ownerProtocolComponentName;
   private String matecherProtocolComponentName;
   private String ownerProtocolMessagingType;
   private String matcherProtocolMessagingType;
   private String ownerProtocolOutgoingMessageName;
   private String matcherProtocolOutgoingMessageName;

   private List<String> queueNames;


    @Override
    public List<String> generateQueueNamesForOwnerApplication(OwnerApplication ownerApplication) {

        logger.debug(ownerApplication.getOwnerApplicationId());
        List<String> queueNames = new ArrayList<>();
        queueNames.add("activemq"+ownerApplication.getOwnerApplicationId()+":queue:OwnerProtocol.Out."+ownerApplication.getOwnerApplicationId());

        /*queueNames.add("activemq"+ownerApplication.getOwnerApplicationId()+":queue:OwnerProtocol."+"connect"+".Out."+ownerApplication.getOwnerApplicationId());
        queueNames.add("activemq"+ownerApplication.getOwnerApplicationId()+":queue:OwnerProtocol."+"hint"+".Out."+ownerApplication.getOwnerApplicationId());
        queueNames.add("activemq"+ownerApplication.getOwnerApplicationId()+":queue:OwnerProtocol."+"textMessage"+".Out."+ownerApplication.getOwnerApplicationId());
        queueNames.add("activemq"+ownerApplication.getOwnerApplicationId()+":queue:OwnerProtocol."+"open"+".Out."+ownerApplication.getOwnerApplicationId());
        queueNames.add("activemq"+ownerApplication.getOwnerApplicationId()+":queue:OwnerProtocol."+"close"+".Out."+ownerApplication.getOwnerApplicationId());
        */
        ownerApplication.setQueueNames(queueNames);
        //logger.debug(ownerApplication.getQueueNames().get(0));
        //logger.debug(ownerApplication.getQueueNames().get(1));

        return ownerApplication.getQueueNames();
    }

    @Override
    public String getEndpointForMessage(String methodName, String ownerApplicationID) {
        OwnerApplication ownerApplication = ownerApplicationRepository.findByOwnerApplicationId(ownerApplicationID).get(0);
        List<String> queueNames = ownerApplication.getQueueNames();
        String endpoint="";
        for (int i = 0; i< queueNames.size();i++){
            if (queueNames.get(i).contains(methodName)){

                endpoint=queueNames.get(i);
                endpoint = endpoint.replaceFirst(ownerApplicationID,"");
                break;
            }
        }
        if (endpoint.equals("")&&queueNames.size()==1){
            endpoint=queueNames.get(0);
            endpoint = endpoint.replaceFirst(ownerApplicationID,"");
        }


        return  endpoint;
    }

    @Override
    public List<String> getEndpointsForOwnerApplication(@Header("ownerApplicationID") String ownerApplicationID, Exchange exchange) {
        //TODO: must implement special handling of the case where the id is unknown
        List<OwnerApplication> ownerApplications = ownerApplicationRepository.findByOwnerApplicationId(ownerApplicationID);
        List<String> endpoints = new ArrayList<>();
        endpoints.addAll(ownerApplications.get(0).getQueueNames());
        return endpoints;
    }


    public void setOwnerApplicationRepository(OwnerApplicationRepository ownerApplicationRepository) {
        this.ownerApplicationRepository = ownerApplicationRepository;
    }


}
