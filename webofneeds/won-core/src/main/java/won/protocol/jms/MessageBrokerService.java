/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.protocol.jms;

import java.net.URI;

/**
 * Interface responsible for extracting the ActiveMQ broker endpoint for
 * connecting to an atom via ActiveMQ from the linked data description of an
 * atom or WON node.
 */
public interface MessageBrokerService {
    /**
     * Extracts the ActiveMQ broker endpoint for connecting via ActiveMQ to the atom
     * or WON node identified by the specified resourceUri.
     * 
     * @param resourceUri
     * @return
     */
    public URI getBrokerEndpoint(URI resourceUri);
}
