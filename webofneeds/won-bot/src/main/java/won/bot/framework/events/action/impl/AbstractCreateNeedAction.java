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

package won.bot.framework.events.action.impl;

import com.hp.hpl.jena.rdf.model.Model;
import won.bot.framework.events.EventListenerContext;
import won.bot.framework.events.action.BaseEventBotAction;
import won.protocol.exception.WonMessageBuilderException;
import won.protocol.message.WonMessage;
import won.protocol.message.WonMessageBuilder;
import won.protocol.model.FacetType;
import won.protocol.service.WonNodeInformationService;
import won.protocol.util.RdfUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Base class for actions that create needs.
 */
public abstract class AbstractCreateNeedAction extends BaseEventBotAction {

  protected List<URI> facets;
  protected String uriListName;

  /**
   * Creates a need with the specified facets.
   * If no facet is specified, the ownerFacet will be used.
   */
  public AbstractCreateNeedAction(EventListenerContext eventListenerContext, String uriListName, URI... facets) {
    super(eventListenerContext);
    if (facets == null || facets.length == 0) {
      //add the default facet if none is present.
      this.facets = new ArrayList<URI>(1);
      this.facets.add(FacetType.OwnerFacet.getURI());
    } else {
      this.facets = Arrays.asList(facets);
    }
    this.uriListName = uriListName;
  }

  /**
   * Creates a need with the specified facets.
   * If no facet is specified, the ownerFacet will be used.
   */
  public AbstractCreateNeedAction(final EventListenerContext eventListenerContext, URI... facets)
  {
    this(eventListenerContext, null, facets);
  }

  protected WonMessage createWonMessage(WonNodeInformationService wonNodeInformationService, URI needURI, URI wonNodeURI,
                                        Model needModel)
          throws WonMessageBuilderException {

    RdfUtils.replaceBaseURI(needModel, needURI.toString());

    WonMessageBuilder builder = new WonMessageBuilder();
    return builder
            .setMessagePropertiesForCreate(
                    wonNodeInformationService.generateEventURI(
                            wonNodeURI),
                    needURI,
                    wonNodeURI)
            .addContent(needModel, null)
            .build();
  }
}
