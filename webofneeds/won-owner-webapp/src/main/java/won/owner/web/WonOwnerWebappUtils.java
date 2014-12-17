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

import com.hp.hpl.jena.query.Dataset;
import won.owner.pojo.MatchPojo;
import won.owner.pojo.NeedPojo;
import won.protocol.model.Match;
import won.protocol.util.NeedModelBuilder;
import won.protocol.util.ProjectingIterator;
import won.protocol.util.RdfUtils;

import java.net.URI;
import java.util.Iterator;

/**
 * User: fkleedorfer
 * Date: 15.04.14
 */
public class WonOwnerWebappUtils
{
  public static Iterator<NeedPojo> toNeedPojos(Iterator<Dataset> modelIterator){
    return new ProjectingIterator<Dataset, NeedPojo>(modelIterator) {
      @Override
      public NeedPojo next() {
        Dataset dataset = baseIterator.next();
        URI baseURI = URI.create(RdfUtils.getBaseResource(dataset.getDefaultModel()).toString());
        return new NeedPojo(baseURI, dataset.getDefaultModel());
      }
    };
  }

  public static Iterator<MatchPojo> toMatchPojos(final Iterator<Dataset> datasetIterator,
    final Iterator<Match> matchIterator){
    return new Iterator<MatchPojo>()
    {
      @Override
      public boolean hasNext() {
        return datasetIterator. hasNext() && matchIterator.hasNext();
      }

      @Override
      public MatchPojo next() {
        MatchPojoNeedBuilder matchPojoNeedBuilder = new MatchPojoNeedBuilder();
        NeedModelBuilder needModelBuilder = new NeedModelBuilder();
        needModelBuilder.copyValuesFromProduct(datasetIterator.next().getDefaultModel());
        needModelBuilder.copyValuesToBuilder(matchPojoNeedBuilder);
        Match match = matchIterator.next();
        MatchPojo matchPojo = matchPojoNeedBuilder.build();
        matchPojo.setNeedURI(match.getFromNeed().toString());
        matchPojo.setScore(match.getScore());
        matchPojo.setOriginator(match.getOriginator().toString());
        return matchPojo;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException("this iterator does not support remove()");
      }
    };
  }
}
