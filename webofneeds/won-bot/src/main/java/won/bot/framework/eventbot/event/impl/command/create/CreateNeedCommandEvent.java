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

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.apache.jena.query.Dataset;

import won.bot.framework.bot.context.BotContext;
import won.bot.framework.eventbot.event.impl.command.MessageCommandEvent;
import won.protocol.message.WonMessageType;
import won.protocol.model.FacetType;


/**
 * Instructs the bot to create a need.
 */
public class CreateNeedCommandEvent implements MessageCommandEvent {
    //the model of the need's content
    private Dataset needDataset;
    //the facets the new need should have
    private List<URI> facets;
    //the name of the need uri list to save the need uri to
    private String uriListName = BotContext.DEFAULT_NEED_LIST_NAME;
    //sets the UsedForTesting flag
    private boolean usedForTesting = false;
    //sets the do not match flag
    private boolean doNotMatch = false;

    public CreateNeedCommandEvent(Dataset needDataset, String uriListName, boolean usedForTesting, boolean doNotMatch, URI... facets) {
        this.needDataset = needDataset;
        if (this.uriListName != null) {
            this.uriListName = uriListName;
        }
        if (facets != null && facets.length > 0) {
            this.facets = Arrays.asList(facets);
        } else {
            this.facets = Arrays.asList(new URI[]{FacetType.ChatFacet.getURI()});
        }
        this.usedForTesting = usedForTesting;
        this.doNotMatch = doNotMatch;
    }
    public CreateNeedCommandEvent(Dataset needDataset, String uriListName, URI... facets) {
        this(needDataset, uriListName, false, false, facets);
    }

    public CreateNeedCommandEvent(Dataset needDataset) {
        this(needDataset, null, null);
    }

    public CreateNeedCommandEvent(Dataset needDataset, URI... facets) {
        this(needDataset, null, facets);
    }

    public CreateNeedCommandEvent(Dataset needDataset, String uriListName) {
        this(needDataset, uriListName, null);
    }

    @Override
    public WonMessageType getWonMessageType() {
        return WonMessageType.CREATE_NEED;
    }



    public Dataset getNeedDataset() {
        return needDataset;
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
