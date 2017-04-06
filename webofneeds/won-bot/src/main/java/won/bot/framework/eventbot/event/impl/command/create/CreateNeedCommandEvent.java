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

package won.bot.framework.eventbot.event.impl.command.create;

import org.apache.jena.rdf.model.Model;
import won.bot.framework.bot.context.BotContext;
import won.bot.framework.eventbot.event.impl.command.MessageCommandEvent;
import won.protocol.message.WonMessageType;
import won.protocol.model.FacetType;

import java.net.URI;
import java.util.Arrays;
import java.util.List;


/**
 * Instructs the bot to create a need.
 */
public class CreateNeedCommandEvent implements MessageCommandEvent {
    //the model of the need's content
    private Model needModel;
    //the facets the new need should have
    private List<URI> facets;
    //the name of the need uri list to save the need uri to
    private String uriListName = BotContext.DEFAULT_NEED_LIST_NAME;
    //sets the UsedForTesting flag
    private boolean usedForTesting = false;
    //sets the do not match flag
    private boolean doNotMatch = false;

    public CreateNeedCommandEvent(Model needModel, String uriListName, boolean usedForTesting, boolean doNotMatch, URI... facets) {
        this.needModel = needModel;
        if (this.uriListName != null) {
            this.uriListName = uriListName;
        }
        if (facets != null && facets.length > 0) {
            this.facets = Arrays.asList(facets);
        } else {
            this.facets = Arrays.asList(new URI[]{FacetType.OwnerFacet.getURI()});
        }
        this.usedForTesting = usedForTesting;
        this.doNotMatch = doNotMatch;
    }
    public CreateNeedCommandEvent(Model needModel, String uriListName, URI... facets) {
        this(needModel, uriListName, false, false, facets);
    }

    public CreateNeedCommandEvent(Model needModel) {
        this(needModel, null, null);
    }

    public CreateNeedCommandEvent(Model needModel, URI... facets) {
        this(needModel, null, facets);
    }

    public CreateNeedCommandEvent(Model needModel, String uriListName) {
        this(needModel, uriListName, null);
    }

    @Override
    public WonMessageType getWonMessageType() {
        return WonMessageType.CREATE_NEED;
    }



    public Model getNeedModel() {
        return needModel;
    }

    public String getUriListName() {
        return uriListName;
    }

    public List<URI> getFacets() {
        return facets;
    }

    public boolean isUsedForTesting() {
        return usedForTesting;
    }

    public boolean isDoNotMatch() {
        return doNotMatch;
    }
}
