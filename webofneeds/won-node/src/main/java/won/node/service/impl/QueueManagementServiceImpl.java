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

    @Override
    public List<String> generateQueueNamesForOwnerApplication(OwnerApplication ownerApplication) {

        logger.info(ownerApplication.getOwnerApplicationId());
        List<String> queueNames = new ArrayList<>();
        queueNames.add("activemq:queue:OwnerProtocol." + "connect" + ".Out." + ownerApplication);
        queueNames.add("activemq:queue:OwnerProtocol."+"hint"+".Out."+ownerApplication);
        ownerApplication.setQueueNames(queueNames);
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
                break;
            }


        }

        return  endpoint;
    }

    @Override
    public List<String> getEndpointsForOwnerApplication(String ownerApplicationID) {
        List<OwnerApplication> ownerApplications = ownerApplicationRepository.findByOwnerApplicationId(ownerApplicationID);
        List<String> endpoints = ownerApplications.get(0).getQueueNames();
        return endpoints;
    }


    public void setOwnerApplicationRepository(OwnerApplicationRepository ownerApplicationRepository) {
        this.ownerApplicationRepository = ownerApplicationRepository;
    }


}
