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

import won.protocol.jms.OwnerProtocolCamelConfigurator;

/**
 * User: LEIH-NB
 * Date: 28.01.14
 */
public class CamelConfiguratorFactory {
    /*public OwnerProtocolCamelConfigurator createCamelConfigurator(String methodName){
        OwnerProtocolCamelConfigurator ownerProtocolCamelConfigurator = null;

        if (methodName.equals("connect")||methodName.equals("deactivate")||methodName.equals("activate")){
            ownerProtocolCamelConfigurator = new OwnerProtocolCamelConfigurator();
        } else if (methodName.equals("register")||methodName.equals("createNeed")){
            ownerProtocolCamelConfigurator = new OwnerProtocolCamelConfigurator();
        } else if (methodName.equals("textMessage")||methodName.equals("close")||methodName.equals("open")){
            ownerProtocolCamelConfigurator = new OwnerProtocolCamelConfigurator();
        }
        return ownerProtocolCamelConfigurator;
    }    */

}
