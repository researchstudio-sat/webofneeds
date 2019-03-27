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
package won.bot.framework.eventbot.action.impl.needlifecycle;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.jena.query.Dataset;

import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.model.FacetType;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.NeedModelWrapper;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.WON;

/**
 * Base class for actions that create needs.
 */
public abstract class AbstractCreateNeedAction extends BaseEventBotAction {
    protected List<URI> facets;
    protected String uriListName;
    // indicates if the won:DoNotMatch flag is to be set
    protected boolean usedForTesting;
    protected boolean doNotMatch;

    /**
     * Creates a need with the specified facets. If no facet is specified, the
     * chatFacet will be used, Flag 'UsedForTesting' will be set. uriListName is
     * used from the set botcontextwrapper getNeedCreateListName
     */
    public AbstractCreateNeedAction(EventListenerContext eventListenerContext, URI... facets) {
        this(eventListenerContext, eventListenerContext.getBotContextWrapper().getNeedCreateListName(), facets);
    }

    /**
     * Creates a need with the specified facets. If no facet is specified, the
     * chatFacet will be used, Flag 'UsedForTesting' will be set.
     */
    public AbstractCreateNeedAction(EventListenerContext eventListenerContext, String uriListName, URI... facets) {
        this(eventListenerContext, uriListName, true, false, facets);
    }

    /**
     * Creates a need with the specified facets. If no facet is specified, the
     * chatFacet will be used.
     */
    public AbstractCreateNeedAction(EventListenerContext eventListenerContext, String uriListName,
                    final boolean usedForTesting, final boolean doNotMatch, URI... facets) {
        super(eventListenerContext);
        if (facets == null || facets.length == 0) {
            // add the default facet if none is present.
            this.facets = new ArrayList<URI>(1);
            this.facets.add(FacetType.ChatFacet.getURI());
        } else {
            this.facets = Arrays.asList(facets);
        }
        this.doNotMatch = doNotMatch;
        this.usedForTesting = usedForTesting;
        this.uriListName = uriListName;
    }

    protected WonMessage createWonMessage(WonNodeInformationService wonNodeInformationService, URI needURI,
                    URI wonNodeURI, Dataset needDataset) throws WonMessageBuilderException {
        return createWonMessage(wonNodeInformationService, needURI, wonNodeURI, needDataset, usedForTesting,
                        doNotMatch);
    }

    protected WonMessage createWonMessage(WonNodeInformationService wonNodeInformationService, URI needURI,
                    URI wonNodeURI, Dataset needDataset, final boolean usedForTesting, final boolean doNotMatch)
                    throws WonMessageBuilderException {
        NeedModelWrapper needModelWrapper = new NeedModelWrapper(needDataset);
        if (doNotMatch) {
            needModelWrapper.addFlag(WON.NO_HINT_FOR_ME);
            needModelWrapper.addFlag(WON.NO_HINT_FOR_COUNTERPART);
        }
        if (usedForTesting) {
            needModelWrapper.addFlag(WON.USED_FOR_TESTING);
        }
        RdfUtils.replaceBaseURI(needDataset, needURI.toString(), true);
        return WonMessageBuilder.setMessagePropertiesForCreate(wonNodeInformationService.generateEventURI(wonNodeURI),
                        needURI, wonNodeURI).addContent(needModelWrapper.copyDataset()).build();
    }

    public void setUsedForTesting(final boolean usedForTesting) {
        this.usedForTesting = usedForTesting;
    }

    public void setDoNotMatch(final boolean doNotMatch) {
        this.doNotMatch = doNotMatch;
    }

    private boolean hasFacet(FacetType facetToCheck) {
        for (URI facet : facets) {
            if (facet.equals(facetToCheck.getURI())) {
                return true;
            }
        }
        return false;
    }
}
