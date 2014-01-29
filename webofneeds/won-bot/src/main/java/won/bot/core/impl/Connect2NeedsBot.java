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

package won.bot.core.impl;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.model.FacetType;
import won.protocol.util.WonRdfUtils;

import java.net.URI;
import java.util.List;

/**
 */
public class Connect2NeedsBot extends CreateNNeedsAndReactBot
{
  private final Logger logger = LoggerFactory.getLogger(getClass());

  protected Connect2NeedsBot()
  {
    super(2);
  }

  @Override
  protected void onNNeedsCreated() throws Exception
  {
    List<URI> needUris = getBotContext ().listNeedUris();
    if (needUris.size() != 2) {
      logger.warn("Connect2NeedsBot expects to create 2 needs, but encountered {}", needUris.size());
      return;
    }
    getOwnerService().connect(needUris.get(0),needUris.get(1),createModelForConnect());
  }

  protected Model createModelForConnect()
  {
    Model model = ModelFactory.createDefaultModel();
    model.setNsPrefix("", "no:uri");
    WonRdfUtils.FacetUtils.addRemoteFacet(model, FacetType.OwnerFacet.getURI());
    return model;
  }
}
