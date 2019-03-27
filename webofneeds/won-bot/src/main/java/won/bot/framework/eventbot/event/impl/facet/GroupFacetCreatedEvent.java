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
package won.bot.framework.eventbot.event.impl.facet;

import java.net.URI;

import org.apache.jena.rdf.model.Model;

import won.bot.framework.eventbot.event.BaseEvent;
import won.protocol.model.FacetType;

/**
 * User: LEIH-NB Date: 05.02.14
 */
public class GroupFacetCreatedEvent extends BaseEvent {
    private URI groupFacetURI;
    private URI wonNodeURI;
    private Model model;
    private final FacetType facetType = FacetType.GroupFacet;

    public GroupFacetCreatedEvent(URI groupFacetURI, URI wonNodeURI, Model model) {
        this.groupFacetURI = groupFacetURI;
        this.wonNodeURI = wonNodeURI;
        this.model = model;
    }

    public URI getGroupFacetURI() {
        return groupFacetURI;
    }

    public URI getWonNodeURI() {
        return wonNodeURI;
    }

    public Model getModel() {
        return model;
    }
}
