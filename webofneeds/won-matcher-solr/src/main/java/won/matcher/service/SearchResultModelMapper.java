/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package won.matcher.service;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;
import won.protocol.util.ModelMapper;
import won.protocol.util.RdfUtils;
import won.protocol.vocabulary.WON;

/**
 * User: fkleedorfer
 * Date: 23.09.13
 */
public class SearchResultModelMapper implements ModelMapper<SearchResult>
{


  @Override
  public Model toModel(final SearchResult searchResult)
  {
    Model ret = ModelFactory.createDefaultModel();
    if (searchResult == null || searchResult.getItems() == null || searchResult.getItems().size() == 0) return ret;
    Resource mainResource = ret.createResource();
    mainResource.addProperty(WON.HAS_ORIGINATOR, ret.createResource(searchResult.getOriginator().toString()));
    for(SearchResultItem item: searchResult.getItems()){
      Resource itemResource = ret.createResource(WON.Match);
      itemResource.addProperty(WON.SEARCH_RESULT_URI, ret.createResource(item.getUri().toString()));
      itemResource.addProperty(WON.HAS_MATCH_SCORE, ret.createTypedLiteral(item.getScore(), XSDDatatype.XSDfloat));
      if (item.getExplanation() != null) {
        RdfUtils.attachModelByBaseResource(itemResource, WON.HAS_ADDITIONAL_DATA, item.getExplanation());
      }
      if (item.getModel() != null) {
        RdfUtils.attachModelByBaseResource(itemResource, WON.SEARCH_RESULT_PREVIEW, item.getModel());
      }
      mainResource.addProperty(RDFS.member, itemResource);
    }
    return ret;
  }



  @Override
  public SearchResult fromModel(final Model model)
  {
    throw new UnsupportedOperationException("not implemented");
  }
}
