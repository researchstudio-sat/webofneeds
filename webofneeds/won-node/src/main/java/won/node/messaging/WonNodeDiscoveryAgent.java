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

package won.node.messaging;

import org.apache.activemq.command.DiscoveryEvent;
import org.apache.activemq.transport.discovery.DiscoveryAgent;
import org.apache.activemq.transport.discovery.DiscoveryListener;

import java.io.IOException;

/**
 * User: LEIH-NB
 * Date: 11.02.14
 */
public class WonNodeDiscoveryAgent implements DiscoveryAgent {
    @Override
    public void setDiscoveryListener(DiscoveryListener listener) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void registerService(String name) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void serviceFailed(DiscoveryEvent event) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void start() throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void stop() throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
