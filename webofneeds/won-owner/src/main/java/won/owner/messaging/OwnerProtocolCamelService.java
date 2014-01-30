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
import won.protocol.exception.CamelConfigurationFailedException;
import won.protocol.model.WonNode;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * User: syim
 * Date: 27.01.14
 */
public abstract class OwnerProtocolCamelService {
    @Autowired
    CamelConfiguratorFactory camelConfiguratorFactory;

    public OwnerProtocolCamelService(CamelConfiguratorFactory camelConfiguratorFactory){
        this.camelConfiguratorFactory = camelConfiguratorFactory;
    }

    final String configureCamelEndpoint(String methodName, URI uri,ArrayList<WonNode> wonNodeList){
         CamelConfigurator camelConfigurator = camelConfiguratorFactory.createCamelCongurator(methodName);

         return camelConfigurator.configureCamelEndpoint(wonNodeList,uri);
    }


}
