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
package won.bot.framework.eventbot.event.impl.socket;

import java.net.URI;

import org.apache.jena.rdf.model.Model;

import won.bot.framework.eventbot.event.BaseEvent;
import won.protocol.model.SocketType;

/**
 * User: LEIH-NB Date: 05.02.14
 */
public class GroupSocketCreatedEvent extends BaseEvent {
    private URI groupSocketURI;
    private URI wonNodeURI;
    private Model model;
    private final SocketType socketType = SocketType.GroupSocket;

    public GroupSocketCreatedEvent(URI groupSocketURI, URI wonNodeURI, Model model) {
        this.groupSocketURI = groupSocketURI;
        this.wonNodeURI = wonNodeURI;
        this.model = model;
    }

    public URI getGroupSocketURI() {
        return groupSocketURI;
    }

    public URI getWonNodeURI() {
        return wonNodeURI;
    }

    public Model getModel() {
        return model;
    }
}
