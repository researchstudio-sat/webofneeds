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

package won.owner.web;

import won.owner.pojo.MatchPojo;
import won.protocol.util.NeedBuilderBase;

/**
 * Bilder for MatchPojo. Interprets the values set on the builder as the remote need's values.
 */
public class MatchPojoNeedBuilder extends NeedBuilderBase<MatchPojo>
{
  @Override
  public MatchPojo build() {
    MatchPojo matchPojo = new MatchPojo();
    matchPojo.setRemoteNeedURI(getNeedURIString());
    matchPojo.setDescription(getDescription());
    //matchPojo.setImageURI(); //TODO: fetch an image uri from the need!
    matchPojo.setTags(getTagsArray());
    matchPojo.setTitle(getTitle());
    return matchPojo;
  }

  @Override
  public void copyValuesFromProduct(final MatchPojo product) {
    setTitle(product.getTitle());
    setTags(product.getTags());
    setDescription(product.getDescription());
    setUri(product.getRemoteNeedURI());
    //TODO: handle image uri
  }
}
